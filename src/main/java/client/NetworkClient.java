package client;

import common.Protocol;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private PrintWriter out;
    private Consumer<String> messageListener;
    private int myPlayerId;
    
 // NetworkClient.java 내부에 추가
    private String myNickname = "Player";

    public void setMyNickname(String nickname) {
        this.myNickname = nickname;
    }

    public String getMyNickname() {
        return myNickname;
    }

    public static NetworkClient getInstance() {
        if (instance == null) instance = new NetworkClient();
        return instance;
    }

    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }

    public int getMyPlayerId() { return myPlayerId; }
    public void setMyPlayerId(int id) { this.myPlayerId = id; }

    public void connect(String host, int port, String nickname) throws IOException {
        socket = new Socket(host, port);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        // 서버 접속 직후 이름 등록 요청
        send(Protocol.JOIN + " " + nickname);

        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> {
                        if (messageListener != null) messageListener.accept(msg);
                    });
                }
            } catch (IOException e) {
                System.out.println("서버 연결 종료: " + e.getMessage());
            }
        }).start();
    }

    public void send(String msg) {
        if (out != null) out.println(msg);
    }
}