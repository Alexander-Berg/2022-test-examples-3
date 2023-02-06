package ru.yandex.market.core.fulfillment.report;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubmethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.report.excel.StatisticsReportGenerator;
import ru.yandex.market.core.fulfillment.report.oebs.OebsReportApi;
import ru.yandex.market.core.fulfillment.report.oebs.OebsSendingService;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.partner.contract.PartnerContractDao;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class OebsSendingSupplierDataTest extends FunctionalTest {
    private static final YearMonth REPORT_GENERATION_DATE = YearMonth.of(2018, 7);
    private static final String OEBS_PUSH_MARKET_REPORT_STATISTIC_URL =
            "https://oebsapi-test.mba.yandex-team.ru/rest/pushMarketReportStatistics";
    private static final String EXPECTED_CONTENT_TYPE = "application/json;charset=UTF-8";
    private static final String SUCCESS_RESPONSE_OK_FILE = "OebsSendingSupplierDataTest.successResponse.json";
    private static final String EXPECTED_JSON_RESPONSE_FILE = "OebsSendingSupplierDataTest.expectedResponseJson.json";

    @Value("${oebs.push.market.report.statistics.url}")
    private String oebsMarketReportStatisticsUrl;

    @Value("${mbi.robot.oebs.api.token}")
    private String oebsApiToken;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockRestServiceServer;

    @Autowired
    private PartnerContractDao supplierContractDao;

    @Autowired
    private StatisticReportQueueService monthlyStatisticReportQueueService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private OebsReportApi oebsReportApi;

    @BeforeEach
    void setUp() {
        mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
    }

    private static SummaryReportData getTestReportData() {
        SummaryReportData summaryReportData = new SummaryReportData();
        YearMonth ym = YearMonth.of(2018, 7);

        OrderStatisticsReportRow delivered = OrderStatisticsReportRow.builder()
                .setPartnerId(465826)
                .setCreationDate(LocalDateTime.of(2018, 7, 10, 0, 0, 0))
                .setPaymentType(PaymentType.PREPAID)
                .setPaymentMethod(PaymentMethod.TINKOFF_CREDIT)
                .setCession(false)
                .setPaymentSubmethod(PaymentSubmethod.DEFAULT)
                .setOrderId(1898847)
                .setShippingTime(LocalDateTime.of(2018, 7, 11, 12, 0, 0))
                .setDeliveryTime(LocalDateTime.of(2018, 7, 12, 12, 0, 0))
                .setOfferName("Attento")
                .setShopSku("cbb2282b-eea7-11e6-810b-00155d000405")
                .setMbiOrderStatus(MbiOrderStatus.DELIVERED)
                .setVatRate(VatRate.VAT_20)
                .setDeliveredPrices(new OrderStatisticsReportRowPrices(10, new BigDecimal(1000),
                        new BigDecimal(100), new BigDecimal(50), new BigDecimal(50)))
                .setDeliveryPrices(new OrderStatisticsReportRowPrices(10, new BigDecimal(1000),
                        new BigDecimal(100), new BigDecimal(50), new BigDecimal(50)))
                .setUnredeemedPrices(new OrderStatisticsReportRowPrices(0, new BigDecimal(1000),
                        new BigDecimal(100), new BigDecimal(50), new BigDecimal(50)))
                .setReturnedPrices(new OrderStatisticsReportRowPrices(0, new BigDecimal(1000),
                        new BigDecimal(100), new BigDecimal(50), new BigDecimal(50)))
                .build();
        summaryReportData.addReportRow(delivered, ym);
        OrderStatisticsReportRow returned = OrderStatisticsReportRow.builder()
                .setPartnerId(465826)
                .setCreationDate(LocalDateTime.of(2018, 7, 10, 0, 0, 0))
                .setPaymentType(PaymentType.PREPAID)
                .setPaymentMethod(PaymentMethod.TINKOFF_CREDIT)
                .setCession(false)
                .setPaymentSubmethod(PaymentSubmethod.DEFAULT)
                .setOrderId(1898848)
                .setOfferName("Attento")
                .setShopSku("cbb2282b-eea7-11e6-810b-00155d000405")
                .setMbiOrderStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY)
                .setVatRate(VatRate.VAT_20)
                .setShippingTime(LocalDateTime.of(2018, 7, 11, 12, 0, 0))
                .setDeliveryTime(LocalDateTime.of(2018, 7, 12, 12, 0, 0))
                .setResupplyTime(LocalDateTime.of(2018, 7, 13, 12, 0, 0))
                .setReturnedPrices(new OrderStatisticsReportRowPrices(2, new BigDecimal(1000),
                        new BigDecimal(100), new BigDecimal(50), new BigDecimal(50)))
                .setDeliveryPrices(new OrderStatisticsReportRowPrices(2, new BigDecimal(1000),
                        new BigDecimal(100), new BigDecimal(50), new BigDecimal(50)))
                .setDeliveredPrices(new OrderStatisticsReportRowPrices(2, new BigDecimal(1000),
                        new BigDecimal(100), new BigDecimal(50), new BigDecimal(50)))
                .setUnredeemedPrices(new OrderStatisticsReportRowPrices(0, new BigDecimal(1000),
                        new BigDecimal(100), new BigDecimal(50), new BigDecimal(50)))
                .build();
        summaryReportData.addReportRow(returned, ym);
        OrderStatisticsReportRow delivery = OrderStatisticsReportRow.builder()
                .setPartnerId(465826)
                .setCreationDate(LocalDateTime.of(2018, 7, 10, 0, 0, 0))
                .setPaymentType(PaymentType.PREPAID)
                .setPaymentMethod(PaymentMethod.TINKOFF_CREDIT)
                .setCession(false)
                .setPaymentSubmethod(PaymentSubmethod.DEFAULT)
                .setOrderId(1898811)
                .setShippingTime(LocalDateTime.of(2018, 7, 12, 12, 0, 0))
                .setOfferName("IPhone")
                .setShopSku("crz2282b-eea7-11e6-810b-00155d000405")
                .setMbiOrderStatus(MbiOrderStatus.DELIVERY)
                .setVatRate(VatRate.VAT_18_118)
                .setDeliveryPrices(new OrderStatisticsReportRowPrices(3, new BigDecimal(10000),
                        new BigDecimal(1000), new BigDecimal(500), new BigDecimal(500)))
                .setDeliveredPrices(new OrderStatisticsReportRowPrices(3, new BigDecimal(10000),
                        new BigDecimal(1000), new BigDecimal(500), new BigDecimal(500)))
                .setUnredeemedPrices(new OrderStatisticsReportRowPrices(0, new BigDecimal(10000),
                        new BigDecimal(1000), new BigDecimal(500), new BigDecimal(500)))
                .setReturnedPrices(new OrderStatisticsReportRowPrices(0, new BigDecimal(10000),
                        new BigDecimal(1000), new BigDecimal(500), new BigDecimal(500)))
                .build();
        summaryReportData.addReportRow(delivery, ym);
        OrderStatisticsReportRow unredeemed = OrderStatisticsReportRow.builder()
                .setPartnerId(465826)
                .setCreationDate(LocalDateTime.of(2018, 7, 10, 0, 0, 0))
                .setPaymentType(PaymentType.PREPAID)
                .setPaymentMethod(PaymentMethod.TINKOFF_CREDIT)
                .setCession(false)
                .setPaymentSubmethod(PaymentSubmethod.DEFAULT)
                .setOrderId(1898810)
                .setOfferName("IPhone")
                .setShopSku("crz2282b-eea7-11e6-810b-00155d000405")
                .setMbiOrderStatus(MbiOrderStatus.PICKUP)
                .setVatRate(VatRate.VAT_18_118)
                .setShippingTime(LocalDateTime.of(2018, 7, 11, 12, 0, 0))
                .setResupplyTime(LocalDateTime.of(2018, 7, 13, 12, 0, 0))
                .setUnredeemedPrices(new OrderStatisticsReportRowPrices(1, new BigDecimal(10000),
                        new BigDecimal(1000), new BigDecimal(500), new BigDecimal(500)))
                .setDeliveryPrices(new OrderStatisticsReportRowPrices(0, new BigDecimal(10000),
                        new BigDecimal(1000), new BigDecimal(500), new BigDecimal(500)))
                .setDeliveredPrices(new OrderStatisticsReportRowPrices(0, new BigDecimal(10000),
                        new BigDecimal(1000), new BigDecimal(500), new BigDecimal(500)))
                .setReturnedPrices(new OrderStatisticsReportRowPrices(0, new BigDecimal(10000),
                        new BigDecimal(1000), new BigDecimal(500), new BigDecimal(500)))
                .build();
        summaryReportData.addReportRow(unredeemed, ym);
        return summaryReportData;
    }

    @Test
    @DbUnitDataSet(before = "OebsSendingSupplierDataTest.before.csv")
    void sendReportSuccessWithCompletedContractTest() throws IOException {
        createMockedOebsRestService();

        StatisticsReportGenerator generator = mock(StatisticsReportGenerator.class);
        when(generator.getSummaryReportData(any(), any(), any())).thenReturn(getTestReportData());
        OebsSendingService oebsSendingService = new OebsSendingService(
                generator,
                supplierContractDao,
                restTemplate,
                oebsMarketReportStatisticsUrl,
                oebsApiToken,
                monthlyStatisticReportQueueService,
                environmentService,
                oebsReportApi
        );

        oebsSendingService.sendReport(REPORT_GENERATION_DATE, List.of(465826L));

        mockRestServiceServer.verify();

    }

    private void createMockedOebsRestService() throws IOException {
        mockRestServiceServer.expect(requestTo(OEBS_PUSH_MARKET_REPORT_STATISTIC_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(request -> {
                    MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
                    final String actualJson = mockRequest.getBodyAsString();

                    String expectedJson = getJsonAsString(EXPECTED_JSON_RESPONSE_FILE);

                    try {
                        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                })
                .andRespond(withSuccess(getJsonAsString(SUCCESS_RESPONSE_OK_FILE), MediaType.APPLICATION_JSON));
    }

    private String getJsonAsString(String successResponseOkFile) throws IOException {
        return IOUtils.readInputStream(
                getClass().getResourceAsStream(successResponseOkFile)
        );
    }
}
