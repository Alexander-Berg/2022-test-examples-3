package ru.yandex.market.rg.asyncreport.shipment;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;

/**
 * Тесты для {@link FirstMileShipmentListReportGenerator}.
 */
@DbUnitDataSet(before = "Tanker.csv")
class FirstMileShipmentListReportGeneratorTest extends AbstractFirstMileShipmentReportGeneratorTest {

    private static final long PARTNER_ID = 101L;
    private static final long SHIPMENT_ID = 123456L;
    private static final long USER_ID = 908765487L;
    private static final long KNOWN_ORDER_ID = 11L;
    private static final long UNKNOWN_ORDER_ID = 2517L;

    @Autowired
    private FirstMileShipmentListReportGenerator reportGenerator;

    @Autowired
    private MboMappingsService patientMboMappingsService;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Value("${mbi.mds.s3.bucket}")
    private String bucket;


    @ParameterizedTest(name = "{0}")
    @MethodSource("generatorTestData")
    void testGenerator(final String testCaseTitle, FirstMileShipmentReportParams params) throws Exception {
        mockCheckouterClient();
        mockNesuClient();
        mockMboClient();
        mockMdsS3();

        final ReportResult reportResult =
                reportGenerator.generate(ReportsType.FIRST_MILE_SHIPMENT_LIST.getId(), params);

        Assertions.assertThat(reportResult).isNotNull()
                .extracting(ReportResult::getNewState).isEqualTo(ReportState.DONE);
    }

    private static Stream<Arguments> generatorTestData() {
        return Stream.of(
                Arguments.of(
                        "Фильтр по заказам отсутствует, берутся все заказы отгрузки",
                        new FirstMileShipmentReportParams(PARTNER_ID, SHIPMENT_ID, USER_ID, List.of())
                ),
                Arguments.of(
                        "В фильтре по заказам присутствует известный ID заказа",
                        new FirstMileShipmentReportParams(PARTNER_ID, SHIPMENT_ID, USER_ID, List.of(KNOWN_ORDER_ID))
                ),
                Arguments.of(
                        "В фильтре по заказам присутствует неизвестный ID заказа",
                        new FirstMileShipmentReportParams(PARTNER_ID, SHIPMENT_ID, USER_ID, List.of(UNKNOWN_ORDER_ID))
                )
        );
    }

    private void mockMboClient() {
        Mockito.when(patientMboMappingsService.searchMappingsByKeys(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setSupplierId(101L)
                                                .setShopSkuId("ssku1")
                                                .setBarcode("barcode1")
                                                .build())
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setSupplierId(101L)
                                                .setShopSkuId("ssku2")
                                                .setBarcode("barcode2")
                                                .build())
                                .build());
    }

    private void mockMdsS3() throws MalformedURLException {
        final String fileName = "reports/101/first_mile_shipment_list/FIRST_MILE_SHIPMENT_LIST.pdf";
        Mockito.when(mdsS3Client.getUrl(ResourceLocation.create(bucket, fileName)))
                .thenReturn(new URL(String.format("https://%s/%s", bucket, fileName)));
    }

}
