package ru.yandex.market.logistics.iris.service.datacamp.conversion;

import java.io.IOException;
import java.util.Collection;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent.OfferContent;
import Market.DataCamp.DataCampOfferIdentifiers.OfferIdentifiers;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.MappingToOffersOuterClass.MappingToOffers;
import Market.DataCamp.MarketSkuMboContentOuterClass.MarketSkuMboContent;
import Market.DataCamp.MarketSkuOuterClass.MarketSku;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.configuration.protobuf.ProtobufMapper;
import ru.yandex.market.logistics.iris.core.index.ImmutableReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.model.ItemDTO;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTO;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyBooleanKey;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyService;
import ru.yandex.market.mbo.export.ExportReportModels.ExportReportModel;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

public class DataCampToIrisConversionServiceTest extends AbstractContextualTest {
    private static final String TEST_PATH = "fixtures/data/datacamp_to_iris_conversion/";

    @Autowired
    private DataCampToIrisConversionService dataCampToIrisConversionService;

    @Autowired
    private ChangeTrackingReferenceIndexer referenceIndexer;

    @SpyBean
    private SystemPropertyService systemPropertyService;

    @Autowired
    ProtobufMapper protobufMapper;

    @Before
    public void setupProperty() {
        doReturn(true).when(systemPropertyService)
                .getBooleanProperty(SystemPropertyBooleanKey.ENABLE_NEW_SHELF_LIFE_FEATURE);
    }

    /**
     * Проверяем, что пустой DataCampOffer.Offer не будет преобразован в корректный ItemDTO.
     */
    @Test
    public void convertEmptyOffer() {
        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.getDefaultInstance();
        Collection<ItemDTO> result = dataCampToIrisConversionService.fromUnitedOffer(unitedOffer);
        assertions().assertThat(result).isEmpty();
    }

    /**
     * Проверяем, что пустой MarketSkuOuterClass.MarketSku не будет преобразован в корректный набор ItemDTO.
     */
    @Test
    public void convertEmptyMarketSku() {
        MarketSku marketSku = MarketSku.getDefaultInstance();
        Collection<ItemDTO> result = dataCampToIrisConversionService.fromMarketSku(marketSku);
        assertions().assertThat(result).isEmpty();
    }

    /**
     * Проверяем сценарий с полноценной конвертацией ssku offer.
     * <p>
     * Данные должны взяться из offer в полном объеме.
     * На выходе должен получиться объект ItemDTO с корректно заполненным индексом.
     */
    @Test
    public void fullConversionOfOffer() throws IOException {
        testShopSku("ssku_offer.json",
                "ssku_offer_expected.json");
    }

    /**
     * Проверяем сценарий с конвертацией ssku offer без Barcodes.
     * <p>
     * Данные должны взяться из offer в полном объеме.
     * На выходе должен получиться объект ItemDTO с корректно заполненным индексом.
     */
    @Test
    public void fullConversionOfOfferWithEmptyBarcodes() throws IOException {
        testShopSku("ssku_offer_barcode.json",
                "ssku_offer_barcode_expected.json");
    }

    /**
     * Проверяем сценарий с полноценной конвертацией Market SKU.
     * <p>
     * Данные должны взяться из msku в полном объеме.
     * На выходе должен получиться коллекция ItemDTO с корректно заполненным индексом.
     */
    @Test
    public void fullConversionOfMarketSku() throws IOException {
        testMarketSku("market_sku.json",
                "market_sku_expected.json");
    }

    /**
     * Проверяем сценарий с конвертацией Market SKU с выключенными карготипами.
     * <p>
     * Данные должны взяться из msku в полном объеме.
     * На выходе должен получиться коллекция ItemDTO с корректно заполненным индексом.
     */
    @Test
    public void fullConversionOfMarketSkuWithEmptyCargoTypes() throws IOException {
        testMarketSku("market_sku_cargo.json",
                "market_sku_cargo_expected.json");
    }

    /**
     * Проверяем сценарий с конвертацией Market SKU с отсутствующим параметром спайка.
     * <p>
     * Данные должны взяться из msku в полном объеме.
     * На выходе должен получиться коллекция ItemDTO с корректно заполненным индексом.
     */
    @Test
    public void fullConversionOfMarketSkuWithNoSpike() throws IOException {
        testMarketSku("market_sku_no_spike.json",
                "market_sku_no_spike_expected.json");
    }

    private void testShopSku(String test, String expected) throws IOException {
        String dataCampOfferJson = extractFileContent(TEST_PATH + test);
        DataCampUnitedOffer.UnitedOffer.Builder builder = DataCampUnitedOffer.UnitedOffer.newBuilder();

        protobufMapper.mergeJson(dataCampOfferJson, builder);
        DataCampUnitedOffer.UnitedOffer unitedOffer = builder.build();

        Collection<ItemDTO> result = dataCampToIrisConversionService.fromUnitedOffer(unitedOffer);

        String expectedSku = extractFileContent(TEST_PATH + expected);
        assertShopSkuConversion(result, expectedSku);
    }

