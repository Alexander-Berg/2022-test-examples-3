package ru.yandex.market.antifraud.orders.tanking.headers;

import ru.yandex.market.antifraud.orders.tanking.TankAmmo;

/**
 * @author dzvyagin
 */
public class HostHeaderFactory implements HeaderFactory {

    private static final String HEADER_NAME = "Host";
    private static final String TARGET_HOST = "sas1-8968-1f1-sas-market-test--41a-17499.gencfg-c.yandex.net:17499";

    @Override
    public String getHeaderName() {
        return HEADER_NAME;
    }

    @Override
    public String getHeader(TankAmmo tankAmmo) {
        return TARGET_HOST;
    }
}
