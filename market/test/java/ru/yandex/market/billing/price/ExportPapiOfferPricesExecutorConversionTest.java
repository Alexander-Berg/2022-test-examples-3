package ru.yandex.market.billing.price;


import java.math.BigDecimal;
import java.time.Instant;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.offer.IndexerOfferKey;
import ru.yandex.market.core.offer.PapiFeedMarketSkuOffer;
import ru.yandex.market.core.offer.PapiFeedMarketSkuOfferDiff;
import ru.yandex.market.core.offer.PapiOfferProperties;
import ru.yandex.market.core.offer.PapiOfferPropertyDiff;
import ru.yandex.market.core.price.PapiOfferPriceValue;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.proto.QPipe;

/**
 * Проверка конвертации данных офера.
 * <p>
 * Проверяем конвертацию
 * <ul>
 * <li>{@link PapiFeedMarketSkuOffer} -&gt; {@link QPipe.Offer}
 * <li>{@link PapiFeedMarketSkuOfferDiff} -&gt; {@link QPipe.Offer}
 * </ul>
 */
@ParametersAreNonnullByDefault
class ExportPapiOfferPricesExecutorConversionTest {
    /**
     * Одна из проверок конвертации офера {@link PapiFeedMarketSkuOffer}.
     * <p>
     * Проверяем конвертацию офера {@link PapiFeedMarketSkuOffer}, который
     * <ul>
     * <li>скрыт;
     * <li>с ценой;
     * <li>цена со скидкой;
     * <li>цена с указанием НДС;
     * </ul>
     */
    @Test
    void testOfferWithAllFieldsConversion() {
        Instant now = Instant.now();
        PapiFeedMarketSkuOffer offer = PapiFeedMarketSkuOffer.of(
                IndexerOfferKey.marketSku(234, 345),
                new PapiOfferProperties.Builder()
                        .setHiddenOffer(true)
                        .setHiddenAt(now)
                        .setUpdatedAt(now)
                        .setCampaignType(CampaignType.SUPPLIER)
                        .setPrice(new PapiOfferPriceValue.Builder()
                                .setValue(BigDecimal.valueOf(12345, 2))
                                .setDiscountBase(BigDecimal.valueOf(14567, 2))
                                .setCurrency(Currency.USD)
                                .setVat(VatRate.VAT_18_118)
                                .setUpdatedAt(now)
                                .build())
                        .build());
        QPipe.Offer qoffer = ExportPapiOfferPricesExecutor.toQPipeOffer(offer);
        Assertions.assertEquals(234L, qoffer.getFeedId());
        Assertions.assertEquals(345L, qoffer.getMarketSku());
        Assertions.assertEquals(QPipe.MarketColor.BLUE, qoffer.getMarketColor());
        Assertions.assertEquals(now.getEpochSecond(), qoffer.getData(0).getTimestamp());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasBinaryPrice());
        Assertions.assertEquals(123_45_00000, qoffer.getData(0).getFields().getBinaryPrice().getPrice());
        Assertions.assertEquals("USD", qoffer.getData(0).getFields().getBinaryPrice().getId());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasBinaryOldprice());
        Assertions.assertEquals(145_67_00000, qoffer.getData(0).getFields().getBinaryOldprice().getPrice());
        Assertions.assertEquals("USD", qoffer.getData(0).getFields().getBinaryOldprice().getId());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasVat());
        Assertions.assertEquals(VatRate.VAT_18_118.getIdAsInt(), qoffer.getData(0).getFields().getVat());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasOfferDeleted());
        Assertions.assertTrue(qoffer.getData(0).getFields().getOfferDeleted());

        Assertions.assertFalse(qoffer.getData(0).getFields().hasPriceDeleted());
    }

    /**
     * Одна из проверок конвертации офера {@link PapiFeedMarketSkuOffer}.
     * <p>
     * Проверяем конвертацию офера {@link PapiFeedMarketSkuOffer}, который
     * <ul>
     * <li>не скрыт;
     * <li>с ценой;
     * <li>цена без скидки;
     * <li>цена без указания НДС;
     * </ul>
     */
    @Test
    void testOfferWithoutOptionalFieldsConversion() {
        Instant now = Instant.now();
        PapiFeedMarketSkuOffer offer = PapiFeedMarketSkuOffer.of(
                IndexerOfferKey.marketSku(234, 345),
                new PapiOfferProperties.Builder()
                        .setHiddenOffer(false)
                        .setHiddenAt(now)
                        .setUpdatedAt(now)
                        .setCampaignType(CampaignType.SUPPLIER)
                        .setPrice(new PapiOfferPriceValue.Builder()
                                .setValue(BigDecimal.valueOf(12345, 2))
                                .setCurrency(Currency.USD)
                                .setUpdatedAt(now)
                                .build())
                        .build());
        QPipe.Offer qoffer = ExportPapiOfferPricesExecutor.toQPipeOffer(offer);
        Assertions.assertEquals(234L, qoffer.getFeedId());
        Assertions.assertEquals(345L, qoffer.getMarketSku());
        Assertions.assertEquals(QPipe.MarketColor.BLUE, qoffer.getMarketColor());
        Assertions.assertEquals(now.getEpochSecond(), qoffer.getData(0).getTimestamp());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasBinaryPrice());
        Assertions.assertEquals(123_45_00000, qoffer.getData(0).getFields().getBinaryPrice().getPrice());
        Assertions.assertEquals("USD", qoffer.getData(0).getFields().getBinaryPrice().getId());

        Assertions.assertFalse(qoffer.getData(0).getFields().hasBinaryOldprice());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasVat());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasOfferDeleted());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasPriceDeleted());
    }

    /**
     * Проверка конвертации "пустого" офера {@link PapiFeedMarketSkuOffer}.
     * <p>
     * Проверяем конвертацию офера {@link PapiFeedMarketSkuOffer}, который
     * <ul>
     * <li>не скрыт;
     * <li>цена не указана (а значит не указаны и скидка и НДС);
     * </ul>
     */
    @Test
    void testEmptyOfferConversion() {
        Instant now = Instant.now();
        PapiFeedMarketSkuOffer offer = PapiFeedMarketSkuOffer.of(
                IndexerOfferKey.marketSku(234, 345),
                new PapiOfferProperties.Builder()
                        .setHiddenOffer(false)
                        .setHiddenAt(now)
                        .setUpdatedAt(now)
                        .setCampaignType(CampaignType.SUPPLIER)
                        .build());
        QPipe.Offer qoffer = ExportPapiOfferPricesExecutor.toQPipeOffer(offer);
        Assertions.assertEquals(234L, qoffer.getFeedId());
        Assertions.assertEquals(345L, qoffer.getMarketSku());
        Assertions.assertEquals(QPipe.MarketColor.BLUE, qoffer.getMarketColor());
        Assertions.assertEquals(now.getEpochSecond(), qoffer.getData(0).getTimestamp());

        Assertions.assertFalse(qoffer.getData(0).getFields().hasBinaryPrice());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasBinaryOldprice());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasVat());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasOfferDeleted());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasPriceDeleted());
    }

    /**
     * Одна из проверок конвертации дифа {@link PapiFeedMarketSkuOfferDiff}.
     * <p>
     * Проверяем конвертацию дифа {@link PapiFeedMarketSkuOfferDiff}, который говорит о том, что
     * <ul>
     * <li>офер был только что скрыт;
     * <li>у офера только что изменилась цена, при этом
     * <ul>
     * <li>цена указана со скидкой;
     * <li>цена указана вместе с НДС;
     * </ul>
     * </ul>
     * <p>
     * Здесь смотрим, что есть поле {@link QPipe.Fields#getOfferDeleted()} и оно {@code true},
     * проверяем свойства цены.
     */
    @Test
    void testDiffWithAllFieldChangedConversion() {
        Instant now = Instant.now();
        PapiFeedMarketSkuOfferDiff diff = PapiFeedMarketSkuOfferDiff.of(
                IndexerOfferKey.marketSku(234, 345),
                new PapiOfferPropertyDiff.Builder()
                        .setPriceJustChanged(true)
                        .setVisibilityJustChanged(true)
                        .setOffer(new PapiOfferProperties.Builder()
                                .setHiddenOffer(true)
                                .setHiddenAt(now)
                                .setUpdatedAt(now)
                                .setCampaignType(CampaignType.SUPPLIER)
                                .setPrice(new PapiOfferPriceValue.Builder()
                                        .setValue(BigDecimal.valueOf(12345, 2))
                                        .setDiscountBase(BigDecimal.valueOf(14567, 2))
                                        .setCurrency(Currency.USD)
                                        .setVat(VatRate.VAT_18_118)
                                        .setUpdatedAt(now)
                                        .build())
                                .build())
                        .build());
        QPipe.Offer qoffer = ExportPapiOfferPricesExecutor.toQPipeOfferFromDiff(diff);
        Assertions.assertEquals(234L, qoffer.getFeedId());
        Assertions.assertEquals(345L, qoffer.getMarketSku());
        Assertions.assertEquals(QPipe.MarketColor.BLUE, qoffer.getMarketColor());
        Assertions.assertEquals(now.getEpochSecond(), qoffer.getData(0).getTimestamp());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasBinaryPrice());
        Assertions.assertEquals(123_45_00000, qoffer.getData(0).getFields().getBinaryPrice().getPrice());
        Assertions.assertEquals("USD", qoffer.getData(0).getFields().getBinaryPrice().getId());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasBinaryOldprice());
        Assertions.assertEquals(145_67_00000, qoffer.getData(0).getFields().getBinaryOldprice().getPrice());
        Assertions.assertEquals("USD", qoffer.getData(0).getFields().getBinaryOldprice().getId());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasVat());
        Assertions.assertEquals(VatRate.VAT_18_118.getIdAsInt(), qoffer.getData(0).getFields().getVat());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasOfferDeleted());
        Assertions.assertTrue(qoffer.getData(0).getFields().getOfferDeleted());

        Assertions.assertFalse(qoffer.getData(0).getFields().hasPriceDeleted());
    }

    /**
     * Одна из проверок конвертации дифа {@link PapiFeedMarketSkuOfferDiff}.
     * <p>
     * Проверяем конвертацию дифа {@link PapiFeedMarketSkuOfferDiff}, который говорит о том, что
     * <ul>
     * <li>офер был только что скрыт;
     * <li>у офера есть цена, но она не менялась с момента последней выгрузки в индексатор
     * </ul>
     * <p>
     * Здесь смотрим, что есть поле {@link QPipe.Fields#getOfferDeleted()} и оно {@code true},
     * смотрим, что цена при этом не передаётся.
     */
    @Test
    void testDiffWithOnlyVisibilityChangedConversion() {
        Instant now = Instant.now();
        PapiFeedMarketSkuOfferDiff diff = PapiFeedMarketSkuOfferDiff.of(
                IndexerOfferKey.marketSku(234, 345),
                new PapiOfferPropertyDiff.Builder()
                        .setPriceJustChanged(false)
                        .setVisibilityJustChanged(true)
                        .setOffer(new PapiOfferProperties.Builder()
                                .setHiddenOffer(true)
                                .setHiddenAt(now)
                                .setUpdatedAt(now)
                                .setCampaignType(CampaignType.SUPPLIER)
                                .setPrice(new PapiOfferPriceValue.Builder()
                                        .setValue(BigDecimal.valueOf(12345, 2))
                                        .setDiscountBase(BigDecimal.valueOf(14567, 2))
                                        .setCurrency(Currency.USD)
                                        .setVat(VatRate.VAT_18_118)
                                        .setUpdatedAt(now)
                                        .build())
                                .build())
                        .build());
        QPipe.Offer qoffer = ExportPapiOfferPricesExecutor.toQPipeOfferFromDiff(diff);
        Assertions.assertEquals(234L, qoffer.getFeedId());
        Assertions.assertEquals(345L, qoffer.getMarketSku());
        Assertions.assertEquals(QPipe.MarketColor.BLUE, qoffer.getMarketColor());
        Assertions.assertEquals(now.getEpochSecond(), qoffer.getData(0).getTimestamp());

        Assertions.assertFalse(qoffer.getData(0).getFields().hasBinaryPrice());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasBinaryOldprice());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasVat());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasOfferDeleted());
        Assertions.assertTrue(qoffer.getData(0).getFields().getOfferDeleted());

        Assertions.assertFalse(qoffer.getData(0).getFields().hasPriceDeleted());
    }

    /**
     * Одна из проверок конвертации дифа {@link PapiFeedMarketSkuOfferDiff}.
     * <p>
     * Проверяем конвертацию дифа {@link PapiFeedMarketSkuOfferDiff}, который говорит о том, что
     * <ul>
     * <li>офер скрыт, но он был так же скрыт в момент последней выгрузки в индексатор;
     * <li>у офера только что изменилась цена, при этом
     * <ul>
     * <li>цена указана со скидкой;
     * <li>цена указана вместе с НДС;
     * </ul>
     * </ul>
     * <p>
     * Здесь смотрим, что нет поля {@link QPipe.Fields#getOfferDeleted()} и проверяем свойства цены.
     */
    @Test
    void testDiffWithOnlyPriceChangedConversion() {
        Instant now = Instant.now();
        PapiFeedMarketSkuOfferDiff diff = PapiFeedMarketSkuOfferDiff.of(
                IndexerOfferKey.marketSku(234, 345),
                new PapiOfferPropertyDiff.Builder()
                        .setPriceJustChanged(true)
                        .setVisibilityJustChanged(false)
                        .setOffer(new PapiOfferProperties.Builder()
                                .setHiddenOffer(true)
                                .setHiddenAt(now)
                                .setUpdatedAt(now)
                                .setCampaignType(CampaignType.SUPPLIER)
                                .setPrice(new PapiOfferPriceValue.Builder()
                                        .setValue(BigDecimal.valueOf(12345, 2))
                                        .setDiscountBase(BigDecimal.valueOf(14567, 2))
                                        .setCurrency(Currency.USD)
                                        .setVat(VatRate.VAT_18_118)
                                        .setUpdatedAt(now)
                                        .build())
                                .build())
                        .build());
        QPipe.Offer qoffer = ExportPapiOfferPricesExecutor.toQPipeOfferFromDiff(diff);
        Assertions.assertEquals(234L, qoffer.getFeedId());
        Assertions.assertEquals(345L, qoffer.getMarketSku());
        Assertions.assertEquals(QPipe.MarketColor.BLUE, qoffer.getMarketColor());
        Assertions.assertEquals(now.getEpochSecond(), qoffer.getData(0).getTimestamp());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasBinaryPrice());
        Assertions.assertEquals(123_45_00000, qoffer.getData(0).getFields().getBinaryPrice().getPrice());
        Assertions.assertEquals("USD", qoffer.getData(0).getFields().getBinaryPrice().getId());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasBinaryOldprice());
        Assertions.assertEquals(145_67_00000, qoffer.getData(0).getFields().getBinaryOldprice().getPrice());
        Assertions.assertEquals("USD", qoffer.getData(0).getFields().getBinaryOldprice().getId());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasVat());
        Assertions.assertEquals(VatRate.VAT_18_118.getIdAsInt(), qoffer.getData(0).getFields().getVat());

        Assertions.assertFalse(qoffer.getData(0).getFields().hasOfferDeleted());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasPriceDeleted());
    }

    /**
     * Одна из проверок конвертации дифа {@link PapiFeedMarketSkuOfferDiff}.
     * <p>
     * Проверяем конвертацию дифа {@link PapiFeedMarketSkuOfferDiff}, который говорит о том, что
     * <ul>
     * <li>офер скрыт, но он был так же скрыт в момент последней выгрузки в индексатор;
     * <li>у офера только что изменилась цена: был выполнен возврат к цене из фида
     * </ul>
     * <p>
     * Здесь смотрим, что есть поле {@link QPipe.Fields#getPriceDeleted()} и оно {@code true}.
     */
    @Test
    void testDiffWithPriceDeletedConversion() {
        Instant now = Instant.now();
        PapiFeedMarketSkuOfferDiff diff = PapiFeedMarketSkuOfferDiff.of(
                IndexerOfferKey.marketSku(234, 345),
                new PapiOfferPropertyDiff.Builder()
                        .setPriceJustChanged(true)
                        .setVisibilityJustChanged(false)
                        .setOffer(new PapiOfferProperties.Builder()
                                .setCampaignType(CampaignType.SUPPLIER)
                                .setHiddenOffer(true)
                                .setHiddenAt(now)
                                .setUpdatedAt(now)
                                .build())
                        .build());
        QPipe.Offer qoffer = ExportPapiOfferPricesExecutor.toQPipeOfferFromDiff(diff);
        Assertions.assertEquals(234L, qoffer.getFeedId());
        Assertions.assertEquals(345L, qoffer.getMarketSku());
        Assertions.assertEquals(QPipe.MarketColor.BLUE, qoffer.getMarketColor());
        Assertions.assertEquals(now.getEpochSecond(), qoffer.getData(0).getTimestamp());

        Assertions.assertFalse(qoffer.getData(0).getFields().hasBinaryPrice());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasBinaryOldprice());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasVat());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasOfferDeleted());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasPriceDeleted());
        Assertions.assertTrue(qoffer.getData(0).getFields().getPriceDeleted());
    }

    /**
     * Одна из проверок конвертации дифа {@link PapiFeedMarketSkuOfferDiff}.
     * <p>
     * Проверяем конвертацию дифа {@link PapiFeedMarketSkuOfferDiff}, который говорит о том, что
     * <ul>
     * <li>офер не скрыт, но был скрыт в момент последней выгрузки в индексатор;
     * <li>для офера используется цена из фида, но это было так же и в момент последней выгрузки в индексатор;
     * </ul>
     * <p>
     * Здесь смотрим, что есть поле {@link QPipe.Fields#getOfferDeleted()} и оно {@code false}.
     */
    @Test
    void testDiffUnhidingOfferConversion() {
        Instant now = Instant.now();
        PapiFeedMarketSkuOfferDiff diff = PapiFeedMarketSkuOfferDiff.of(
                IndexerOfferKey.marketSku(234, 345),
                new PapiOfferPropertyDiff.Builder()
                        .setPriceJustChanged(false)
                        .setVisibilityJustChanged(true)
                        .setOffer(new PapiOfferProperties.Builder()
                                .setCampaignType(CampaignType.SUPPLIER)
                                .setHiddenOffer(false)
                                .setHiddenAt(now)
                                .setUpdatedAt(now)
                                .build())
                        .build());
        QPipe.Offer qoffer = ExportPapiOfferPricesExecutor.toQPipeOfferFromDiff(diff);
        Assertions.assertEquals(234L, qoffer.getFeedId());
        Assertions.assertEquals(345L, qoffer.getMarketSku());
        Assertions.assertEquals(QPipe.MarketColor.BLUE, qoffer.getMarketColor());
        Assertions.assertEquals(now.getEpochSecond(), qoffer.getData(0).getTimestamp());

        Assertions.assertFalse(qoffer.getData(0).getFields().hasBinaryPrice());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasBinaryOldprice());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasVat());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasOfferDeleted());
        Assertions.assertFalse(qoffer.getData(0).getFields().getOfferDeleted());

        Assertions.assertFalse(qoffer.getData(0).getFields().hasPriceDeleted());
    }

    /**
     * Одна из проверок конвертации дифа {@link PapiFeedMarketSkuOfferDiff}.
     * <p>
     * Проверяем конвертацию дифа {@link PapiFeedMarketSkuOfferDiff}, который соответствует:
     * <ul>
     * <li>офер на Белом Маркете</li>
     * <li>офер не скрыт, но был скрыт в момент последней выгрузки в индексатор;
     * <li>для офера используется цена из фида, но это было так же и в момент последней выгрузки в индексатор;
     * </ul>
     * <p>
     * Здесь смотрим, что есть поле {@link QPipe.Fields#getOfferDeleted()} и оно {@code false}.
     */
    @Test
    void testWhiteOfferConversion() {
        Instant now = Instant.now();
        PapiFeedMarketSkuOfferDiff diff = PapiFeedMarketSkuOfferDiff.of(
                IndexerOfferKey.offerId(234, "qwerty"),
                new PapiOfferPropertyDiff.Builder()
                        .setPriceJustChanged(false)
                        .setVisibilityJustChanged(true)
                        .setOffer(new PapiOfferProperties.Builder()
                                .setCampaignType(CampaignType.SHOP)
                                .setHiddenOffer(false)
                                .setHiddenAt(now)
                                .setUpdatedAt(now)
                                .build())
                        .build());
        QPipe.Offer qoffer = ExportPapiOfferPricesExecutor.toQPipeOfferFromDiff(diff);
        Assertions.assertEquals(234L, qoffer.getFeedId());
        Assertions.assertEquals(0L, qoffer.getMarketSku());
        Assertions.assertEquals("qwerty", qoffer.getOfferId());
        Assertions.assertEquals(QPipe.MarketColor.WHITE, qoffer.getMarketColor());
        Assertions.assertEquals(now.getEpochSecond(), qoffer.getData(0).getTimestamp());

        Assertions.assertFalse(qoffer.getData(0).getFields().hasBinaryPrice());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasBinaryOldprice());
        Assertions.assertFalse(qoffer.getData(0).getFields().hasVat());

        Assertions.assertTrue(qoffer.getData(0).getFields().hasOfferDeleted());
        Assertions.assertFalse(qoffer.getData(0).getFields().getOfferDeleted());

        Assertions.assertFalse(qoffer.getData(0).getFields().hasPriceDeleted());
    }
}
