package ru.yandex.market.volva.tank.headers;


import ru.yandex.market.volva.tank.TankAmmo;

/**
 * @author dzvyagin
 */
public class AcceptHeaderFactory implements HeaderFactory{

    private static final String HEADER_NAME = "accept";
    private static final String APPLICATION_JSON = "*/*";


    @Override
    public String getHeaderName() {
        return HEADER_NAME;
    }

    @Override
    public String getHeader(TankAmmo tankAmmo) {
        return APPLICATION_JSON;
    }
}
