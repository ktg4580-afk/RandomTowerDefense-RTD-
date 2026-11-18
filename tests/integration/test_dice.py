import socket
import subprocess
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PORT = 39872
server = subprocess.Popen(['java', '-cp', 'out', 'server.GameServer', str(PORT)],
                          cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

class Client:
    def __init__(self, name):
        self.socket = socket.create_connection(('127.0.0.1', PORT), timeout=3)
        self.socket.settimeout(3)
        self.stream = self.socket.makefile('r', encoding='utf-8', newline='\n')
        self.send('/JOIN ' + name)
        self.expect('/JOIN_OK ')

    def send(self, msg):
        self.socket.sendall((msg + '\n').encode('utf-8'))

    def read(self):
        msg = self.stream.readline().strip()
        if not msg: raise AssertionError('Socket closed')
        return msg

    def expect(self, prefix):
        msg = self.read()
        assert msg.startswith(prefix), f'{prefix!r} != {msg!r}'
        return msg

    def close(self):
        self.stream.close()
        self.socket.close()


def next_state(c, player_id):
    fields = c.expect('/DICE_STATE ').split()
    assert int(fields[1]) == player_id, fields
    assert len(fields) == 22, fields
    return fields

clients = []
try:
    time.sleep(.4)
    a, b, outsider = [Client(name) for name in ['alpha', 'beta', 'gamma']]
    clients += [a, b, outsider]
    a.send('/ROOM_CREATE RoomA')
    a.expect('/ROOM_CREATED 1'); a.expect('/ROOM_STATE 1 ')
    b.send('/ROOM_JOIN 1')
    b.expect('/ROOM_JOINED 1'); b.expect('/ROOM_STATE 1 ')
    a.expect('/ROOM_STATE 1 ')
    outsider.send('/ROOM_CREATE RoomB')
    outsider.expect('/ROOM_CREATED 2'); outsider.expect('/ROOM_STATE 2 ')
    a.send('/SUMMON')
    a.expect('/ERROR GAME_NOT_STARTED')
    a.send('/READY'); a.expect('/ROOM_STATE 1 '); b.expect('/ROOM_STATE 1 ')
    b.send('/READY'); b.expect('/ROOM_STATE 1 '); a.expect('/ROOM_STATE 1 ')
    a.send('/START_REQUEST')
    for c in [a, b]:
        c.expect('/START 1')
        s1 = next_state(c, 1)
        s2 = next_state(c, 2)
        assert s1[2:4] == ['100', '10'] and s2[2:4] == ['100', '10']
        c.expect('/ROOM_STATE 1 1 PLAYING')
    a.send('/SUMMON')
    for c in [a, b]:
        first = next_state(c, 1)
        second = next_state(c, 2)
        assert first[2:4] == ['90', '15'] and sum(1 for d in first[7:] if d != '-') == 1
        assert second[2:4] == ['100', '10']
    a.send('/MERGE 0 0')
    a.expect('/ERROR INVALID_CELL')
    a.send('/MERGE x 1')
    a.expect('/ERROR INVALID_ARGUMENTS')
    a.send('/MERGE 0 1')
    a.expect('/ERROR EMPTY_CELL')
    a.send('/UPGRADE INVALID')
    a.expect('/ERROR INVALID_DICE_TYPE')
    a.send('/UPGRADE BASIC')
    for c in [a, b]:
        first = next_state(c, 1)
        next_state(c, 2)
        assert first[2] == '40' and first[4] == '1'
    a.send('/UPGRADE BASIC')
    a.expect('/ERROR NOT_ENOUGH_SP')
    a.send('/SUMMON')
    for c in [a, b]:
        first = next_state(c, 1)
        next_state(c, 2)
        assert first[2:4] == ['25', '20'] and sum(1 for d in first[7:] if d != '-') == 2
    # Merge requires same dice type; random summon may yield different types.
    active_cells = [i for i, val in enumerate(first[7:]) if val != '-']
    assert len(active_cells) == 2
    a.send(f'/MERGE {active_cells[0]} {active_cells[1]}')
    response = a.read()
    if response.startswith('/ERROR '):
        assert response == '/ERROR MERGE_MISMATCH', response
    else:
        assert response.startswith('/DICE_STATE 1 '), response
        a.expect('/DICE_STATE 2 ')
        next_state(b, 1); next_state(b, 2)
    outsider.send('/SUMMON')
    outsider.expect('/ERROR GAME_NOT_STARTED')
    outsider.send('/ROOM_LIST')
    outsider.expect('/ROOM_LIST_RESULT ')
    b.send('/ROOM_LEAVE')
    b.expect('/ROOM_LEFT 1')
    a.expect('/PLAYER_LEFT 2')
    a.expect('/ERROR GAME_INTERRUPTED')
    a.expect('/ROOM_STATE 1 ')
    a.send('/SUMMON')
    a.expect('/ERROR GAME_NOT_STARTED')
    print('PASS: actual TCP join, rooms, start, dice events, state fan-out, validation, interruption')
finally:
    for c in clients:
        try: c.close()
        except Exception: pass
    server.terminate()
    try: server.communicate(timeout=3)
    except subprocess.TimeoutExpired: server.kill()
