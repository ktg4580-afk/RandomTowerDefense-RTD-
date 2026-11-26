package client;

public class SlowDice extends Dice {
    public SlowDice(BulletLayerPanel bulletLayer, Board.BoardTile tile, int level) {
        this(bulletLayer, null, true, tile, level);
    }

    public SlowDice(
            BulletLayerPanel bulletLayer,
            MainBackgroundPanel backgroundPanel,
            boolean ownLane,
            Board.BoardTile tile,
            int level
    ) {
        super(bulletLayer, backgroundPanel, ownLane, tile, 2500, level);
    }

    @Override
    protected void updateDiceImage() {
        String imagePath = (level == 1) ? "images/heart.png" : "images/heart" + level + ".png";
        this.diceIcon = createHalfDiceIcon(imagePath);
    }
}