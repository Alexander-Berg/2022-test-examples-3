package ru.yandex.market.core.logo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.logo.model.ImageType;
import ru.yandex.market.core.logo.model.Logo;
import ru.yandex.market.core.logo.model.LogoType;
import ru.yandex.market.core.logo.service.PartnerLogoService;

import static org.junit.jupiter.api.Assertions.assertNull;

@DbUnitDataSet(before = "ShopLogoTest.before.csv")
public class PartnerLogoServiceTest extends FunctionalTest {

    private static final long NEW_SHOP = 333;
    private static final long TURBO_NEW = 444;

    private static final Instant NEW_TEST_UPLOAD_TIME = LocalDate
            .of(2018, Month.NOVEMBER, 5)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant();

    @Autowired
    private PartnerLogoService partnerLogoService;

    /**
     * Проверка получения информации об оригинале логотипа магазина.
     * Если у магазина не загружен логотип, должен быть возвращен NULL.
     */
    @Test
    @DbUnitDataSet(after = "ShopLogoTest.before.csv")
    void testFindOrigByPartnerIdZeroPictures() {
        Logo logo = partnerLogoService.findOriginalVersionByPartnerId(NEW_SHOP);
        assertNull(logo);
    }

    /**
     * Update Turbo
     */
    @Test
    @DbUnitDataSet(
            before = "ShopLogoTurboTestUpload.before.csv",
            after = "ShopLogoTurboTestUpload.after.csv")
    void testUploadTurbo() {
        partnerLogoService.upload(TURBO_NEW, Collections.singletonList(
                Logo.builder()
                        .withPartnerId(TURBO_NEW)
                        .withRetina(false)
                        .withUrl("url")
                        .withDeleteUrl("url/delete")
                        .withImageType(ImageType.SVG)
                        .withHeight(28)
                        .withWidth(28)
                        .withUploadTime(NEW_TEST_UPLOAD_TIME)
                        .withLogoType(LogoType.TURBO_LOGO)
                        .build()), 3, false);
    }

    /**
     * Insert Turbo
     */
    @Test
    @DbUnitDataSet(after = "ShopLogoTurboTest.after.csv")
    void testInsertShopLogoListTurbo() {
        partnerLogoService.upload(TURBO_NEW, Collections.singletonList(
                Logo.builder()
                        .withPartnerId(TURBO_NEW)
                        .withRetina(true)
                        .withUrl("test.ru/orig")
                        .withDeleteUrl("test.ru/delete")
                        .withImageType(ImageType.PNG)
                        .withHeight(80)
                        .withWidth(80)
                        .withUploadTime(NEW_TEST_UPLOAD_TIME)
                        .withLogoType(LogoType.TURBO_LOGO)
                        .build()), 2, false);
    }

    /**
     * Delete Turbo
     */
    @Test
    @DbUnitDataSet(before = "ShopLogoTurboTestUpload.before.csv", after = "ShopLogoTurboTestDelete.after.csv")
    void testDeleteTurbo() {
        partnerLogoService.deleteByPartnerIdAndLogoType(
                444L, LogoType.TURBO_LOGO, 1);
    }
}
