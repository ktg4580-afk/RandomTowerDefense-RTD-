package client;

import common.Protocol;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board extends JPanel {
    private final int ROWS = 3;
    private final int COLS = 5;
    private BoardTile[][] tiles;

    private BulletLayerPanel bulletLayer;

    public void setBulletLayer(BulletLayerPanel bulletLayer) {
        this.bulletLayer = bulletLayer;
    }

    public Board(String playerTitle) {
        setBackground(Color.WHITE);
        setLayout(new FlowLayout(FlowLayout.CENTER, 5, 12));

        JPanel gridPanel = new JPanel(new GridLayout(ROWS, COLS, 5, 5));
        gridPanel.setBackground(Color.WHITE);

        tiles = new BoardTile[ROWS][COLS];

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                tiles[r][c] = new BoardTile();
                gridPanel.add(tiles[r][c]);
            }
        }

        add(gridPanel);
    }

    public void updateBoardState(int sp, int summonCost, String[] cellData) {
        int index = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (index >= cellData.length) break;
                String cell = cellData[index++];
                BoardTile tile = tiles[r][c];

                if ("-".equals(cell)) {
                    tile.clearDice();
                } else {
                    String[] parts = cell.split(":");
                    String type = parts[0];
                    int level = 1;
                    if (parts.length > 1) {
                        try {
                            level = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException ignored) {
                            level = 1;
                        }
                    }

                    // 합성/이동으로 종류나 레벨이 바뀔 수 있으므로 기존 주사위 정리 후 새로 반영
                    tile.clearDice();

                    if ("BASIC".equals(type)) {
                        BasicDice dice = new BasicDice(this.bulletLayer, tile, level);
                        tile.setBasicDice(dice);
                    } else if ("RAPID".equals(type)) {
                        RapidDice dice = new RapidDice(this.bulletLayer, tile, level);
                        tile.setRapidDice(dice);
                    } else if ("SLOW".equals(type)) {
                        SlowDice dice = new SlowDice(this.bulletLayer, tile, level);
                        tile.setSlowDice(dice);
                    }
                }
            }
        }
        repaint();
    }

    public boolean spawnRandomDice(BulletLayerPanel bulletLayer) {
        List<Point> emptyTiles = new ArrayList<>();

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (!tiles[r][c].hasDice()) {
                    emptyTiles.add(new Point(r, c));
                }
            }
        }

        if (emptyTiles.isEmpty()) {
            return false;
        }

        Point p = emptyTiles.get(new Random().nextInt(emptyTiles.size()));
        BoardTile targetTile = tiles[p.x][p.y];

        int diceType = new Random().nextInt(3);

        if (diceType == 0) {
            RapidDice rapidDice = new RapidDice(bulletLayer, targetTile, 1);
            targetTile.setRapidDice(rapidDice);
        } else if (diceType == 1) {
            SlowDice slowDice = new SlowDice(bulletLayer, targetTile, 1);
            targetTile.setSlowDice(slowDice);
        } else {
            BasicDice basicDice = new BasicDice(bulletLayer, targetTile, 1);	
            targetTile.setBasicDice(basicDice);
        }

        return true;
    }

    public BoardTile getTileAtScreenPoint(Point screenPoint) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Rectangle bounds = tiles[r][c].getBounds();
                bounds.setLocation(tiles[r][c].getLocationOnScreen());
                if (bounds.contains(screenPoint)) {
                    return tiles[r][c];
                }
            }
        }
        return null;
    }

    private int getTileIndex(BoardTile target) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (tiles[r][c] == target) {
                    return r * COLS + c;
                }
            }
        }
        return -1;
    }

    public void processMergeOrMove(BoardTile source, BoardTile target) {
        int fromIndex = getTileIndex(source);
        int toIndex = getTileIndex(target);

        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) {
            return;
        }

        // 서버 권위 동기화: 합성/이동 요청을 서버로 전송하고,
        // 실제 보드 상태 반영은 서버가 브로드캐스트하는 /DICE_STATE를 통해 처리합니다.
        NetworkClient.getInstance().send(Protocol.MERGE + " " + fromIndex + " " + toIndex);
    }

    public static class BoardTile extends JPanel {
        private JLabel imageLabel;
        private BasicDice currentBasicDice;
        private RapidDice currentRapidDice;
        private SlowDice currentSlowDice;

        private static BoardTile draggedTile = null;

        public BoardTile() {
            setBackground(new Color(245, 245, 245));
            setBorder(new LineBorder(new Color(200, 200, 200), 2));
            setLayout(new BorderLayout());

            imageLabel = new JLabel();
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            add(imageLabel, BorderLayout.CENTER);

            setupMouseListeners();
        }

        public void removeDiceForMove() {
            this.currentBasicDice = null;
            this.currentRapidDice = null;
            this.currentSlowDice = null;
            imageLabel.setIcon(null);
            revalidate();
            repaint();
        }

        private void setupMouseListeners() {
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    if (!hasDice()) return;
                    draggedTile = BoardTile.this;
                    setBorder(new LineBorder(new Color(70, 130, 180), 3));
                }

                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    if (draggedTile == null) return;

                    Point dropPoint = e.getLocationOnScreen();
                    Board board = (Board) SwingUtilities.getAncestorOfClass(Board.class, BoardTile.this);
                    if (board != null) {
                        BoardTile targetTile = board.getTileAtScreenPoint(dropPoint);

                        if (targetTile != null && targetTile != draggedTile) {
                            board.processMergeOrMove(draggedTile, targetTile);
                        }
                    }

                    draggedTile.setBorder(new LineBorder(new Color(200, 200, 200), 2));
                    draggedTile = null;
                }
            });
        }

        public boolean hasDice() {
            return currentBasicDice != null || currentRapidDice != null || currentSlowDice != null;
        }

        public void setBasicDice(BasicDice basicDice) {
            this.currentBasicDice = basicDice;
            this.currentRapidDice = null;
            this.currentSlowDice = null;
            imageLabel.setIcon(basicDice != null ? basicDice.getDiceIcon() : null);
            revalidate();
            repaint();
        }

        public void setRapidDice(RapidDice rapidDice) {
            this.currentRapidDice = rapidDice;
            this.currentBasicDice = null;
            this.currentSlowDice = null;
            imageLabel.setIcon(rapidDice != null ? rapidDice.getDiceIcon() : null);
            revalidate();
            repaint();
        }

        public void setSlowDice(SlowDice slowDice) {
            this.currentSlowDice = slowDice;
            this.currentBasicDice = null;
            this.currentRapidDice = null;
            imageLabel.setIcon(slowDice != null ? slowDice.getDiceIcon() : null);
            revalidate();
            repaint();
        }

        public void clearDice() {
            if (currentBasicDice != null) currentBasicDice.stopAttack();
            if (currentRapidDice != null) currentRapidDice.stopAttack();
            if (currentSlowDice != null) currentSlowDice.stopAttack();

            removeDiceForMove();
        }

        public BasicDice getCurrentBasicDice() { return currentBasicDice; }
        public RapidDice getCurrentRapidDice() { return currentRapidDice; }
        public SlowDice getCurrentSlowDice() { return currentSlowDice; }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(60, 60);
        }
    }
}