package ru.yandex.market.terminal;

import ru.yandex.common.util.terminal.Terminal;

public class TestTerminal extends Terminal {
    public TestTerminal() {
        super(System.in, System.out);
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected void onClose() {
    }
}