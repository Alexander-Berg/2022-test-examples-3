package ru.yandex.market.billing.checkout;

import java.io.InputStream;

public interface ResourceUtilities {

    default InputStream getResourceAsInputStream(String filename) {
        return this.getClass().getResourceAsStream(getResourcePrefix() + filename);
    }

    String getResourcePrefix();
}
