package ru.yandex.market.api.partner.controllers.stats.ordersreport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.stats.ordersreport.model.OrderStatus;
import ru.yandex.market.api.partner.controllers.stats.ordersreport.model.OrdersReportRequest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.fulfillment.tariff.TariffsIterator;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.FulfillmentTariffsJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.PartnerType;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.SupplierCategoryFeeTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.mockito.Mockito.doAnswer;

@DbUnitDataSet(before = "OrdersReportControllerTest.before.csv")
class OrdersReportControllerTest extends FunctionalTest {
    @Autowired
    private TariffsService clientTariffsService;

    private static final long CAMPAIGN_ID = 1000571241L;
    private static final long DROPSHIP_CAMPAIGN_ID = 10668L;

    private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private TariffDTO createTariff(
            ServiceTypeEnum serviceType,
            Integer priceTo,
            Integer dimensionsTo,
            Integer weightTo,
            BigDecimal value,
            CommonJsonSchema.TypeEnum valueType,
            BillingUnitEnum billingUnit,
            ModelType modelType
    ) {
        TariffDTO tariff = new TariffDTO();
        tariff.setDateFrom(LocalDate.of(2018, 1, 1));
        tariff.setDateTo(null);
        tariff.setServiceType(serviceType);
        tariff.setModelType(modelType);
        Partner partner = new Partner();
        partner.setType(PartnerType.SUPPLIER);
        partner.setId(null);
        tariff.setPartner(partner);
        FulfillmentTariffsJsonSchema schema = new FulfillmentTariffsJsonSchema();
        schema.setPriceTo(priceTo);
        schema.setDimensionsTo(dimensionsTo);
        schema.setWeightTo(weightTo);
        schema.setCurrency("RUB");
        schema.setAmount(value);
        schema.setMinValue(null);
        schema.setMaxValue(null);
        schema.setType(valueType);
        schema.setBillingUnit(billingUnit);
        tariff.setMeta(List.of(schema));
        return tariff;
    }

    private TariffDTO createFee(
            Long hyperId
    ) {
        TariffDTO tariff = new TariffDTO();
        tariff.setDateFrom(LocalDate.of(2018, 1, 1));
        tariff.setDateTo(null);
        Partner partner = new Partner();
        partner.setType(PartnerType.SUPPLIER);
        partner.setId(666L);
        tariff.setPartner(partner);
        tariff.setServiceType(ServiceTypeEnum.FEE);
        tariff.setModelType(ModelType.FULFILLMENT_BY_YANDEX);
        SupplierCategoryFeeTariffJsonSchema schema = new SupplierCategoryFeeTariffJsonSchema();
        schema.setCategoryId(hyperId);
        schema.setCurrency("RUB");
        schema.setAmount(new BigDecimal(0.5));
        tariff.setMeta(List.of(schema));
        return tariff;
    }

