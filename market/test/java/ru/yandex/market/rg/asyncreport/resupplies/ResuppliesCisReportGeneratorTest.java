package ru.yandex.market.rg.asyncreport.resupplies;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.id.HasId;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.delivery.DeliveryServiceInfo;
import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.orderservice.client.model.LogisticItemDTO;
import ru.yandex.market.orderservice.client.model.ReturnDTO;
import ru.yandex.market.orderservice.client.model.ReturnLineDTO;
import ru.yandex.market.orderservice.client.model.ReturnTypeDTO;
import ru.yandex.market.orderservice.client.model.StockTypeDTO;
import ru.yandex.market.rg.client.orderservice.RgOrderServiceClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Тест на {@link ResuppliesCisReportGenerator}
 */
class ResuppliesCisReportGeneratorTest {

    private static final LocalDateTime ORDER_CREATION_DATE =
            LocalDateTime.of(2021, 1, 1, 5, 0);

    private static final LocalDateTime FF_FINALIZATION =
            LocalDateTime.of(2021, 1, 1, 5, 0);

    @ParameterizedTest(name = "{0}")
    @MethodSource("args")
    void testXlsReport(@SuppressWarnings("unused") String description,
                       ResuppliesCisParams resuppliesCisParams,
                       List<ReturnDTO> returns,
                       String expectedFilePath) throws Exception {

        final ResuppliesCisReportGenerator resuppliesCisReportGenerator = new ResuppliesCisReportGenerator(
                createDeliveryServiceMock(),
                createRgOrderServiceClientMock(returns)
        );

        final Path tempFilePath = Files.createTempFile("UnredeemedCisReportGeneratorTest", ".xlsx");
        final File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            resuppliesCisReportGenerator.generateXls(resuppliesCisParams, output);
        }

        final XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        final XSSFWorkbook expected = new XSSFWorkbook(
                Preconditions.checkNotNull(getClass().getResourceAsStream(expectedFilePath)));

        ExcelTestUtils.assertEquals(expected, actual);
    }

    private RgOrderServiceClient createRgOrderServiceClientMock(List<ReturnDTO> osData) {
        final RgOrderServiceClient client = mock(RgOrderServiceClient.class);
        doReturn(osData.stream()).when(client).streamReturns(any());
        return client;
    }

    private DeliveryInfoService createDeliveryServiceMock() {
        final DeliveryServiceInfo warehouse1 = new DeliveryServiceInfo(11L, "Склад1");
        final DeliveryServiceInfo warehouse2 = new DeliveryServiceInfo(111L, "Склад2");
        final Map<Long, String> warehouses = Stream.of(warehouse1, warehouse2)
                .collect(Collectors.toMap(HasId::getId, DeliveryServiceInfo::getName));

        final DeliveryInfoService service = mock(DeliveryInfoService.class);
        doReturn(warehouses).when(service).fetchDeliveryServiceNames(anySet());
        return service;
    }

    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(
                        "Генерация отчета через OS",
                        new ResuppliesCisParams(1, null, Instant.now(), Instant.now(), null),
                        List.of(
                                new ReturnDTO()
                                        .returnType(ReturnTypeDTO.UNREDEEMED)
                                        .orderId(44L)
                                        .orderCreationDate(toDateTime(ORDER_CREATION_DATE))
                                        .returnLines(List.of(
                                                new ReturnLineDTO()
                                                        .shopSku("sku")
                                                        .offerName("Name1")
                                                        .logisticItems(List.of(
                                                                new LogisticItemDTO()
                                                                        .stockType(StockTypeDTO.FIT)
                                                                        .warehouseId(11L)
                                                                        .ffRequestStatusCommittedAt(toDateTime(FF_FINALIZATION))
                                                                        .itemInfo(Map.of("CIS", "cis"))
                                                        ))
                                        )),
                                new ReturnDTO()
                                        .returnType(ReturnTypeDTO.RETURN)
                                        .orderId(441L)
                                        .orderCreationDate(toDateTime(ORDER_CREATION_DATE))
                                        .returnLines(List.of(
                                                new ReturnLineDTO()
                                                        .shopSku("sku1")
                                                        .offerName("Name2")
                                                        .logisticItems(List.of(
                                                                new LogisticItemDTO()
                                                                        .stockType(StockTypeDTO.DEFECT)
                                                                        .warehouseId(111L)
                                                                        .ffRequestStatusCommittedAt(toDateTime(FF_FINALIZATION))
                                                                        .itemInfo(Map.of("CIS", "cis1"))
                                                        ))
                                        ))
                        ),
                        "UnredeemedCisReportGeneratorTest.expected.1.xlsx"
                )
        );
    }

    private static OffsetDateTime toDateTime(LocalDateTime orderCreationDate) {
        return DateTimes.toOffsetDateTime(orderCreationDate, DateTimes.MOSCOW_TIME_ZONE);
    }

}
