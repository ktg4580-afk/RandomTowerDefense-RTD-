package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BulletLayerPanel extends JPanel {
    private List<Bullet> bullets = new ArrayList<>();
    private Timer gameTimer;

    public BulletLayerPanel() {
        setOpaque(false); // 배경을 투명하게 설정하여 아래 보드가 다 보이게 함
        setLayout(null);  // 절대 좌표 배치를 위해 null 레이아웃 사용

        // 60 FPS 총알 이동 갱신 타이머
        gameTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Iterator<Bullet> iterator = bullets.iterator();
                while (iterator.hasNext()) {
                    Bullet bullet = iterator.next();
                    bullet.update();
                    if (!bullet.isActive()) {
                        iterator.remove();
                    }
                }
                repaint();
            }
        });
        gameTimer.start();
    }

    public void addBullet(double startX, double startY, double targetX, double targetY) {
        bullets.add(new Bullet(startX, startY, targetX, targetY));
        repaint();
    }

    @Override
    public boolean contains(int x, int y) {
        // The animation layer must let the board receive mouse input.
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 모든 활성 총알 그리기
        for (Bullet bullet : bullets) {
            bullet.draw(g2d);
        }
    }
}