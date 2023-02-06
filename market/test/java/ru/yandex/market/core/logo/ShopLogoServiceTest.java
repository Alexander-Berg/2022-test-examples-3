package ru.yandex.market.core.logo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.logo.model.ImageType;
import ru.yandex.market.core.logo.model.Logo;
import ru.yandex.market.core.logo.model.LogoType;
import ru.yandex.market.core.logo.service.PartnerLogoService;
import ru.yandex.market.core.logo.service.ShopLogoService;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Функциональные тесты на {@link PartnerLogoService}
 *
 * @author au-rikka
 */
@DbUnitDataSet(before = "ShopLogoTest.before.csv")
public class ShopLogoServiceTest extends FunctionalTest {

    private static final long ONE_PICTURE_SHOP = 111;
    private static final long TWO_PICTURES_SHOP = 222;
    private static final long NEW_SHOP = 333;

    private static final String TEST_URL = "test.ru/orig";
    private static final String TEST_DELETE_URL = "test.ru/delete";

    private static final Instant TEST_UPLOAD_TIME = LocalDate
            .of(2018, Month.JULY, 1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant();
    private static final Instant NEW_TEST_UPLOAD_TIME = LocalDate
            .of(2018, Month.NOVEMBER, 5)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant();

    @Autowired
    private ShopLogoService shopLogoService;

    /**
     * Проверка получения информации о логотипах магазина.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogoTest.before.csv")
    void testFindByPartnerId() {
        List<Logo> logos = shopLogoService.findByPartnerId(TWO_PICTURES_SHOP);
        assertThat(logos, containsInAnyOrder(
                Logo.builder()
                        .withPartnerId(TWO_PICTURES_SHOP)
                        .withRetina(true)
                        .withUrl(TEST_URL)
                        .withDeleteUrl(TEST_DELETE_URL)
                        .withImageType(ImageType.PNG)
                        .withWidth(20)
                        .withHeight(222)
                        .withUploadTime(TEST_UPLOAD_TIME)
                        .withLogoType(LogoType.SHOP_LOGO)
                        .build(),
                Logo.builder()
                        .withPartnerId(TWO_PICTURES_SHOP)
                        .withRetina(false)
                        .withUrl(TEST_URL)
                        .withImageType(ImageType.PNG)
                        .withWidth(10)
                        .withHeight(111)
                        .withUploadTime(TEST_UPLOAD_TIME)
                        .withLogoType(LogoType.SHOP_LOGO)
                        .build()
        ));
    }

    /**
     * Проверка удаления информации о логотипах магазина.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogoTest.delete.after.csv", before = "ShopLogoTest.delete.before.csv")
    void testDeleteByPartnerId() {
        shopLogoService.deleteByPartnerIdAndLogoType(
                TWO_PICTURES_SHOP, LogoType.SHOP_LOGO, 100500);
    }

    /**
     * Проверка получения информации об оригинале логотипа магазина.
     * Если у магазина только одна версия логотипа, она и должна быть выбрана.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogoTest.before.csv")
    void testFindOrigByPartnerIdOnePicture() {
        Logo logo = shopLogoService.findOriginalVersionByPartnerId(ONE_PICTURE_SHOP);
        assertEquals(Logo.builder()
                .withPartnerId(ONE_PICTURE_SHOP)
                .withRetina(false)
                .withUrl(TEST_URL)
                .withDeleteUrl(TEST_DELETE_URL)
                .withImageType(ImageType.SVG)
                .withWidth(10)
                .withHeight(123)
                .withUploadTime(TEST_UPLOAD_TIME)
                .withLogoType(LogoType.SHOP_LOGO)
                .build(), logo);
    }

    /**
     * Проверка получения информации об оригинале логотипа магазина.
     * Если у магазина две версии, должна быть выбрана ретина-версия.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogoTest.before.csv")
    void testFindOrigByPartnerIdTwoPictures() {
        Logo logo = shopLogoService.findOriginalVersionByPartnerId(TWO_PICTURES_SHOP);
        assertEquals(Logo.builder()
                .withPartnerId(TWO_PICTURES_SHOP)
                .withRetina(true)
                .withUrl(TEST_URL)
                .withDeleteUrl(TEST_DELETE_URL)
                .withImageType(ImageType.PNG)
                .withWidth(20)
                .withHeight(222)
                .withUploadTime(TEST_UPLOAD_TIME)
                .withLogoType(LogoType.SHOP_LOGO)
                .build(), logo);
    }

    /**
     * Проверка получения информации об оригинале логотипа магазина.
     * Если у магазина не загружен логотип, должен быть возвращен NULL.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogoTest.before.csv")
    void testFindOrigByPartnerIdZeroPictures() {
        Logo logo = shopLogoService.findOriginalVersionByPartnerId(NEW_SHOP);
        assertNull(logo);
    }

    /**
     * Проверка добавления информации о логотипах разных магазинов в одном вызове метода.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogoTest.before.csv")
    void testUploadForSeveralPartnerId() {
        List<Logo> logoList = Arrays.asList(
                Logo.builder()
                        .withPartnerId(ONE_PICTURE_SHOP)
                        .withRetina(true)
                        .withUrl("url1")
                        .withDeleteUrl("url1/delete")
                        .withImageType(ImageType.PNG)
                        .withHeight(14)
                        .withWidth(100)
                        .withUploadTime(NEW_TEST_UPLOAD_TIME)
                        .withLogoType(LogoType.SHOP_LOGO)
                        .build(),
                Logo.builder()
                        .withPartnerId(NEW_SHOP)
                        .withRetina(false)
                        .withUrl("url2")
                        .withDeleteUrl("url2/delete")
                        .withImageType(ImageType.PNG)
                        .withHeight(14)
                        .withWidth(100)
                        .withUploadTime(NEW_TEST_UPLOAD_TIME)
                        .withLogoType(LogoType.SHOP_LOGO)
                        .build());

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> shopLogoService.upload(ONE_PICTURE_SHOP, logoList, 1, false)
        );
        Assertions.assertEquals(
                "All logos should be attached to the given partnerId: " + ONE_PICTURE_SHOP,
                exception.getMessage()
        );
    }

    /**
     * Проверка добавления информации о разных версия логотипа магазина в одном вызове метода.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogoTest.upload.after.csv")
    void testUpload() {
        List<Logo> logoList = Arrays.asList(
                Logo.builder()
                        .withPartnerId(TWO_PICTURES_SHOP)
                        .withRetina(true)
                        .withUrl("url1")
                        .withDeleteUrl("url1/delete")
                        .withImageType(ImageType.PNG)
                        .withHeight(28)
                        .withWidth(200)
                        .withUploadTime(NEW_TEST_UPLOAD_TIME)
                        .withLogoType(LogoType.SHOP_LOGO)
                        .build(),
                Logo.builder()
                        .withPartnerId(TWO_PICTURES_SHOP)
                        .withRetina(false)
                        .withUrl("url2")
                        .withImageType(ImageType.PNG)
                        .withHeight(14)
                        .withWidth(100)
                        .withUploadTime(NEW_TEST_UPLOAD_TIME)
                        .withLogoType(LogoType.SHOP_LOGO)
                        .build());
        shopLogoService.upload(TWO_PICTURES_SHOP, logoList, 1, false);
    }


}