    @BeforeEach
    void mock() {
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");
            List<ServiceTypeEnum> types = findQuery.getServiceTypes();
            if (types.contains(ServiceTypeEnum.FEE)) {
                return new TariffsIterator((pageNumber, batchSize) -> List.of(
                        createFee(1L),
                        createFee(278423L),
                        createFee(15510954L),
                        createFee(12341959L),
                        createFee(91650L),
                        createFee(226667L),
                        createFee(90016L)
                ));
            }
            else if (types.contains(ServiceTypeEnum.MIN_FEE)) {
                return new TariffsIterator((pageNumber, batchSize) -> List.of());
            }
            else if (types.contains(ServiceTypeEnum.CASH_ONLY_ORDER)) {
                return new TariffsIterator((pageNumber, batchSize) -> List.of());
            }
            else if (types.contains(ServiceTypeEnum.CANCELLED_ORDER_FEE)) {
                return new TariffsIterator((pageNumber, batchSize) -> List.of());
            }
            else {
                return new TariffsIterator((pageNumber, batchSize) -> List.of(
                        createTariff(
                                ServiceTypeEnum.FF_PROCESSING,
                                100,
                                150,
                                25000,
                                new BigDecimal("0.01"),
                                CommonJsonSchema.TypeEnum.ABSOLUTE,
                                BillingUnitEnum.ITEM,
                                ModelType.FULFILLMENT_BY_YANDEX
                        ),
                        createTariff(
                                ServiceTypeEnum.FF_PROCESSING,
                                200,
                                150,
                                25000,
                                new BigDecimal("0.02"),
                                CommonJsonSchema.TypeEnum.ABSOLUTE,
                                BillingUnitEnum.SUPPLY_PALLET,
                                ModelType.FULFILLMENT_BY_YANDEX
                        ),
                        createTariff(
                                ServiceTypeEnum.FF_PROCESSING,
                                null,
                                150,
                                25000,
                                new BigDecimal("0.03"),
                                CommonJsonSchema.TypeEnum.RELATIVE,
                                BillingUnitEnum.SUPPLY_PALLET,
                                ModelType.FULFILLMENT_BY_YANDEX
                        ),
                        createTariff(
                                ServiceTypeEnum.FF_PROCESSING,
                                null,
                                150,
                                15000,
                                new BigDecimal("0.04"),
                                CommonJsonSchema.TypeEnum.ABSOLUTE,
                                BillingUnitEnum.SUPPLY_BOX,
                                ModelType.FULFILLMENT_BY_YANDEX_PLUS
                        ),
                        createTariff(
                                ServiceTypeEnum.FF_STORAGE_BILLING,
                                null,
                                150,
                                15000,
                                new BigDecimal("0.05"),
                                CommonJsonSchema.TypeEnum.RELATIVE,
                                BillingUnitEnum.ITEM,
                                ModelType.FULFILLMENT_BY_YANDEX
                        ),
                        createTariff(
                                ServiceTypeEnum.FF_WITHDRAW,
                                null,
                                250,
                                15000,
                                new BigDecimal("0.06"),
                                CommonJsonSchema.TypeEnum.ABSOLUTE,
                                BillingUnitEnum.SUPPLY_PALLET,
                                ModelType.FULFILLMENT_BY_YANDEX
                        )
                ));
            }
        }).when(clientTariffsService).findTariffs(Mockito.any(TariffFindQuery.class));
    }

    @Test
    void getAllOrders() throws Exception {
        OrdersReportRequest request = new OrdersReportRequest(null, null, null, null, null, null, false);
        checkResponse(doRequest(Collections.emptyMap(), CAMPAIGN_ID, request),
                "OrdersReportControllerTest.allOrders.json");
    }

    @Test
    void getMultiOrderWithReturn() throws Exception {
        OrdersReportRequest request = new OrdersReportRequest(null, null, null, null, null, null, false);
        checkResponse(
                doRequest(Collections.emptyMap(), 10222L, request),
                "OrdersReportControllerTest.multiOrderWithReturn.json"
        );
    }

    @Test
    void getRejectedOrderDropship() throws Exception {
        OrdersReportRequest request = new OrdersReportRequest(null, null, null, null, null, null, false);
        checkResponse(doRequest(Collections.emptyMap(), DROPSHIP_CAMPAIGN_ID, request),
                "OrdersReportControllerTest.rejectedDropshipOrder.json");
    }

    @DbUnitDataSet(before = "OrdersReportControllerTest.dsbs.before.csv")
    @Test
    void getAllDsbsOrders() throws Exception {
        OrdersReportRequest request = new OrdersReportRequest(null, null, null, null, null, null, false);
        checkResponse(doRequest(Collections.emptyMap(), 778855, request), "OrdersReportControllerTest.dsbs.json");
    }

    @Test
    void getByOrders() throws Exception {
        List<Long> orderIds = List.of(4843881L, 3829565L, 4451400L);
        OrdersReportRequest request = new OrdersReportRequest(orderIds, null, null, null, null, null, false);
        checkResponse(doRequest(Collections.emptyMap(), CAMPAIGN_ID, request),
                "OrdersReportControllerTest.byOrders.json");
    }

    @Test
    void getWithDates() throws Exception {
        LocalDate dateFrom = LocalDate.parse("2020-02-15");
        LocalDate dateTo = LocalDate.parse("2020-02-20");
        OrdersReportRequest request = new OrdersReportRequest(null, dateFrom, dateTo, null, null, null, false);
        checkResponse(doRequest(Collections.emptyMap(), CAMPAIGN_ID, request), "OrdersReportControllerTest.withDates" +
                ".json");
    }

    @Test
    void getWithDatesInclToDate() throws Exception {
        LocalDate dateFrom = LocalDate.parse("2020-02-15");
        LocalDate dateTo = LocalDate.parse("2020-02-18");
        OrdersReportRequest request = new OrdersReportRequest(null, dateFrom, dateTo, null, null, null, false);
        checkResponse(doRequest(Collections.emptyMap(), CAMPAIGN_ID, request),
                "OrdersReportControllerTest.withDates.json");
    }

    @Test
    void getWithDatesSame() throws Exception {
        LocalDate dateFrom = LocalDate.parse("2020-02-18");
        LocalDate dateTo = LocalDate.parse("2020-02-18");
        OrdersReportRequest request = new OrdersReportRequest(null, dateFrom, dateTo, null, null, null, false);
        checkResponse(doRequest(Collections.emptyMap(), CAMPAIGN_ID, request),
                "OrdersReportControllerTest.withDatesSame.json");
    }

    @Test
    void getByStatusUpdateDate() throws Exception {
        LocalDate updateFrom = LocalDate.parse("2019-12-01");
        LocalDate updateTo = LocalDate.parse("2020-02-01");
        OrdersReportRequest request = new OrdersReportRequest(null, null, null, updateFrom, updateTo, null,
            false);
        checkResponse(doRequest(Collections.emptyMap(), CAMPAIGN_ID, request),
                "OrdersReportControllerTest.statusDates.json");
    }

    @Test
    void getByStatusCount() throws Exception {
        OrdersReportRequest request = new OrdersReportRequest(List.of(1234569L, 1234568L), null, null, null, null,
                null, false);
        checkResponse(doRequest(Collections.emptyMap(), CAMPAIGN_ID, request), "OrdersReportControllerTest.count.json");
    }

    @Test
    void getByStatusUpdateDateSame() throws Exception {
        LocalDate updateFrom = LocalDate.parse("2019-12-25");
        LocalDate updateTo = LocalDate.parse("2019-12-25");
        OrdersReportRequest request = new OrdersReportRequest(null, null, null, updateFrom, updateTo, null, false);
        checkResponse(doRequest(Collections.emptyMap(), CAMPAIGN_ID, request),
                "OrdersReportControllerTest.statusDatesSame.json");
    }

    @Test
    void getByStatusUpdateDateLast() throws Exception {
        LocalDate updateFrom = LocalDate.parse("2019-12-01");
        LocalDate updateTo = LocalDate.parse("2019-12-25");
        OrdersReportRequest request = new OrdersReportRequest(null, null, null, updateFrom, updateTo, null, false);
        checkResponse(doRequest(Collections.emptyMap(), CAMPAIGN_ID, request),
                "OrdersReportControllerTest.statusDatesSame.json");
    }

    @Test
    void getByStatuses() throws Exception {
        List<OrderStatus> statuses = List.of(OrderStatus.DELIVERY, OrderStatus.CANCELLED_IN_DELIVERY);
        OrdersReportRequest request = new OrdersReportRequest(null, null, null, null, null, statuses, false);
        checkResponse(doRequest(Collections.emptyMap(), CAMPAIGN_ID, request),
                "OrdersReportControllerTest.statuses.json");
    }

    @Test
    void withLimit() throws Exception {
        OrdersReportRequest request = new OrdersReportRequest(null, null, null, null, null, null, false);
        checkResponse(doRequest(Collections.singletonMap("limit", "2"), CAMPAIGN_ID, request),
                "OrdersReportControllerTest.limitOrders.json");
    }

    @Test
    void withCises() throws Exception {
        OrdersReportRequest request = new OrdersReportRequest(null, null, null, null, null, null, true);
        checkResponse(doRequest(Collections.emptyMap(), CAMPAIGN_ID, request),
                "OrdersReportControllerTest.withCises.json");
    }

    @Test
    void getWithoutBody() throws Exception {
        checkResponse(doRequest(Collections.singletonMap("limit", "2"), CAMPAIGN_ID, null),
                "OrdersReportControllerTest.limitOrders.json");
    }

    private ResponseEntity<String> doRequest(
            Map<String, String> queryParams,
            long campaignId,
            @Nullable OrdersReportRequest request
    ) throws Exception {
        URIBuilder uriBuilder =
                new URIBuilder(
                        String.format(Locale.US, "%s/campaigns/%d/stats/orders.%s",
                                urlBasePrefix, campaignId, Format.JSON));
        for (Map.Entry<String, String> argument : queryParams.entrySet()) {
            uriBuilder.addParameter(argument.getKey(), argument.getValue());
        }

        return request != null ?
                FunctionalTestHelper.makeRequest(uriBuilder.build(), HttpMethod.POST, Format.JSON, toJson(request))
                : FunctionalTestHelper.makeRequest(uriBuilder.build(), HttpMethod.POST, Format.JSON);
    }

    protected String toJson(OrdersReportRequest request) throws JsonProcessingException {
        return JSON_OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(request);
    }

    private void checkResponse(final ResponseEntity<String> response, final String fileName) {
        JsonTestUtil.assertEquals(response, this.getClass(), fileName);
    }
}
