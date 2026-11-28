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
    private record Flight(Bullet bullet, int playerId, int launchTick) {}
    private record Impact(Point2D.Double point, long expiresAt) {}

    private final List<Impact> impacts = new ArrayList<>();

    private final List<Flight> bullets = new ArrayList<>();
    private final Timer gameTimer;

    public BulletLayerPanel() {
        setOpaque(false);
        setLayout(null);

        gameTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Iterator<Flight> iterator = bullets.iterator();
                while (iterator.hasNext()) {
                    Bullet bullet = iterator.next().bullet();
                    bullet.update();
                    if (!bullet.isActive()) {
                        iterator.remove();
                    }
                }
                impacts.removeIf(impact -> System.nanoTime() >= impact.expiresAt());
                repaint();
            }
        });
        gameTimer.start();
    }

    public void addBullet(double startX, double startY, double targetX, double targetY) {
        bullets.add(new Flight(new Bullet(startX, startY, targetX, targetY), -1, -1));
        repaint();
    }

    void addHomingBullet(double startX, double startY, Point2D.Double target,
                         Supplier<Point2D.Double> targetSupplier, int playerId, int tick) {
        bullets.add(new Flight(new Bullet(startX, startY, target.x, target.y, targetSupplier), playerId, tick));
        repaint();
    }

    void finishAttacks(int playerId, int tick) {
        Point2D.Double impactPoint = null;
        Iterator<Flight> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            Flight flight = iterator.next();
            if (flight.playerId() != playerId || flight.launchTick() >= tick) continue;
            if (impactPoint == null) impactPoint = flight.bullet().impactPoint();
            iterator.remove();
        }
        if (impactPoint != null) impacts.add(new Impact(impactPoint, System.nanoTime() + 140_000_000L));
        repaint();
    }

    void clearAttacks() {
        bullets.clear();
        impacts.clear();
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

        for (Flight flight : bullets) {
            flight.bullet().draw(g2d);
        }
        g2d.setColor(new Color(237, 102, 22, 210));
        g2d.setStroke(new BasicStroke(3));
        for (Impact impact : impacts) {
            g2d.drawOval((int) impact.point().x - 12, (int) impact.point().y - 12, 24, 24);
        }
    }
}