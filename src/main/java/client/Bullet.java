package client;

import common.Protocol;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.function.Supplier;

public class Bullet {
    private double x, y;
    private final double startX, startY;
    private final long launchedAt = System.nanoTime();
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
        this.startX = this.x = startX;
        this.startY = this.y = startY;
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
            double fraction = Math.min(1.0, (System.nanoTime() - launchedAt)
                    / (Protocol.GAME_TICK_MS * 1_000_000.0));
            x = startX + (targetX - startX) * fraction;
            y = startY + (targetY - startY) * fraction;
            return;
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

    Point2D.Double impactPoint() {
        Point2D.Double point = targetSupplier.get();
        return point == null ? new Point2D.Double(targetX, targetY) : point;
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
