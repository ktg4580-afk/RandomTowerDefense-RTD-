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
    private DragLayerPanel dragLayer;
    private MainBackgroundPanel backgroundPanel;
    private boolean ownLane = true;

    public void setBulletLayer(BulletLayerPanel bulletLayer) {
        this.bulletLayer = bulletLayer;
    }

    public void setDragLayer(DragLayerPanel dragLayer) {
        this.dragLayer = dragLayer;
    }

    public void setBackgroundPanel(MainBackgroundPanel backgroundPanel, boolean ownLane) {
        this.backgroundPanel = backgroundPanel;
        this.ownLane = ownLane;
    }

    public Board(boolean allowMerge) {
        setBackground(Color.WHITE);
        setLayout(new FlowLayout(FlowLayout.CENTER, 5, 12));

        JPanel gridPanel = new JPanel(new GridLayout(ROWS, COLS, 5, 5));
        gridPanel.setBackground(Color.WHITE);

        tiles = new BoardTile[ROWS][COLS];

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                BoardTile tile = new BoardTile();
                tiles[r][c] = tile;
                gridPanel.add(tile);
                if (allowMerge) tile.setupMouseListeners(this);
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
                    if (tile.hasDice()) {
                        tile.clearDice();
                    }
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

                    if (tile.matchesDice(type, level)) {
                        continue;
                    }

                    tile.clearDice();

                    if ("BASIC".equals(type)) {
                        BasicDice dice = new BasicDice(this.bulletLayer, this.backgroundPanel, this.ownLane, tile, level);
                        tile.setBasicDice(dice);
                    } else if ("RAPID".equals(type)) {
                        RapidDice dice = new RapidDice(this.bulletLayer, this.backgroundPanel, this.ownLane, tile, level);
                        tile.setRapidDice(dice);
                    } else if ("SLOW".equals(type)) {
                        SlowDice dice = new SlowDice(this.bulletLayer, this.backgroundPanel, this.ownLane, tile, level);
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
            RapidDice rapidDice = new RapidDice(bulletLayer, this.backgroundPanel, this.ownLane, targetTile, 1);
            targetTile.setRapidDice(rapidDice);
        } else if (diceType == 1) {
            SlowDice slowDice = new SlowDice(bulletLayer, this.backgroundPanel, this.ownLane, targetTile, 1);
            targetTile.setSlowDice(slowDice);
        } else {
            BasicDice basicDice = new BasicDice(bulletLayer, this.backgroundPanel, this.ownLane, targetTile, 1);
            targetTile.setBasicDice(basicDice);
        }

        return true;
    }

    private BoardTile getTileAt(Point boardPoint) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                BoardTile tile = tiles[r][c];
                Point tilePoint = SwingUtilities.convertPoint(this, boardPoint, tile);
                if (tile.contains(tilePoint)) return tile;
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

    private void requestMerge(BoardTile source, BoardTile target) {
        int fromIndex = getTileIndex(source);
        int toIndex = getTileIndex(target);

        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex || !target.hasDice()) {
            return;
        }

        NetworkClient.getInstance().send(Protocol.MERGE + " " + fromIndex + " " + toIndex);
    }

    public static class BoardTile extends JPanel {
        private JLabel imageLabel;
        private BasicDice currentBasicDice;
        private RapidDice currentRapidDice;
        private SlowDice currentSlowDice;

        private boolean dragging;

        public BoardTile() {
            setBackground(new Color(245, 245, 245));
            setBorder(new LineBorder(new Color(200, 200, 200), 2));
            setLayout(new BorderLayout());

            imageLabel = new JLabel();
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            add(imageLabel, BorderLayout.CENTER);
        }

        public void removeDiceForMove() {
            this.currentBasicDice = null;
            this.currentRapidDice = null;
            this.currentSlowDice = null;
            imageLabel.setIcon(null);
            revalidate();
            repaint();
        }

        private ImageIcon getCurrentDiceIcon() {
            if (currentBasicDice != null) return currentBasicDice.getDiceIcon();
            if (currentRapidDice != null) return currentRapidDice.getDiceIcon();
            if (currentSlowDice != null) return currentSlowDice.getDiceIcon();
            return null;
        }

        private void setupMouseListeners(Board board) {
            java.awt.event.MouseAdapter listener = new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent event) {
                    if (!SwingUtilities.isLeftMouseButton(event) || !hasDice()) return;
                    dragging = true;
                    setBorder(new LineBorder(new Color(70, 130, 180), 2));

                    if (board.dragLayer != null) {
                        Point pt = SwingUtilities.convertPoint(
                                event.getComponent(), event.getPoint(), board.dragLayer
                        );
                        board.dragLayer.startDrag(getCurrentDiceIcon(), pt);
                    }
                }

                @Override
                public void mouseDragged(java.awt.event.MouseEvent event) {
                    if (!SwingUtilities.isLeftMouseButton(event) || !dragging) return;
                    if (board.dragLayer != null) {
                        Point pt = SwingUtilities.convertPoint(
                                event.getComponent(), event.getPoint(), board.dragLayer
                        );
                        board.dragLayer.updateDragPosition(pt);
                    }
                }

                @Override
                public void mouseReleased(java.awt.event.MouseEvent event) {
                    if (!SwingUtilities.isLeftMouseButton(event) || !dragging) return;
                    dragging = false;
                    setBorder(new LineBorder(new Color(200, 200, 200), 2));

                    if (board.dragLayer != null) {
                        board.dragLayer.stopDrag();
                    }

                    Point dropPoint = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), board);
                    BoardTile target = board.getTileAt(dropPoint);
                    if (hasDice() && target != null) {
                        board.requestMerge(BoardTile.this, target);
                    }
                }
            };
            addMouseListener(listener);
            addMouseMotionListener(listener);
            imageLabel.addMouseListener(listener);
            imageLabel.addMouseMotionListener(listener);
        }

        public boolean hasDice() {
            return currentBasicDice != null || currentRapidDice != null || currentSlowDice != null;
        }

        public boolean matchesDice(String type, int level) {
            if ("BASIC".equals(type)) {
                return currentBasicDice != null && currentBasicDice.level == level;
            } else if ("RAPID".equals(type)) {
                return currentRapidDice != null && currentRapidDice.level == level;
            } else if ("SLOW".equals(type)) {
                return currentSlowDice != null && currentSlowDice.level == level;
            }
            return false;
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