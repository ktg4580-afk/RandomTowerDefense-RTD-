package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntPredicate;
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

    public void addHomingBullet(
            double startX,
            double startY,
            Supplier<Point2D.Double> targetSupplier,
            IntPredicate aliveCheck,
            Bullet.HitListener hitListener,
            double fallbackX,
            double fallbackY,
            int damage,
            int targetMonsterId
    ) {
        bullets.add(new Bullet(
                startX,
                startY,
                targetSupplier,
                aliveCheck,
                hitListener,
                fallbackX,
                fallbackY,
                damage,
                targetMonsterId
        ));
        repaint();
    }

    public int getIncomingDamageForMonster(int monsterId) {
        if (monsterId < 0) return 0;
        int sum = 0;
        for (Bullet bullet : bullets) {
            if (bullet.isActive() && bullet.getTargetMonsterId() == monsterId) {
                sum += bullet.getDamage();
            }
        }
        return sum;
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