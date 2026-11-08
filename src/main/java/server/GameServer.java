package server;

import common.Protocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/** 최초 버전: 2명 입장 / 준비 / 채팅만 서버에서 관리합니다. */
public final class GameServer {
    private final Object lock = new Object();
    private final List<ClientHandler> players = new ArrayList<>();
    private boolean started;

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        new GameServer().serve(port);
    }

    private void serve(int port) throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[SERVER] Listening on port " + port);
            while (true) {
                Socket socket = server.accept();
                new Thread(new ClientHandler(this, socket), "client-handler").start();
            }
        }
    }

    /** 호출하는 핸들러는 이 메서드가 성공해야만 참가자가 됩니다. */
    boolean join(ClientHandler client, String name) {
        synchronized (lock) {
            if (started || players.size() >= 2) {
                client.send(Protocol.JOIN_FAIL + " ROOM_FULL");
                return false;
            }
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
            int id = players.isEmpty() ? 1 : (players.get(0).getId() == 1 ? 2 : 1);
            client.assign(id, name);
            players.add(client);
            client.send(Protocol.JOIN_OK + " " + id);
            broadcastLocked(Protocol.WAIT + " " + players.size() + "/2");
            System.out.println("[SERVER] Joined: " + name + " (" + id + ")");
            return true;
        }
    }

    void ready(ClientHandler client) {
        synchronized (lock) {
            if (!players.contains(client) || started) return;
            client.setReady(true);
            if (players.size() == 2 && players.stream().allMatch(ClientHandler::isReady)) {
                started = true;
                broadcastLocked(Protocol.START);
                System.out.println("[SERVER] Both players ready, game started (network demo)");
            }
        }
    }

    void chat(ClientHandler client, String message) {
        synchronized (lock) {
            if (!players.contains(client)) return;
            String trimmed = message.strip();
            if (trimmed.isEmpty()) return;
            if (trimmed.length() > 120) {
                client.send(Protocol.ERROR + " CHAT_TOO_LONG");
                return;
            }
            broadcastLocked(Protocol.CHAT + " " + client.getId() + " " + trimmed);
        }
    }

    void leave(ClientHandler client) {
        synchronized (lock) {
            if (players.remove(client)) {
                broadcastLocked(Protocol.PLAYER_LEFT + " " + client.getId());
                // 최초 단계에서는 대전 재설정(리매치) 미구현. 남은 참가자는 재접속 필요.
                if (started) {
                    for (ClientHandler player : List.copyOf(players)) {
                        player.send(Protocol.ERROR + " GAME_INTERRUPTED_RECONNECT");
                        player.close();
                    }
                    players.clear();
                    started = false;
                }
                System.out.println("[SERVER] Left: " + client.getPlayerName());
            }
        }
    }

    private void broadcastLocked(String line) {
        for (ClientHandler player : players) player.send(line);
    }
}
