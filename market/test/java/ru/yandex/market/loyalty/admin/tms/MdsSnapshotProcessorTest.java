package ru.yandex.market.loyalty.admin.tms;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.xerial.snappy.Snappy;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.loyalty.admin.mds.BytesContentConsumer;
import ru.yandex.market.loyalty.admin.mds.DirectoryEntry;
import ru.yandex.market.loyalty.admin.mds.Magic;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminRegionSettingsTest;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.dao.PromoKeyIndexDao;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.ReportPromoType;
import ru.yandex.market.loyalty.core.model.cashback.entity.PromoKeyIndexEntry;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromocodePromoBuilder;
import ru.yandex.market.loyalty.core.model.spread.SpreadDiscountPromoDescription;
import ru.yandex.market.loyalty.core.proto.FastPipeLine;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.FeedSskuSet;
import ru.yandex.market.loyalty.core.service.flash.FlashPromoService;
import ru.yandex.market.loyalty.core.service.spread.SpreadPromoService;
import ru.yandex.market.loyalty.core.utils.FlashPromoUtils;
import ru.yandex.market.loyalty.core.utils.PromoBundleUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;
import ru.yandex.misc.time.TimeUtils;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.admin.tms.MdsSnapshotProcessor.VERY_BIG_THRESHOLD;
import static ru.yandex.market.loyalty.admin.tms.MdsSnapshotProcessor.hashEntryPattern;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.BLUE_SET;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.CHEAPEST_AS_GIFT;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.service.discount.delivery.DeliveryDiscountCalculator.AMURSKI_DISTRICT;
import static ru.yandex.market.loyalty.core.service.discount.delivery.DeliveryDiscountCalculator.FAR_EASTERN_FEDERAL_DISTRICT;
import static ru.yandex.market.loyalty.core.service.discount.delivery.DeliveryDiscountCalculator.NORTH_CAUCASIAN_FEDERAL_DISTRICT;
import static ru.yandex.market.loyalty.core.service.discount.delivery.DeliveryDiscountCalculator.RUSSIA;
import static ru.yandex.market.loyalty.core.service.discount.delivery.DeliveryDiscountCalculator.SIBERIAN_FEDERAL_DISTRICT;
import static ru.yandex.market.loyalty.core.service.discount.delivery.DeliveryDiscountCalculator.URAL_FEDERAL_DISTRICT;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.flashDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.blueSet;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.cheapestAsGift;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.directionalMapping;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primaryItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.proportion;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.quantityInBundle;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.then;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.withQuantityInBundle;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.Cashback.defaultPercent;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
@TestFor(MdsSnapshotProcessor.class)
public class MdsSnapshotProcessorTest extends MarketLoyaltyAdminRegionSettingsTest {
    private static final long FEED_ID = 123;
    private static final String FLASH_PROMO_KEY = "flash promo";
    private static final String BUNDLE_PROMO_KEY = "bundle promo";
    private static final String CHEAPEST_AS_GIFT_PROMO_KEY = "cheapest as gift promo";
    private static final String BLUE_SET_PROMO_KEY = "blue set promo";
    private static final String PROMOCODE_PROMO_KEY = "promocode promo";
    private static final String PROMOCODE_PROMO_KEY_WITH_BUDGET = "promocode promo with budget";
    private static final String PROMOCODE_PROMO_KEY_INACTIVE = "promocode promo inactive";
    private static final String PROMOCODE = "promocode";
    private static final String PROMOCODE_WITH_BUDGET = "promocode with budget";
    private static final String PROMOCODE_INACTIVE = "promocode inactive";
    private static final String SPREAD_COUNT_PROMO_KEY = "spread count promo";
    private static final String SPREAD_COUNT_PROMO_KEY_ENDED = "spread count promo ended";
    private static final String FIRST_SSKU = "some promo offer";
    private static final String SECOND_SSKU = "some gift offer";
    private static final String REPORT_PROMO_KEY_INACTIVE = "reportPromoKey";

