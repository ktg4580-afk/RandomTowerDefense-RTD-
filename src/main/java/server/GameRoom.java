package server;

import java.util.ArrayList;
import java.util.List;

final class GameRoom {
    final int id;
    final String title;
    final List<ClientHandler> members = new ArrayList<>();
    ClientHandler host;
    boolean started;
    DiceGame game;
    MonsterGame monsters;

    GameRoom(int id, String title, ClientHandler creator) {
        this.id = id;
        this.title = title;
        this.host = creator;
        members.add(creator);
    }

    String summary() {
        return id + ":" + title + ":" + members.size() + "/2:" + (started ? "PLAYING" : "WAITING");
    }

    // 방 상태 전송용 데이터
    String state() {
        StringBuilder value = new StringBuilder(id + " " + host.getId() + " " + (started ? "PLAYING" : "WAITING"));
        for (ClientHandler player : members) {
            value.append(' ').append(player.getId()).append(':').append(player.isReady() ? "READY" : "NOT_READY");
        }
        return value.toString();
    }
}
