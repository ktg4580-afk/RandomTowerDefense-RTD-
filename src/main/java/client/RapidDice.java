package client;

public class RapidDice extends Dice {
    public RapidDice(BulletLayerPanel bulletLayer, Board.BoardTile tile, int level) {
        super(bulletLayer, tile, 500, level);
    }

    @Override
    protected void updateDiceImage() {
        String imagePath = (level == 1) ? "images/ginger.png" : "images/ginger" + level + ".png";
        this.diceIcon = createHalfDiceIcon(imagePath);
    }
}