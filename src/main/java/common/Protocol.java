package common;

/** 한 줄에 명령 1개, UTF-8, 공백으로 명령/인자를 구분합니다. */
public final class Protocol {
    private Protocol() { }
    public static final String JOIN = "/JOIN";
    public static final String JOIN_OK = "/JOIN_OK";
    public static final String JOIN_FAIL = "/JOIN_FAIL";
    public static final String WAIT = "/WAIT";
    public static final String READY = "/READY";
    public static final String START = "/START";
    public static final String CHAT = "/CHAT";
    public static final String PLAYER_LEFT = "/PLAYER_LEFT";
    public static final String ERROR = "/ERROR";
    public static final String ROOM_LIST = "/ROOM_LIST";
    public static final String ROOM_LIST_RESULT = "/ROOM_LIST_RESULT";
    public static final String ROOM_CREATE = "/ROOM_CREATE";
    public static final String ROOM_CREATED = "/ROOM_CREATED";
    public static final String ROOM_JOIN = "/ROOM_JOIN";
    public static final String ROOM_JOINED = "/ROOM_JOINED";
    public static final String ROOM_LEAVE = "/ROOM_LEAVE";
    public static final String ROOM_LEFT = "/ROOM_LEFT";
    public static final String ROOM_STATE = "/ROOM_STATE";
    public static final String UNREADY = "/UNREADY";
    public static final String START_REQUEST = "/START_REQUEST";
    public static final String READY_STATE = "/READY_STATE";
    public static final String SUMMON = "/SUMMON";
    public static final String MERGE = "/MERGE";
    public static final String UPGRADE = "/UPGRADE";
    public static final String DICE_STATE = "/DICE_STATE";
    public static final String ELITE_SUMMON = "/ELITE_SUMMON";
    public static final String MONSTER_STATE = "/MONSTER_STATE";
    public static final String GAME_OVER = "/GAME_OVER";
}