    @Autowired
    private PromoBundleService bundleService;
    @Autowired
    private FlashPromoService flashPromoService;
    @Autowired
    private SpreadPromoService spreadPromoService;
    @Autowired
    private PromoUtils promoUtils;
    @Autowired
    private MdsSnapshotProcessor mdsSnapshotProcessor;
    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private ClockForTests clock;
    @Value("${market.loyalty.mds.prefix}")
    private String mdsPrefix;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoKeyIndexDao promoKeyIndexDao;

    private static void notEmpty(FastPipeLine.MetaInfo metaInfo) {
        assertTrue(metaInfo.hasBlueFreeDeliveryThresholds());
        FastPipeLine.BlueFreeDeliveryThresholds blueFreeDeliveryThresholds = metaInfo.getBlueFreeDeliveryThresholds();
        assertTrue(blueFreeDeliveryThresholds.hasCurrency());
        assertEquals("RUR", blueFreeDeliveryThresholds.getCurrency());
        List<FastPipeLine.BlueFreeDeliveryThresholdByRegion> thresholdByRegionList =
                blueFreeDeliveryThresholds.getThresholdByRegionList();
        assertThat(thresholdByRegionList, hasItems(
                allOf(
                        hasProperty("regionTo", equalTo(NORTH_CAUCASIAN_FEDERAL_DISTRICT)),
                        hasProperty("threshold", equalTo(5000))
                ),
                allOf(
                        hasProperty("regionTo", equalTo(SIBERIAN_FEDERAL_DISTRICT)),
                        hasProperty("threshold", equalTo(5000))
                ),
                allOf(
                        hasProperty("regionTo", equalTo(URAL_FEDERAL_DISTRICT)),
                        hasProperty("threshold", equalTo(5000))
                ),
                allOf(
                        hasProperty("regionTo", equalTo(FAR_EASTERN_FEDERAL_DISTRICT)),
                        hasProperty("threshold", equalTo(7000))
                ),
                allOf(
                        hasProperty("regionTo", equalTo(RUSSIA)),
                        hasProperty("threshold", equalTo(2499))
                )
        ));

        assertThat(thresholdByRegionList, hasItem(
                allOf(
                        hasProperty("regionTo", equalTo(AMURSKI_DISTRICT)),
                        hasProperty("threshold", equalTo(VERY_BIG_THRESHOLD))
                )
        ));

        assertThat(
                metaInfo.getBlueGenericBundlesPromos().getPromosList(),
                hasItems(
                        hasProperty("promoKey", equalTo(BUNDLE_PROMO_KEY)),
                        hasProperty("promoKey", equalTo(CHEAPEST_AS_GIFT_PROMO_KEY)),
                        hasProperty("promoKey", equalTo(BLUE_SET_PROMO_KEY)),
                        hasProperty("promoKey", equalTo(FLASH_PROMO_KEY)),
                        hasProperty("promoKey", equalTo(SPREAD_COUNT_PROMO_KEY))
                )
        );

        assertThat(
                metaInfo.getPromoKeysDisabled().getPromosList(),
                hasItems(hasProperty("promoKey", equalTo(PROMOCODE_PROMO_KEY)),
                        hasProperty("promoKey", equalTo(SPREAD_COUNT_PROMO_KEY_ENDED)),
                        hasProperty("promoKey", equalTo(REPORT_PROMO_KEY_INACTIVE)))
        );

        assertThat(metaInfo.getPromoKeysDisabled().getPromosList(), hasSize(5));

        assertTrue(metaInfo.hasBlueItemWeightThresholds());
        FastPipeLine.BlueItemWeightThresholds blueItemWeightThresholds = metaInfo.getBlueItemWeightThresholds();
        List<FastPipeLine.BlueItemWeightThresholdByRegion> itemWeightThresholdByRegionList =
                blueItemWeightThresholds.getThresholdByRegionList();
        assertThat(itemWeightThresholdByRegionList, hasItems(
                allOf(
                        hasProperty("regionTo", equalTo(RUSSIA)),
                        hasProperty("threshold", equalTo(20))
                )
        ));

        assertTrue(metaInfo.hasBluePromoWithExceededBudget());
        final FastPipeLine.BluePromoWithExceededBudget promoWithExceededBudget =
                metaInfo.getBluePromoWithExceededBudget();
        final List<FastPipeLine.BluePromoWithExceededBudget.ItemWithExceededBudget> itemsWithExceededBudget =
                promoWithExceededBudget.getItemsWithExceededBudgetList();
        assertThat(itemsWithExceededBudget, hasSize(0));
    }

