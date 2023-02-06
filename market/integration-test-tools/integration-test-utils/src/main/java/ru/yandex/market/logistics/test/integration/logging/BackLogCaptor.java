package ru.yandex.market.logistics.test.integration.logging;

import org.apache.logging.log4j.Level;

import ru.yandex.market.logistics.logging.backlog.layout.log4j.BackLogLayout;

public class BackLogCaptor extends CustomLogCaptor<BackLogLayout> {

    public BackLogCaptor() {
        this(null);
    }

    public BackLogCaptor(String loggerName) {
        super(loggerName, "BACK_LOG", Level.INFO);
    }
}
