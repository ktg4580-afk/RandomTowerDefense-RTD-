package client;

import javax.swing.*;
import java.awt.*;

public class DragLayerPanel extends JPanel {
    private Image dragImage;
    private Point cursorPoint;
    private int iconWidth = 55;
    private int iconHeight = 55;

    public DragLayerPanel() {
        setOpaque(false);
        setLayout(null);
    }

    public void startDrag(ImageIcon icon, Point screenOrPanelPoint) {
        if (icon == null || icon.getImage() == null) {
            stopDrag();
            return;
        }
        this.dragImage = icon.getImage();
        this.iconWidth = icon.getIconWidth() > 0 ? icon.getIconWidth() : 55;
        this.iconHeight = icon.getIconHeight() > 0 ? icon.getIconHeight() : 55;
        updateDragPosition(screenOrPanelPoint);
        repaint();
    }

    public void updateDragPosition(Point pointInDragLayer) {
        if (dragImage == null || pointInDragLayer == null) return;
        this.cursorPoint = pointInDragLayer;
        repaint();
    }

    public void stopDrag() {
        this.dragImage = null;
        this.cursorPoint = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (dragImage == null || cursorPoint == null) return;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));

        int drawX = cursorPoint.x - iconWidth / 2;
        int drawY = cursorPoint.y - iconHeight / 2;
        g2d.drawImage(dragImage, drawX, drawY, iconWidth, iconHeight, this);

        g2d.dispose();
    }
}