package server;

import java.util.Random;

public final class DiceGameTest {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class FixedRandom extends Random {
        @Override public int nextInt(int bound) { return 0; }
    }

    public static void main(String[] args) {
        DiceGame g = new DiceGame(1, 2, new FixedRandom());
        DiceGame.PlayerField a = g.player(1);
        DiceGame.PlayerField b = g.player(2);
        require(a.sp == 100 && b.sp == 100, "initial SP per player");
        require(a.summonCost() == 10, "initial summon cost");
        require(g.summon(1).success(), "first summon");
        require(a.sp == 90 && a.summonCost() == 15 && a.cells[0] != null, "summon SP and placement");
        require(b.sp == 100 && b.cells[0] == null, "fields must be separate");
        require(g.summon(1).success(), "second summon");
        require(a.sp == 75 && a.cells[1] != null, "second summon cost");
        require("EMPTY_CELL".equals(g.merge(1, 0, 2).error), "empty merge rejection");
        require("INVALID_CELL".equals(g.merge(1, 0, 0).error), "same-cell rejection");
        for (String type : DiceGame.TYPES) {
            a.cells[0] = new DiceGame.Dice(type, 1);
            a.cells[1] = new DiceGame.Dice(type, 1);
            require(g.merge(1, 0, 1).success(), "same type/level merges");
            require(a.cells[0] == null && a.cells[1].level == 2 && a.cells[1].type.equals(type),
                    "merge consumes source and retains " + type);
        }
        require("INVALID_DICE_TYPE".equals(g.upgrade(1, "UNKNOWN").error), "unknown type");
        require(g.upgrade(1, "BASIC").success(), "upgrade");
        require(a.sp == 25 && a.upgrades[0] == 1, "upgrade cost");
        require("NOT_ENOUGH_SP".equals(g.upgrade(1, "BASIC").error), "upgrade insufficient SP");
        require(g.summon(1).success(), "third summon");
        require(a.sp == 5 && a.summonCost() == 25, "summon growth");
        require("NOT_ENOUGH_SP".equals(g.summon(1).error), "summon insufficient SP");
        require(g.player(999) == null, "no unauthorized player");
        require(a.encode().split(" ").length == 22, "serialized state fields");
        System.out.println("PASS: DiceGame rules and validation");
    }
}
