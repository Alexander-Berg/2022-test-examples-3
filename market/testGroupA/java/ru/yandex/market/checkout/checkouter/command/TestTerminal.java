package ru.yandex.market.checkout.checkouter.command;

import java.io.InputStream;
import java.io.OutputStream;

import ru.yandex.common.util.terminal.Terminal;

public class TestTerminal extends Terminal {

    public TestTerminal(InputStream input, OutputStream output) {
        super(input, output);
    }

    @Override
    protected void onStart() {

    }

    @Override
    protected void onClose() {

    }
}
