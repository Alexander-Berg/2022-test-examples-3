package ru.yandex.market.psku.postprocessor.common.db.dao;

import ru.yandex.market.psku.postprocessor.msku_creation.PskuIdentity;

import java.util.Objects;

public class SimplePsku implements PskuIdentity {
    private final long pskuId;

    public SimplePsku(long pskuId) {
        this.pskuId = pskuId;
    }

    public static SimplePsku ofId(long pskuId) {
        return new SimplePsku(pskuId);
    }

    @Override
    public Long getPskuId() {
        return pskuId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimplePsku that = (SimplePsku) o;
        return pskuId == that.pskuId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pskuId);
    }
}
