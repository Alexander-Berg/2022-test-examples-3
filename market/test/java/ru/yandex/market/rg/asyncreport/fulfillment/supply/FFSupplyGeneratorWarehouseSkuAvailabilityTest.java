package ru.yandex.market.rg.asyncreport.fulfillment.supply;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampUnitedOffer;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.core.feed.validation.result.XlsTestUtils;
import ru.yandex.market.core.fulfillment.supply.FFSupplyGenerator;
import ru.yandex.market.core.fulfillment.supply.FFSupplyTemplateParams;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static ru.yandex.market.core.feed.validation.result.XlsTestUtils.buildExpectedMap;

public class FFSupplyGeneratorWarehouseSkuAvailabilityTest extends AbstractFFSupplyGeneratorTest {

    @Autowired
    private FFSupplyGenerator ffSupplyGenerator;
    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Nonnull
    private static Stream<Arguments> skuWarehouseAvailabilitySet() {
        return Stream.of(
                Arguments.of(145, 4,
                        ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                .putAll(buildExpectedMap(1,
                                        "145_147_true_true",
                                        "145 147 true true"
                                ))
                                .putAll(buildExpectedMap(2,
                                        "145_true_true_147_false_false",
                                        "145 true true 147 false false"
                                ))
                                .putAll(buildExpectedMap(3,
                                        "145_true_true_147_false_true",
                                        "145 true true 147 false true"
                                ))
                                .build()
                ),
                Arguments.of(147, 2,
                        ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                .putAll(buildExpectedMap(1,
                                        "145_147_true_true",
                                        "145 147 true true"
                                ))
                                .build()
                )
        );
    }

    @BeforeEach
    void init() throws IOException {
        Mockito.when(dataCampShopClient.searchBusinessOffers(Mockito.any()))
                .thenReturn(SearchBusinessOffersResult.builder()
                        .setOffers(List.of(
                                generateDataCampOffer("145 147 true false", "145_147_true_false"),
                                generateDataCampOffer("145 147 false true", "145_147_false_true"),
                                generateDataCampOffer("145 147 true true", "145_147_true_true"),
                                generateDataCampOffer("145 147 false false", "145_147_false_false"),
                                generateDataCampOffer("145 true true 147 false false", "145_true_true_147_false_false"),
                                generateDataCampOffer("145 true true 147 false true", "145_true_true_147_false_true")
                        ))
                        .build());
        mockMboDeliveryParamsClient("searchFulfillmentSskuParams.filter.warehouses.data.json");
    }

    private DataCampUnitedOffer.UnitedOffer generateDataCampOffer(String title, String offerId) {
        return DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId(offerId)
                                .build())
                        .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                        .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                                                .setName(DataCampOfferMeta.StringValue.newBuilder()
                                                        .setValue(title)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())

                .build();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Проверка генерации отчета о доступности складов")
    @MethodSource({"skuWarehouseAvailabilitySet"})
    void testSkuWarehouseAvailability(long warehouseId, int expectedRowCount,
                                      Map<XlsTestUtils.CellInfo, String> expected) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(1000);
        ffSupplyGenerator.xlsFFSupplyReport(
                FFSupplyTemplateParams.newBuilder()
                        .withPartnerId(10103L)
                        .withWarehouseId(warehouseId)
                        .build(),
                output
        );

        assertSheet(expectedRowCount, output, expected);
    }
}
