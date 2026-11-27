package server;

import common.Protocol;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

final class MonsterGame {
    static final int INITIAL_HP = 20;
    static final int ELITE_COST = 40;
    static final int ELITE_COOLDOWN_TICKS = 10;
    static final int WAVE_TICKS = 15;
    static final int SPAWN_TICKS = 3;
    static final int PATH_LENGTH = Protocol.MONSTER_PATH_LENGTH;
    static final int KILL_REWARD = 5;
    static final String[] ELITE_TYPES = {"TANK", "SPRINTER", "SHIELDED"};

    static final class Monster {
        final int id;
        final String type;
        int hp;
        final int maxHp;
        int position;
        final int speed;
        final boolean elite;

        Monster(int id, String type, int hp, int speed, boolean elite) {
            this.id = id;
            this.type = type;
            this.hp = hp;
            this.maxHp = hp;
            this.speed = speed;
            this.elite = elite;
        }

        String encode() { return id + ":" + type + ":" + hp + ":" + maxHp + ":" + position; }
    }

    private record Attack(Monster target, int damage, int diceMask) {}

    static final class Lane {
        final int playerId;
        int hp = INITIAL_HP;
        final List<Monster> monsters = new ArrayList<>();
        int eliteCooldown = 0;
        private Attack pendingAttack;
        Lane(int playerId) { this.playerId = playerId; }
    }

    private final DiceGame diceGame;
    private final Random random;
    private final Lane first;
    private final Lane second;
    private int tick;
    private int wave = 1;
    private int nextMonsterId = 1;
    private boolean finished;
    private int winnerId = -1;

    MonsterGame(DiceGame diceGame) { this(diceGame, new Random()); }

    MonsterGame(DiceGame diceGame, Random random) {
        this.diceGame = diceGame;
        this.random = random;
        List<DiceGame.PlayerField> fields = diceGame.fields();
        first = new Lane(fields.get(0).playerId);
        second = new Lane(fields.get(1).playerId);
    }

    Lane lane(int playerId) {
        if (first.playerId == playerId) return first;
        if (second.playerId == playerId) return second;
        return null;
    }

    int wave() { return wave; }
    int tickCount() { return tick; }
    boolean finished() { return finished; }
    int winnerId() { return winnerId; }

    // 상대 필드에 엘리트 소환
    DiceGame.ActionResult eliteSummon(int senderId) {
        Lane sender = lane(senderId);
        if (sender == null) return DiceGame.ActionResult.error("PLAYER_NOT_IN_GAME");
        if (finished) return DiceGame.ActionResult.error("GAME_FINISHED");
        if (sender.eliteCooldown > 0) return DiceGame.ActionResult.error("ELITE_COOLDOWN");
        DiceGame.PlayerField payer = diceGame.player(senderId);
        if (payer.sp < ELITE_COST) return DiceGame.ActionResult.error("NOT_ENOUGH_SP");
        payer.sp -= ELITE_COST;
        sender.eliteCooldown = ELITE_COOLDOWN_TICKS;
        Lane opponent = sender == first ? second : first;
        String type = ELITE_TYPES[random.nextInt(ELITE_TYPES.length)];
        int hp = type.equals("TANK") ? 16 : type.equals("SHIELDED") ? 12 : 8;
        int speed = type.equals("SPRINTER") ? 2 : 1;
        opponent.monsters.add(new Monster(nextMonsterId++, type, hp, speed, true));
        return DiceGame.ActionResult.ok();
    }

    // 1초마다 몬스터 이동 및 웨이브/스폰 갱신
    void tick() {
        if (finished) return;
        tick++;
        if ((tick - 1) % WAVE_TICKS == 0 && tick > 1) wave++;
        if ((tick - 1) % SPAWN_TICKS == 0) {
            spawnBasic(first);
            spawnBasic(second);
        }
        resolveAttack(first);
        resolveAttack(second);
        advance(first);
        advance(second);
        if (first.eliteCooldown > 0) first.eliteCooldown--;
        if (second.eliteCooldown > 0) second.eliteCooldown--;
        if (first.hp <= 0 || second.hp <= 0) {
            finished = true;
            winnerId = first.hp <= 0 && second.hp <= 0 ? 0 : first.hp <= 0 ? second.playerId : first.playerId;
            return;
        }
        queueAttack(first);
        queueAttack(second);
    }

    private void spawnBasic(Lane lane) {
        lane.monsters.add(new Monster(nextMonsterId++, "BASIC", 5 + (wave - 1) * 2, 1, false));
    }

    private void queueAttack(Lane lane) {
        Monster target = null;
        for (Monster monster : lane.monsters) {
            if (target == null || monster.position > target.position) target = monster;
        }
        if (target == null) return;

        DiceGame.PlayerField field = diceGame.player(lane.playerId);
        int damage = 0;
        int diceMask = 0;
        for (int cell = 0; cell < field.cells.length; cell++) {
            DiceGame.Dice dice = field.cells[cell];
            if (dice == null) continue;
            diceMask |= 1 << cell;
            int base = dice.type.equals("RAPID") ? 2 : 1;
            damage += base * dice.level;
            for (int i = 0; i < DiceGame.TYPES.length; i++) {
                if (DiceGame.TYPES[i].equals(dice.type)) damage += field.upgrades[i];
            }
        }

        if (diceMask != 0) lane.pendingAttack = new Attack(target, damage, diceMask);
    }

    private void resolveAttack(Lane lane) {
        Attack attack = lane.pendingAttack;
        lane.pendingAttack = null;
        if (attack == null) return;

        Monster target = attack.target();
        target.hp -= attack.damage();
        if (target.hp <= 0) {
            lane.monsters.remove(target);
            diceGame.player(lane.playerId).sp += target.elite ? KILL_REWARD * 3 : KILL_REWARD;
        }
    }

    List<String> attackEvents() {
        List<String> events = new ArrayList<>(2);
        for (Lane lane : List.of(first, second)) {
            Attack attack = lane.pendingAttack;
            if (attack != null) {
                events.add(Protocol.ATTACK + " " + lane.playerId + " " + tick + " "
                        + attack.target().id + " " + attack.diceMask());
            }
        }
        return events;
    }

    private void advance(Lane lane) {
        Iterator<Monster> iterator = lane.monsters.iterator();
        while (iterator.hasNext()) {
            Monster monster = iterator.next();
            monster.position += monster.speed;
            if (monster.position >= PATH_LENGTH) {
                lane.hp = Math.max(0, lane.hp - (monster.elite ? 3 : 1));
                iterator.remove();
            }
        }
    }

    String encode(Lane lane) {
        StringBuilder sb = new StringBuilder("/MONSTER_STATE ")
            .append(lane.playerId).append(' ').append(lane.hp).append(' ')
            .append(wave).append(' ').append(tick).append(' ')
            .append(lane.eliteCooldown).append(' ').append(lane.monsters.size());
        for (Monster monster : lane.monsters) sb.append(' ').append(monster.encode());
        return sb.toString();
    }

    List<String> states() { return List.of(encode(first), encode(second)); }
}