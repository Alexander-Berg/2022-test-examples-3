package ru.yandex.market.volva.tank.headers;


import ru.yandex.market.volva.tank.TankAmmo;

/**
 * @author dzvyagin
 */
public class AcceptEncodingHeaderFactory implements HeaderFactory {

    private static final String HEADER_NAME = "accept-encoding";
    private static final String ALL_ENCODINGS = "br, gzip, deflate";

    @Override
    public String getHeaderName() {
        return HEADER_NAME;
    }

    @Override
    public String getHeader(TankAmmo tankAmmo) {
        return ALL_ENCODINGS;
    }
}
