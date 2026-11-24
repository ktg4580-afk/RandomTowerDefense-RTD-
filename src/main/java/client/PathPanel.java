package client;

import javax.swing.*;
import java.awt.*;

public class PathPanel extends JPanel {
    
    public PathPanel() {
        setBackground(Color.WHITE);
        // 위쪽 보드와 아래쪽 보드 사이의 적절한 높이 설정
        setPreferredSize(new Dimension(800, 7050)); 
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 부드러운 선 표현을 위한 안티앨리어싱 설정
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 길 배경색 (예: 연한 회색 혹은 모래 색상)
        g2d.setColor(new Color(230, 230, 230));
        
        int width = getWidth();
        int height = getHeight();

        // H모양 길 좌표 계산 (좌우 통로 및 중앙 횡단 통로)
        int pathWidth = 40; // 길의 두께
        int leftX = width / 4 - pathWidth / 2;
        int rightX = (width / 4) * 3 - pathWidth / 2;
        int centerY = height / 2 - pathWidth / 2;

        // 1. 왼쪽 세로 길
        g2d.fillRect(leftX, 0, pathWidth, height);
        // 2. 오른쪽 세로 길
        g2d.fillRect(rightX, 0, pathWidth, height);
        // 3. 가운데를 잇는 가로 길 (H모양의 허리)
        g2d.fillRect(leftX, centerY, rightX - leftX + pathWidth, pathWidth);

        // 길 테두리 선 (선택 사항)
        g2d.setColor(new Color(180, 180, 180));
        g2d.setStroke(new BasicStroke(2));
        
        // 왼쪽 테두리
        g2d.drawRect(leftX, 0, pathWidth, height);
        // 오른쪽 테두리
        g2d.drawRect(rightX, 0, pathWidth, height);
    }
}