    private void testMarketSku(String test, String expected) throws IOException {
        String dataCampMskuJson = extractFileContent(TEST_PATH + test);
        MarketSku.Builder builder = MarketSku.newBuilder();

        protobufMapper.mergeJson(dataCampMskuJson, builder);
        MarketSku marketSku = builder.build();

        Collection<ItemDTO> result = dataCampToIrisConversionService.fromMarketSku(marketSku);

        String expectedSku = extractFileContent(TEST_PATH + expected);

        assertNotNull(result);
        assertions().assertThat(result).isNotEmpty();
        assertions().assertThat(result).hasSize(3);

        assertMarketSkuConversion(result, expectedSku);
    }

    /**
     * Проверяем сценарий конвертации с отсутствием ReferenceIndex.
     * <p>
     * Данные offer отсутствуют.
     * На выходе должен получиться объект ItemDTO с пустым ReferenceIndex'ом.
     */
    @Test
    public void convertOfferWithoutIndex() {
        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(OfferIdentifiers.newBuilder())
                        .setContent(OfferContent.newBuilder().build())
                        .build())
                .putService(1, DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(OfferIdentifiers.newBuilder()
                                .setOfferId("ssku_1")
                                .setShopId(1)
                                .build())
                        .build())
                .putService(2, DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(OfferIdentifiers.newBuilder()
                                .setOfferId("ssku_2")
                                .setShopId(2)
                                .build())
                        .build())
                .build();

        Collection<ItemDTO> result = dataCampToIrisConversionService.fromUnitedOffer(unitedOffer);

        assertShopSkuConversion(result, "{}");
    }

    /**
     * Проверяем сценарий конвертации с отсутствием ReferenceIndex.
     * <p>
     * Данные market sku отсутствуют.
     * На выходе должен получиться объект ItemDTO с пустым ReferenceIndex'ом.
     */
    @Test
    public void convertMarketSkuWithoutIndex() {
        MarketSku marketSku = MarketSku.newBuilder()
                .setMappingToOffers(MappingToOffers.newBuilder()
                        .addOffers(OfferIdentifiers.newBuilder().setOfferId("msku_1").setShopId(1))
                        .addOffers(OfferIdentifiers.newBuilder().setOfferId("msku_2").setShopId(2))
                        .addOffers(OfferIdentifiers.newBuilder().setOfferId("msku_3").setShopId(3)))
                .setMboContent(MarketSkuMboContent.newBuilder()
                        .setMsku(ExportReportModel.newBuilder()
                                .setModifiedTs(1623772841000L))
                        .build())
                .build();

        Collection<ItemDTO> result = dataCampToIrisConversionService.fromMarketSku(marketSku);

        assertNotNull(result);
        assertions().assertThat(result).isNotEmpty();
        assertions().assertThat(result).hasSize(3);

        String expectedSku = extractFileContent(TEST_PATH + "market_sku_empty_expected.json");

        assertMarketSkuConversion(result, expectedSku);
    }

    private void assertMarketSkuConversion(Collection<ItemDTO> items, String expectedSku) {
        int i = 1;
        for (ItemDTO item : items) {
            assertNotNull(item);
            ItemIdentifierDTO itemIdentifier = item.getNaturalKey().getItemIdentifier();

            assertions().assertThat(itemIdentifier.getPartnerId()).isEqualTo(String.valueOf(i));
            assertions().assertThat(itemIdentifier.getPartnerSku()).isEqualTo("msku_" + i);

            ImmutableReferenceIndex index = item.getReferenceIndex();
            String dataCampInfoJson = index.toJson(referenceIndexer);

            assertEquals(expectedSku, dataCampInfoJson, JSONCompareMode.STRICT);
            i++;
        }
    }

    private void assertShopSkuConversion(Collection<ItemDTO> items, String expectedSku) {
        int i = 1;

        for (ItemDTO item : items) {
            assertNotNull(item);
            ItemIdentifierDTO itemIdentifier = item.getNaturalKey().getItemIdentifier();

            assertions().assertThat(itemIdentifier.getPartnerId()).isEqualTo(String.valueOf(i));
            assertions().assertThat(itemIdentifier.getPartnerSku()).isEqualTo("ssku_" + i);

            ImmutableReferenceIndex index = item.getReferenceIndex();
            String dataCampInfoJson = index.toJson(referenceIndexer);

            assertions().assertThat(dataCampInfoJson).is(jsonMatch(expectedSku));
            i++;
        }
    }
}
