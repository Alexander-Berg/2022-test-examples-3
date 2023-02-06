package ru.yandex.market.deepmind.tms.executors.category;

/**
 * @author danfertev
 * @since 08.07.2020
 */
public class DeepmindYtStuffSessionReaderMock implements DeepmindYtStuffSessionReader {
    private String lastSessionId;

    @Override
    public String getLastSessionId() {
        return lastSessionId;
    }

    public DeepmindYtStuffSessionReaderMock setLastSessionId(String lastSessionId) {
        this.lastSessionId = lastSessionId;
        return this;
    }
}
