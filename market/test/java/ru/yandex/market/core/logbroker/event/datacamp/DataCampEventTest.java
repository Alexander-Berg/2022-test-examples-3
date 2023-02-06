package ru.yandex.market.core.logbroker.event.datacamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampOfferStatus;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.datacamp.DataCampUtil;
import ru.yandex.market.core.offer.IndexerOfferKey;
import ru.yandex.market.core.offer.PapiHidingEvent;
import ru.yandex.market.core.offer.PapiHidingSource;
import ru.yandex.market.core.price.ExpirablePapiOfferPrice;
import ru.yandex.market.core.price.PapiOfferPriceValue;
import ru.yandex.market.core.tax.model.VatRate;

import static ru.yandex.market.core.offer.IndexerOfferKey.anyMarketOrShopSkuUsedAndOtherIgnored;

/**
 * Тесты на конвертацию DataCampEvent'ов в протобуфы.
 * Date: 30.03.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
public class DataCampEventTest {

    private final long businessId = 90774;
    private final int shopId = 774;
    private final int feedId = 1;
    private final long marketSku = 1;
    private final IndexerOfferKey key = anyMarketOrShopSkuUsedAndOtherIgnored(feedId, marketSku, "shopSku");
    private final Instant now = Instant.now();
    private final int expireTime = 60 * 60 * 10;
    private final Instant expiresAt = now.plusSeconds(expireTime);
    private final CampaignType BLUE = CampaignType.SUPPLIER;

    @Nonnull
    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(PapiHidingSource.PUSH_PARTNER_API, DataCampOfferMeta.DataSource.PUSH_PARTNER_API, null,
                        true),
                Arguments.of(PapiHidingSource.PUSH_PARTNER_API, DataCampOfferMeta.DataSource.PUSH_PARTNER_API, false,
                        false),
                Arguments.of(PapiHidingSource.PUSH_PARTNER_API, DataCampOfferMeta.DataSource.PUSH_PARTNER_API, true,
                        false),
                Arguments.of(PapiHidingSource.PULL_PARTNER_API, DataCampOfferMeta.DataSource.PULL_PARTNER_API, null,
                        true),
                Arguments.of(PapiHidingSource.PULL_PARTNER_API, DataCampOfferMeta.DataSource.PULL_PARTNER_API, false,
                        false),
                Arguments.of(PapiHidingSource.PULL_PARTNER_API, DataCampOfferMeta.DataSource.PULL_PARTNER_API, true,
                        true),
                Arguments.of(PapiHidingSource.MARKET_PRICELABS, DataCampOfferMeta.DataSource.MARKET_PRICELABS, true,
                        true)
        );
    }

    /**
     * Тест на конвертацию {@link PapiOfferPriceDataCampEvent}.
     */
    @Test
    void testPapiOfferPriceConvert() {

        Currency currency = Currency.RUR;
        VatRate vat = VatRate.VAT_20;
        long priceValue = 300;

        PapiOfferPriceValue.Builder builder = new PapiOfferPriceValue.Builder();
        builder.setCurrency(currency);
        builder.setVat(vat);

        builder.setUpdatedAt(now);
        builder.setValue(BigDecimal.valueOf(priceValue));

        ExpirablePapiOfferPrice price = ExpirablePapiOfferPrice.of(builder.build(), expiresAt);
        PapiOfferPriceDataCampEvent updateOfferPriceEvent =
                new PapiOfferPriceDataCampEvent.UpdateOfferPriceBuilder()
                        .setBusinessId(businessId)
                        .setShopId(shopId)
                        .setIndexerOfferKey(key)
                        .setPapiOfferPrice(price)
                        .setCampaignType(BLUE)
                        .build();
        DataCampOffer.Offer offer = updateOfferPriceEvent.convertToDataCampOffer();

        //проверяем идентификаторы оффера
        checkIdentifiers(offer);

        //проверяем цену
        DataCampOfferPrice.PriceBundle offerPrice = offer.getPrice().getPriority();
        Assertions.assertEquals(currency.name(), offerPrice.getBinaryPrice().getId());
        Assertions.assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(priceValue)),
                offerPrice.getBinaryPrice().getPrice());
        Assertions.assertTrue(offerPrice.getEnabled());

        //проверяем метаинформацию
        Assertions.assertEquals(DataCampOfferMeta.DataSource.PULL_PARTNER_API, offerPrice.getMeta().getSource());
        Timestamp timestamp = offerPrice.getMeta().getTimestamp();
        Assertions.assertEquals(now, Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()));
        Assertions.assertEquals(DataCampOfferMeta.MarketColor.BLUE, offer.getMeta().getRgb());
    }

    @DisplayName("Тест на конвертацию PapiHiddenOfferDataCampEvent")
    @ParameterizedTest(name = "push = {0}, priority = {2}, hiding = {3}, dataSource = {1}")
    @MethodSource("args")
    void testPapiHiddenOfferConvert(PapiHidingSource source, DataCampOfferMeta.DataSource expectedSource,
                                    Boolean priority, boolean hiding) {

        PapiHidingEvent.Builder hidingEventBuilder = new PapiHidingEvent.Builder();

        hidingEventBuilder.setHiddenAt(now);
        hidingEventBuilder.setHidingExpiresAt(expiresAt);
        hidingEventBuilder.setPriority(priority);
        hidingEventBuilder.setSource(source);

        PapiHiddenOfferDataCampEvent updateHiddenOfferEvent = PapiHiddenOfferDataCampEvent
                .builder()
                .withPapiHidingEvent(hidingEventBuilder.build())
                .withShopId(shopId)
                .withIndexerOfferKey(key)
                .withBusinessId(businessId)
                .withCampaignType(BLUE)
                .withHiding(hiding)
                .build();

        DataCampOffer.Offer offer = updateHiddenOfferEvent.convertToDataCampOffer();

        //проверяем идентификаторы оффера
        checkIdentifiers(offer);

        DataCampOfferStatus.OfferStatus offerStatus = offer.getStatus();
        Assertions.assertEquals(1, offerStatus.getDisabledCount());
        DataCampOfferMeta.Flag disabled = offerStatus.getDisabled(0);

        //проверяем скрытие
        Assertions.assertEquals(expectedSource, disabled.getMeta().getSource());
        Timestamp timestamp = disabled.getMeta().getTimestamp();
        Assertions.assertEquals(now, Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()));
        Assertions.assertEquals(hiding, disabled.getFlag());
    }

    private void checkIdentifiers(DataCampOffer.Offer offer) {
        DataCampOfferIdentifiers.OfferIdentifiers offerIdentifiers = offer.getIdentifiers();
        Assertions.assertEquals(feedId, offerIdentifiers.getFeedId());
        Assertions.assertEquals(marketSku, offerIdentifiers.getExtra().getMarketSkuId());
        Assertions.assertEquals(shopId, offerIdentifiers.getShopId());
        Assertions.assertEquals(businessId, offerIdentifiers.getBusinessId());
    }

    @Test
    @DisplayName("Для всех источников скрытий есть маппинг")
    void testSourceMapping() {
        for (PapiHidingSource source : PapiHidingSource.values()) {
            Assertions.assertNotNull(DataCampEvent.mapSource(source), source.name());
        }
    }
}
