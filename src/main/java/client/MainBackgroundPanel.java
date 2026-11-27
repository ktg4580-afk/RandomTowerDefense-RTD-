package client;

import common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class MainBackgroundPanel extends JPanel {
    private record Monster(int id, String type, int hp, int maxHp, int position) {}

    record FrontMonsterTarget(int monsterId, Point2D.Double point) {}

    private List<Monster> ownMonsters = List.of();
    private List<Monster> opponentMonsters = List.of();

    public MainBackgroundPanel() {
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
    }

    void applyMonsterState(String line, int myPlayerId) {
        String[] fields = line.split("\\s+");
        if (fields.length < 7) return;
        try {
            int playerId = Integer.parseInt(fields[1]);
            int count = Integer.parseInt(fields[6]);
            if (count != fields.length - 7) return;

            List<Monster> monsters = new ArrayList<>(count);
            for (int i = 7; i < fields.length; i++) {
                String[] monster = fields[i].split(":");
                if (monster.length != 5) return;
                int id = Integer.parseInt(monster[0]);
                int hp = Integer.parseInt(monster[2]);
                int maxHp = Integer.parseInt(monster[3]);
                int position = Integer.parseInt(monster[4]);
                if (hp <= 0 || hp > maxHp || position < 0 || position >= Protocol.MONSTER_PATH_LENGTH) return;
                monsters.add(new Monster(id, monster[1], hp, maxHp, position));
            }
            if (playerId == myPlayerId) ownMonsters = monsters;
            else opponentMonsters = monsters;
            repaint();
        } catch (NumberFormatException ignored) {
            // Keep the last valid snapshot.
        }
    }

    FrontMonsterTarget getFrontMonsterTarget(boolean ownLane) {
        List<Monster> monsters = ownLane ? ownMonsters : opponentMonsters;
        Monster front = null;
        for (Monster monster : monsters) {
            if (front == null || monster.position() > front.position()) front = monster;
        }
        return front == null ? null : new FrontMonsterTarget(front.id(), monsterPoint(front, ownLane));
    }

    Point2D.Double getMonsterPoint(boolean ownLane, int monsterId) {
        List<Monster> monsters = ownLane ? ownMonsters : opponentMonsters;
        for (Monster monster : monsters) {
            if (monster.id() == monsterId) return monsterPoint(monster, ownLane);
        }
        return null;
    }

    private static final int PATH_WIDTH = 55;

    private Point2D.Double monsterPoint(Monster monster, boolean ownLane) {
        int left = PATH_WIDTH / 2;
        int right = Math.max(left, getWidth() - left);
        int startY = ownLane ? getHeight() - 65 : 65;
        int centerY = getHeight() / 2 + (ownLane ? 14 : -14);
        int verticalLength = Math.abs(centerY - startY);
        double routeLength = 2 * verticalLength + right - left;

        double distance = routeLength * monster.position() / Protocol.MONSTER_PATH_LENGTH;
        double x = left + Math.max(0, Math.min(right - left, distance - verticalLength));
        double vertical = Math.min(distance, routeLength - distance);
        double y = startY + (ownLane ? -1 : 1) * Math.min(verticalLength, vertical);
        return new Point2D.Double(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // 1. H모양 길 배경 그리기
        g2d.setColor(new Color(235, 235, 235));
        int pathWidth = PATH_WIDTH;
        int leftX = 0;
        int rightX = width - pathWidth;
        int centerY = height / 2 - pathWidth / 2;

        g2d.fillRect(leftX, 0, pathWidth, height);
        g2d.fillRect(rightX, 0, pathWidth, height);
        g2d.fillRect(leftX, centerY, rightX - leftX + pathWidth, pathWidth);

        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(2));

        g2d.drawRect(leftX, 0, pathWidth, height);
        g2d.drawRect(rightX, 0, pathWidth, height);

        g2d.setColor(new Color(235, 235, 235));
        g2d.fillRect(leftX + pathWidth, centerY + 1, rightX - leftX - pathWidth, pathWidth - 1);

        g2d.setColor(new Color(200, 200, 200));
        g2d.drawLine(leftX + pathWidth, centerY, rightX, centerY);
        g2d.drawLine(leftX + pathWidth, centerY + pathWidth, rightX, centerY + pathWidth);

        drawMonsters(g2d, opponentMonsters, false);
        drawMonsters(g2d, ownMonsters, true);
    }

    private void drawMonsters(Graphics2D graphics, List<Monster> monsters, boolean ownLane) {
        for (Monster monster : monsters) {
            Point2D.Double point = monsterPoint(monster, ownLane);
            int x = (int) point.x;
            int y = (int) point.y;

            graphics.setColor(monster.type().equals("BASIC") ? new Color(50, 96, 160) : new Color(164, 57, 42));
            graphics.fillOval(x - 9, y - 9, 18, 18);
            graphics.setColor(Color.DARK_GRAY);
            graphics.fillRect(x - 10, y - 14, 20, 3);
            graphics.setColor(new Color(47, 164, 95));
            graphics.fillRect(x - 10, y - 14, (int) (20L * monster.hp() / monster.maxHp()), 3);
        }
    }
}