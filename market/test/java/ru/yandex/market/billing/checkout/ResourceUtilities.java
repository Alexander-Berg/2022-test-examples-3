package ru.yandex.market.billing.checkout;

import java.io.InputStream;

/**
 * Возможность читать файл. Копия из проекта mbi.
 */
public interface ResourceUtilities {

    default InputStream getResourceAsInputStream(String filename) {
        return this.getClass().getResourceAsStream(getResourcePrefix() + filename);
    }

    String getResourcePrefix();
}
