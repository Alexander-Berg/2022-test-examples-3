package ru.yandex.market.partner.mvc.controller.fulfillment.calculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.billing.commission.DbSupplierCategoryFeeDao;
import ru.yandex.market.core.billing.commission.InternalFeeServiceType;
import ru.yandex.market.core.billing.commission.SupplierCategoryFee;
import ru.yandex.market.core.billing.fulfillment.storage.StorageMultiplierService;
import ru.yandex.market.core.billing.fulfillment.xdoc.tariff.XdocSupplyTariffService;
import ru.yandex.market.core.date.Period;
import ru.yandex.market.core.fulfillment.FulfillmentTariffDao;
import ru.yandex.market.core.fulfillment.OrderType;
import ru.yandex.market.core.fulfillment.ReturnedOrdersStorageTariffService;
import ru.yandex.market.core.fulfillment.billing.disposal.DisposalTariffService;
import ru.yandex.market.core.fulfillment.calculator.TarifficatorService;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.fulfillment.model.BillingUnit;
import ru.yandex.market.core.fulfillment.model.FulfillmentTariff;
import ru.yandex.market.core.fulfillment.model.ValueType;
import ru.yandex.market.core.fulfillment.tariff.YtTariffsService;
import ru.yandex.market.core.order.model.MbiBlueOrderType;
import ru.yandex.market.core.sorting.SortingDailyTariffDao;
import ru.yandex.market.core.sorting.SortingOrdersTariffDao;
import ru.yandex.market.core.sorting.model.SortingDailyTariff;
import ru.yandex.market.core.sorting.model.SortingIntakeType;
import ru.yandex.market.core.sorting.model.SortingOrderTariff;
import ru.yandex.market.fulfillment.entities.base.DateTimeInterval;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.XdocSupplyServiceEnum;
import ru.yandex.market.mbi.tariffs.client.model.XdocSupplyTariffJsonSchema;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.mvc.controller.fulfillment.model.OfferCartDTO;
import ru.yandex.market.partner.mvc.controller.fulfillment.model.OfferCartListDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.JsonTestUtil.parseJson;

/**
 * Тесты для {@link TarifficatorController}
 */
@DbUnitDataSet(before = "db/TariffCalculatorControllerTest.common.before.csv")
class TarifficatorControllerTest extends FunctionalTest {
    private static final int CAMPAIGN_ID = 10100;
    private static final long BUSINESS_ID = 1010;
    private static final long SUPPLIER_ID = 10;
    private static final int WAREHOUSE_ID = 145;
    private static final int CAMPAIGN_EMPTY_TARIFFS_ID = 10102;

    private static final int DROPSHIP_CAMPAIGN_ID = 10101;
    private static final int DROPSHIP_CAMPAIGN_EMPTY_TARIFFS_ID = 10103;
    /**
     * Идентификатор кампании для ДБС в едином каталоге
     */
    private static final int DBS_UC_CAMPAIGN_ID = 10301;

    private static final int EXPRESS_CAMPAIGN_ID = 10505;

    private static final String SHOP_SKU = "ssku10";
    public static final DateTimeInterval ALL_DATE_TIME_INTERVAL = new DateTimeInterval(
            createOffsetDateTime(LocalDate.of(2018, 11, 1)),
            createOffsetDateTime(LocalDate.MAX)
    );

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private TarifficatorService tarifficatorService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private DbSupplierCategoryFeeDao dbSupplierCategoryFeeDao;

    @Autowired
    private FulfillmentTariffDao fulfillmentTariffDao;

    @Autowired
    private DisposalTariffService disposalTariffService;

    @Autowired
    private XdocSupplyTariffService xdocSupplyTariffService;

    @Autowired
    private StorageMultiplierService storageMultiplierService;

    @Autowired
    private SortingDailyTariffDao sortingDailyTariffDao;

    @Autowired
    private SortingOrdersTariffDao sortingOrdersTariffDao;

