package ru.yandex.market.antifraud.orders.tanking.headers;


import ru.yandex.market.antifraud.orders.tanking.TankAmmo;

/**
 * @author dzvyagin
 */
public class ContentTypeHeaderFactory implements HeaderFactory {

    private static final String HEADER_NAME = "Content-type";
    private static final String APPLICATION_JSON = "application/json";

    @Override
    public String getHeaderName() {
        return HEADER_NAME;
    }

    @Override
    public String getHeader(TankAmmo tankAmmo) {
        return APPLICATION_JSON;
    }
}
