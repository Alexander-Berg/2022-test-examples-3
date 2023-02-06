package ru.yandex.direct.jobs.bannersystem.export.job;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.mobilecontent.converter.MobileContentYtConverter;
import ru.yandex.direct.core.entity.mobilecontent.model.AgeLabel;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentAvatarSize;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentExternalWorldMoney;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreActionForPrices;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreCountry;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.mobilecontent.util.MobileContentUtil.getExternalWorldMoney;
import static ru.yandex.direct.core.testing.data.TestMobileContents.androidMobileContent;
import static ru.yandex.direct.core.testing.data.TestMobileContents.iosMobileContent;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

@JobsTest
@ExtendWith(SpringExtension.class)
class BsExportMobileContentParametersJobFunctionalTest extends BaseBsExportMobileContentJobFunctionalTest {
    private static final String NEW_ZEALAND = "NZ";
    private static final Map<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>> TEST_PRICES =
            ImmutableMap.<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>>builder()
                    .put(StoreCountry.RU.toString(),
                            ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                    .put(StoreActionForPrices.update, getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                    .build())
                    .put(StoreCountry.BY.toString(),
                            ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                    .put(StoreActionForPrices.open, getExternalWorldMoney("0", CurrencyCode.USD))
                                    .build())
                    .put(NEW_ZEALAND, ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                            .put(StoreActionForPrices.more, getExternalWorldMoney("0.15", CurrencyCode.USD))
                            .build())
                    .put(StoreCountry.TR.toString(),
                            ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                    .put(StoreActionForPrices.more, getExternalWorldMoney("0.15", "CAD"))
                                    .build())
                    .put(StoreCountry.US.toString(),
                            ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                    .put(StoreActionForPrices.more, new MobileContentExternalWorldMoney())
                                    .build())
                    .build();

    private static final Map<String, Map<String, Map<String, Object>>> TEST_PRICES_SERIALIZED;
    private static final List<Map<String, String>> MOBILE_APP_SCREENS = List.of(Map.of("width", "500", "path", "/aaaa", "height", "500"));
    private static final List<Map<String, String>> MOBILE_APP_SCREENS_BS = List.of(Map.of("width", "500", "path", "//avatars.mds.yandex.net/aaaa", "height", "500"));

    static {
        Map<String, Object> zeroPriceMap = new HashMap<>();
        zeroPriceMap.put("price", 0);
        zeroPriceMap.put("price_currency", null);

        TEST_PRICES_SERIALIZED = ImmutableMap.<String, Map<String, Map<String, Object>>>builder()
                .put("RU", ImmutableMap.<String, Map<String, Object>>builder()
                        .put("update", ImmutableMap.<String, Object>builder()
                                .put("price", 1.23)
                                .put("price_currency", 643)
                                .build())
                        .build())
                .put("BY", ImmutableMap.<String, Map<String, Object>>builder()
                        .put("open", zeroPriceMap)
                        .build())
                .put(NEW_ZEALAND, ImmutableMap.<String, Map<String, Object>>builder()
                        .put("more", ImmutableMap.<String, Object>builder()
                                .put("price", 0.15)
                                .put("price_currency", 840)
                                .build())
                        .build())
                .build();
    }


    private String convertAgeLabel(AgeLabel ageLabel) {
        String ageLabelNum = null;
        switch (ageLabel) {
            case _0_2B:
                ageLabelNum = "0";
                break;
            case _6_2B:
                ageLabelNum = "6";
                break;
            case _12_2B:
                ageLabelNum = "12";
                break;
            case _16_2B:
                ageLabelNum = "16";
                break;
            case _18_2B:
                ageLabelNum = "18";
                break;
        }
        return ageLabelNum;
    }

    private void checkDataSentCorrectly(Map<String, Object> requestItem, MobileContentInfo mobileContentInfo) {
        MobileContent mc = mobileContentInfo.getMobileContent();

        String osType = mc.getOsType() == OsType.IOS ? "iOS" : "Android";
        String storeName = mc.getOsType() == OsType.IOS ? "App Store" : "Google Play";
        String storeAppId = mc.getOsType() == OsType.IOS ? mc.getBundleId() : mc.getStoreContentId();
        String ageLabel = convertAgeLabel(mc.getAgeLabel());

        // Проверяем сначала ключи, чтобы не отправить лишнего
        assertThat(requestItem)
                .as("Отправили только корректные данные")
                .containsOnlyKeys(
                        "icon",
                        "rating",
                        "store_country",
                        "os_type",
                        "store_name",
                        "review_count",
                        "is_accessible",
                        "app_size_bytes",
                        "store_app_id",
                        "store_content_id",
                        "age_label",
                        "mobile_app_id",
                        "name",
                        "prices",
                        "download_count",
                        "screenshots");

        assertThat(requestItem)
                .as("Все ключи в хеше содержат ожидаемые данные")
                .contains(
                        entry("rating", mc.getRating().doubleValue()),
                        entry("store_country", mc.getStoreCountry()),
                        entry("os_type", osType),
                        entry("store_name", storeName),
                        entry("review_count", mc.getRatingVotes().intValue()),
                        entry("is_accessible", true),
                        entry("app_size_bytes", Ints.checkedCast(mc.getAppSize().toBytes())),
                        entry("store_app_id", storeAppId),
                        entry("store_content_id", mc.getStoreContentId()),
                        entry("age_label", ageLabel),
                        entry("mobile_app_id", mc.getId().intValue()),
                        entry("name", mc.getName()),
                        entry("download_count", ifNotNull(mc.getDownloads(), o -> o.toString() + "+")),
                        entry("screenshots", MOBILE_APP_SCREENS_BS))
                .hasEntrySatisfying("prices", matchedBy(beanDiffer(TEST_PRICES_SERIALIZED)))
                .hasEntrySatisfying("icon", matchedBy(beanDiffer(getIcons(mc))));
    }

    private Map<String, Map<String, Object>> getIcons(MobileContent mc) {
        List<MobileContentAvatarSize> sizes = Arrays.stream(MobileContentAvatarSize.values())
                .filter(MobileContentAvatarSize::hasDimensions)
                .collect(Collectors.toList());

        assertThat(sizes)
                .size().isGreaterThan(0);

        Map<String, Map<String, Object>> icons = new HashMap<>();
        for (MobileContentAvatarSize size : sizes) {
            icons.put(size.getName(), ImmutableMap.<String, Object>builder()
                    .put("URL", String.format("//avatars.mdst.yandex.net/get-%s/%s/%s",
                            mc.getOsType() == OsType.IOS ? "itunes-icon" : "google-play-app-icon",
                            mc.getIconHash(), size.getName()))
                    .put("Height", size.getHeight())
                    .put("Width", size.getWidth())
                    .build());
        }

        return icons;
    }

    /**
     * Проверяем, что для мобильного контента iOs отправляются верные значения в верных полях
     */
    @Test
    void testJobForIosMobileContent() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContent(
                new MobileContentInfo().withMobileContent(
                        iosMobileContent()
                                .withPrices(TEST_PRICES)
                                .withScreens(MOBILE_APP_SCREENS)
                ));
        Map<String, Object> requestItem = performRequestAndGetRequestItem(mobileContentInfo);

        checkDataSentCorrectly(requestItem, mobileContentInfo);
    }

