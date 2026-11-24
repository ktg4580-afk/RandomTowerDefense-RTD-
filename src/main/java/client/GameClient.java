package client;

import common.Protocol;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** 초기 네트워크 테스트 UI. 게임 필드 및 게임 로직은 아직 없습니다. */
public final class GameClient extends JFrame {
    private final JTextField host = new JTextField("127.0.0.1", 11);
    private final JTextField port = new JTextField("5000", 5);
    private final JTextField name = new JTextField("Player", 9);
    private final JTextField chat = new JTextField(22);
    private final JTextArea log = new JTextArea(14, 48);
    private final JLabel status = new JLabel("연결되지 않음");
    private final JLabel spLabel = new JLabel("SP - / 소환 -");
    private final JButton connectButton = new JButton("접속");
    private final JButton readyButton = new JButton("준비 완료");
    private final JButton summonButton = new JButton("주사위 소환");
    private final JButton chatButton = new JButton("전송");
    private volatile Socket socket;
    private volatile PrintWriter writer;

    private int myId = -1;
    private int mySp;
    private int mySummonCost;
    private boolean spShort;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameClient().setVisible(true));
    }

    private GameClient() {
        super("태산 - 랜덤 타워 디펜스 (네트워크 테스트)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("서버")); top.add(host);
        top.add(new JLabel("포트")); top.add(port);
        top.add(new JLabel("이름")); top.add(name);
        top.add(connectButton); top.add(readyButton);
        top.add(spLabel); top.add(summonButton);
        log.setEditable(false);
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(status, BorderLayout.NORTH);
        JPanel chatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chatPanel.add(new JLabel("메시지")); chatPanel.add(chat); chatPanel.add(chatButton);
        bottom.add(chatPanel, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(log), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        readyButton.setEnabled(false);
        chatButton.setEnabled(false);
        summonButton.setEnabled(false);
        connectButton.addActionListener(e -> connect());
        readyButton.addActionListener(e -> send(Protocol.READY));
        summonButton.addActionListener(e -> send(Protocol.SUMMON));
        chatButton.addActionListener(e -> sendChat());
        chat.addActionListener(e -> sendChat());
        pack(); setLocationByPlatform(true);
    }

    private void connect() {
        connectButton.setEnabled(false);
        final String hostname = host.getText().strip();
        final String nickname = name.getText().strip();
        final int portNumber;
        try {
            portNumber = Integer.parseInt(port.getText().strip());
            if (portNumber < 1 || portNumber > 65535 || !nickname.matches("[\\p{L}\\p{N}_-]{1,20}"))
                throw new IllegalArgumentException("포트 또는 이름이 올바르지 않습니다.");
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "포트(1~65535)와 이름(1~20자, 문자/숫자/_/-)을 확인하세요.");
            connectButton.setEnabled(true);
            return;
        }
        new Thread(() -> {
            try (Socket connection = new Socket(hostname, portNumber);
                 BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8), true)) {
                socket = connection; writer = out;
                send(Protocol.JOIN + " " + nickname);
                String line;
                while ((line = in.readLine()) != null) {
                    final String response = line;
                    SwingUtilities.invokeLater(() -> receive(response));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> append("연결 오류: " + e.getMessage()));
            } finally {
                writer = null; socket = null;
                SwingUtilities.invokeLater(() -> {
                    readyButton.setEnabled(false); chatButton.setEnabled(false);
                    summonButton.setEnabled(false);
                    connectButton.setEnabled(true); status.setText("연결 종료");
                });
            }
        }, "server-reader").start();
    }

    private void receive(String line) {
        if (line.startsWith(Protocol.JOIN_OK + " ")) {
            try {
                myId = Integer.parseInt(line.substring(Protocol.JOIN_OK.length() + 1).strip());
            } catch (NumberFormatException ignored) {
                myId = -1;
            }
            readyButton.setEnabled(true);
            chatButton.setEnabled(true);
            status.setText("접속됨 (id=" + myId + ")");
            append(line);
        } else if (line.startsWith(Protocol.DICE_STATE + " ")) {
            String[] t = line.split("\\s+");
            if (t.length < 4) return;
            final int ownerId;
            try {
                ownerId = Integer.parseInt(t[1]);
            } catch (NumberFormatException ex) {
                return;
            }
            if (ownerId != myId) return;              // 남의 SP는 무시
            mySp = Integer.parseInt(t[2]);
            mySummonCost = Integer.parseInt(t[3]);
            spLabel.setText("SP " + mySp + " / 소환 " + mySummonCost);
            summonButton.setEnabled(mySp >= mySummonCost);   // 충전되면 자동 복구
            if (mySp >= mySummonCost) spShort = false;       // 플래그 해제
        } else if (line.startsWith(Protocol.ERROR + " ")) {
            String code = line.substring(Protocol.ERROR.length() + 1).strip();
            if ("NOT_ENOUGH_SP".equals(code)) {
                if (!spShort) {
                    spShort = true;
                    JOptionPane.showMessageDialog(this,
                        "SP가 부족합니다. (현재 " + mySp + " / 필요 " + mySummonCost + ")\nSP가 충전되면 자동으로 다시 소환할 수 있습니다.");
                }
            } else if ("FIELD_FULL".equals(code)) {
                JOptionPane.showMessageDialog(this, "보드가 가득 찼습니다. 합성으로 칸을 비워주세요.");
            } else {
                append(line);
            }
        } else {
            append(line);
        }
    }

    private void sendChat() {
        String message = chat.getText().strip();
        if (message.isEmpty()) return;
        if (message.length() > 120 || message.contains("\n") || message.contains("\r")) {
            JOptionPane.showMessageDialog(this, "메시지는 120자 이하, 한 줄만 가능합니다."); return;
        }
        send(Protocol.CHAT + " " + message);
        chat.setText("");
    }

    private void send(String line) {
        PrintWriter out = writer;
        if (out != null) out.println(line);
    }
    private void append(String line) { log.append(line + "\n"); log.setCaretPosition(log.getDocument().getLength()); }
}
