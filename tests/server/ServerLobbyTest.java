package server;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** 서버 프로세스와 실제 TCP 연결을 사용한 2인 대기실 통합 테스트. */
public final class ServerLobbyTest {
    private ServerLobbyTest() { }

    private static final class Peer implements Closeable {
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;
        Peer(int port) throws IOException {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(2000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }
        void send(String text) { out.println(text); }
        void expect(String expected) throws IOException {
            String actual = in.readLine();
            if (!expected.equals(actual)) {
                throw new AssertionError("expected=[" + expected + "] actual=[" + actual + "]");
            }
        }
        @Override public void close() throws IOException { socket.close(); }
    }

    public static void main(String[] args) throws Exception {
        int port;
        try (ServerSocket reservation = new ServerSocket(0)) {
            port = reservation.getLocalPort();
        }
        Process process = new ProcessBuilder("java", "-cp", "out", "server.GameServer", Integer.toString(port))
                .inheritIO().start();
        try {
            waitForServer(port);
            checkMatching(port);
            System.out.println("PASS: duplicate/full room, lobby leave, replacement, ready/start, protocol errors");
        } finally {
            process.destroy();
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
        }
    }

    private static void waitForServer(int port) throws Exception {
        for (int i = 0; i < 30; i++) {
            try (Socket ignored = new Socket("127.0.0.1", port)) { return; }
            catch (IOException e) { Thread.sleep(50); }
        }
        throw new AssertionError("server failed to start");
    }

    private static void checkMatching(int port) throws Exception {
        try (Peer first = new Peer(port)) {
            first.send("/JOIN Alice");
            first.expect("/JOIN_OK 1");
            first.expect("/WAIT 1/2");
            try (Peer duplicate = new Peer(port)) {
                duplicate.send("/JOIN Alice");
                duplicate.expect("/JOIN_FAIL DUPLICATE_NAME");
            }
            try (Peer second = new Peer(port)) {
                second.send("/JOIN Bob");
                second.expect("/JOIN_OK 2");
                first.expect("/WAIT 2/2");
                second.expect("/WAIT 2/2");
                try (Peer third = new Peer(port)) {
                    third.send("/JOIN Charlie");
                    third.expect("/JOIN_FAIL ROOM_FULL");
                }
                first.send("/READY");
                first.send("/READY");
                first.expect("/ERROR ALREADY_READY");
                second.close();
                first.expect("/PLAYER_LEFT 2");
                first.expect("/WAIT 1/2");
            }
            try (Peer replacement = new Peer(port)) {
                replacement.send("/JOIN Dana");
                replacement.expect("/JOIN_OK 2");
                first.expect("/WAIT 2/2");
                replacement.expect("/WAIT 2/2");
                replacement.send("/READY");
                first.expect("/START");
                replacement.expect("/START");
                first.send("/READY");
                first.expect("/ERROR GAME_ALREADY_STARTED");
            }
        }
    }
}
