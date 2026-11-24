package client;

import common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class RoomScreen extends JFrame {
    private JLabel statusLabel;
    private JButton readyBtn;
    private JButton startBtn;
    private boolean isReady = false;
    private volatile boolean isTransitioning = false;

    public RoomScreen() {
        setTitle("Dice Clash - Room");
        setSize(480, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);
        setContentPane(mainPanel);

        JLabel titleLabel = new JLabel("대전 대기실", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        statusLabel = new JLabel("참가자 정보를 불러오는 중...", JLabel.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        statusLabel.setForeground(new Color(60, 60, 60));
        mainPanel.add(statusLabel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        bottomPanel.setBackground(Color.WHITE);

        readyBtn = new JButton("준비 완료");
        readyBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        readyBtn.setPreferredSize(new Dimension(140, 45));
        readyBtn.setBackground(new Color(40, 140, 230));
        readyBtn.setForeground(Color.WHITE);
        readyBtn.setFocusPainted(false);
        readyBtn.setOpaque(true);
        readyBtn.setBorderPainted(false);
        readyBtn.addActionListener(e -> toggleReadyState());

        startBtn = new JButton("게임 시작");
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        startBtn.setPreferredSize(new Dimension(140, 45));
        startBtn.setBackground(new Color(40, 180, 80));
        startBtn.setForeground(Color.WHITE);
        startBtn.setFocusPainted(false);
        startBtn.setOpaque(true);
        startBtn.setBorderPainted(false);
        startBtn.addActionListener(e -> NetworkClient.getInstance().send(Protocol.START_REQUEST));

        bottomPanel.add(readyBtn);
        bottomPanel.add(startBtn);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                isTransitioning = true;
            }
        });

        NetworkClient.getInstance().setMessageListener(this::handleServerMessage);
        setVisible(true);
    }

    private void toggleReadyState() {
        NetworkClient.getInstance().send(isReady ? Protocol.UNREADY : Protocol.READY);
    }

    private void handleServerMessage(String line) {
        if (isTransitioning) return;
        System.out.println("[RoomScreen 수신] " + line);

        if (line.startsWith(Protocol.ROOM_STATE + " ")) {
            parseAndDisplayRoomState(line.substring(Protocol.ROOM_STATE.length() + 1).strip());
        } else if (line.startsWith(Protocol.START)) {
            isTransitioning = true;
            SwingUtilities.invokeLater(() -> {
                new GameFrame();
                dispose();
            });
        } else if (line.startsWith(Protocol.ERROR + " ")) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "알림: " + line.substring(Protocol.ERROR.length() + 1));
            });
        }
    }

    private void parseAndDisplayRoomState(String data) {
        String[] tokens = data.split("\\s+");
        String myNickname = NetworkClient.getInstance().getMyNickname();
        Boolean myServerReady = null;

        StringBuilder sb = new StringBuilder("<html><div style='text-align: center;'>");
        sb.append("<b>참가자 명단</b><br><br>");

        for (String token : tokens) {
            if (token.contains(":")) {
                String[] userParts = token.split(":");
                String playerName = userParts[0];
                String state = userParts[1];
                boolean ready = "READY".equals(state);

                String displayName = playerName;
                if (playerName.equals(myNickname)) {
                    displayName += " (나)";
                    myServerReady = ready;
                }

                sb.append("• <b>").append(displayName).append("</b> : ")
                  .append(ready ? "<font color='green'><b>준비 완료</b></font>" : "<font color='gray'>대기 중</font>")
                  .append("<br>");
            }
        }
        sb.append("</div></html>");

        final Boolean resolvedMyReady = myServerReady;
        SwingUtilities.invokeLater(() -> {
            if (isTransitioning) return;
            statusLabel.setText(sb.toString());
            if (resolvedMyReady != null) {
                isReady = resolvedMyReady;
                updateReadyButtonUI();
            }
        });
    }

    private void updateReadyButtonUI() {
        if (isReady) {
            readyBtn.setText("준비 취소");
            readyBtn.setBackground(new Color(220, 80, 80));
        } else {
            readyBtn.setText("준비 완료");
            readyBtn.setBackground(new Color(40, 140, 230));
        }
    }
}