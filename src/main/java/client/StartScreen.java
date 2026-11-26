package client;

import common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class StartScreen extends JFrame {
    private final JTextField hostField = new JTextField("127.0.0.1", 10);
    private final JTextField portField = new JTextField("5000", 5);
    private final JTextField nameField = new JTextField("Player", 8);
    private final JButton connectBtn = new JButton("게임 시작 (자동 입장)");
    private final JTextArea logArea = new JTextArea(12, 45);
    private final JLabel statusLabel = new JLabel("연결되지 않음");

    public StartScreen() {
        setTitle("Dice Clash - Start");
        setSize(700, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(240, 242, 245));
        setContentPane(mainPanel);

        // 1. 상단 입력 영역 (각 입력 항목을 패널로 묶어서 간격과 텍스트 유실 방지)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        topPanel.setBackground(new Color(240, 242, 245));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Font labelFont = new Font("SansSerif", Font.BOLD, 13);
        Font inputFont = new Font("SansSerif", Font.PLAIN, 13);

        JLabel hostLabel = new JLabel("서버:"); hostLabel.setFont(labelFont);
        hostField.setFont(inputFont); hostField.setPreferredSize(new Dimension(110, 32));

        JLabel portLabel = new JLabel("포트:"); portLabel.setFont(labelFont);
        portField.setFont(inputFont); portField.setPreferredSize(new Dimension(65, 32));

        JLabel nameLabel = new JLabel("이름:"); nameLabel.setFont(labelFont);
        nameField.setFont(inputFont); nameField.setPreferredSize(new Dimension(90, 32));

        // 버튼 텍스트가 절대 잘리지 않도록 크기와 폰트 명시적 설정
        connectBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        connectBtn.setPreferredSize(new Dimension(170, 36));
        connectBtn.setBackground(new Color(40, 140, 230));
        connectBtn.setForeground(Color.WHITE);
        connectBtn.setFocusPainted(false);
        connectBtn.setOpaque(true);
        connectBtn.setBorderPainted(false);

        topPanel.add(hostLabel);
        topPanel.add(hostField);
        topPanel.add(portLabel);
        topPanel.add(portField);
        topPanel.add(nameLabel);
        topPanel.add(nameField);
        topPanel.add(connectBtn); // 텍스트가 포함된 버튼 추가

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 2. 중앙 로그 영역
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(logArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 3. 하단 상태 표시 영역
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(240, 242, 245));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // 접속 버튼 이벤트 연결
        connectBtn.addActionListener(e -> connectToServer());
    }

    private void connectToServer() {
        String host = hostField.getText().strip();
        String nickname = nameField.getText().strip();
        int port;

        NetworkClient.getInstance().setMyNickname(nickname);
        
        try {
            port = Integer.parseInt(portField.getText().strip());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "올바른 포트 번호를 입력하세요.");
            return;
        }

        if (nickname.isBlank() || !nickname.matches("[\\p{L}\\p{N}_-]{1,20}")) {
            JOptionPane.showMessageDialog(this, "이름은 1~20자의 문자/숫자/_/-만 가능합니다.");
            return;
        }

        connectBtn.setEnabled(false);
        statusLabel.setText("서버 연결 중...");
        appendLog("서버(" + host + ":" + port + ")에 연결 시도 중...");

        try {
            NetworkClient netClient = NetworkClient.getInstance();
            
            // 서버 응답 리스너 설정
            netClient.setMessageListener(this::onServerMessageReceived);

            new Thread(() -> {
                try {
                    netClient.connect(host, port, nickname);
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "서버 연결 실패: " + ex.getMessage());
                        appendLog("연결 오류: " + ex.getMessage());
                        connectBtn.setEnabled(true);
                        statusLabel.setText("연결 실패");
                    });
                }
            }).start();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "오류 발생: " + ex.getMessage());
            connectBtn.setEnabled(true);
            statusLabel.setText("오류 발생");
        }
    }

    private void onServerMessageReceived(String line) {
        appendLog("수신: " + line);

        if (line.startsWith(Protocol.JOIN_OK + " ")) {
            int myId = Integer.parseInt(line.substring(Protocol.JOIN_OK.length() + 1).strip());
            NetworkClient.getInstance().setMyPlayerId(myId);
            
            statusLabel.setText("참가 성공 - ID: " + myId);

            // 접속 성공하자마자 1번 방 확인 후 자동 입장/생성 요청
            NetworkClient.getInstance().send(Protocol.ROOM_LIST);

        } else if (line.startsWith(Protocol.ROOM_LIST_RESULT)) {
            String roomData = line.substring(Protocol.ROOM_LIST_RESULT.length()).strip();
            
            if (roomData.isEmpty()) {
                NetworkClient.getInstance().send(Protocol.ROOM_CREATE + " 1번방");
            } else {
                String[] rooms = roomData.split("\\s+");
                String firstRoomInfo = rooms[0];
                String roomId = firstRoomInfo.split(":")[0];
                
                NetworkClient.getInstance().send(Protocol.ROOM_JOIN + " " + roomId);
            }

        } else if (line.startsWith(Protocol.ROOM_CREATED + " ") || line.startsWith(Protocol.ROOM_JOINED + " ")) {
            // 방 생성 또는 참가가 성공하면 RoomScreen으로 이동
            new RoomScreen();
            dispose();

        } else if (line.startsWith(Protocol.JOIN_FAIL + " ") || line.startsWith(Protocol.ERROR + " ")) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "서버 알림: " + line);
                appendLog("에러/거절: " + line);
                connectBtn.setEnabled(true);
            });
        }
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StartScreen().setVisible(true));
    }
}