    /**
     * Проверяем, что для мобильного контента Android отправляются верные значения в верных полях
     */
    @Test
    void testJobForAndroidMobileContent() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContent(
                new MobileContentInfo().withMobileContent(
                        androidMobileContent()
                                .withPrices(TEST_PRICES)
                                .withScreens(MOBILE_APP_SCREENS)
                ));
        Map<String, Object> requestItem = performRequestAndGetRequestItem(mobileContentInfo);

        checkDataSentCorrectly(requestItem, mobileContentInfo);
    }

    /**
     * Проверяем, что для мобильного контента c непромодерированной иконкой отправляется null в icon
     */
    @Test
    void testJobForMobileContentWithUnmoderatedIcon() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContentWithUnmoderatedIcon();
        Map<String, Object> requestItem = performRequestAndGetRequestItem(mobileContentInfo);

        assertThat(requestItem)
                .as("Отправляется null в icon")
                .contains(entry("icon", null));
    }

    /**
     * Проверяем, что для мобильного контента c незаданным размером отправляется null в app_size_bytes
     */
    @Test
    void testJobForMobileContentWithAbsentAppSize() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContentWithNoSize();
        Map<String, Object> requestItem = performRequestAndGetRequestItem(mobileContentInfo);

        assertThat(requestItem)
                .as("Отправляется null в app_size_bytes")
                .contains(entry("app_size_bytes", null));
    }

    /**
     * Проверяем, что для мобильного контента c незаданным возрастным ограничением отправляется null в age_label
     */
    @Test
    void testJobForMobileContentWithAbsentAgeLabel() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContentWithNoAgeLabel();
        Map<String, Object> requestItem = performRequestAndGetRequestItem(mobileContentInfo);

        assertThat(requestItem)
                .as("Отправляется null в age_label")
                .contains(entry("age_label", null));
    }

    /**
     * Проверяем, что для мобильного контента c незаданными оценками отправляется null в review_count и rating
     */
    @Test
    void testJobForMobileContentWithAbsentReview() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContentWithNoReview();
        Map<String, Object> requestItem = performRequestAndGetRequestItem(mobileContentInfo);

        assertThat(requestItem)
                .as("Отправляется null в review_count и rating")
                .contains(entry("rating", null), entry("review_count", null));
    }

    /**
     * Проверяем, что для мобильного контента c незаданными именем отправляется null
     */
    @Test
    void testJobForMobileContentWithName() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContent(
                new MobileContentInfo().withMobileContent(androidMobileContent().withName(null)));
        Map<String, Object> requestItem = performRequestAndGetRequestItem(mobileContentInfo);

        assertThat(requestItem)
                .as("Отправляется null в name")
                .contains(entry("name", null));
    }

    /**
     * Проверяем, что для мобильного контента c именем "undefined" отправляется null
     */
    @Test
    void testJobForMobileContentWithUndefinedName() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContent(
                new MobileContentInfo().withMobileContent(
                        androidMobileContent().withName(MobileContentYtConverter.UNDEFINED_NAME)));
        Map<String, Object> requestItem = performRequestAndGetRequestItem(mobileContentInfo);

        assertThat(requestItem)
                .as("Отправляется null в name")
                .contains(entry("name", null));
    }

    /**
     * Проверяем, что для мобильного контента с неизвестным кол-вом установок отправляется null
     */
    @Test
    void testJobForMobileContentWithAbsentDownloads() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContent(
                new MobileContentInfo().withMobileContent(androidMobileContent().withDownloads(null)));
        Map<String, Object> requestItem = performRequestAndGetRequestItem(mobileContentInfo);

        assertThat(requestItem)
                .as("Отправляется null в downloads")
                .contains(entry("download_count", null));
    }
}
