package client;

import common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameFrame extends JFrame {
    private DragLayerPanel dragLayer;
    private MainBackgroundPanel mainBgPanel;
    private Board topBoard;
    private Board bottomBoard;
    private BulletLayerPanel bulletLayer;

    // 타이머 및 SP 관련 변수
    private JLabel timerLabel;
    private JLabel spLabel;
    private Timer gameTimer;

    private int elapsedSeconds = 0;
    private int mySp = 0;
    private int mySummonCost = 0;
    private boolean spShortShown = false;

    public GameFrame() {
        setTitle("RandomTowerDefense");
        setSize(500, 880);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 서버 메시지 수신 리스너 등록
        NetworkClient.getInstance()
                .setMessageListener(this::handleServerMessage);

        // 레이어드 패널 생성
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);

        mainBgPanel = new MainBackgroundPanel();
        mainBgPanel.setBounds(0, 0, 500, 880);

        bulletLayer = new BulletLayerPanel();
        bulletLayer.setBounds(0, 0, 500, 880);

        dragLayer = new DragLayerPanel();
        dragLayer.setBounds(0, 0, 500, 880);

     // 위쪽 보드: 상대방
        topBoard = new Board(false);
        topBoard.setBulletLayer(bulletLayer);
        topBoard.setBackgroundPanel(mainBgPanel, false);

        // 아래쪽 보드: 나
        bottomBoard = new Board(true);
        bottomBoard.setBulletLayer(bulletLayer);
        bottomBoard.setDragLayer(dragLayer);
        bottomBoard.setBackgroundPanel(mainBgPanel, true);

        // 내 보드 + 소환 버튼 패널
        JPanel bottomAreaPanel = new JPanel(new BorderLayout());
        bottomAreaPanel.setOpaque(false);

        JButton summonBtn = new JButton("소환");
        summonBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        summonBtn.setPreferredSize(new Dimension(120, 40));

        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(summonBtn);

        // 실제 소환과 SP 차감은 서버에서 처리
        summonBtn.addActionListener(e ->
                NetworkClient.getInstance().send(Protocol.SUMMON)
        );

        bottomAreaPanel.add(bottomBoard, BorderLayout.CENTER);
        bottomAreaPanel.add(btnPanel, BorderLayout.SOUTH);

        // 배경 패널에 보드 배치
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(80, 0, 80, 0);

        gbc.gridy = 0;
        mainBgPanel.add(topBoard, gbc);

        gbc.gridy = 1;
        mainBgPanel.add(bottomAreaPanel, gbc);

        layeredPane.add(mainBgPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(bulletLayer, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(dragLayer, JLayeredPane.DRAG_LAYER);

        // 우측 상단 시간 표시
        timerLabel = new JLabel("00:00", JLabel.RIGHT);
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        timerLabel.setForeground(new Color(50, 50, 50));
        timerLabel.setBounds(350, 15, 120, 30);
        layeredPane.add(timerLabel, JLayeredPane.MODAL_LAYER);

        // 우측 상단 현재 SP 표시
        spLabel = new JLabel("SP: -", JLabel.RIGHT);
        spLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        spLabel.setForeground(new Color(30, 110, 200));
        spLabel.setBounds(320, 45, 150, 30);
        layeredPane.add(spLabel, JLayeredPane.MODAL_LAYER);

        // 클라이언트에서는 시간만 갱신
        // SP는 서버에서 3초마다 50씩 지급
        gameTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                elapsedSeconds++;

                int minutes = elapsedSeconds / 60;
                int seconds = elapsedSeconds % 60;

                timerLabel.setText(
                        String.format("%02d:%02d", minutes, seconds)
                );
            }
        });
        gameTimer.start();

        add(layeredPane);
        setVisible(true);
    }

    private void updateSpLabel() {
        // 소환 비용 표시 제거: 현재 보유 SP만 표시
        spLabel.setText("SP: " + mySp);
        spLabel.repaint();
    }

    // 서버 메시지 수신 및 보드 동기화
    private void handleServerMessage(String line) {
        System.out.println("[GameFrame 수신] " + line);

        if (line.startsWith(Protocol.DICE_STATE + " ")) {
            String payload = line.substring(
                    Protocol.DICE_STATE.length() + 1
            ).strip();

            SwingUtilities.invokeLater(
                    () -> parseAndApplyDiceState(payload)
            );

        } else if (line.startsWith(Protocol.MONSTER_STATE + " ")) {
            String[] fields = line.split("\\s+", 6);
            if (fields.length < 6) return;
            try {
                bulletLayer.finishAttacks(Integer.parseInt(fields[1]), Integer.parseInt(fields[4]));
                mainBgPanel.applyMonsterState(line, NetworkClient.getInstance().getMyPlayerId());
            } catch (NumberFormatException ignored) {
                // 잘못된 몬스터 상태는 무시한다.
            }
        } else if (line.startsWith(Protocol.ATTACK + " ")) {
            String[] fields = line.split("\\s+");
            if (fields.length != 5) return;
            try {
                int playerId = Integer.parseInt(fields[1]);
                Board board = playerId == NetworkClient.getInstance().getMyPlayerId() ? bottomBoard : topBoard;
                board.animateAttack(playerId, Integer.parseInt(fields[2]),
                        Integer.parseInt(fields[3]), Integer.parseInt(fields[4]));
            } catch (NumberFormatException ignored) {
                // 잘못된 공격 알림은 표시하지 않는다.
            }
        } else if (line.startsWith(Protocol.ERROR + " ")) {
            String errMsg = line.substring(
                    Protocol.ERROR.length() + 1
            ).strip();

            if ("GAME_INTERRUPTED".equals(errMsg)) bulletLayer.clearAttacks();

            if ("EMPTY_CELL".equals(errMsg)) {
                System.out.println("[무시됨] 빈 칸 이동 불가");
                return;
            }

            if ("NOT_ENOUGH_SP".equals(errMsg)) {
                if (!spShortShown) {
                    spShortShown = true;

                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(
                                    this,
                                    "SP가 부족합니다.\n"
                                            + "SP가 충전되면 다시 소환할 수 있습니다."
                            )
                    );
                }
                return;
            }

            if ("FIELD_FULL".equals(errMsg)) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(
                                this,
                                "보드가 가득 찼습니다. 합성으로 칸을 비워주세요."
                        )
                );
                return;
            }

            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(
                            this,
                            "게임 에러: " + errMsg
                    )
            );
        }
    }

    private void parseAndApplyDiceState(String data) {
        String[] tokens = data.split("\\s+");

        if (tokens.length < 21) {
            System.out.println(
                    "DICE_STATE 토큰 개수 부족: " + tokens.length
            );
            return;
        }

        try {
            int playerId = Integer.parseInt(tokens[0]);
            int sp = Integer.parseInt(tokens[1]);
            int summonCost = Integer.parseInt(tokens[2]);

            String[] cellData = new String[15];

            for (int i = 0; i < 15; i++) {
                cellData[i] = tokens[6 + i];
            }

            int myId = NetworkClient.getInstance().getMyPlayerId();

            SwingUtilities.invokeLater(() -> {
                if (playerId == myId) {
                    mySp = sp;
                    mySummonCost = summonCost;

                    // 실제로 소환 가능한 SP가 충전되면 안내 플래그 해제
                    if (mySp >= mySummonCost) {
                        spShortShown = false;
                    }

                    updateSpLabel();

                    bottomBoard.updateBoardState(
                            sp, summonCost, cellData
                    );
                    bottomBoard.repaint();
                } else {
                    topBoard.updateBoardState(
                            sp, summonCost, cellData
                    );
                    topBoard.repaint();
                }
            });

        } catch (Exception e) {
            System.out.println(
                    "DICE_STATE 파싱 오류: " + e.getMessage()
            );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameFrame::new);
    }
}