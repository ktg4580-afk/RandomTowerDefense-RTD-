package server;

import common.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

final class ClientHandler implements Runnable {
    private final GameServer server;
    private final Socket socket;
    private PrintWriter output;
    private volatile String playerName = "";
    private volatile int id;
    private volatile boolean ready;
    private volatile GameRoom room;

    ClientHandler(GameServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override public void run() {
        try (socket;
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            output = out;
            // 참가 요청 전에 임의 명령을 실행하지 못하도록 첫 메시지를 확인합니다.
            String first = input.readLine();
            if (first == null || !first.startsWith(Protocol.JOIN + " ")) {
                send(Protocol.JOIN_FAIL + " EXPECT_JOIN");
                return;
            }
            if (!server.join(this, first.substring((Protocol.JOIN + " ").length()).strip())) return;

            String line;
            while ((line = input.readLine()) != null) {
                if (line.equals(Protocol.ROOM_LIST)) server.listRooms(this);
                else if (line.startsWith(Protocol.ROOM_CREATE + " ")) server.createRoom(this, line.substring(Protocol.ROOM_CREATE.length() + 1).strip());
                else if (line.startsWith(Protocol.ROOM_JOIN + " ")) server.joinRoom(this, line.substring(Protocol.ROOM_JOIN.length() + 1).strip());
                else if (line.equals(Protocol.ROOM_LEAVE)) server.leaveRoom(this);
                else if (line.equals(Protocol.READY)) server.setReady(this, true);
                else if (line.equals(Protocol.UNREADY)) server.setReady(this, false);
                else if (line.equals(Protocol.START_REQUEST)) server.requestStart(this);
                else if (line.equals(Protocol.SUMMON)) server.summon(this);
                else if (line.startsWith(Protocol.MERGE + " ")) server.merge(this, line.substring(Protocol.MERGE.length() + 1).strip());
                else if (line.startsWith(Protocol.UPGRADE + " ")) server.upgrade(this, line.substring(Protocol.UPGRADE.length() + 1).strip());
                else if (line.startsWith(Protocol.CHAT + " ")) server.chat(this, line.substring(Protocol.CHAT.length() + 1));
                else send(Protocol.ERROR + " UNKNOWN_COMMAND");
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Connection closed: " + e.getMessage());
        } finally {
            server.leave(this);
        }
    }

    void assign(int id, String playerName) { this.id = id; this.playerName = playerName; }
    int getId() { return id; }
    String getPlayerName() { return playerName; }
    boolean isReady() { return ready; }
    void setReady(boolean ready) { this.ready = ready; }
    synchronized void send(String line) { if (output != null) output.println(line); }
    GameRoom getRoom() { return room; }
    void setRoom(GameRoom room) { this.room = room; }
    void close() { try { socket.close(); } catch (IOException ignored) { } }
}
