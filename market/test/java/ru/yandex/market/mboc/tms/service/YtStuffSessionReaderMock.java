package ru.yandex.market.mboc.tms.service;

/**
 * @author danfertev
 * @since 08.07.2020
 */
public class YtStuffSessionReaderMock implements YtStuffSessionReader {
    private String lastSessionId;

    @Override
    public String getLastSessionId() {
        return lastSessionId;
    }

    public YtStuffSessionReaderMock setLastSessionId(String lastSessionId) {
        this.lastSessionId = lastSessionId;
        return this;
    }
}
