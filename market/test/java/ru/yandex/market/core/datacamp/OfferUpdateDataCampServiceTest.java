package ru.yandex.market.core.datacamp;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampOfferStatus;
import NMarketIndexer.Common.Common;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.logbroker.MarketQuickLogbrokerSendingService;
import ru.yandex.market.core.logbroker.event.datacamp.DataCampEvent;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.core.offer.IndexerOfferKey;
import ru.yandex.market.core.offer.KnownShopIndexerOfferKey;
import ru.yandex.market.core.offer.OfferUpdate;
import ru.yandex.market.core.offer.PapiMarketSkuOfferService;
import ru.yandex.market.core.price.PapiOfferPriceValue;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "OfferUpdateDataCampServiceTest.before.csv")
class OfferUpdateDataCampServiceTest extends FunctionalTest {
    private static final int SHOP_ID = 774;
    private static final int SHOP_FEED_ID = 113;
    private static final int SUPPLIER_ID = 876;
    private static final long SUPPLIER_FEED_ID = 76;
    private static final String SHOP_SKU1 = "SKU123";
    private OfferUpdateDataCampService offerUpdateDataCampService;

    @Autowired
    @Qualifier("marketQuickLogbrokerService")
    private LogbrokerEventPublisher<SyncChangeOfferLogbrokerEvent> logbrokerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MarketQuickLogbrokerSendingService logbrokerSendingService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PushSettingsService pushSettingsService;

    @BeforeEach
    void setUp() {
        ZonedDateTime time = ZonedDateTime.of(2019, 8, 1, 12, 0, 0, 0, ZoneId.systemDefault());
        offerUpdateDataCampService = new OfferUpdateDataCampService(
                new PapiMarketSkuOfferService(jdbcTemplate),
                logbrokerSendingService,
                transactionTemplate,
                pushSettingsService
        );
        offerUpdateDataCampService.setClock(Clock.fixed(time.toInstant(), time.getZone()));
    }

    @AfterEach
    void tearDown() {
        offerUpdateDataCampService.setClock(null);
    }

