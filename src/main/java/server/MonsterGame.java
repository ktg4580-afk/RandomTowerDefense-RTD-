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

    static final class Lane {
        final int playerId;
        int hp = INITIAL_HP;
        final List<Monster> monsters = new ArrayList<>();
        int eliteCooldown = 0;
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
        advance(first);
        advance(second);
        if (first.eliteCooldown > 0) first.eliteCooldown--;
        if (second.eliteCooldown > 0) second.eliteCooldown--;
        if (first.hp <= 0 || second.hp <= 0) {
            finished = true;
            winnerId = first.hp <= 0 && second.hp <= 0 ? 0 : first.hp <= 0 ? second.playerId : first.playerId;
        }
    }

    private void spawnBasic(Lane lane) {
        lane.monsters.add(new Monster(nextMonsterId++, "BASIC", 5 + (wave - 1) * 2, 1, false));
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

    // 총알 명중 시 서버 권위로 실제 HP 차감, 처치 및 SP 보상 처리
    synchronized boolean applyBulletHit(int senderId, int monsterId, int damage) {
        if (finished || monsterId < 0 || damage <= 0) return false;
        Lane lane = lane(senderId);
        if (lane == null) return false;

        Iterator<Monster> iterator = lane.monsters.iterator();
        while (iterator.hasNext()) {
            Monster monster = iterator.next();
            if (monster.id == monsterId) {
                monster.hp -= damage;
                if (monster.hp <= 0) {
                    iterator.remove();
                    DiceGame.PlayerField field = diceGame.player(senderId);
                    if (field != null) {
                        field.sp += monster.elite ? KILL_REWARD * 3 : KILL_REWARD;
                    }
                }
                if (first.hp <= 0 || second.hp <= 0) {
                    finished = true;
                    winnerId = first.hp <= 0 && second.hp <= 0 ? 0 : first.hp <= 0 ? second.playerId : first.playerId;
                }
                return true;
            }
        }
        return false;
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