    private static final List<FulfillmentTariff> ALL_TARIFFS = List.of(
            createTariff(
                    BillingServiceType.FF_PROCESSING,
                    100L,
                    150L,
                    15000L,
                    1,
                    1L,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.FULFILLMENT,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.FF_PROCESSING,
                    200L,
                    150L,
                    15000L,
                    2,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.SUPPLY_PALLET,
                    OrderType.FULFILLMENT,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.FF_PROCESSING,
                    null,
                    150L,
                    15000L,
                    3,
                    3L,
                    ValueType.RELATIVE,
                    BillingUnit.SUPPLY_PALLET,
                    OrderType.FULFILLMENT,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.FF_PROCESSING,
                    null,
                    150L,
                    15000L,
                    4,
                    4L,
                    ValueType.ABSOLUTE,
                    BillingUnit.SUPPLY_BOX,
                    OrderType.CROSSDOCK,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.FF_STORAGE_BILLING,
                    null,
                    150L,
                    15000L,
                    100, // 100 копеек
                    5L,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.FULFILLMENT,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.FF_WITHDRAW,
                    null,
                    250L,
                    15000L,
                    6,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.SUPPLY_PALLET,
                    OrderType.FULFILLMENT,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.DELIVERY_TO_CUSTOMER,
                    null,
                    150L,
                    15000L,
                    7,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.FULFILLMENT,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.DELIVERY_TO_CUSTOMER,
                    null,
                    150L,
                    15000L,
                    8,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.CROSSDOCK,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.DELIVERY_TO_CUSTOMER,
                    null,
                    150L,
                    15000L,
                    9,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.DROP_SHIP,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.DELIVERY_TO_CUSTOMER_RETURN,
                    null,
                    150L,
                    15000L,
                    10,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.FULFILLMENT,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.DELIVERY_TO_CUSTOMER_RETURN,
                    null,
                    150L,
                    15000L,
                    11,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.CROSSDOCK,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.DELIVERY_TO_CUSTOMER_RETURN,
                    null,
                    150L,
                    15000L,
                    12,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.DROP_SHIP,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.CROSSREGIONAL_DELIVERY,
                    null,
                    150L,
                    15000L,
                    150,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.FULFILLMENT,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.CROSSREGIONAL_DELIVERY,
                    null,
                    250L,
                    15000L,
                    100,
                    null,
                    ValueType.RELATIVE,
                    BillingUnit.ITEM,
                    OrderType.FULFILLMENT,
                    1L,
                    5L
            ),
            createTariff(
                    BillingServiceType.CROSSREGIONAL_DELIVERY,
                    null,
                    150L,
                    15000L,
                    150,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.CROSSDOCK,
                    3L,
                    4L
            ),
            createTariff(
                    BillingServiceType.CROSSREGIONAL_DELIVERY,
                    null,
                    150L,
                    15000L,
                    150,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.DROP_SHIP,
                    1L,
                    2L
            ),
            createTariff(
                    BillingServiceType.CROSSREGIONAL_DELIVERY,
                    null,
                    150L,
                    15000L,
                    100,
                    null,
                    ValueType.RELATIVE,
                    BillingUnit.ITEM,
                    OrderType.DROP_SHIP,
                    3L,
                    2L
            ),
            createTariff(
                    BillingServiceType.CROSSREGIONAL_DELIVERY,
                    null,
                    150L,
                    15000L,
                    500,
                    null,
                    ValueType.RELATIVE,
                    BillingUnit.ITEM,
                    OrderType.DROP_SHIP,
                    5L,
                    2L
            ),
            createTariff(
                    BillingServiceType.EXPRESS_DELIVERED,
                    null,
                    150L,
                    15000L,
                    4,
                    null,
                    ValueType.RELATIVE,
                    BillingUnit.ITEM,
                    OrderType.DROP_SHIP,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.FEE,
                    10L,
                    150L,
                    15000L,
                    4,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.DROP_SHIP_BY_SELLER,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.AGENCY_COMMISSION,
                    100L,
                    150L,
                    15000L,
                    40,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.DROP_SHIP_BY_SELLER,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.CANCELLED_ORDER_FEE,
                    null,
                    null,
                    null,
                    7500,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.FULFILLMENT,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.CANCELLED_ORDER_FEE,
                    null,
                    null,
                    null,
                    7500,
                    null,
                    ValueType.ABSOLUTE,
                    BillingUnit.ITEM,
                    OrderType.DROP_SHIP,
                    null,
                    null
            ),
            createTariff(
                    BillingServiceType.EXPRESS_CANCELLED_BY_PARTNER,
                    null,
                    150L,
                    25000L,
                    500,
                    6000L,
                    35000L,
                    ValueType.RELATIVE,
                    BillingUnit.ITEM,
                    OrderType.DROP_SHIP,
                    null,
                    null
            )
    );

