package client;

public class BasicDice extends Dice {
    public BasicDice(BulletLayerPanel bulletLayer, Board.BoardTile tile, int level) {
        this(bulletLayer, null, true, tile, level);
    }

    public BasicDice(
            BulletLayerPanel bulletLayer,
            MainBackgroundPanel backgroundPanel,
            boolean ownLane,
            Board.BoardTile tile,
            int level
    ) {
        super(bulletLayer, backgroundPanel, ownLane, tile, 1500, level);
    }

    @Override
    protected void updateDiceImage() {
        String imagePath = "images/Bblack_" + level + ".png";
        this.diceIcon = createHalfDiceIcon(imagePath);
    }
}