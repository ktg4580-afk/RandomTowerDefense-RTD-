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
}
