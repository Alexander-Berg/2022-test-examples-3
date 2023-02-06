package ru.yandex.market.antifraud.orders.tanking.headers;

import ru.yandex.market.antifraud.orders.tanking.TankAmmo;

/**
 * @author dzvyagin
 */
public class AcceptLanguageFactory implements HeaderFactory {

    private static final String HEADER_NAME = "accept-language";
    private static final String RU = "ru";

    @Override
    public String getHeaderName() {
        return HEADER_NAME;
    }

    @Override
    public String getHeader(TankAmmo tankAmmo) {
        return RU;
    }
}