    private static final List<Pair<SupplierCategoryFee, MbiBlueOrderType>> ALL_FEES = List.of(
            createSupplierCategoryFee(
                    10L,
                    50,
                    MbiBlueOrderType.FULFILLMENT
            ),
            createSupplierCategoryFee(
                    11L,
                    150,
                    MbiBlueOrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    505L,
                    150,
                    MbiBlueOrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    3L,
                    11757L,
                    150,
                    MbiBlueOrderType.DROP_SHIP
            )
    );
    private static final List<Pair<SupplierCategoryFee, MbiBlueOrderType>> ALL_CANCELLED_ORDER_FEES = List.of(
            createCancelledOrderFee(
                    null,
                    7500,
                    MbiBlueOrderType.FULFILLMENT
            ),
            createCancelledOrderFee(
                    null,
                    7500,
                    MbiBlueOrderType.DROP_SHIP
            )

    );
    private static final List<SortingDailyTariff> ALL_SORTING_DAILY_TARIFFS = List.of(
    );
    private static final List<SortingOrderTariff> ALL_SORTING_ORDER_TARIFFS = List.of(
            SortingOrderTariff
                    .builder()
                    .setValue(3919L)
                    .setIntakeType(SortingIntakeType.SELF_DELIVERY)
                    .setDateTimeInterval(ALL_DATE_TIME_INTERVAL)
                    .setServiceType(BillingServiceType.SORTING)
                    .build(),
            SortingOrderTariff
                    .builder()
                    .setValue(9421L)
                    .setIntakeType(SortingIntakeType.INTAKE)
                    .setDateTimeInterval(ALL_DATE_TIME_INTERVAL)
                    .setServiceType(BillingServiceType.SORTING)
                    .build()
    );

