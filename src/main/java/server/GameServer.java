package server;

import common.Protocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class GameServer {
    private static final int SP_REGEN_INTERVAL_TICKS = 3;
    private static final int SP_REGEN_AMOUNT = 50;
    private final Object lock = new Object();
    private final List<ClientHandler> players = new ArrayList<>();
    private final Map<Integer, GameRoom> rooms = new LinkedHashMap<>();
    private int nextPlayerId = 1;
    private int nextRoomId = 1;
    private final ScheduledExecutorService gameClock = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "monster-game-clock");
        thread.setDaemon(true);
        return thread;
    });

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        new GameServer().serve(port);
    }

    private void serve(int port) throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[SERVER] Listening on port " + port);
            gameClock.scheduleAtFixedRate(this::tickMonsters, Protocol.GAME_TICK_MS, Protocol.GAME_TICK_MS, TimeUnit.MILLISECONDS);
            while (true) {
                Socket socket = server.accept();
                new Thread(new ClientHandler(this, socket), "client-handler").start();
            }
        }
    }

    // 플레이어 등록
    boolean join(ClientHandler client, String name) {
        synchronized (lock) {
            if (name.isBlank() || name.length() > 20 || !name.matches("[\\p{L}\\p{N}_-]+")) {
                client.send(Protocol.JOIN_FAIL + " INVALID_NAME");
                return false;
            }
            for (ClientHandler player : players) {
                if (player.getPlayerName().equalsIgnoreCase(name)) {
                    client.send(Protocol.JOIN_FAIL + " DUPLICATE_NAME");
                    return false;
                }
            }
            int id = nextPlayerId++;
            client.assign(id, name);
            players.add(client);
            client.send(Protocol.JOIN_OK + " " + id);
            System.out.println("[SERVER] Registered: " + name + " (" + id + ")");
            return true;
        }
    }

    void listRooms(ClientHandler client) {
        synchronized (lock) {
            if (!registered(client)) return;
            StringBuilder list = new StringBuilder(Protocol.ROOM_LIST_RESULT);
            for (GameRoom room : rooms.values()) list.append(' ').append(room.summary());
            client.send(list.toString());
        }
    }

    void createRoom(ClientHandler client, String title) {
        synchronized (lock) {
            if (!registered(client)) return;
            if (client.getRoom() != null) { client.send(Protocol.ERROR + " ALREADY_IN_ROOM"); return; }
            if (!title.matches("[\\p{L}\\p{N}_-]{1,20}")) {
                client.send(Protocol.ERROR + " INVALID_ROOM_NAME"); return;
            }
            GameRoom room = new GameRoom(nextRoomId++, title, client);
            rooms.put(room.id, room);
            client.setRoom(room);
            client.setReady(false);
            client.send(Protocol.ROOM_CREATED + " " + room.id);
            roomState(room);
        }
    }

    void joinRoom(ClientHandler client, String rawId) {
        synchronized (lock) {
            if (!registered(client)) return;
            if (client.getRoom() != null) { client.send(Protocol.ERROR + " ALREADY_IN_ROOM"); return; }
            int id;
            try { id = Integer.parseInt(rawId); }
            catch (NumberFormatException e) { client.send(Protocol.ERROR + " INVALID_ROOM_ID"); return; }
            GameRoom room = rooms.get(id);
            if (room == null) { client.send(Protocol.ERROR + " ROOM_NOT_FOUND"); return; }
            if (room.started) { client.send(Protocol.ERROR + " GAME_ALREADY_STARTED"); return; }
            if (room.members.size() >= 2) { client.send(Protocol.ERROR + " ROOM_FULL"); return; }
            room.members.add(client);
            client.setRoom(room);
            client.setReady(false);
            client.send(Protocol.ROOM_JOINED + " " + room.id);
            roomState(room);
        }
    }

    void leaveRoom(ClientHandler client) {
        synchronized (lock) {
            if (!registered(client)) return;
            if (client.getRoom() == null) { client.send(Protocol.ERROR + " NOT_IN_ROOM"); return; }
            removeFromRoom(client, true);
        }
    }

    void setReady(ClientHandler client, boolean ready) {
        synchronized (lock) {
            if (!registered(client)) return;
            GameRoom room = client.getRoom();
            if (room == null) { client.send(Protocol.ERROR + " NOT_IN_ROOM"); return; }
            if (room.started) { client.send(Protocol.ERROR + " GAME_ALREADY_STARTED"); return; }
            if (client.isReady() == ready) { client.send(Protocol.ERROR + (ready ? " ALREADY_READY" : " ALREADY_NOT_READY")); return; }
            client.setReady(ready);
            roomState(room);
        }
    }

    // 준비 상태 확인 후 게임 시작
    void requestStart(ClientHandler client) {
        synchronized (lock) {
            if (!registered(client)) return;
            GameRoom room = client.getRoom();
            if (room == null) { client.send(Protocol.ERROR + " NOT_IN_ROOM"); return; }
            if (room.started) { client.send(Protocol.ERROR + " GAME_ALREADY_STARTED"); return; }
            if (room.host != client) { client.send(Protocol.ERROR + " NOT_HOST"); return; }
            if (room.members.size() != 2 || room.members.stream().anyMatch(p -> !p.isReady())) {
                client.send(Protocol.ERROR + " PLAYERS_NOT_READY"); return;
            }
            room.game = new DiceGame(room.members.get(0).getId(), room.members.get(1).getId());
            room.monsters = new MonsterGame(room.game);
            room.started = true;
            broadcast(room, Protocol.START + " " + room.id);
            broadcastDiceState(room);
            roomState(room);
            System.out.println("[SERVER] Room " + room.id + " started (gameplay pending)");
        }
    }

    void summon(ClientHandler client) {
        synchronized (lock) {
            GameRoom room = playingRoom(client);
            if (room == null) return;
            sendAction(room, client, room.game.summon(client.getId()));
        }
    }

    void eliteSummon(ClientHandler client) {
        synchronized (lock) {
            GameRoom room = playingRoom(client);
            if (room == null) return;
            DiceGame.ActionResult result = room.monsters.eliteSummon(client.getId());
            if (!result.success()) {
                client.send(Protocol.ERROR + " " + result.error);
                return;
            }
            broadcastDiceState(room);
            broadcastMonsterState(room);
        }
    }

    void merge(ClientHandler client, String input) {
        synchronized (lock) {
            GameRoom room = playingRoom(client);
            if (room == null) return;
            String[] tokens = input.split("\\s+");
            if (tokens.length != 2) { client.send(Protocol.ERROR + " INVALID_ARGUMENTS"); return; }
            int from, to;
            try {
                from = Integer.parseInt(tokens[0]);
                to = Integer.parseInt(tokens[1]);
            } catch (NumberFormatException e) {
                client.send(Protocol.ERROR + " INVALID_ARGUMENTS"); return;
            }
            sendAction(room, client, room.game.merge(client.getId(), from, to));
        }
    }

    void upgrade(ClientHandler client, String type) {
        synchronized (lock) {
            GameRoom room = playingRoom(client);
            if (room == null) return;
            sendAction(room, client, room.game.upgrade(client.getId(), type));
        }
    }

    private GameRoom playingRoom(ClientHandler client) {
        if (!registered(client)) return null;
        GameRoom room = client.getRoom();
        if (room == null) { client.send(Protocol.ERROR + " NOT_IN_ROOM"); return null; }
        if (!room.started || room.game == null) {
            client.send(Protocol.ERROR + " GAME_NOT_STARTED"); return null;
        }
        return room;
    }

    private void sendAction(GameRoom room, ClientHandler client, DiceGame.ActionResult result) {
        if (!result.success()) {
            client.send(Protocol.ERROR + " " + result.error);
            return;
        }
        broadcastDiceState(room);
    }

    // 주사위 상태 전송
    private void broadcastDiceState(GameRoom room) {
        if (room.game == null) return;
        for (DiceGame.PlayerField field : room.game.fields()) broadcast(room, field.encode());
    }

    private void broadcastMonsterState(GameRoom room) {
        if (room.monsters == null) return;
        for (String state : room.monsters.states()) broadcast(room, state);
    }

    private void tickMonsters() {
        synchronized (lock) {
            for (GameRoom room : rooms.values()) {
                if (!room.started || room.monsters == null) continue;
                room.monsters.tick();
                if (!room.monsters.finished() && room.monsters.tickCount() % SP_REGEN_INTERVAL_TICKS == 0) {
                    for (DiceGame.PlayerField field : room.game.fields()) field.sp += SP_REGEN_AMOUNT;
                }
                broadcastMonsterState(room);
                broadcastDiceState(room);
                for (String event : room.monsters.attackEvents()) broadcast(room, event);
                if (room.monsters.finished()) {
                    broadcast(room, Protocol.GAME_OVER + " " + room.monsters.winnerId());
                    room.started = false;
                    room.game = null;
                    room.monsters = null;
                    for (ClientHandler member : room.members) member.setReady(false);
                    roomState(room);
                }
            }
        }
    }

    void chat(ClientHandler client, String message) {
        synchronized (lock) {
            if (!registered(client)) return;
            GameRoom room = client.getRoom();
            if (room == null) { client.send(Protocol.ERROR + " NOT_IN_ROOM"); return; }
            String trimmed = message.strip();
            if (trimmed.isEmpty()) return;
            if (trimmed.length() > 120) { client.send(Protocol.ERROR + " CHAT_TOO_LONG"); return; }
            broadcast(room, Protocol.CHAT + " " + client.getId() + " " + trimmed);
        }
    }

    void leave(ClientHandler client) {
        synchronized (lock) {
            if (players.remove(client)) {
                removeFromRoom(client, false);
                System.out.println("[SERVER] Disconnected: " + client.getPlayerName());
            }
        }
    }

    private boolean registered(ClientHandler client) {
        if (players.contains(client)) return true;
        client.send(Protocol.ERROR + " NOT_JOINED");
        return false;
    }

    private void removeFromRoom(ClientHandler client, boolean acknowledge) {
        GameRoom room = client.getRoom();
        if (room == null) return;
        room.members.remove(client);
        client.setRoom(null);
        client.setReady(false);
        if (acknowledge) client.send(Protocol.ROOM_LEFT + " " + room.id);
        broadcast(room, Protocol.PLAYER_LEFT + " " + client.getId());
        if (room.members.isEmpty()) {
            rooms.remove(room.id);
            return;
        }
        if (room.host == client) room.host = room.members.get(0);
        if (room.started) {
            room.started = false;
            room.game = null;
            room.monsters = null;
            for (ClientHandler member : room.members) member.setReady(false);
            broadcast(room, Protocol.ERROR + " GAME_INTERRUPTED");
        }
        roomState(room);
    }

    private void roomState(GameRoom room) { broadcast(room, Protocol.ROOM_STATE + " " + room.state()); }
    private void broadcast(GameRoom room, String line) {
        for (ClientHandler member : List.copyOf(room.members)) member.send(line);
    }
}