    @Test
    @DbUnitDataSet(after = "OfferUpdateDataCampServiceTest.testSupplierPriceSending.after.csv")
    void testSupplierPriceSendingFeedExpansionDisabled() {
        Instant updateTime = Instant.now();
        IndexerOfferKey offerKey = IndexerOfferKey.offerId(SUPPLIER_FEED_ID, SHOP_SKU1);
        PartnerId partner = PartnerId.supplierId(SUPPLIER_ID);
        OfferUpdate offerUpdate = getOfferUpdateForSupplierPriceTests(updateTime, offerKey, partner);
        offerUpdateDataCampService.publishUpdates(partner, Collections.singletonList(offerUpdate), updateTime, 123);
        List<DataCampOffer.Offer> offers = verifyOfferUpdatePublished();
        DataCampOffer.Offer expectedOffer1 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setFeedId(76)
                        .setOfferId(SHOP_SKU1)
                        .setShopId(SUPPLIER_ID)
                        .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                .setShopSku(SHOP_SKU1)
                                .build())
                        .build())
                .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                        .setRgb(DataCampOfferMeta.MarketColor.BLUE)
                        .build())
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(123_0000000)
                                        .setId("RUR")
                                        .build())
                                .setBinaryOldprice(Common.PriceExpression.newBuilder()
                                        .setPrice(200_0000000)
                                        .setId("RUR")
                                        .build())
                                .setEnabled(true)
                                .setVat(VatRate.VAT_20.getId())
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setSource(DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                                        .setTimestamp(DateTimes.toTimestamp(updateTime))
                                        .build())
                                .build())
                        .setOriginalPriceFields(DataCampOfferPrice.OriginalPriceFields.newBuilder()
                                .setVat(DataCampOfferPrice.VatValue.newBuilder()
                                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                .setSource(DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                                                .setTimestamp(DateTimes.toTimestamp(updateTime))
                                                .build())
                                        .setValue(DataCampOfferPrice.Vat.VAT_20)
                                        .build())
                                .build())
                        .build())
                .build();
        assertThat(offers).singleElement().isEqualTo(expectedOffer1).satisfies(actualOffer1 -> {
            assertThat(actualOffer1.getIdentifiers().hasWarehouseId()).isFalse();
        });
    }

    @Test
    @DbUnitDataSet(after = "OfferUpdateDataCampServiceTest.testSupplierPriceSending.after.csv")
    void testSupplierPriceAndAutoOldPriceSending() {
        Instant updateTime = Instant.now();
        IndexerOfferKey offerKey = IndexerOfferKey.offerId(SUPPLIER_FEED_ID, SHOP_SKU1);
        PartnerId partner = PartnerId.supplierId(SUPPLIER_ID);
        OfferUpdate offerUpdate = OfferUpdate.builder()
                .setUpdateTime(updateTime)
                .setOfferKey(KnownShopIndexerOfferKey.of(partner, offerKey))
                .setRequestedPriceUpdate(PapiOfferPriceValue.builder()
                        .setUpdatedAt(updateTime)
                        .setVat(VatRate.VAT_20)
                        .setCurrency(Currency.RUR)
                        .setValue(BigDecimal.valueOf(123))
                        .setDiscountBase(BigDecimal.valueOf(200))
                        .build())
                .setRequestedAutoOldPriceUpdate(false)
                .build();
        offerUpdateDataCampService.publishUpdates(partner, Collections.singletonList(offerUpdate), updateTime, 123);
        List<DataCampOffer.Offer> offers = verifyOfferUpdatePublished();
        DataCampOffer.Offer expectedOffer1 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setFeedId(76)
                        .setOfferId(SHOP_SKU1)
                        .setShopId(SUPPLIER_ID)
                        .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                .setShopSku(SHOP_SKU1)
                                .build())
                        .build())
                .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                        .setRgb(DataCampOfferMeta.MarketColor.BLUE)
                        .build())
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setEnableAutoDiscounts(DataCampOfferMeta.Flag.newBuilder()
                                .setFlag(false)
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setSource(DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                                        .setTimestamp(DateTimes.toTimestamp(updateTime))
                                        .build())
                                .build())
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(123_0000000)
                                        .setId("RUR")
                                        .build())
                                .setBinaryOldprice(Common.PriceExpression.newBuilder()
                                        .setPrice(200_0000000)
                                        .setId("RUR")
                                        .build())
                                .setEnabled(true)
                                .setVat(VatRate.VAT_20.getId())
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setSource(DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                                        .setTimestamp(DateTimes.toTimestamp(updateTime))
                                        .build())
                                .build())
                        .setOriginalPriceFields(DataCampOfferPrice.OriginalPriceFields.newBuilder()
                                .setVat(DataCampOfferPrice.VatValue.newBuilder()
                                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                .setSource(DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                                                .setTimestamp(DateTimes.toTimestamp(updateTime))
                                                .build())
                                        .setValue(DataCampOfferPrice.Vat.VAT_20)
                                        .build())
                                .build())
                        .build())
                .build();
        assertThat(offers).singleElement().isEqualTo(expectedOffer1);
    }

    @Test
    @DbUnitDataSet(after = "OfferUpdateDataCampServiceTest.testSupplierHidingSending.after.csv")
    void testSupplierHidingSendingFeedExpansionDisabled() {
        Instant updateTime = Instant.now();
        IndexerOfferKey offerKey = IndexerOfferKey.offerId(SUPPLIER_FEED_ID, SHOP_SKU1);
        PartnerId partner = PartnerId.supplierId(SUPPLIER_ID);
        OfferUpdate offerUpdate = OfferUpdate.builder()
                .setUpdateTime(updateTime)
                .setOfferKey(KnownShopIndexerOfferKey.of(partner, offerKey))
                .setRequestedIsHiddenUpdate(true)
                .build();
        offerUpdateDataCampService.publishUpdates(partner, Collections.singletonList(offerUpdate), updateTime, 123);
        List<DataCampOffer.Offer> offers = verifyOfferUpdatePublished();
        DataCampOffer.Offer expectedOffer1 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setFeedId(76)
                        .setOfferId(SHOP_SKU1)
                        .setShopId(SUPPLIER_ID)
                        .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                .setShopSku(SHOP_SKU1)
                                .build())
                        .build())
                .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                        .setRgb(DataCampOfferMeta.MarketColor.BLUE)
                        .build())
                .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                        .addDisabled(DataCampOfferMeta.Flag.newBuilder()
                                .setFlag(true)
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setSource(DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                                        .setTimestamp(DateTimes.toTimestamp(updateTime))
                                        .build())
                                .build())
                        .build())
                .build();
        assertThat(offers).singleElement().isEqualTo(expectedOffer1).satisfies(actualOffer1 -> {
            assertThat(actualOffer1.getIdentifiers().hasWarehouseId()).isFalse();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    @DbUnitDataSet(after = "OfferUpdateDataCampServiceTest.testSupplierAutoOldPriceSending.after.csv")
    void testSupplierAutoOldPriceSending(String autoOldPriceString) {
        boolean autoOldPrice = Boolean.parseBoolean(autoOldPriceString);
        Instant updateTime = Instant.now();
        IndexerOfferKey offerKey = IndexerOfferKey.offerId(SUPPLIER_FEED_ID, SHOP_SKU1);
        PartnerId partner = PartnerId.supplierId(SUPPLIER_ID);
        OfferUpdate offerUpdate = OfferUpdate.builder()
                .setUpdateTime(updateTime)
                .setOfferKey(KnownShopIndexerOfferKey.of(partner, offerKey))
                .setRequestedAutoOldPriceUpdate(autoOldPrice)
                .build();
        offerUpdateDataCampService.publishUpdates(partner, Collections.singletonList(offerUpdate), updateTime, 123);
        List<DataCampOffer.Offer> offers = verifyOfferUpdatePublished();
        DataCampOffer.Offer expectedOffer1 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setFeedId(76)
                        .setOfferId(SHOP_SKU1)
                        .setShopId(SUPPLIER_ID)
                        .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                .setShopSku(SHOP_SKU1)
                                .build())
                        .build())
                .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                        .setRgb(DataCampOfferMeta.MarketColor.BLUE)
                        .build())
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setEnableAutoDiscounts(DataCampOfferMeta.Flag.newBuilder()
                                .setFlag(autoOldPrice)
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setSource(DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                                        .setTimestamp(DateTimes.toTimestamp(updateTime))
                                        .build())
                                .build())
                        .build())
                .build();
        assertThat(offers).singleElement().isEqualTo(expectedOffer1);
    }

    @Test
    @DbUnitDataSet(after = "OfferUpdateDataCampServiceTest.testSupplierRevealingSending.after.csv")
    void testSupplierRevealingSending() {
        Instant updateTime = Instant.now();
        IndexerOfferKey offerKey = IndexerOfferKey.offerId(SUPPLIER_FEED_ID, SHOP_SKU1);
        PartnerId partner = PartnerId.supplierId(SUPPLIER_ID);
        OfferUpdate offerUpdate = OfferUpdate.builder()
                .setUpdateTime(updateTime)
                .setOfferKey(KnownShopIndexerOfferKey.of(partner, offerKey))
                .setRequestedIsHiddenUpdate(false)
                .build();
        offerUpdateDataCampService.publishUpdates(partner, Collections.singletonList(offerUpdate), updateTime, 123);
        List<DataCampOffer.Offer> offers = verifyOfferUpdatePublished();
        DataCampOffer.Offer expectedOffer1 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setFeedId(76)
                        .setOfferId(SHOP_SKU1)
                        .setShopId(SUPPLIER_ID)
                        .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                .setShopSku(SHOP_SKU1)
                                .build())
                        .build())
                .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                        .setRgb(DataCampOfferMeta.MarketColor.BLUE)
                        .build())
                .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                        .addDisabled(DataCampOfferMeta.Flag.newBuilder()
                                .setFlag(false)
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setSource(DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                                        .setTimestamp(DateTimes.toTimestamp(updateTime))
                                        .build())
                                .build())
                        .build())
                .build();
        assertThat(offers).singleElement().isEqualTo(expectedOffer1);
    }

    @Test
    @DbUnitDataSet(after = "OfferUpdateDataCampServiceTest.testShopHidingSending.after.csv")
    void testShopHidingSending() {
        Instant updateTime = Instant.now();
        IndexerOfferKey offerKey = IndexerOfferKey.offerId(SHOP_FEED_ID, SHOP_SKU1);
        PartnerId partner = PartnerId.datasourceId(SHOP_ID);
        OfferUpdate offerUpdate = OfferUpdate.builder()
                .setUpdateTime(updateTime)
                .setOfferKey(KnownShopIndexerOfferKey.of(partner, offerKey))
                .setRequestedIsHiddenUpdate(true)
                .build();
        offerUpdateDataCampService.publishUpdates(partner, Collections.singletonList(offerUpdate), updateTime, 123);
        List<DataCampOffer.Offer> offers = verifyOfferUpdatePublished();
        DataCampOffer.Offer expectedOffer1 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setFeedId(SHOP_FEED_ID)
                        .setOfferId(SHOP_SKU1)
                        .setShopId(SHOP_ID)
                        .build())
                .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                        .setRgb(DataCampOfferMeta.MarketColor.WHITE)
                        .build())
                .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                        .addDisabled(DataCampOfferMeta.Flag.newBuilder()
                                .setFlag(true)
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setSource(DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                                        .setTimestamp(DateTimes.toTimestamp(updateTime))
                                        .build())
                                .build())
                        .build())
                .build();
        assertThat(offers).singleElement().isEqualTo(expectedOffer1);
    }

    private OfferUpdate getOfferUpdateForSupplierPriceTests(Instant updateTime, IndexerOfferKey offerKey,
                                                            PartnerId partner) {
        return OfferUpdate.builder()
                .setUpdateTime(updateTime)
                .setOfferKey(KnownShopIndexerOfferKey.of(partner, offerKey))
                .setRequestedPriceUpdate(PapiOfferPriceValue.builder()
                        .setUpdatedAt(updateTime)
                        .setVat(VatRate.VAT_20)
                        .setCurrency(Currency.RUR)
                        .setValue(BigDecimal.valueOf(123))
                        .setDiscountBase(BigDecimal.valueOf(200))
                        .build())
                .build();
    }

    private List<DataCampOffer.Offer> verifyOfferUpdatePublished() {
        ArgumentCaptor<SyncChangeOfferLogbrokerEvent> captor = ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);
        verify(logbrokerService).publishEvent(captor.capture());
        SyncChangeOfferLogbrokerEvent genericEvent = captor.getValue();
        assertThat(genericEvent).isInstanceOf(SyncChangeOfferLogbrokerEvent.class);
        return genericEvent.getPayload()
                .stream()
                .map(DataCampEvent::convertToDataCampOffer)
                .collect(Collectors.toList());
    }
}
