package ru.yandex.direct.jobs.bannersystem.export.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.bannersystem.BsImportAppStoreDataClient;
import ru.yandex.direct.bannersystem.container.appstoredata.BsImportAppStoreDataIcon;
import ru.yandex.direct.bannersystem.container.appstoredata.BsImportAppStoreDataItem;
import ru.yandex.direct.bannersystem.container.appstoredata.BsImportAppStoreDataPrice;
import ru.yandex.direct.bannersystem.container.appstoredata.BsImportAppStoreDataResponseItem;
import ru.yandex.direct.core.entity.mobilecontent.model.AgeLabel;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentAvatarSize;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentExternalWorldMoney;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.mobilecontent.model.StatusIconModerate;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreActionForPrices;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreCountry;
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.misc.dataSize.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.mobilecontent.util.MobileContentUtil.getExternalWorldMoney;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

class BsMobileContentExporterTest {
    private static final int TEST_SHARD = 2;
    private static final int TEST_LIMIT = 5;
    private static final String TEST_ICON_HASH = "Test";
    private static final String TEST_ICON_URL = "http://yandex.ru";
    private static final String TEST_CONTENT_ID = "Some Id";
    private static final String TEST_STORE = "GOOGLE PLAY";
    private static final long MOB_CONT_ID_ONE = 1;
    private static final long MOB_CONT_ID_TWO = 2;
    private static final long MOB_CONT_ID_THREE = 3;
    private static final long MOB_CONT_ID_FOUR = 4;
    private static final List<Long> TEST_IDS =
            Arrays.asList(MOB_CONT_ID_ONE, MOB_CONT_ID_TWO, MOB_CONT_ID_THREE, MOB_CONT_ID_FOUR);

    @Mock
    private BsExportMobileContentService bsExportMobileContentService;
    @Mock
    private MobileContentService mobileContentService;
    @Mock
    private BsImportAppStoreDataClient bsClient;

