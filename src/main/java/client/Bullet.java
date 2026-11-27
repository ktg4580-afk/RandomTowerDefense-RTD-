package client;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.function.Supplier;

public class Bullet {
    private double x, y;
    private double targetX, targetY;
    private final Supplier<Point2D.Double> targetSupplier;
    private static final double SPEED = 4.0;
    private int lifeTime = 300;
    private boolean active = true;

    public Bullet(double startX, double startY, double targetX, double targetY) {
        this(startX, startY, targetX, targetY, null);
    }

    Bullet(double startX, double startY, double targetX, double targetY,
           Supplier<Point2D.Double> targetSupplier) {
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetSupplier = targetSupplier;
    }

    public void update() {
        if (!active) return;
        if (--lifeTime <= 0) {
            active = false;
            return;
        }

        if (targetSupplier != null) {
            Point2D.Double liveTarget = targetSupplier.get();
            // 표적이 사라지면 마지막으로 확인한 위치까지 이동한다.
            if (liveTarget != null) {
                targetX = liveTarget.x;
                targetY = liveTarget.y;
            }
        }

        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);
        if (distance <= SPEED) {
            active = false;
            return;
        }
        x += dx / distance * SPEED;
        y += dy / distance * SPEED;
    }

    public void draw(Graphics2D g2d) {
        if (!active) return;

        g2d.setColor(Color.BLACK);
        g2d.fillOval((int) x - 6, (int) y - 6, 12, 12);

        g2d.setColor(Color.DARK_GRAY);
        g2d.drawOval((int) x - 6, (int) y - 6, 12, 12);
    }

    public boolean isActive() {
        return active;
    }
}
