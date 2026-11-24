package client;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainBackgroundPanel extends JPanel {
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

        // 2. 패널 위에 존재하는 모든 총알들 그리기
        for (Bullet bullet : bullets) {
            bullet.draw(g2d);
        }
    }
}