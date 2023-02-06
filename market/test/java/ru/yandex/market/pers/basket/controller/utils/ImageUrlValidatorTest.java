package ru.yandex.market.pers.basket.controller.utils;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.pers.basket.PersBasketTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author ifilippov5
 */
public class ImageUrlValidatorTest extends PersBasketTest {

    @Value("${pers.basket.avatarnica-domains}")
    private String avatarnicaDomains;

    @Test
    public void test() {
        ImageUrlValidator validator = new ImageUrlValidator(avatarnicaDomains);
        assertTrue(validator.isValid("https://avatars.mds.yandex.net/", null));
        assertTrue(validator.isValid("http://avatars.mds.yandex.net/", null));
        assertTrue(validator.isValid("//avatars.mds.yandex.net/", null));
        assertTrue(validator.isValid("//avatars-int.mds.yandex.net/", null));

        assertFalse(validator.isValid("http://avatars.mds.yandex.net", null));
        assertFalse(validator.isValid("avatars.mds.yandex.net/", null));
        assertFalse(validator.isValid("http://avatars.md.yandex.net/", null));
    }

}
