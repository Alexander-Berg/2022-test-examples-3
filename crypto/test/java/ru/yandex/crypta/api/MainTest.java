package ru.yandex.crypta.api;

import org.junit.Test;

public class MainTest {

    @Test
    public void testServerSetsUp() {
        Main main = getMain();
        main.createServer();
    }

    private Main getMain() {
        return new Main(new String[]{}) {
        };
    }

}
