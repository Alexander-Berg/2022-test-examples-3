package ru.yandex.market.pers.address.config;

public class GeoCoderMock {
    public Response find(String request) {
        throw new UnsupportedOperationException();
    }

    public enum Response {
        TVERSKAI_6,
        TOLSTOGO_16,
        GRUZINSKAYA_12,
        BOLSHAYA_CHEREMUSHKINSKAYA_11_W1,
        BOLSHAYA_CHEREMUSHKINSKAYA_11_W2,
        PROFSOYUZNAYA_146,
        ZARECHNAYA_12,
        FAIL_OR_NOTHING,
        TWO_GEO_OBJECTS,
        NEAR_PRECISION,
        BAD_PRECISION,
        PRECISION_NEAR_BAD_ACCURACY
    }
}
