package ru.yandex.market.checkout.util.checkerx;

import java.io.IOException;

public final class ResourceUtils {

    private ResourceUtils() {
    }

    public static String readResourceFile(String filePath) {
        try {
            return ru.yandex.common.util.IOUtils.readInputStream(
                    ResourceUtils.class.getResourceAsStream(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Reading resource " + filePath + "failed");
        }
    }
}