    private static OffsetDateTime createOffsetDateTime(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.systemDefault()).toOffsetDateTime();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static FulfillmentTariff createTariff(
            BillingServiceType serviceType,
            Long priceTo,
            Long dimensionsTo,
            Long weightTo,
            int value,
            Long minValue,
            ValueType valueType,
            BillingUnit billingUnit,
            OrderType orderType,
            Long areaFrom,
            Long areaTo) {
        return createTariff(
                serviceType,
                priceTo,
                dimensionsTo,
                weightTo,
                value,
                minValue,
                null,
                valueType,
                billingUnit,
                orderType,
                areaFrom,
                areaTo
        );
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static FulfillmentTariff createTariff(
            BillingServiceType serviceType,
            Long priceTo,
            Long dimensionsTo,
            Long weightTo,
            int value,
            Long minValue,
            Long maxValue,
            ValueType valueType,
            BillingUnit billingUnit,
            OrderType orderType,
            Long areaFrom,
            Long areaTo) {
        return new FulfillmentTariff(
                ALL_DATE_TIME_INTERVAL,
                serviceType,
                priceTo,
                dimensionsTo,
                weightTo,
                value,
                minValue,
                maxValue,
                valueType,
                billingUnit,
                orderType,
                null,
                areaFrom,
                areaTo
        );
    }

    private static Instant createInstant(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private static Pair<SupplierCategoryFee, MbiBlueOrderType> createSupplierCategoryFee(
            Long supplierId,
            int value,
            MbiBlueOrderType orderType) {
        return createSupplierCategoryFee(1L, supplierId, value, orderType);
    }

    private static Pair<SupplierCategoryFee, MbiBlueOrderType> createSupplierCategoryFee(
            long categoryId,
            Long supplierId,
            int value,
            MbiBlueOrderType orderType) {
        return new Pair<>(new SupplierCategoryFee(
                categoryId,
                supplierId,
                value,
                new ru.yandex.market.core.date.Period(createInstant(
                        LocalDate.of(2018, 11, 1)),
                        createInstant(LocalDate.MAX)
                ),
                BillingServiceType.FEE
        ), orderType);
    }

    private static Pair<SupplierCategoryFee, MbiBlueOrderType> createCancelledOrderFee(
            Long supplierId,
            int value,
            MbiBlueOrderType orderType) {
        return new Pair<>(new SupplierCategoryFee(
                InternalFeeServiceType.CANCELLED_ORDER_FEE_CATEGORY_VALUE,
                supplierId,
                value,
                new ru.yandex.market.core.date.Period(createInstant(
                        LocalDate.of(2018, 11, 1)),
                        createInstant(LocalDate.MAX)
                ),
                BillingServiceType.CANCELLED_ORDER_FEE
        ), orderType);
    }

    private void mock(
            List<Pair<SupplierCategoryFee, MbiBlueOrderType>> feeList,
            List<Pair<SupplierCategoryFee, MbiBlueOrderType>> cancelledOrderFeeList,
            List<FulfillmentTariff> tariffList,
            List<Pair<SupplierCategoryFee, MbiBlueOrderType>> minFeeList,
            List<SortingDailyTariff> sortingDailyTariffsList,
            List<SortingOrderTariff> sortingOrderTariffList) {
        Mockito.doAnswer(getFees(feeList)).when(dbSupplierCategoryFeeDao).getFee(Mockito.any(), Mockito.any());
        Mockito.doAnswer(getFees(feeList)).when(dbSupplierCategoryFeeDao).getCashOnly(Mockito.any(), Mockito.any());
        Mockito.doAnswer(getFees(cancelledOrderFeeList)).when(dbSupplierCategoryFeeDao)
                .getCancelledOrderFee(Mockito.any(), Mockito.any());
        Mockito.doReturn(tariffList).when(fulfillmentTariffDao).getOrderedTariffs(Mockito.any());
        Mockito.doAnswer(getFees(minFeeList)).when(dbSupplierCategoryFeeDao).getMinFee(Mockito.any(), Mockito.any());
        Mockito.doReturn(sortingDailyTariffsList).when(sortingDailyTariffDao).getAllSortingDailyTariffs(Mockito.any());
        Mockito.doReturn(sortingOrderTariffList).when(sortingOrdersTariffDao).getAllSortingTariffs(Mockito.any());
    }

    private Answer<List<SupplierCategoryFee>> getFees(List<Pair<SupplierCategoryFee, MbiBlueOrderType>> feeList) {
        return invocation -> {
            Instant date = createInstant(invocation.getArgument(0));
            MbiBlueOrderType orderType = invocation.getArgument(1);
            return feeList.stream().filter(fee -> {
                Period period = fee.getFirst().getPeriod();
                Instant from = period.getFrom();
                Instant to = period.getTo();
                return fee.getSecond() == orderType && from.compareTo(date) <= 0 && date.compareTo(to) < 0;
            }).map(fee -> fee.getFirst()).collect(Collectors.toList());
        };
    }

    private void mock() {
        mock(ALL_FEES, ALL_CANCELLED_ORDER_FEES, ALL_TARIFFS, List.of(), ALL_SORTING_DAILY_TARIFFS,
                ALL_SORTING_ORDER_TARIFFS);
    }

    private void mockEmpty() {
        mock(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public static String getResult(ResponseEntity<String> entity) {
        var body = entity.getBody();
        assertNotNull(body);
        return JsonTestUtil.parseJson(body)
                .getAsJsonObject()
                .get("result")
                .toString();
    }

    @BeforeEach
    void setUp() {
        tarifficatorService.invalidateCache();
        when(disposalTariffService.getTariffs(any())).thenReturn(List.of());

        XdocSupplyTariffJsonSchema xDocBoxMeta = new XdocSupplyTariffJsonSchema();
        xDocBoxMeta.setType(CommonJsonSchema.TypeEnum.ABSOLUTE);
        xDocBoxMeta.setBillingUnit(BillingUnitEnum.SUPPLY_BOX);
        xDocBoxMeta.setAmount(BigDecimal.valueOf(15));
        xDocBoxMeta.setCurrency("RUB");
        xDocBoxMeta.setSupplyDirectionFrom(XdocSupplyServiceEnum.MSK);
        xDocBoxMeta.setSupplyDirectionTo(XdocSupplyServiceEnum.SPB);

        XdocSupplyTariffJsonSchema xDocPalleteMeta = new XdocSupplyTariffJsonSchema();
        xDocPalleteMeta.setType(CommonJsonSchema.TypeEnum.ABSOLUTE);
        xDocPalleteMeta.setBillingUnit(BillingUnitEnum.SUPPLY_PALLET);
        xDocPalleteMeta.setAmount(BigDecimal.valueOf(150));
        xDocPalleteMeta.setCurrency("RUB");
        xDocPalleteMeta.setSupplyDirectionFrom(XdocSupplyServiceEnum.MSK);
        xDocPalleteMeta.setSupplyDirectionTo(XdocSupplyServiceEnum.SPB);

        TariffDTO xDocBox = new TariffDTO();
        xDocBox.setMeta(List.of(xDocBoxMeta));

        TariffDTO xDocPallete = new TariffDTO();
        xDocPallete.setMeta(List.of(xDocPalleteMeta));

        when(xdocSupplyTariffService.getTariffs(any())).thenReturn(List.of(xDocBox, xDocPallete));
        when(storageMultiplierService.getStorageMultiplier(nullable(Long.class), nullable(Long.class)))
                .thenReturn(StorageMultiplierService.DEFAULT_MULTIPLIERS);
        when(storageMultiplierService.getStorageMultiplier(nullable(Long.class), nullable(Long.class),
                eq(StorageMultiplierService.MultiplierType.STANDARD)))
                .thenReturn(StorageMultiplierService.DEFAULT_MULTIPLIERS.get(1));
        when(storageMultiplierService.getStorageMultiplier(nullable(Long.class), nullable(Long.class),
                eq(StorageMultiplierService.MultiplierType.EXTRA)))
                .thenReturn(StorageMultiplierService.DEFAULT_MULTIPLIERS.get(2));
        environmentService.setValue(TarifficatorService.ENV_ADDITIONAL_TARIFFS, "true");
    }

    @Test
    @DisplayName("Тест на получение всех нужных тарифов ФФ+кросдок поставщика")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.getAllTariffs.csv",
            "db/TariffCalculatorControllerTest.skuMapping.csv"
    })
    void testGetAllTariffsForFF_Crossdock() {
        mock();
        mockAgencyCommission();
        SyncGetOffer.GetUnitedOffersResponse mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "json/TarifficatorController.datacamp.ssku10NoPrice.json",
                this.getClass()
        );
        doReturn(mockedResponse)
                .when(dataCampShopClient)
                .getBusinessUnitedOffer(anyLong(), anyCollection(), any());

        var responseEntity = FunctionalTestHelper.get(buildUrl(CAMPAIGN_ID, SHOP_SKU));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testGetAllTariffsForFF_Crossdock.json"
        );
    }

