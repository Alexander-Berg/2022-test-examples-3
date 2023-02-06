package ru.yandex.market.mbo.yt.index.read.data;

import ru.yandex.market.mbo.yt.index.Key;
import ru.yandex.market.mbo.yt.index.LongKey;
import ru.yandex.market.mbo.yt.index.RowEntity;

/**
 * @author apluhin
 * @created 7/12/21
 */
public class TestIndexRow extends RowEntity {

    private final Long key;
    private final Long payload;

    public TestIndexRow(Long key, Long payload) {
        this.key = key;
        this.payload = payload;
    }

    @Override
    public Key getKey() {
        return new LongKey(key);
    }

    public Long getPayload() {
        return payload;
    }

    @Override
    public boolean getDeleted() {
        return false;
    }

    @Override
    public boolean hasDeleted() {
        return false;
    }

}
