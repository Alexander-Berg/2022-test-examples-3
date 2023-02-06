package ru.yandex.market.mbo.user;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 02.04.2018
 */
public class TestAutoUser {

    public static final long ID = 2802737800000000000L; // differ from 28027378

    private TestAutoUser() {
    }

    public static AutoUser create() {
        return new AutoUser(ID);
    }
}
