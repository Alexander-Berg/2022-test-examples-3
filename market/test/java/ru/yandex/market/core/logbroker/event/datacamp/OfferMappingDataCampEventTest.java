package ru.yandex.market.core.logbroker.event.datacamp;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.feed.offer.united.OfferPicture;
import ru.yandex.market.core.feed.offer.united.OfferPictureSource;
import ru.yandex.market.core.logbroker.quick.united.assortment.OfferMappingEntry;
import ru.yandex.market.core.offer.IndexerOfferKey;
import ru.yandex.market.core.offer.mapping.AvailabilityStatus;
import ru.yandex.market.core.offer.mapping.MarketEntityReference;
import ru.yandex.market.core.offer.mapping.MasterData;
import ru.yandex.market.core.offer.mapping.ShopOffer;
import ru.yandex.market.core.offer.mapping.TimePeriodWithUnits;
import ru.yandex.market.core.offer.mapping.WeightDimensions;

/**
 * Date: 14.01.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class OfferMappingDataCampEventTest extends AbstractDataCampEventTest {

    @DisplayName("Корректность заполнения всех полей DataCampOffer.Offer")
    @Test
    void convertToDataCampOffer_allField_correctMapping() {
        OfferMappingEntry partnerOfferMapping = new OfferMappingEntry(
                ShopOffer.builder()
                        .setSupplierId(6669L)
                        .setShopSku("shop-sku-blue")
                        .setTitle("positive-supplier-name")
                        .setCategoryName("category-name-supplier")
                        .addUrl("https://beru.ru/ol")
                        .setVendor("Апельсинка")
                        .setVendorCode("3841")
                        .addAllBarcodes(List.of("9999999999999999999", "841239031245321"))
                        .setDescription("Для личного пользования")
                        .setCertificate("06234097025234156")
                        .setAvailabilityStatus(AvailabilityStatus.ACTIVE)
                        .setMasterData(new MasterData.Builder()
                                .setManufacturer("manufacturer-ok")
                                .addManufacturerCountries(Collections.singletonList("Krakozhia"))
                                .setWeightDimensions(new WeightDimensions.Builder()
                                        .setLength(120_000L)
                                        .setWidth(7_800_000L)
                                        .setHeight(55_000L)
                                        .setWeight(95_500_000L)
                                        .build())
                                .setShelfLife(TimePeriodWithUnits.ofDays(350))
                                .setShelfLifeComment("Долго")
                                .setLifeTime(TimePeriodWithUnits.ofYears(10))
                                .setLifeTimeComment("Очень долго")
                                .setGuaranteePeriod(TimePeriodWithUnits.ofHours(480))
                                .setGuaranteePeriodComment("Мало")
                                .setCustomsCommodityCode("832,258")
                                .setTransportUnitSize(1)
                                .setMinShipment(5)
                                .setQuantumOfSupply(6)
                                .addSupplyScheduleDays(
                                        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.FRIDAY))
                                .setDeliveryDuration(Duration.ofDays(10L))
                                .setBoxCount(2)
                                .build())
                        .addPicture(OfferPicture.builder()
                                .withUrl("https://example.com/picture1.jpg")
                                .withSource(OfferPictureSource.DIRECT_LINK)
                                .build())
                        .addPicture(OfferPicture.builder()
                                .withUrl("https://example.com/picture2.jpg")
                                .withSource(OfferPictureSource.DIRECT_LINK)
                                .build())
                        .build(),
                MarketEntityReference.marketSku(123456789L),
                123L
        );

        OfferMappingDataCampEvent dataCampEvent = new OfferMappingDataCampEvent(
                PartnerId.partnerId(6669L, CampaignType.SUPPLIER),
                partnerOfferMapping,
                IndexerOfferKey.anyMarketOrShopSkuUsedAndOtherIgnored(423L, 123456789L, "shop-sku-blue")
                        .withFeedIdAndWarehouseId(423L, 300),
                9403L,
                Instant.ofEpochSecond(1610602214L, 480271000L)
        );

        assertDataCampEvent("offer.json", dataCampEvent);
    }

    @DisplayName("Корректность заполнения неуказанных полей")
    @Test
    void convertToDataCampOffer_empty_fields_meta() {
        OfferMappingEntry partnerOfferMapping = new OfferMappingEntry(
                ShopOffer.builder()
                        .setSupplierId(6669L)
                        .setShopSku("shop-sku-blue")
                        .build(),
                MarketEntityReference.marketSku(123456789L),
                null);

        OfferMappingDataCampEvent dataCampEvent = new OfferMappingDataCampEvent(
                PartnerId.partnerId(6669L, CampaignType.SUPPLIER),
                partnerOfferMapping,
                IndexerOfferKey.anyMarketOrShopSkuUsedAndOtherIgnored(423L, 123456789L, "shop-sku-blue")
                        .withFeedIdAndWarehouseId(423L, 300),
                9403L,
                Instant.ofEpochSecond(1610602214L, 480271000L)
        );

        assertDataCampEvent("emptyOffer.json", dataCampEvent);
    }
}
