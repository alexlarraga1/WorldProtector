package com.alex_escobar.worldprotector.command;

public enum Command {
    WP("wp"),
    W_P("w-p"),
    WP_LONG("worldprotector"),
    REGION("region"),
    DIMENSION("dimension"),
    FLAG("flag"),
    HELP("help"),
    ADD("add"),
    ADD_OFFLINE("add-offline"),
    REMOVE("remove"),
    REMOVE_OFFLINE("remove-offline"),
    REMOVE_ALL("remove-all"),
    ALL("all"),
    INFO("info"),
    NAME("name"),
    EXPAND("expand"),
    VERT("vert"),
    DEFAULT_Y("y-default"),
    LIST("list"),
    DEFINE("define"),
    REDEFINE("redefine"),
    TELEPORT("teleport"),
    TELEPORT_SHORT("tp"),

    START_POS("startPos"),

    END_POS("endPos"),
    ACTIVATE("activate"),
    DEACTIVATE("deactivate"),
    MUTE("mute"),
    UNMUTE("unmute"),
    PRIORITY("priority"),
    SET_PRIORITY("set-priority"),
    PLAYER("player"),
    TEAM("team"),
    Y1("Y1"),
    Y2("Y2");

    private final String cmdString;

    Command(final String cmdString) {
        this.cmdString = cmdString;
    }

    @Override
    public String toString() {
        return cmdString;
    }
}
