package client;

import java.awt.*;

public class Bullet {
    private double x, y;          
    private double targetX, targetY; 
    private double speed = 4.0;   // 💡 속도를 낮춰서(기존 15.0 -> 4.0) 천천히 오래 날아가게 조절
    private int lifeTime = 300;   // 💡 생명 주기 프레임 (60 FPS 기준 약 3초 = 180프레임)
    private boolean active = true; 

    public Bullet(double startX, double startY, double targetX, double targetY) {
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;	
        this.targetY = targetY;
    }

    public void update() {
        if (!active) return;

        // 시간(프레임)이 지나면 자동 소멸
        lifeTime--;
        if (lifeTime <= 0) {
            active = false;
            return;
        }

        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance <= speed) {
            active = false; // 목표 도달 시 소멸
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
}