    @Test
    @DisplayName("Тест на получение всех нужных тарифов для дропшип+клик&коллект поставщика")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.getAllTariffs.csv",
            "db/TariffCalculatorControllerTest.skuMapping.csv"
    })
    void testGetAllTariffsForDropship_ClickAndCollect() {
        mock();
        mockAgencyCommission();
        mockDataCamp();

        var responseEntity = FunctionalTestHelper.get(buildUrl(DROPSHIP_CAMPAIGN_ID, SHOP_SKU));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testGetAllTariffsForDropship_ClickAndCollect.json"
        );
    }

    @Test
    @DisplayName("Тест на получение всех нужных тарифов для экспресс поставщика")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.getAllTariffs.csv",
            "db/TariffCalculatorControllerTest.skuMapping.csv"
    })
    void testGetAllTariffsForExpress() {
        mock();
        mockAgencyCommission();
        mockDataCamp();

        var responseEntity = FunctionalTestHelper.get(buildUrl(EXPRESS_CAMPAIGN_ID, SHOP_SKU));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testGetAllTariffsForExpress.json"
        );
    }

    @Test
    @DisplayName("Тест на получение тарифов ФФ+кросдок поставщика, причем ВГХ для ssku+supplierId не найдется")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.withoutDimensions.csv",
            "db/TariffCalculatorControllerTest.skuMapping.csv"
    })
    void testSkuNotFoundForFF_Crossdock() {
        mock();
        mockAgencyCommission();
        mockDataCamp();

        var responseEntity = FunctionalTestHelper.get(buildUrl(CAMPAIGN_ID, SHOP_SKU));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testSkuNotFoundForFF_Crossdock.json"
        );
    }

    @Test
    @DisplayName("Тест на получение ошибки, если не существует campaign_id")
    void testInvalidCampaignId() {
        var exception = Assertions.assertThrows(HttpClientErrorException.BadRequest.class, () -> {
            FunctionalTestHelper.get(buildUrl(100500, SHOP_SKU));
        });
        assertThat(exception.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Тест на сортировку тарифов")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.getAllTariffs.csv",
            "db/TariffCalculatorControllerTest.skuMapping.csv"
    })
    void testSortTariffs() {
        mock();
        SyncGetOffer.GetUnitedOffersResponse mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "json/TarifficatorController.datacamp.ssku10NoPrice.json",
                this.getClass()
        );
        doReturn(mockedResponse)
                .when(dataCampShopClient)
                .getBusinessUnitedOffer(anyLong(), anyCollection(), any());

        var responseEntity = FunctionalTestHelper.get(buildUrl(CAMPAIGN_ID, SHOP_SKU));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));

        List<JsonObject> rates = extractFulfillmentRates(responseEntity);

        assertThat(rates, hasSize(15));
        assertThat(rates, contains(
                rateJsonMatcher("fee"),
                rateJsonMatcher("cancelled_order_fee"),
                rateJsonMatcher("agency_commission"),
                rateJsonMatcher("ff_processing", 0, 100),
                rateJsonMatcher("ff_processing", 100, 200),
                rateJsonMatcher("ff_processing", 200, null),
                rateJsonMatcher("ff_storage_billing"),
                rateJsonMatcher("ff_storage_billing_extra"),
                rateJsonMatcher("ff_withdraw"),
                rateJsonMatcher("ff_surplus_supply"),
                rateJsonMatcher("delivery_to_customer"),
                rateJsonMatcher("delivery_to_customer_return"),
                rateJsonMatcher("crossregional_delivery"),
                rateJsonMatcher("ff_xdoc_supply_box"),
                rateJsonMatcher("ff_xdoc_supply_pallet")
        ));
    }

    @Test
    @DisplayName("Тест на получение всех нужных тарифов (учитывая офферное хранилище) ФФ+кросдок поставщика")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.getAllTariffs.csv",
            "db/TariffCalculatorControllerTest.skuMapping.csv"
    })
    void testGetAllTariffsForFFWithPrice() {
        mock();

        mockDataCamp();

        SyncGetOffer.GetOfferResponse mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetOfferResponse.class,
                "json/DataCampResponse.testGetAllTariffsForFFWithPrice.json",
                this.getClass()
        );
        when(dataCampShopClient.getOffer(SUPPLIER_ID, SHOP_SKU, WAREHOUSE_ID, BUSINESS_ID))
                .thenReturn(mockedResponse);

        var responseEntity = FunctionalTestHelper.get(buildUrl(CAMPAIGN_ID, SHOP_SKU, true));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "json/TarifficatorController" +
                ".testGetAllTariffsForFFWithPrice.response.json");
    }

    @Test
    @DisplayName("Тест на получение всех нужных тарифов (учитывая офферное хранилище) ФФ+кросдок поставщика")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.getAllTariffs.csv",
            "db/TariffCalculatorControllerTest.catalog.before.csv"
    })
    void testGetAllTariffsForFFWithPriceFromDatacamp() {
        mock();
        mockAgencyCommission();
        SyncGetOffer.GetUnitedOffersResponse mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "json/TarifficatorController.datacamp.ssku10.json",
                this.getClass()
        );
        doReturn(mockedResponse)
                .when(dataCampShopClient)
                .getBusinessUnitedOffer(anyLong(), anyCollection(), any());

        var responseEntity = FunctionalTestHelper.get(buildUrl(CAMPAIGN_ID, SHOP_SKU, true));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "json/TarifficatorController" +
                ".testGetAllTariffsForFFWithPrice.response.json");
    }

    @Test
    @DisplayName("Тест на получение пустого ответа для фулфилменты+кросдок, если не удалось рассчитать тарифы ( без " +
            "получения ошибки)")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.emptyResponse.csv",
            "db/TariffCalculatorControllerTest.skuMapping.csv"
    })
    void testGetEmptyResponseForFulfillmentAndCrossdock() {
        mockEmpty();
        mockAgencyCommission();
        mockDataCamp();

        var responseEntity = FunctionalTestHelper.get(buildUrl(CAMPAIGN_EMPTY_TARIFFS_ID, SHOP_SKU));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testGetEmptyResponseForFulfillmentAndCrossdock.json"
        );
    }

    @Test
    @DisplayName("Тест на получение пустого ответа для дропшип+c&c, если не удалось рассчитать тарифы ( без получения" +
            " ошибки)")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.emptyResponse.csv",
            "db/TariffCalculatorControllerTest.skuMapping.csv"
    })
    void testGetEmptyResponseForDropshipAndClickCollect() {
        mockEmpty();
        mockDataCamp();

        var responseEntity = FunctionalTestHelper.get(buildUrl(DROPSHIP_CAMPAIGN_EMPTY_TARIFFS_ID, SHOP_SKU));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testGetEmptyResponseForDropshipAndClickCollect.json"
        );
    }

    @Test
    @DisplayName("Тест на получение данных по DBS-партнёру через DataCamp.")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.emptyResponse.csv"
    })
    void testDBSResponseDataCamp() {
        internalDbsTest(DBS_UC_CAMPAIGN_ID);
    }

    @Test
    @DisplayName("Тест на получение данных по DBS-партнёру через батчевую ручку.")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.emptyResponse.csv"
    })
    void testBatchDBSResponse() {
        mock();
        mockAgencyCommission();
        var responseEntity = FunctionalTestHelper.post(buildPostUrl(745L), getPostObject());
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testGetDBS.response.json"
        );
    }

    @Test
    @DisplayName("Тест на получение данных по Fulfillment-партнёру через батчевую ручку.")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.emptyResponse.csv"
    })
    void testBatchFullfilmentResponse() {
        mock();
        mockAgencyCommission();
        var responseEntity = FunctionalTestHelper.post(
                buildPostUrl(745L), getPostObject("fulfillment"));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testPostFulfillment.response.json"
        );
    }

    @Test
    @DisplayName("Тест на получение ошибки, если не существует partnerType")
    void testBatchInvalidPartnerType() {
        var exception = Assertions.assertThrows(HttpClientErrorException.BadRequest.class, () -> {
            FunctionalTestHelper.post(
                    buildPostUrl(745L), getPostObject("google"));
        });
        assertThat(exception.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Тест на получение ошибки, если передан неподдерживаемый partnerType")
    void testBatchUnsupportedPartnerType() {
        var exception = Assertions.assertThrows(HttpClientErrorException.BadRequest.class, () -> {
            FunctionalTestHelper.post(
                    buildPostUrl(745L), getPostObject("crossdock"));
        });
        assertThat(exception.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Тест на получение ошибки, если переданы несколько одинаковых значений partnerType")
    void testBatchDuplicatePartnerTypes() {
        var exception = Assertions.assertThrows(HttpClientErrorException.BadRequest.class, () -> {
            FunctionalTestHelper.post(
                    buildPostUrl(745L), getPostObject("fulfillment", "fulfillment"));
        });
        assertThat(exception.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Тест на получение результата по нескольким моделям размещения")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.emptyResponse.csv"
    })
    void testBatchSeveralPartnerTypes() {
        mock();
        mockAgencyCommission();
        var responseEntity = FunctionalTestHelper.post(
                buildPostUrl(745L), getPostObject("fulfillment", "express"));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testPostSeveralPartnerTypes.response.json"
        );
    }

    @Test
    @DisplayName("Тест на время выполнения операции")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.emptyResponse.csv"
    })
    void testTimeFullQuery() {
        mock();
        mockAgencyCommission();
        OfferCartListDTO fullPostObject = getFullPostObject();
        long start = System.currentTimeMillis();
        var responseEntity = FunctionalTestHelper.post(buildPostUrl(745L),
                fullPostObject
        );
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertTrue(System.currentTimeMillis() - start <= 5_000L, "Too long execution time.");
    }

    @Test
    @DisplayName("Тест на вызов ручки для калькулятора")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.emptyResponse.csv"
    })
    void testCalcHandleInvocation() {
        mock();
        mockAgencyCommission();
        var responseEntity = FunctionalTestHelper.post(
                buildPostCalcUrl(), getPostObject("fulfillment", "dropship"));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testCalculatorInvocation.response.json"
        );
    }

    @Test
    @DisplayName("Тест на вызов ручки для калькулятора с новыми тарифами по агентскому вознаграждению")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.emptyResponse.csv"
    })
    void testCalcNewAgencyCommissionsHandleInvocation() {
        mock();
        mockAgencyCommission();
        environmentService.setValue(
                "ru.yandex.market.core.fulfillment.calculator.AgencyCommissionRatio.daily", "220");
        environmentService.setValue(
                "ru.yandex.market.core.fulfillment.calculator.AgencyCommissionRatio.weekly", "180");
        environmentService.setValue(
                "ru.yandex.market.core.fulfillment.calculator.AgencyCommissionRatio.biweekly", "130");
        environmentService.setValue(
                "ru.yandex.market.core.fulfillment.calculator.AgencyCommissionRatio.monthly", "100");
        var responseEntity = FunctionalTestHelper.post(
                buildPostCalcUrl(), getPostObject("fulfillment", "dropship"));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testNewAgencyCommissionsCalculatorInvocation.response.json"
        );
    }

    @Test
    @DisplayName("Тест на партнёра FBS с платой за сортировку")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.fbs.before.csv"
    })
    void testGetFBSTariff() {
        mock();
        mockAgencyCommission();
        var responseEntity = FunctionalTestHelper.post(buildPostUrl(11757L),
                getPostObject("dropship"));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testGetFBS.response.json"
        );
    }

    @Test
    @DisplayName("Тест на выкидывание ошибки в случае получения партнёра с неизвестным типом размещения")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.emptyResponse.csv"
    })
    void testExceptionOnUnknownPartnerShopSkusResponse() {
        var exception = Assertions.assertThrows(HttpClientErrorException.BadRequest.class, () -> {
            FunctionalTestHelper.post(buildPostUrl(39219391L), getPostObject());
        });
        assertThat(exception.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Тест на партнёра FBS с получением тарифа returned_orders_storage из YT.")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.fbs.before.csv"
    })
    void testGetFBSYtTariff() {
        mock();
        mockAgencyCommission();
        environmentService.setValue(ReturnedOrdersStorageTariffService.USE_EXTERNAL_TARIFFS_EVN_KEY, "true");
        environmentService.setValue(YtTariffsService.USE_YT_TABLE_ENV_KEY, "true");

        var responseEntity = FunctionalTestHelper.post(buildPostUrl(11757L),
                getPostObject("dropship"));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testGetFBSYtContent.response.json"
        );
    }

    @Test
    @DisplayName("Тест на партнёра FBS с неизвестной категорией товара")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.fbs.unkonwnCategory.before.csv"
    })
    void testGetFBSUnknownCategoryTariff() {
        mock();
        mockAgencyCommission();
        var responseEntity = FunctionalTestHelper.post(buildPostUrl(11757L),
                getPostObject((Long) null, "dropship"));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testGetFBS.unknownCategory.response.json"
        );
    }

    @Test
    @DisplayName("Тест на партнёра FBS с неизвестной категорией товара, переданной с UI")
    @DbUnitDataSet(before = {
            "db/TariffCalculatorControllerTest.fbs.unkonwnCategory.before.csv"
    })
    void testGetFBSUnknownUICategoryTariff() {
        mock();
        mockAgencyCommission();
        var responseEntity = FunctionalTestHelper.post(buildPostUrl(11757L),
                getPostObject(0L, "dropship"));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testGetFBS.unknownCategory.response.json"
        );
    }


    private OfferCartListDTO getFullPostObject() {
        List<String> types = List.of(
                "fulfillment",
                "dropship",
                "dropshipBySeller",
                "express");
        ArrayList<OfferCartDTO> list = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            OfferCartDTO offer = new OfferCartDTO();
            offer.setShopSku("sku" + i);
            offer.setHeight(3 + i);
            offer.setWidth(5 + i);
            offer.setLength(7 + i);
            offer.setPrice(BigDecimal.valueOf(100 + i));
            offer.setWeight(BigDecimal.valueOf(((double) 10) / i));
            offer.setCategoryId((long) i);
            list.add(offer);
        }
        return new OfferCartListDTO(types, list);
    }

    private OfferCartListDTO getPostObject(String... partnerTypes) {
        return getPostObject(1L, partnerTypes);
    }

    private OfferCartListDTO getPostObject(Long categoryId, String... partnerTypes) {
        ArrayList<OfferCartDTO> list = new ArrayList<>();
        OfferCartDTO offer = new OfferCartDTO();
        offer.setShopSku(SHOP_SKU);
        offer.setCategoryId(categoryId);
        list.add(offer);
        return new OfferCartListDTO(Arrays.asList(partnerTypes), list);
    }

    private void internalDbsTest(int dbsCampaignId) {
        mock();
        mockAgencyCommission();
        mockDataCamp();
        SyncGetOffer.GetUnitedOffersResponse mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "json/TarifficatorController.datacamp.ssku10.json",
                this.getClass()
        );
        doReturn(mockedResponse)
                .when(dataCampShopClient)
                .getBusinessUnitedOffer(anyLong(), anyCollection(), any());
        var responseEntity = FunctionalTestHelper.get(buildUrl(dbsCampaignId, SHOP_SKU));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonTestUtil.assertEquals(
                responseEntity,
                this.getClass(),
                "json/TarifficatorController.testGetDBS.response.json"
        );
    }

    private List<JsonObject> extractFulfillmentRates(ResponseEntity<String> responseEntity) {
        String body = Objects.requireNonNull(responseEntity.getBody());
        JsonElement responseObject = parseJson(body)
                .getAsJsonObject()
                .get("result");

        JsonObject fulfillmentObject = responseObject.getAsJsonArray().get(0)
                .getAsJsonObject();
        assertThat(fulfillmentObject.get("supplierType").getAsString(), is("fulfillment"));

        JsonArray fulfillmentRates = fulfillmentObject.get("rates").getAsJsonArray();

        return StreamSupport.stream(fulfillmentRates.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .collect(Collectors.toList());
    }

    private Matcher<JsonObject> rateJsonMatcher(String serviceName) {
        return rateJsonMatcher(serviceName, 0, null);
    }

    private Matcher<JsonObject> rateJsonMatcher(
            String serviceName,
            int priceFrom,
            @Nullable Integer priceTo
    ) {
        MbiMatchers.AllOfBuilder<JsonObject> builder = MbiMatchers.<JsonObject>newAllOfBuilder()
                .add(rate -> rate.get("service").getAsString(), is(serviceName), "serviceName")
                .add(rate -> rate.get("priceFrom").getAsInt(), is(priceFrom), "priceFrom");

        if (priceTo == null) {
            builder.add(rate -> rate.get("priceTo"), nullValue(), "priceTo");
        } else {
            builder.add(rate -> rate.get("priceTo").getAsInt(), is(priceTo), "priceTo");
        }
        return builder.build();
    }

    private String buildUrl(long campaignId, String shopSku) {
        return String.format("%s/tariffs/%d/shop_sku?shop_sku=%s", baseUrl, campaignId, shopSku);
    }

    private String buildUrl(long campaignId, String shopSku, boolean usePrice) {
        return String.format("%s/tariffs/%d/shop_sku?shop_sku=%s&use_price=%s", baseUrl, campaignId, shopSku, usePrice);
    }

    private String buildPostUrl(long partnerId) {
        return String.format("%s/tariffs/shop_skus?partner_id=%d", baseUrl, partnerId);
    }

    private String buildPostCalcUrl() {
        return String.format("%s/tariffs/calculator", baseUrl);
    }

    private void mockDataCamp() {
        SyncGetOffer.GetUnitedOffersResponse mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "json/TarifficatorController.datacamp.ssku10.json",
                this.getClass()
        );
        doReturn(mockedResponse)
                .when(dataCampShopClient)
                .getBusinessUnitedOffer(anyLong(), anyCollection(), any());
    }

    private void mockAgencyCommission() {
        environmentService.setValue("suppliers.agencyCommission", "200");
    }
}
