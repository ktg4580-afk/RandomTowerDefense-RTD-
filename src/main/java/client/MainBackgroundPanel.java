package client;

import common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainBackgroundPanel extends JPanel {
    private record Monster(String type, int hp, int maxHp, int position) {}

    private List<Monster> ownMonsters = List.of();
    private List<Monster> opponentMonsters = List.of();

    private List<Bullet> bullets = new ArrayList<>();
    private Timer gameTimer;

    public MainBackgroundPanel() {
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);

        // 60 FPS 주사위/총알 렌더링 루프 (약 16ms 마다 실행)
        gameTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 활성화된 총알들 위치 업데이트
                Iterator<Bullet> iterator = bullets.iterator();
                while (iterator.hasNext()) {
                    Bullet bullet = iterator.next();
                    bullet.update();
                    if (!bullet.isActive()) {
                        iterator.remove(); // 도착한 총알 제거
                    }
                }
                repaint(); // 화면 다시 그리기
            }
        });
        gameTimer.start();
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
                int hp = Integer.parseInt(monster[2]);
                int maxHp = Integer.parseInt(monster[3]);
                int position = Integer.parseInt(monster[4]);
                if (hp <= 0 || hp > maxHp || position < 0 || position >= Protocol.MONSTER_PATH_LENGTH) return;
                monsters.add(new Monster(monster[1], hp, maxHp, position));
            }
            if (playerId == myPlayerId) ownMonsters = monsters;
            else opponentMonsters = monsters;
            repaint();
        } catch (NumberFormatException ignored) {
            // Keep the last valid snapshot.
        }
    }

    // 외부(보드나 프레임)에서 총알을 생성해 추가하는 메서드
    public void addBullet(double startX, double startY, double targetX, double targetY) {
        bullets.add(new Bullet(startX, startY, targetX, targetY));
        repaint();
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
        int pathWidth = 55;
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

        drawMonsters(g2d, opponentMonsters, false, pathWidth);
        drawMonsters(g2d, ownMonsters, true, pathWidth);

        // 2. 패널 위에 존재하는 모든 총알들 그리기
        for (Bullet bullet : bullets) {
            bullet.draw(g2d);
        }
    }

    private void drawMonsters(Graphics2D graphics, List<Monster> monsters, boolean ownLane, int pathWidth) {
        int left = pathWidth / 2;
        int right = getWidth() - left;
        int startY = ownLane ? getHeight() - 65 : 65;
        int centerY = getHeight() / 2 + (ownLane ? 14 : -14);
        int verticalLength = Math.abs(centerY - startY);
        double routeLength = 2 * verticalLength + right - left;
        for (Monster monster : monsters) {
            double distance = routeLength * monster.position() / Protocol.MONSTER_PATH_LENGTH;
            int x = left + (int) Math.max(0, Math.min(right - left, distance - verticalLength));
            double vertical = Math.min(distance, routeLength - distance);
            int y = startY + (ownLane ? -1 : 1) * (int) Math.min(verticalLength, vertical);

            graphics.setColor(monster.type().equals("BASIC") ? new Color(50, 96, 160) : new Color(164, 57, 42));
            graphics.fillOval(x - 9, y - 9, 18, 18);
            graphics.setColor(Color.DARK_GRAY);
            graphics.fillRect(x - 10, y - 14, 20, 3);
            graphics.setColor(new Color(47, 164, 95));
            graphics.fillRect(x - 10, y - 14, (int) (20L * monster.hp() / monster.maxHp()), 3);
        }
    }
}