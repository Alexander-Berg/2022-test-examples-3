package ru.yandex.market.mbi.api.controller.shoplogo;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.api.client.entity.shoplogo.ShopLogoResponse;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.api.controller.ShopLogoController;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Функциональные тесты на {@link ShopLogoController}.
 *
 * @author au-rikka
 */
@DbUnitDataSet(before = "ShopLogoTest.before.csv")
public class ShopLogoControllerTest extends FunctionalTest {
    private static final long ONE_PICTURE_SHOP = 111;
    private static final long TWO_PICTURES_SHOP = 222;
    private static final long NEW_SHOP = 333;

    private static final String TEST_URL = "test.ru/orig";
    private static final Date TEST_UPLOAD_TIME = Date.from(
            LocalDate.of(2018, Month.JULY, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

    /**
     * Тест для ручки {@code /shop/${shopId}/logo/info}.
     * Проверка получения информации о логотипе для магазина без логотипов.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogoTest.before.csv")
    void testGetShopLogoInfoNewShop() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.getShopLogoInfo(NEW_SHOP)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
        MbiAsserts.assertXmlEquals(
                //language=xml
                "<error>\n" +
                        "    <message>Shop logo not found for partnerId: 333</message>\n" +
                        "</error>",
                exception.getResponseBodyAsString()
        );
    }

    /**
     * Тест для ручки {@code /shop/${shopId}/logo/info}.
     * Проверка получения информации о логотипе для магазина с одной версией изображения.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogoTest.before.csv")
    void testGetShopLogoInfoOnePictureShop() {
        ShopLogoResponse response = mbiApiClient.getShopLogoInfo(ONE_PICTURE_SHOP);
        assertEquals(new ShopLogoResponse(ONE_PICTURE_SHOP, TEST_URL, TEST_UPLOAD_TIME), response);
    }

    /**
     * Тест для ручки {@code /shop/${shopId}/logo/info}.
     * Проверка получения информации о логотипе для магазина с двумя версиями изображения.
     * Ожидается, что вернется ссылка на оригинал (ретина-версию).
     */
    @Test
    @DbUnitDataSet(after = "ShopLogoTest.before.csv")
    void testGetShopLogoInfoTwoPicturesShop() {
        ShopLogoResponse response = mbiApiClient.getShopLogoInfo(TWO_PICTURES_SHOP);
        assertEquals(new ShopLogoResponse(TWO_PICTURES_SHOP, TEST_URL, TEST_UPLOAD_TIME), response);
    }
}
