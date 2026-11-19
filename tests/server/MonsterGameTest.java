package server;

import java.util.Random;

public final class MonsterGameTest {
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }
    private static final class ZeroRandom extends Random {
        @Override public int nextInt(int bound) { return 0; }
    }
    public static void main(String[] args) {
        DiceGame dice = new DiceGame(10, 20, new ZeroRandom());
        MonsterGame game = new MonsterGame(dice, new ZeroRandom());
        check(game.lane(10).hp == 20 && game.lane(20).hp == 20, "initial HP");
        check(game.lane(999) == null, "nonparticipant");
        check(game.eliteSummon(999).error.equals("PLAYER_NOT_IN_GAME"), "outsider cannot summon");
        check(game.eliteSummon(10).success(), "first elite");
        check(dice.player(10).sp == 60 && dice.player(20).sp == 100, "only summoner pays");
        check(game.lane(20).monsters.size() == 1 && game.lane(10).monsters.isEmpty(), "opponent lane");
        check(game.eliteSummon(10).error.equals("ELITE_COOLDOWN"), "cooldown");
        game.tick();
        check(game.lane(10).monsters.size() == 1, "automatic basic spawn");
        check(game.states().get(0).startsWith("/MONSTER_STATE 10 20 1 1 9 "), "state encoding");
        for (int i = 0; i < 9; i++) game.tick();
        check(game.eliteSummon(10).success(), "cooldown expired");
        check(dice.player(10).sp == 20, "second elite SP");
        check(game.eliteSummon(10).error.equals("ELITE_COOLDOWN"), "second cooldown");
        for (int i = 0; i < 6; i++) game.tick();
        check(game.wave() == 2, "wave increments after 15 ticks");
        check(game.lane(10).hp < 20, "leaked monsters damage HP");
        check(game.lane(20).hp < 20, "opponent monsters damage HP");
        for (int i = 0; i < 200 && !game.finished(); i++) game.tick();
        check(game.finished(), "eventual ending");
        check(game.winnerId() == 0 || game.winnerId() == 10 || game.winnerId() == 20, "valid winner");
        System.out.println("PASS: MonsterGame wave, elite, HP, serialization, ending");
    }
}
