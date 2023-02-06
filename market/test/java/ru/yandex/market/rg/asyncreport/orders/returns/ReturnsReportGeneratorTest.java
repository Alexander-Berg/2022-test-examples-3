package ru.yandex.market.rg.asyncreport.orders.returns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.orderservice.client.model.LogisticItemDTO;
import ru.yandex.market.orderservice.client.model.LogisticReturnStatusDTO;
import ru.yandex.market.orderservice.client.model.MerchantItemStatusDTO;
import ru.yandex.market.orderservice.client.model.OrderReturnRefundStatus;
import ru.yandex.market.orderservice.client.model.PagedReturnResponse;
import ru.yandex.market.orderservice.client.model.PagedReturnsResponse;
import ru.yandex.market.orderservice.client.model.PagerWithToken;
import ru.yandex.market.orderservice.client.model.RefundDTO;
import ru.yandex.market.orderservice.client.model.ReturnDTO;
import ru.yandex.market.orderservice.client.model.ReturnItemDecisionType;
import ru.yandex.market.orderservice.client.model.ReturnLineDTO;
import ru.yandex.market.orderservice.client.model.ReturnReasonType;
import ru.yandex.market.orderservice.client.model.ReturnSubreasonType;
import ru.yandex.market.orderservice.client.model.StockTypeDTO;
import ru.yandex.market.rg.client.orderservice.RgOrderServiceClient;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link  ReturnsGenerator}.
 */
@DbUnitDataSet(before = {"ReturnsReportGeneratorTest.csv", "Tanker.csv"})
public class ReturnsReportGeneratorTest extends FunctionalTest {

    private static final String PAGE_TOKEN_1 = "eyBuZXh0SWQ6IDIzNDIgfQ==";

    @Autowired
    private ReturnsGenerator returnsGenerator;

    @Autowired
    private RgOrderServiceClient orderServiceClient;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    public void prepare() {
        clock.setFixed(Instant.parse("2021-09-10T15:15:30.00Z"), ZoneOffset.UTC);
    }

    @AfterEach
    void tearDown() {
        clock.clearFixed();
    }

    @Test
    public void testGenerationSuccess() throws IOException, InvalidFormatException {
        var mockResponses = mockOsResponse();

        when(orderServiceClient.streamReturns(any())).thenCallRealMethod();
        doReturn(mockResponses.get(0)).when(orderServiceClient)
                .listReturns(Mockito.argThat(filter -> filter.getPageToken() == null));
        doReturn(mockResponses.get(1)).when(orderServiceClient)
                .listReturns(Mockito.argThat(filter -> Objects.equals(filter.getPageToken(), PAGE_TOKEN_1)));

        assertFilled(
                "returnsReportExpected.xlsx",
                new ReturnsParams(1234L, null, null, null)
        );
    }

