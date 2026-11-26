package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class DiceGame {
    static final int FIELD_SIZE = 15;
    static final int START_SP = 100;
    static final int MAX_LEVEL = 5;
    static final String[] TYPES = {"BASIC", "RAPID", "SLOW"};

    static final class Dice {
        final String type;
        final int level;

        Dice(String type, int level) {
            this.type = type;
            this.level = level;
        }

        String encode() { return type + ":" + level; }
    }

    static final class PlayerField {
        final int playerId;
        int sp = START_SP;
        int summonCount;
        final Dice[] cells = new Dice[FIELD_SIZE];
        final int[] upgrades = new int[TYPES.length];

        PlayerField(int playerId) { this.playerId = playerId; }
        int summonCost() { return 10 + 5 * summonCount; }
        int upgradeCost(int index) { return 50 * (upgrades[index] + 1); }

        String encode() {
            StringBuilder sb = new StringBuilder("/DICE_STATE ").append(playerId)
                    .append(' ').append(sp).append(' ').append(summonCost());
            for (int upgrade : upgrades) sb.append(' ').append(upgrade);
            for (Dice dice : cells) sb.append(' ').append(dice == null ? "-" : dice.encode());
            return sb.toString();
        }
    }

    static final class ActionResult {
        final String error;
        private ActionResult(String error) { this.error = error; }
        static ActionResult ok() { return new ActionResult(null); }
        static ActionResult error(String error) { return new ActionResult(error); }
        boolean success() { return error == null; }
    }

    private final Random random;
    private final List<PlayerField> fields = new ArrayList<>();

    DiceGame(int firstId, int secondId) {
        this(firstId, secondId, new Random());
    }

    DiceGame(int firstId, int secondId, Random random) {
        this.random = random;
        fields.add(new PlayerField(firstId));
        fields.add(new PlayerField(secondId));
    }

    List<PlayerField> fields() { return List.copyOf(fields); }

    PlayerField player(int id) {
        for (PlayerField field : fields) if (field.playerId == id) return field;
        return null;
    }

    // 주사위 소환
    ActionResult summon(int id) {
        PlayerField field = player(id);
        if (field == null) return ActionResult.error("PLAYER_NOT_IN_GAME");
        int count = 0;
        for (Dice dice : field.cells) if (dice == null) count++;
        if (count == 0) return ActionResult.error("FIELD_FULL");
        int cost = field.summonCost();
        if (field.sp < cost) return ActionResult.error("NOT_ENOUGH_SP");
        int choice = random.nextInt(count);
        for (int i = 0; i < FIELD_SIZE; i++) {
            if (field.cells[i] != null) continue;
            if (choice-- == 0) {
                field.cells[i] = new Dice(TYPES[random.nextInt(TYPES.length)], 1);
                break;
            }
        }
        field.sp -= cost;
        field.summonCount++;
        return ActionResult.ok();
    }

    // 같은 종류와 레벨만 합성
    ActionResult merge(int id, int from, int to) {
        PlayerField field = player(id);
        if (field == null) return ActionResult.error("PLAYER_NOT_IN_GAME");
        if (from < 0 || from >= FIELD_SIZE || to < 0 || to >= FIELD_SIZE || from == to)
            return ActionResult.error("INVALID_CELL");
        Dice a = field.cells[from];
        Dice b = field.cells[to];
        if (a == null || b == null) return ActionResult.error("EMPTY_CELL");
        if (!a.type.equals(b.type) || a.level != b.level)
            return ActionResult.error("MERGE_MISMATCH");
        if (a.level >= MAX_LEVEL) return ActionResult.error("MAX_LEVEL");
        field.cells[from] = null;
        field.cells[to] = new Dice(a.type, a.level + 1);
        return ActionResult.ok();
    }

    // 주사위 종류별 강화
    ActionResult upgrade(int id, String type) {
        PlayerField field = player(id);
        if (field == null) return ActionResult.error("PLAYER_NOT_IN_GAME");
        int index = -1;
        for (int i = 0; i < TYPES.length; i++) if (TYPES[i].equals(type)) index = i;
        if (index < 0) return ActionResult.error("INVALID_DICE_TYPE");
        if (field.upgrades[index] >= MAX_LEVEL - 1) return ActionResult.error("MAX_UPGRADE");
        int cost = field.upgradeCost(index);
        if (field.sp < cost) return ActionResult.error("NOT_ENOUGH_SP");
        field.sp -= cost;
        field.upgrades[index]++;
        return ActionResult.ok();
    }
}