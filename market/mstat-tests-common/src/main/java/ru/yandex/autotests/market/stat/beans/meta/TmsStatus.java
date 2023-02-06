package ru.yandex.autotests.market.stat.beans.meta;

import ru.yandex.autotests.market.stat.WithMask;

/**
 * Created by jkt on 01.07.14.
 */
public enum TmsStatus implements WithMask {

    OK("OK") ,
    RUNNING(null) {
        @Override
        public boolean matches(String status) {
            return status == null;
        }
    };

    private String mask;

    @Override
    public String mask() {
        return mask;
    }

    TmsStatus(String status) {
        this.mask = status;
    }

    public boolean matches(String status) {
        return this.mask.equals(status);
    }
}