    private void assertFilled(String expectedPath,
                              ReturnsParams reportParams) throws IOException, InvalidFormatException {
        File reportFile = TempFileUtils.createTempFile("tmpReport", ".xlsx");
        try (OutputStream output = new FileOutputStream(reportFile)) {
            returnsGenerator.generateReport(reportParams, output);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expectedXls = new XSSFWorkbook(getClass().getResourceAsStream(expectedPath));

        ExcelTestUtils.assertEquals(
                expectedXls,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    private List<CompletableFuture<PagedReturnResponse>> mockOsResponse() {
        var returnsResponse1 = new PagedReturnsResponse()
                .orderReturns(List.of(
                        new ReturnDTO()
                                .returnId(20L)
                                .orderId(30L)
                                .partnerOrderId("222-20")
                                .createdAt(OffsetDateTime.parse("2020-02-01T14:30:30+03:00"))
                                .updatedAt(OffsetDateTime.parse("2020-02-02T14:30:30+03:00"))
                                .returnStatus(OrderReturnRefundStatus.STARTED_BY_USER)
                                .logisticStatus(LogisticReturnStatusDTO.IN_TRANSIT)
                                .refundAmount(new BigDecimal(2000))
                                .applicationUrl("https://s3.mdst.yandex.net/return-application-20.pdf")
                                .returnLines(List.of(
                                        new ReturnLineDTO()
                                                .shopSku("sku-20")
                                                .marketSku(2000L)
                                                .count(1L)
                                                .refunds(List.of(
                                                        new RefundDTO()
                                                                .count(1)
                                                                .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                                                .returnSubreasonType(ReturnSubreasonType
                                                                        .USER_DID_NOT_LIKE)
                                                                .returnReason("Не подошел размер. Большая")
                                                                .partnerCompensation(new BigDecimal(100))
                                                                .refundAmount(new BigDecimal(1000))
                                                ))
                                                .logisticItems(List.of())
                                )),
                        new ReturnDTO()
                                .returnId(21L)
                                .orderId(31L)
                                .partnerOrderId("222-21")
                                .createdAt(OffsetDateTime.parse("2020-02-01T14:30:30+03:00"))
                                .updatedAt(OffsetDateTime.parse("2020-02-02T14:30:30+03:00"))
                                .returnStatus(OrderReturnRefundStatus.FAILED)
                                .logisticStatus(LogisticReturnStatusDTO.LOST)
                                .refundAmount(new BigDecimal(2000))
                                .applicationUrl("https://s3.mdst.yandex.net/return-application-21.pdf")
                                .returnLines(List.of(
                                        new ReturnLineDTO()
                                                .shopSku("sku-20")
                                                .marketSku(2000L)
                                                .count(1L)
                                                .refunds(List.of(
                                                        new RefundDTO()
                                                                .count(1)
                                                                .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                                                .returnSubreasonType(ReturnSubreasonType
                                                                        .USER_DID_NOT_LIKE)
                                                                .returnReason("Не подошел размер. Большая")
                                                                .partnerCompensation(new BigDecimal(100))
                                                                .refundAmount(new BigDecimal(1000))
                                                ))
                                                .logisticItems(List.of())
                                ))
                ));
        returnsResponse1.pager(new PagerWithToken()
                .pageSize(2)
                .nextPageToken(PAGE_TOKEN_1));
        var returnsResponse2 = new PagedReturnsResponse()
                .orderReturns(List.of(
                        new ReturnDTO()
                                .returnId(22L)
                                .orderId(32L)
                                .partnerOrderId("222-23")
                                .createdAt(OffsetDateTime.parse("2020-02-03T14:30:30+03:00"))
                                .updatedAt(OffsetDateTime.parse("2020-02-04T14:30:30+03:00"))
                                .returnStatus(OrderReturnRefundStatus.REFUNDED)
                                .logisticStatus(LogisticReturnStatusDTO.READY_FOR_PICKUP)
                                .refundAmount(new BigDecimal(3000))
                                .applicationUrl("https://s3.mdst.yandex.net/return-application-22.pdf")
                                .returnLines(List.of(
                                        new ReturnLineDTO()
                                                .shopSku("sku-21-0")
                                                .marketSku(2100L)
                                                .count(3L)
                                                .refunds(List.of(
                                                                new RefundDTO()
                                                                        .count(1)
                                                                        .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                                                        .returnSubreasonType(ReturnSubreasonType
                                                                                .USER_DID_NOT_LIKE)
                                                                        .returnReason("Не подошел размер. Большая")
                                                                        .decisionType(ReturnItemDecisionType
                                                                                .REFUND_MONEY)
                                                                        .partnerCompensation(new BigDecimal(100))
                                                                        .refundAmount(new BigDecimal(1000)),
                                                                new RefundDTO()
                                                                        .count(2)
                                                                        .returnReasonType(ReturnReasonType
                                                                                .WRONG_ITEM)
                                                                        .returnSubreasonType(ReturnSubreasonType
                                                                                .WRONG_ITEM)
                                                                        .returnReason("Не подошел размер. Большая")
                                                                        .decisionType(ReturnItemDecisionType
                                                                                .OTHER_DECISION)
                                                                        .partnerCompensation(new BigDecimal(200))
                                                                        .refundAmount(new BigDecimal(2000))
                                                        )
                                                )
                                                .logisticItems(List.of(
                                                        new LogisticItemDTO()
                                                                .stockType(StockTypeDTO.FIT)
                                                                .status(MerchantItemStatusDTO.RETURN_READY_FOR_PICKUP))
                                                )
                                ))
                ));
        returnsResponse2.pager(new PagerWithToken()
                .pageSize(1)
                .nextPageToken(null));
        return List.of(
                CompletableFuture.completedFuture(new PagedReturnResponse().result(returnsResponse1)),
                CompletableFuture.completedFuture(new PagedReturnResponse().result(returnsResponse2)));
    }
}