    private static void verifyThresholds(
            Verifier<byte[]> fastPipeLineVerifier, byte[] bytes,
            DirectoryEntry directoryEntry
    ) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            assertHeader(bais, Magic.SNAPPY);
            byte[] fileContent = IOUtils.toByteArray(bais, readLength(bais));
            try (ByteArrayInputStream bais2 = new ByteArrayInputStream(Snappy.uncompress(fileContent))) {
                assertHeader(bais2, directoryEntry.getMagic());
                fastPipeLineVerifier.verify(IOUtils.toByteArray(bais2, readLength(bais2)));
            }
        }
    }

    private static void assertHeader(InputStream is, Magic magic) throws IOException {
        byte[] actualHeader = IOUtils.toByteArray(is, 4);
        assertArrayEquals(magic.getSignature(), actualHeader);
    }

    private static int readLength(InputStream is) throws IOException {
        return ByteBuffer.wrap(IOUtils.toByteArray(is, 4)).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    @Test
    public void shouldUploadFixed() throws Exception {
        processAndVerify(
                MdsSnapshotProcessorTest::notEmpty,
                ""
        );
    }

    @Test
    public void shouldUploadFixedForPreviousEntryFile() throws Exception {
        processAndVerify(
                MdsSnapshotProcessorTest::notEmpty,
                "baadf00d *loyalty_info.pbuf.sn\n" +
                        "deadbeef *timestamp"
        );
    }

    /**
     * Test cases is taken from <a href="https://en.wikipedia.org/wiki/MD5#MD5_hashes">wiki</a>.
     */
    @Test
    public void testEvalHash() {
        assertEquals(
                "9e107d9d372bb6826bd81d3542a419d6",
                MdsSnapshotProcessor.evalHash(
                        "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.ISO_8859_1))
        );
        assertEquals(
                "e4d909c290d0fb1ca068ffaddf22cbd0",
                MdsSnapshotProcessor.evalHash(
                        "The quick brown fox jumps over the lazy dog.".getBytes(StandardCharsets.ISO_8859_1))
        );
        //noinspection ZeroLengthArrayAllocation
        assertEquals(
                "d41d8cd98f00b204e9800998ecf8427e",
                MdsSnapshotProcessor.evalHash(new byte[0])
        );
    }

    private void processAndVerify(
            Verifier<FastPipeLine.MetaInfo> assertMetaInfo, String previousHashes
    ) throws Exception {
        when(mdsS3Client.download(
                argThat(hasProperty("key", equalTo(mdsPrefix + DirectoryEntry.HASHES.getFileName()))),
                argThat(isA(BytesContentConsumer.class))
        ))
                .thenReturn(previousHashes.getBytes(StandardCharsets.ISO_8859_1));

        mdsSnapshotProcessor.process();

        ArgumentCaptor<ResourceLocation> resourceLocationCaptor = ArgumentCaptor.forClass(ResourceLocation.class);
        ArgumentCaptor<ContentProvider> contentProviderCaptor = ArgumentCaptor.forClass(ContentProvider.class);
        int directoryEntriesCnt = DirectoryEntry.values().length;
        verify(mdsS3Client, times(directoryEntriesCnt)).upload(
                resourceLocationCaptor.capture(),
                contentProviderCaptor.capture()
        );

        Map<DirectoryEntry, byte[]> fileContents = new EnumMap<>(DirectoryEntry.class);

        List<ResourceLocation> resourceLocations = resourceLocationCaptor.getAllValues();
        List<ContentProvider> contentProviders = contentProviderCaptor.getAllValues();

        for (int i = 0; i < directoryEntriesCnt; ++i) {
            String resourceLocationKey = resourceLocations.get(i).getKey();
            ContentProvider contentProvider = contentProviders.get(i);
            for (DirectoryEntry directoryEntry : DirectoryEntry.values()) {
                if (resourceLocationKey.contains(directoryEntry.getFileName())) {
                    try (InputStream is = contentProvider.getInputStream()) {
                        fileContents.put(directoryEntry, IOUtils.toByteArray(is));
                    }
                }
            }
        }

        Map<DirectoryEntry, Verifier<byte[]>> ENTRY_VERIFIERS = new EnumMap<>(DirectoryEntry.class);
        ENTRY_VERIFIERS.put(DirectoryEntry.TIMESTAMP, this::verifyTimestamp);
        ENTRY_VERIFIERS.put(DirectoryEntry.HASHES, bytes -> verifyHashes(bytes, fileContents));
        ENTRY_VERIFIERS.put(DirectoryEntry.LOYALTY_INFO, bytes ->
                verifyThresholds(
                        bytes1 -> assertMetaInfo.verify(FastPipeLine.MetaInfo.parseFrom(bytes1)), bytes,
                        DirectoryEntry.LOYALTY_INFO
                )
        );

        for (DirectoryEntry directoryEntry : DirectoryEntry.values()) {
            Verifier<byte[]> verifier = ENTRY_VERIFIERS.get(directoryEntry);
            assertNotNull(verifier);
            verifier.verify(fileContents.get(directoryEntry));
        }
    }

    private void verifyTimestamp(byte[] bytes) {
        long timestamp = Long.parseLong(new String(bytes, StandardCharsets.ISO_8859_1));
        assertThat(timestamp, equalTo(TimeUnit.MILLISECONDS.toSeconds(clock.millis())));
    }

    @Before
    public void prepare() {
        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoId(BUNDLE_PROMO_KEY),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                primaryItem(FEED_ID, FIRST_SSKU),
                giftItem(FEED_ID, directionalMapping(
                        PromoBundleUtils.when(FIRST_SSKU),
                        then(SECOND_SSKU),
                        proportion(40)
                ))
        ));

        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoId(CHEAPEST_AS_GIFT_PROMO_KEY),
                strategy(CHEAPEST_AS_GIFT),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                withQuantityInBundle(3),
                item(
                        condition(cheapestAsGift(FeedSskuSet.of(FEED_ID, List.of(FIRST_SSKU, SECOND_SSKU)))),
                        quantityInBundle(3),
                        primary()
                )
        ));

        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoId(BLUE_SET_PROMO_KEY),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(FIRST_SSKU, 30),
                                proportion(SECOND_SSKU, 40)
                        )),
                        primary()
                )
        ));

        flashPromoService.createPromo(flashDescription(
                FlashPromoUtils.promoSource(LOYALTY_VALUE),
                FlashPromoUtils.feedId(FEED_ID),
                FlashPromoUtils.promoKey(FLASH_PROMO_KEY),
                FlashPromoUtils.shopPromoId(FLASH_PROMO_KEY),
                FlashPromoUtils.starts(clock.dateTime()),
                FlashPromoUtils.ends(clock.dateTime().plusYears(10))
        ));

        TimeUtils.setDefaultTimeZone();
        PromocodePromoBuilder builder = PromoUtils.SmartShopping.defaultFixedPromocode();
        builder.setBudget(BigDecimal.ZERO)
                .setStatus(PromoStatus.ACTIVE)
                .setActionCode(PROMOCODE)
                .setPromoKey(PROMOCODE_PROMO_KEY);
        PromocodePromoBuilder builderNotZeroBudget = PromoUtils.SmartShopping.defaultFixedPromocode();
        builderNotZeroBudget.setBudget(BigDecimal.TEN)
                .setStatus(PromoStatus.ACTIVE)
                .setEndDate(Date.from(LocalDateTime.now().minusDays(3L).toInstant(ZoneOffset.UTC)))
                .setActionCode(PROMOCODE_WITH_BUDGET)
                .setPromoKey(PROMOCODE_PROMO_KEY_WITH_BUDGET);

        PromocodePromoBuilder builderInactivePromocode = PromoUtils.SmartShopping.defaultFixedPromocode();
        builderInactivePromocode.setBudget(BigDecimal.TEN)
                .setStatus(PromoStatus.INACTIVE)
                .setEndDate(Date.from(LocalDateTime.now().minusDays(3L).toInstant(ZoneOffset.UTC)))
                .setActionCode(PROMOCODE_INACTIVE)
                .setPromoKey(PROMOCODE_PROMO_KEY_INACTIVE);

        promoUtils.buildPromocodePromo(builder);
        promoUtils.buildPromocodePromo(builderNotZeroBudget);
        promoUtils.buildPromocodePromo(builderInactivePromocode);

        spreadPromoService.createOrUpdateSpreadPromo(SpreadDiscountPromoDescription.builder()
                .promoSource(LOYALTY_VALUE)
                .feedId(FEED_ID)
                .promoKey(SPREAD_COUNT_PROMO_KEY)
                .source(SPREAD_COUNT_PROMO_KEY)
                .shopPromoId(SPREAD_COUNT_PROMO_KEY)
                .startTime(clock.dateTime())
                .endTime(clock.dateTime().plusYears(10))
                .name(SPREAD_COUNT_PROMO_KEY)
                .budgetLimit(BigDecimal.ZERO)
                .promoType(ReportPromoType.SPREAD_COUNT)
                .build()
        );
        spreadPromoService.createOrUpdateSpreadPromo(SpreadDiscountPromoDescription.builder()
                .promoSource(LOYALTY_VALUE)
                .feedId(FEED_ID)
                .promoKey(SPREAD_COUNT_PROMO_KEY_ENDED)
                .source(SPREAD_COUNT_PROMO_KEY_ENDED)
                .shopPromoId(SPREAD_COUNT_PROMO_KEY_ENDED)
                .startTime(clock.dateTime().minusYears(10))
                .endTime(clock.dateTime().minusYears(1))
                .name(SPREAD_COUNT_PROMO_KEY_ENDED)
                .budgetLimit(BigDecimal.ZERO)
                .promoType(ReportPromoType.SPREAD_COUNT)
                .build()
        );


        Promo promo = promoManager.createCashbackPromo(
                defaultPercent(10)
                        .setStatus(PromoStatus.INACTIVE));
        promoKeyIndexDao.save(PromoKeyIndexEntry.builder()
                .setLoyaltyPromoKey(promo.getPromoKey())
                .setReportPromoKey(REPORT_PROMO_KEY_INACTIVE)
                .build());

    }

    private interface Verifier<T> {
        void verify(T t) throws Exception;
    }

    private static void verifyHashes(byte[] bytes, Map<DirectoryEntry, byte[]> fileContents) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(bytes),
                StandardCharsets.ISO_8859_1
        ))
        ) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (StringUtils.isEmpty(line)) {
                    continue;
                }
                count++;
                Matcher matcher = hashEntryPattern.matcher(line);
                assertTrue(matcher.matches());
                String hashStr = matcher.group("hash");
                String fileName = matcher.group("fileName");
                byte[] fileContent = fileContents.get(DirectoryEntry.findByFileName(fileName));
                assertNotNull(fileContent);
                assertEquals(MdsSnapshotProcessor.evalHash(fileContent), hashStr);
            }
            // Do not count HASHES file itself
            assertEquals(fileContents.size() - 1, count);
        }
    }
}
