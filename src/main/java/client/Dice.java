package client;

import common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public abstract class Dice {
    protected ImageIcon diceIcon;
    protected Timer attackTimer;
    protected BulletLayerPanel bulletLayer;
    protected MainBackgroundPanel backgroundPanel;
    protected boolean ownLane = true;
    protected Board.BoardTile hostTile;
    protected int level = 1;

    public Dice(BulletLayerPanel bulletLayer, Board.BoardTile tile, int intervalMs, int level) {
        this(bulletLayer, null, true, tile, intervalMs, level);
    }

    public Dice(
            BulletLayerPanel bulletLayer,
            MainBackgroundPanel backgroundPanel,
            boolean ownLane,
            Board.BoardTile tile,
            int intervalMs,
            int level
    ) {
        this.bulletLayer = bulletLayer;
        this.backgroundPanel = backgroundPanel;
        this.ownLane = ownLane;
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

    protected abstract void updateDiceImage();

    protected int getAttackDamage() {
        int base = (this instanceof RapidDice) ? 2 : 1;
        return base * level;
    }

    protected void shootBullet() {
        if (hostTile == null || bulletLayer == null) return;

        Point tileLoc = SwingUtilities.convertPoint(
                hostTile,
                hostTile.getWidth() / 2,
                hostTile.getHeight() / 2,
                bulletLayer
        );

        double startX = tileLoc.x;
        double startY = tileLoc.y;

        if (backgroundPanel != null) {
            var front = backgroundPanel.getFrontMonsterTarget(ownLane, bulletLayer);
            if (front == null || front.point() == null) {
                return;
            }
            int targetId = front.monsterId();
            bulletLayer.addHomingBullet(
                    startX,
                    startY,
                    () -> {
                        var current = backgroundPanel.getFrontMonsterTarget(ownLane, bulletLayer);
                        return current != null ? current.point() : null;
                    },
                    id -> backgroundPanel.isMonsterAlive(ownLane, id),
                    (id, dmg) -> {
                        if (ownLane && id >= 0 && dmg > 0) {
                            NetworkClient.getInstance().send(Protocol.BULLET_HIT + " " + id + " " + dmg);
                        }
                    },
                    front.point().x,
                    front.point().y,
                    getAttackDamage(),
                    targetId
            );
        } else {
            bulletLayer.addBullet(startX, startY, startX, 0);
        }
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