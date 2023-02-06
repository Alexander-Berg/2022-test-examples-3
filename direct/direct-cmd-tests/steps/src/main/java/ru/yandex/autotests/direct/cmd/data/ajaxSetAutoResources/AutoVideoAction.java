package ru.yandex.autotests.direct.cmd.data.ajaxSetAutoResources;

public enum AutoVideoAction {

    SET("set"),
    RESET("reset");

    private String name;


    AutoVideoAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
