package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public abstract class Dice {
    protected ImageIcon diceIcon;
    protected Timer attackTimer;
    protected BulletLayerPanel bulletLayer; 
    protected Board.BoardTile hostTile;
    protected int level = 1;

    public Dice(BulletLayerPanel bulletLayer, Board.BoardTile tile, int intervalMs, int level) {
        this.bulletLayer = bulletLayer;
        this.hostTile = tile;
        this.level = Math.max(1, Math.min(5, level));
        updateDiceImage();

        attackTimer = new Timer(intervalMs, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shootBullet();
            }
        });
        attackTimer.start();
    }

    public boolean upgradeLevel() {
        if (level < 5) {
            this.level++;
            updateDiceImage();
            return true;
        }
        return false;
    }

    // 자식 클래스마다 이미지 경로 규칙이 다르므로 추상 메서드로 정의
    protected abstract void updateDiceImage();

    protected void shootBullet() {
        if (hostTile == null || bulletLayer == null) return;

        Point tileScreenLoc = SwingUtilities.convertPoint(hostTile, hostTile.getWidth() / 2, hostTile.getHeight() / 2, bulletLayer);
        
        double startX = tileScreenLoc.x;
        double startY = tileScreenLoc.y;
        double targetX = startX;
        double targetY = 0; 

        bulletLayer.addBullet(startX, startY, targetX, targetY);
    }

    public void stopAttack() {
        if (attackTimer != null) {
            attackTimer.stop();
        }
    }

    public void setHostTile(Board.BoardTile newTile) {
        this.hostTile = newTile;
    }

    protected ImageIcon createHalfDiceIcon(String imagePath) {
        try {
            ImageIcon originalIcon = new ImageIcon(imagePath);
            Image img = originalIcon.getImage();
            int imgWidth = img.getWidth(null);
            int imgHeight = img.getHeight(null);

            if (imgWidth <= 0 || imgHeight <= 0) return null;

            BufferedImage bufferedImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.drawImage(img, 0, 0, null);
            g2d.dispose();

            BufferedImage halfImage = bufferedImage.getSubimage(0, 0, imgWidth, imgHeight / 2);
            Image scaledImg = halfImage.getScaledInstance(55, 55, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImg);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ImageIcon getDiceIcon() {
        return diceIcon;
    }

    public int getLevel() {
        return level;
    }
}