package client;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

public class Bullet {
    @FunctionalInterface
    public interface HitListener {
        void onHit(int monsterId, int damage);
    }

    private double x, y;
    private double targetX, targetY;
    private final Supplier<Point2D.Double> targetSupplier;
    private final IntPredicate aliveCheck;
    private final HitListener hitListener;
    private final int damage;
    private final int targetMonsterId;
    private double speed = 4.0;
    private int lifeTime = 300;
    private boolean active = true;

    public Bullet(double startX, double startY, double targetX, double targetY) {
        this(startX, startY, null, null, null, targetX, targetY, 1, -1);
    }

    public Bullet(
            double startX,
            double startY,
            Supplier<Point2D.Double> targetSupplier,
            IntPredicate aliveCheck,
            HitListener hitListener,
            double fallbackX,
            double fallbackY,
            int damage,
            int targetMonsterId
    ) {
        this.x = startX;
        this.y = startY;
        this.targetSupplier = targetSupplier;
        this.aliveCheck = aliveCheck;
        this.hitListener = hitListener;
        this.targetX = fallbackX;
        this.targetY = fallbackY;
        this.damage = Math.max(1, damage);
        this.targetMonsterId = targetMonsterId;
    }

    public void update() {
        if (!active) return;

        lifeTime--;
        if (lifeTime <= 0) {
            active = false;
            return;
        }

        if (targetMonsterId >= 0 && aliveCheck != null && !aliveCheck.test(targetMonsterId)) {
            active = false;
            return;
        }

        if (targetSupplier != null) {
            Point2D.Double liveTarget = targetSupplier.get();
            if (liveTarget == null) {
                active = false;
                return;
            }
            this.targetX = liveTarget.x;
            this.targetY = liveTarget.y;
        }

        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance <= speed) {
            x = targetX;
            y = targetY;
            if (hitListener != null && targetMonsterId >= 0) {
                hitListener.onHit(targetMonsterId, damage);
            }
            active = false;
        } else {
            x += (dx / distance) * speed;
            y += (dy / distance) * speed;
        }
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

    public int getDamage() {
        return damage;
    }

    public int getTargetMonsterId() {
        return targetMonsterId;
    }
}