"""Real TCP regression test for the week-2 monster/wave server.
No third-party packages needed. Run after compiling Java sources into out/.
"""
import socket
import subprocess
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CLIENTS = []

class Client:
    def __init__(self, name, port):
        self.sock = socket.create_connection(("127.0.0.1", port), timeout=4)
        self.sock.settimeout(4)
        self.stream = self.sock.makefile("r", encoding="utf-8", newline="\n")
        CLIENTS.append(self)
        self.send("/JOIN " + name)
        self.expect("/JOIN_OK ")

    def send(self, message):
        self.sock.sendall((message + "\n").encode("utf-8"))

    def read(self):
        line = self.stream.readline()
        if not line:
            raise AssertionError("Server disconnected unexpectedly")
        return line.strip()

    def expect(self, prefix, timeout=4):
        """Ignore unrelated async state broadcasts, but fail on unexpected errors."""
        end = time.monotonic() + timeout
        while time.monotonic() < end:
            self.sock.settimeout(max(0.1, end - time.monotonic()))
            try:
                line = self.read()
            except (socket.timeout, TimeoutError):
                break
            if line.startswith(prefix):
                return line
            if line.startswith("/ERROR "):
                raise AssertionError(f"Expected {prefix!r}, received {line!r}")
        raise AssertionError(f"Timed out waiting for {prefix!r}")

    def close(self):
        try:
            self.sock.shutdown(socket.SHUT_RDWR)
        except OSError:
            pass
        try:
            self.stream.close()
        except OSError:
            pass
        self.sock.close()

def state_parts(client, owner_id, timeout=4):
    line = client.expect("/MONSTER_STATE " + str(owner_id) + " ", timeout)
    parts = line.split()
    assert len(parts) >= 7, line
    num_monsters = int(parts[6])
    assert len(parts) == 7 + num_monsters, line
    return parts

def main():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as temp:
        temp.bind(("127.0.0.1", 0))
        port = temp.getsockname()[1]
    server = subprocess.Popen(
        ["java", "-cp", "out", "server.GameServer", str(port)],
        cwd=ROOT, stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT,
    )
    try:
        deadline = time.monotonic() + 5
        while True:
            try:
                a = Client("wave_alpha", port)
                break
            except (ConnectionRefusedError, OSError):
                if time.monotonic() >= deadline:
                    raise AssertionError("Server failed to start")
                time.sleep(0.1)
        b = Client("wave_beta", port)
        outsider = Client("wave_gamma", port)
        a.send("/ROOM_CREATE WaveRoom")
        a.expect("/ROOM_CREATED 1")
        a.expect("/ROOM_STATE 1 ")
        b.send("/ROOM_JOIN 1")
        b.expect("/ROOM_JOINED 1")
        b.expect("/ROOM_STATE 1 ")
        a.expect("/ROOM_STATE 1 ")
        outsider.send("/ROOM_CREATE OtherRoom")
        outsider.expect("/ROOM_CREATED 2")
        outsider.expect("/ROOM_STATE 2 ")

        outsider.send("/ELITE_SUMMON")
        outsider.expect("/ERROR GAME_NOT_STARTED")
        a.send("/READY")
        a.expect("/ROOM_STATE 1 ")
        b.expect("/ROOM_STATE 1 ")
        b.send("/READY")
        a.expect("/ROOM_STATE 1 ")
        b.expect("/ROOM_STATE 1 ")
        a.send("/START_REQUEST")
        a.expect("/START 1")
        b.expect("/START 1")
        initial_a = state_parts(a, 1, timeout=4)
        initial_b = state_parts(b, 2, timeout=4)
        assert initial_a[2] == "20" and initial_b[2] == "20", (initial_a, initial_b)
        assert int(initial_a[4]) >= 1 and int(initial_b[4]) >= 1
        print("PASS: two clients receive automatic monster state over TCP")

        a.send("/ELITE_SUMMON")
        paid = None
        for _ in range(8):
            observed = a.expect("/DICE_STATE 1 ")
            if observed.split()[2] == "60": 
                paid = observed
                break
        assert paid is not None, "SP deduction was not broadcast"
        target = None
        for _ in range(8):
            observed = state_parts(b, 2)
            if any(
                ":TANK:" in value or ":SPRINTER:" in value or ":SHIELDED:" in value
                for value in observed[7:]
            ):
                target = observed
                break
        assert target is not None, "Elite never appeared on opponent lane"
        print("PASS: elite request deducts sender SP and appears on opponent lane")

        a.send("/ELITE_SUMMON")
        a.expect("/ERROR ELITE_COOLDOWN")
        outsider.send("/ELITE_SUMMON")
        outsider.expect("/ERROR GAME_NOT_STARTED")
        print("PASS: elite cooldown and unrelated room validation")

        for client in (a, b):
            for player_id, expected_sp in ((1, "110"), (2, "150")):
                deadline = time.monotonic() + 5
                while time.monotonic() < deadline:
                    state = client.expect(f"/DICE_STATE {player_id} ", deadline - time.monotonic())
                    if state.split()[2] == expected_sp:
                        break
                else:
                    raise AssertionError(f"Player {player_id} did not receive SP regeneration")
        print("PASS: both clients receive the scheduled SP increase for both players")

        b.send("/ROOM_LEAVE")
        b.expect("/ROOM_LEFT 1")
        a.expect("/PLAYER_LEFT 2")
        a.expect("/ERROR GAME_INTERRUPTED")
        a.expect("/ROOM_STATE 1 ")
        a.send("/ELITE_SUMMON")
        a.expect("/ERROR GAME_NOT_STARTED")
        print("PASS: disconnect/leave interrupts game and rejects further actions")
        print("PASS: week2 real TCP monster and elite integration checks")
    finally:
        for client in CLIENTS:
            client.close()
        server.terminate()
        try:
            server.wait(timeout=3)
        except subprocess.TimeoutExpired:
            server.kill()
            server.wait()

if __name__ == "__main__":
    main()
