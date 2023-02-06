package ru.yandex.autotests.direct.cmd.data.commons;

public enum Lang {
    EN, RU, UA, KZ, TR, DE;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
