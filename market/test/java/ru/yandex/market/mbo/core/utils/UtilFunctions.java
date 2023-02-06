package ru.yandex.market.mbo.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.yandex.market.mbo.core.kdepot.services.ImageValidatorImplTest;

public final class UtilFunctions {

    private UtilFunctions() {
    }

    @SuppressWarnings("checkstyle:magicNumber")
    public static byte[] getFileBytes(String fileName) throws IOException {
        try (InputStream is = ImageValidatorImplTest.class.getResourceAsStream("/images_for_validation/" + fileName)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buff = new byte[2048];
            int read;
            while ((read = is.read(buff)) > 0) {
                baos.write(buff, 0, read);
            }
            return baos.toByteArray();
        }
    }
}