    private BsMobileContentExporter exporter;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        exporter = new BsMobileContentExporter(TEST_SHARD, TEST_LIMIT,
                TEST_IDS,
                bsExportMobileContentService,
                mobileContentService, bsClient);
    }

    @Test
    void testParseBsResponse() {
        Set<Long> processedIds = exporter.parseBsResponse(
                ImmutableSet.<Long>builder()
                        .add(MOB_CONT_ID_ONE, MOB_CONT_ID_TWO, MOB_CONT_ID_THREE, MOB_CONT_ID_FOUR)
                        .build(),
                Arrays.asList(
                        new BsImportAppStoreDataResponseItem().withMobileAppId(MOB_CONT_ID_ONE),
                        new BsImportAppStoreDataResponseItem().withMobileAppId(MOB_CONT_ID_FOUR),
                        new BsImportAppStoreDataResponseItem().withMobileAppId(MOB_CONT_ID_THREE).withError(1),
                        new BsImportAppStoreDataResponseItem().withMobileAppId(MOB_CONT_ID_TWO).withError(1)
                )
        );

        assertThat(processedIds)
                .as("В результат попали только идентификаторы без ошибок")
                .containsOnly(MOB_CONT_ID_ONE, MOB_CONT_ID_FOUR);
    }

    @Test
    void testParseBsResponseFillsIndicators() {
        exporter.parseBsResponse(
                ImmutableSet.<Long>builder()
                        .add(MOB_CONT_ID_ONE, MOB_CONT_ID_TWO, MOB_CONT_ID_THREE, MOB_CONT_ID_FOUR)
                        .build(),
                Arrays.asList(
                        new BsImportAppStoreDataResponseItem().withMobileAppId(MOB_CONT_ID_ONE),
                        new BsImportAppStoreDataResponseItem().withMobileAppId(MOB_CONT_ID_THREE).withError(1),
                        new BsImportAppStoreDataResponseItem().withMobileAppId(MOB_CONT_ID_TWO).withError(1),
                        new BsImportAppStoreDataResponseItem()
                )
        );

        assertThat(exporter.getIndicators().getItemsError())
                .as("Посчитали отправленные с ошибкой, потерянные не посчитали")
                .isEqualTo(3);
    }

    @Test
    void testProcessBsResponse() {
        doReturn(3)
                .when(bsExportMobileContentService).setStatusSynced(eq(TEST_SHARD), any());

        exporter.processBsResponse(
                ImmutableSet.<Long>builder()
                        .add(MOB_CONT_ID_ONE, MOB_CONT_ID_TWO, MOB_CONT_ID_THREE, MOB_CONT_ID_FOUR)
                        .build()
        );

        verify(bsExportMobileContentService).setStatusSynced(eq(TEST_SHARD), any());

        assertThat(exporter.getIndicators().getItemsSynced())
                .as("Посчитали успешно обработанные в БК (а не успешно сохраненные)")
                .isEqualTo(4);
    }

    @Test
    void testParseBsResponseWithAbsent() {
        Set<Long> processedIds = exporter.parseBsResponse(
                ImmutableSet.<Long>builder()
                        .add(MOB_CONT_ID_ONE, MOB_CONT_ID_TWO)
                        .build(),
                Collections.singletonList(
                        new BsImportAppStoreDataResponseItem().withMobileAppId(MOB_CONT_ID_ONE)
                )
        );

        assertThat(processedIds)
                .as("В результат попали только идентификаторы из списка отправленных о которых есть информация")
                .containsOnly(MOB_CONT_ID_ONE);
    }

    @Test
    void testParseBsResponseWithExtra() {
        Set<Long> processedIds = exporter.parseBsResponse(
                Collections.singleton(MOB_CONT_ID_ONE),
                Arrays.asList(
                        new BsImportAppStoreDataResponseItem().withMobileAppId(MOB_CONT_ID_ONE),
                        new BsImportAppStoreDataResponseItem().withMobileAppId(MOB_CONT_ID_FOUR)
                )
        );

        assertThat(processedIds)
                .as("В результат попали только идентификаторы из списка отправленных")
                .containsOnly(MOB_CONT_ID_ONE);
    }

    @Test
    void testGetPricesData() {
        Map<String, Map<String, BsImportAppStoreDataPrice>> pricesData = exporter.getPricesData(
                ImmutableMap.<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>>builder()
                        .put(StoreCountry.KZ.toString(),
                                Collections
                                        .singletonMap(StoreActionForPrices.get,
                                                getExternalWorldMoney("1.0", CurrencyCode.RUB)))
                        .put(StoreCountry.BY.toString(),
                                Collections
                                        .singletonMap(StoreActionForPrices.get,
                                                getExternalWorldMoney("1.0", "CAD")))
                        .build()
        );
        assertThat(pricesData)
                .as("В списке только переданная страна")
                .containsOnlyKeys(StoreCountry.KZ.toString());

        Map<String, BsImportAppStoreDataPrice> value = pricesData.get(StoreCountry.KZ.toString());
        assertThat(value)
                .as("В списке только переданное действие по заданной цене")
                .containsOnly(entry(StoreActionForPrices.get.toString(),
                        new BsImportAppStoreDataPrice(new BigDecimal("1.0"), 643)));
    }

    @Test
    void testGetPricesDataNullPrice() {
        Map<String, Map<String, BsImportAppStoreDataPrice>> pricesData = exporter.getPricesData(
                ImmutableMap.<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>>builder()
                        .put(StoreCountry.KZ.toString(),
                                Collections
                                        .singletonMap(StoreActionForPrices.get,
                                                new MobileContentExternalWorldMoney()
                                                        .withSum(null)
                                                        .withCurrency(CurrencyCode.RUB.toString())))
                        .build()
        );
        assertThat(pricesData)
                .as("В списке только переданная страна")
                .containsOnlyKeys(StoreCountry.KZ.toString());

        Map<String, BsImportAppStoreDataPrice> value = pricesData.get(StoreCountry.KZ.toString());
        assertThat(value)
                .as("В списке только переданное действие по заданной цене")
                .containsOnly(entry(StoreActionForPrices.get.toString(),
                        new BsImportAppStoreDataPrice(BigDecimal.ZERO, null)));
    }

    @Test
    void testGetPricesDataZeroPrice() {
        Map<String, Map<String, BsImportAppStoreDataPrice>> pricesData = exporter.getPricesData(
                ImmutableMap.<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>>builder()
                        .put(StoreCountry.RU.toString(),
                                Collections
                                        .singletonMap(StoreActionForPrices.download,
                                                getExternalWorldMoney("0.0", CurrencyCode.RUB)))
                        .build()
        );
        assertThat(pricesData)
                .as("В списке только переданная страна")
                .containsOnlyKeys(StoreCountry.RU.toString());

        Map<String, BsImportAppStoreDataPrice> value = pricesData.get(StoreCountry.RU.toString());
        assertThat(value)
                .as("В списке только переданное действие по заданной цене")
                .contains(entry(StoreActionForPrices.download.toString(),
                        new BsImportAppStoreDataPrice(BigDecimal.ZERO, null)));
    }

    @Test
    void testGetPricesDataPriceNull() {
        //noinspection ConstantConditions
        Map<String, Map<String, BsImportAppStoreDataPrice>> pricesData = exporter.getPricesData(
                ImmutableMap.<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>>builder()
                        .put(StoreCountry.RU.toString(),
                                Collections
                                        .singletonMap(StoreActionForPrices.download,
                                                getExternalWorldMoney("0.0", (String) null)))
                        .build()
        );
        assertThat(pricesData)
                .isEmpty();
    }

    @Test
    void testGetIconsNotModerated() {
        Map<String, BsImportAppStoreDataIcon> iconMap = exporter.getIconsData(
                new MobileContent()
                        .withIconHash(TEST_ICON_HASH)
                        .withStatusIconModerate(StatusIconModerate.NO));

        assertThat(iconMap)
                .isNull();
    }

    @Test
    void testGetIconsIconHashNull() {
        Map<String, BsImportAppStoreDataIcon> iconMap = exporter.getIconsData(
                new MobileContent()
                        .withStatusIconModerate(StatusIconModerate.YES));

        assertThat(iconMap)
                .isNull();
    }

    @Test
    void testGetIcons() {
        doReturn(Collections.singletonList(MobileContentAvatarSize.ICON_L))
                .when(mobileContentService).getAvatarsSizes();

        doReturn(TEST_ICON_URL)
                .when(mobileContentService)
                .generateUrlString(eq(OsType.ANDROID), eq(TEST_ICON_HASH), eq(MobileContentAvatarSize.ICON_L));

        Map<String, BsImportAppStoreDataIcon> iconMap = exporter.getIconsData(
                new MobileContent()
                        .withOsType(OsType.ANDROID)
                        .withIconHash(TEST_ICON_HASH)
                        .withStatusIconModerate(StatusIconModerate.YES));

        assertThat(iconMap)
                .as("Конвертация выполнена ожидамо")
                .containsOnly(
                        entry(MobileContentAvatarSize.ICON_L.getName(),
                                new BsImportAppStoreDataIcon(
                                        TEST_ICON_URL,
                                        MobileContentAvatarSize.ICON_L.getWidth(),
                                        MobileContentAvatarSize.ICON_L.getHeight()
                                )
                        )
                );
    }

    @Test
    void testGetQuery() {
        doReturn(
                Collections.singletonList(
                        new MobileContent()
                                .withId(MOB_CONT_ID_ONE)
                                .withIsAvailable(false)
                                .withAppSize(DataSize.MEGABYTE)
                                .withOsType(OsType.ANDROID)
                                .withStoreContentId(TEST_CONTENT_ID)
                                .withPrices(
                                        ImmutableMap.<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>>builder()
                                                .put(StoreCountry.BY.toString(),
                                                        Collections.singletonMap(StoreActionForPrices.buy,
                                                                getExternalWorldMoney("1.23", CurrencyCode.RUB)))
                                                .build())
                                .withAgeLabel(AgeLabel._0_2B)
                                .withStoreCountry(StoreCountry.RU.toString())
                                .withRating(BigDecimal.ONE)
                                .withRatingVotes(1024L)
                                .withIconHash("somehash")
                                .withStatusIconModerate(StatusIconModerate.YES)
                ))
                .when(bsExportMobileContentService)
                .getMobileContentForBsExport(eq(TEST_SHARD), eq(TEST_LIMIT), eq(TEST_IDS));

        doReturn(TEST_STORE)
                .when(mobileContentService).getStoreName(any());
        doReturn(TEST_CONTENT_ID)
                .when(mobileContentService).getStoreAppId(any());
        doReturn(Collections.singletonList(MobileContentAvatarSize.ICON_LD_RETINA))
                .when(mobileContentService).getAvatarsSizes();
        doReturn("//yandex.ru/test")
                .when(mobileContentService).generateUrlString(any(), any(), any());

        List<BsImportAppStoreDataItem> query = exporter.getExportQuery();
        assertThat(query).size()
                .as("Получили только один результат в запросе")
                .isEqualTo(1);

        BsImportAppStoreDataItem item = query.get(0);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(item.getAgeLabel())
                .isEqualTo("0");
        soft.assertThat(item.getAppSizeBytes())
                .isEqualTo(DataSize.MEGABYTE.toBytes());
        soft.assertThat(item.getIsAccessible())
                .isEqualTo(false);
        soft.assertThat(item.getMobileAppId())
                .isEqualTo(MOB_CONT_ID_ONE);
        soft.assertThat(item.getStoreAppId())
                .isEqualTo(TEST_CONTENT_ID);
        soft.assertThat(item.getStoreContentId())
                .isEqualTo(TEST_CONTENT_ID);
        soft.assertThat(item.getStoreCountry())
                .isEqualTo(StoreCountry.RU.toString());
        soft.assertThat(item.getStoreName())
                .isEqualTo(TEST_STORE);
        soft.assertThat(item.getOsType())
                .isEqualTo("Android");
        soft.assertThat(item.getReviewCount())
                .isEqualTo(1024L);
        soft.assertThat(item.getReviewRating())
                .isEqualTo(BigDecimal.ONE);
        soft.assertThat(item.getIcons())
                .is(matchedBy(beanDiffer(Collections.singletonMap(MobileContentAvatarSize.ICON_LD_RETINA.getName(),
                        new BsImportAppStoreDataIcon(
                                "//yandex.ru/test",
                                MobileContentAvatarSize.ICON_LD_RETINA.getWidth(),
                                MobileContentAvatarSize.ICON_LD_RETINA.getHeight())))));
        soft.assertThat(item.getPrices())
                .is(matchedBy(beanDiffer(
                        ImmutableMap.<String, Map<String, BsImportAppStoreDataPrice>>builder()
                                .put("BY", Collections.singletonMap("buy",
                                        new BsImportAppStoreDataPrice(new BigDecimal("1.23"), 643)))
                                .build()
                )));

        soft.assertAll();
    }

    @Test
    void testOneBsIteration() {
        List<BsImportAppStoreDataItem> queryItems = new ArrayList<>();
        List<BsImportAppStoreDataResponseItem> responseItems = new ArrayList<>();
        for (int i = 1; i <= TEST_LIMIT; i++) {
            queryItems.add(new BsImportAppStoreDataItem());
            if (i != TEST_LIMIT) {
                responseItems.add(new BsImportAppStoreDataResponseItem());
            }
        }

        exporter = spy(exporter);
        doReturn(queryItems)
                .when(exporter).getExportQuery();
        doReturn(Collections.emptySet())
                .when(exporter).parseBsResponse(any(), any());
        doNothing()
                .when(exporter).processBsResponse(any());

        doReturn(responseItems)
                .when(bsClient).sendAppStoreData(any(), any(), any());

        exporter.sendOneChunkOfMobileContent();

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(exporter.getIndicators().getItemsSent())
                .as("Посчитали отправленные в БК")
                .isEqualTo(TEST_LIMIT);

        soft.assertThat(exporter.getIndicators().getItemsReceived())
                .as("Посчитали полученные из БК")
                .isEqualTo(TEST_LIMIT - 1);

        soft.assertAll();
    }

    @Test
    void testOneBsIterationLimitFlagOn() {
        List<BsImportAppStoreDataItem> queryItems = new ArrayList<>();
        List<BsImportAppStoreDataResponseItem> responseItems = new ArrayList<>();
        for (int i = 1; i <= TEST_LIMIT; i++) {
            queryItems.add(new BsImportAppStoreDataItem());
            if (i != TEST_LIMIT) {
                responseItems.add(new BsImportAppStoreDataResponseItem());
            }
        }

        exporter = spy(exporter);
        doReturn(queryItems)
                .when(exporter).getExportQuery();
        doReturn(Collections.emptySet())
                .when(exporter).parseBsResponse(any(), any());
        doNothing()
                .when(exporter).processBsResponse(any());

        doReturn(responseItems)
                .when(bsClient).sendAppStoreData(any(), any(), any());

        exporter.sendOneChunkOfMobileContent();

        assertThat(exporter.isLimitExceeded())
                .isTrue();
    }

    @Test
    void testOneBsIterationLimitFlagOff() {
        List<BsImportAppStoreDataItem> queryItems = new ArrayList<>();
        List<BsImportAppStoreDataResponseItem> responseItems = new ArrayList<>();
        for (int i = 1; i <= TEST_LIMIT - 1; i++) {
            queryItems.add(new BsImportAppStoreDataItem());
            responseItems.add(new BsImportAppStoreDataResponseItem());
        }

        exporter = spy(exporter);
        doReturn(queryItems)
                .when(exporter).getExportQuery();
        doReturn(Collections.emptySet())
                .when(exporter).parseBsResponse(any(), any());
        doNothing()
                .when(exporter).processBsResponse(any());

        doReturn(responseItems)
                .when(bsClient).sendAppStoreData(any(), any(), any());

        exporter.sendOneChunkOfMobileContent();

        assertThat(exporter.isLimitExceeded())
                .isFalse();
    }
}
