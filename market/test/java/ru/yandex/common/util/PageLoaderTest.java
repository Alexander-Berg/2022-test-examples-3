package ru.yandex.common.util;

import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author imelnikov
 */
public class PageLoaderTest {

    @Test
    @Disabled
    public void openUrl() throws Exception {
        InputStream inputStream = URLUtils.getInputStream(new URL("https://fenix-russia.ru/fenix-e15-cree-xp-e/"), 1);
        String page = IOUtils.readInputStream(inputStream);
        assertNotNull(page);
    }

}
