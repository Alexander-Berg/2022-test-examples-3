package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.mobilecontent.container.MobileAppStoreUrl;
import ru.yandex.direct.core.entity.mobilecontent.model.AgeLabel;
import ru.yandex.direct.core.entity.mobilecontent.model.AvailableAction;
import ru.yandex.direct.core.entity.mobilecontent.model.ContentType;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentExternalWorldMoney;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.mobilecontent.model.StatusIconModerate;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreActionForPrices;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreCountry;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.misc.dataSize.DataSize;

import static ru.yandex.direct.core.entity.mobilecontent.util.MobileAppStoreUrlParser.parseStrict;
import static ru.yandex.direct.core.entity.mobilecontent.util.MobileContentUtil.getExternalWorldMoney;

public class TestMobileContents {
    private TestMobileContents() {
    }


    private static final String ANDROID_STORE_URL = "https://play.google.com/store/apps/details?id=com.ya.test";

    public static MobileContent defaultMobileContentWithUrl() {
        return mobileContentFromStoreUrl(ANDROID_STORE_URL);
    }

    public static MobileContent defaultMobileContent() {
        return androidMobileContent();
    }

    public static MobileContent mobileContentFromStoreUrl(String storeUrl) {
        MobileAppStoreUrl parsedUrl = parseStrict(storeUrl);
        return fillDummyMobileContent(parsedUrl.toMobileContent());
    }

    public static MobileContent mobileContentWithAppIconModerationStatusReady() {
        return defaultMobileContent()
                .withStatusIconModerate(StatusIconModerate.READY);
    }

    public static MobileContent mobileContentWithAppIconModerationStatusSending() {
        return defaultMobileContent()
                .withStatusIconModerate(StatusIconModerate.SENDING);
    }

    public static MobileContent mobileContentWithAppIconModerationStatusSent() {
        return defaultMobileContent()
                .withStatusIconModerate(StatusIconModerate.SENT);
    }

    public static MobileContent mobileContentWithAppIconModerationStatusYes() {
        return defaultMobileContent()
                .withStatusIconModerate(StatusIconModerate.YES);
    }

    public static MobileContent mobileContentWithAppIconModerationStatusNo() {
        return defaultMobileContent()
                .withStatusIconModerate(StatusIconModerate.NO);
    }

    public static MobileContent mobileContentWithNoSize() {
        return defaultMobileContent()
                .withAppSize(null);
    }

    public static MobileContent mobileContentWithNoAgeLabel() {
        return defaultMobileContent()
                .withAgeLabel(null);
    }

    public static MobileContent mobileContentWithNoReview() {
        return defaultMobileContent()
                .withRating(null)
                .withRatingVotes(null);
    }

    public static MobileContent androidMobileContent() {
        return getDummyMobileContent()
                .withStoreContentId(generateBundleId())
                .withDownloads(1000L)
                .withScreens(List.of(Map.of("width", "500", "path", "/aaaa", "height", "500")))
                .withOsType(OsType.ANDROID);
    }

    public static MobileContent iosMobileContent() {
        return getDummyMobileContent()
                .withStoreContentId("id" + RandomStringUtils.randomNumeric(9))
                .withOsType(OsType.IOS)
                .withBundleId(generateBundleId());
    }

    private static MobileContent fillDummyMobileContent(MobileContent mobileContent) {
        return mobileContent
                .withIsAvailable(true)
                .withCreateTime(LocalDateTime.now())
                .withName("Test content")
                .withRating(BigDecimal.ONE)
                .withRatingVotes(nextPositiveSmallNum())
                .withIconHash(UUID.randomUUID().toString())
                .withStatusIconModerate(StatusIconModerate.YES)
                .withAppSize(DataSize.fromBytes(nextPositiveSmallNum()))
                .withStatusBsSynced(StatusBsSynced.NO)
                .withTriesCount(0)
                .withAvailableActions(ImmutableSet.<AvailableAction>builder()
                        .add(AvailableAction.buy)
                        .add(AvailableAction.update)
                        .build())
                .withAgeLabel(AgeLabel._6_2B)
                .withPrices(
                        ImmutableMap.<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>>builder()
                                .put(StoreCountry.RU.toString(),
                                        ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                                .put(StoreActionForPrices.update,
                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                .put(StoreActionForPrices.open,
                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                .put(StoreActionForPrices.buy,
                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                .put(StoreActionForPrices.more,
                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                .put(StoreActionForPrices.download,
                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                .put(StoreActionForPrices.install,
                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                .put(StoreActionForPrices.play,
                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                .put(StoreActionForPrices.get,
                                                        getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                                .build())
                                .put(StoreCountry.BY.toString(),
                                        ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                                .put(StoreActionForPrices.update,
                                                        getExternalWorldMoney("0", CurrencyCode.USD))
                                                .put(StoreActionForPrices.open,
                                                        getExternalWorldMoney("0", CurrencyCode.USD))
                                                .put(StoreActionForPrices.buy,
                                                        getExternalWorldMoney("0", CurrencyCode.USD))
                                                .put(StoreActionForPrices.more,
                                                        getExternalWorldMoney("0", CurrencyCode.USD))
                                                .put(StoreActionForPrices.download,
                                                        getExternalWorldMoney("0", CurrencyCode.USD))
                                                .put(StoreActionForPrices.install,
                                                        getExternalWorldMoney("0", CurrencyCode.USD))
                                                .put(StoreActionForPrices.play,
                                                        getExternalWorldMoney("0", CurrencyCode.USD))
                                                .put(StoreActionForPrices.get,
                                                        getExternalWorldMoney("0", CurrencyCode.USD))
                                                .build())
                                .put(StoreCountry.KZ.toString(),
                                        ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                                .put(StoreActionForPrices.update,
                                                        getExternalWorldMoney("0.15", CurrencyCode.USD))
                                                .put(StoreActionForPrices.open,
                                                        getExternalWorldMoney("0.15", CurrencyCode.USD))
                                                .put(StoreActionForPrices.buy,
                                                        getExternalWorldMoney("0.15", CurrencyCode.USD))
                                                .put(StoreActionForPrices.more,
                                                        getExternalWorldMoney("0.15", CurrencyCode.USD))
                                                .put(StoreActionForPrices.download,
                                                        getExternalWorldMoney("0.15", CurrencyCode.USD))
                                                .put(StoreActionForPrices.install,
                                                        getExternalWorldMoney("0.15", CurrencyCode.USD))
                                                .put(StoreActionForPrices.play,
                                                        getExternalWorldMoney("0.15", CurrencyCode.USD))
                                                .put(StoreActionForPrices.get,
                                                        getExternalWorldMoney("0.15", CurrencyCode.USD))
                                                .build())
                                // Нужно помнить, что тут могут быть неподдерживаемые валюты
                                .put(StoreCountry.TR.toString(),
                                        ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                                .put(StoreActionForPrices.update, getExternalWorldMoney("0.15", "CAD"))
                                                .build())
                                .build());
    }

    private static MobileContent getDummyMobileContent() {
        return fillDummyMobileContent(new MobileContent()
                .withContentType(ContentType.APP)
                .withStoreCountry(StoreCountry.RU.toString()));
    }

    private static String generateBundleId() {
        return "ru.yandex.dummy." + RandomStringUtils.randomAlphabetic(10);
    }

    private static long nextPositiveSmallNum() {
        return (long) Math.abs(RandomUtils.nextInt(0, Integer.MAX_VALUE));
    }
}
