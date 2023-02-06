package ru.yandex.market.volva.tank.headers;


import ru.yandex.market.volva.tank.TankAmmo;

/**
 * @author dzvyagin
 */
public class HostHeaderFactory implements HeaderFactory {

    private static final String HEADER_NAME = "Host";
    private static final String TARGET_HOST = "sas2-0121-398-sas-market-test--90a-23816.gencfg-c.yandex.net:23816";

    @Override
    public String getHeaderName() {
        return HEADER_NAME;
    }

    @Override
    public String getHeader(TankAmmo tankAmmo) {
        return TARGET_HOST;
    }
}
