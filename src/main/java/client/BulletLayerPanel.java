package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class BulletLayerPanel extends JPanel {
    private final List<Bullet> bullets = new ArrayList<>();
    private final Timer gameTimer;

    public BulletLayerPanel() {
        setOpaque(false);
        setLayout(null);

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

    void addHomingBullet(double startX, double startY, Point2D.Double target,
                         Supplier<Point2D.Double> targetSupplier) {
        bullets.add(new Bullet(startX, startY, target.x, target.y, targetSupplier));
        repaint();
    }

    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Bullet bullet : bullets) {
            bullet.draw(g2d);
        }
    }
}