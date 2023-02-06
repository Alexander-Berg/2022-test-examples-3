package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deepmind.client.ApiException;
import ru.yandex.market.deepmind.client.api.AvailabilitiesApi;
import ru.yandex.market.deepmind.client.model.AvailabilitiesByIntervalResponse;
import ru.yandex.market.deepmind.client.model.AvailabilityByIntervalInfo;
import ru.yandex.market.deepmind.client.model.BlockInfo;
import ru.yandex.market.deepmind.client.model.Message;
import ru.yandex.market.deepmind.client.model.ShopSkuKey;
import ru.yandex.market.mboc.http.DeliveryParams;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.SpecialOrderDateType;
import ru.yandex.market.replenishment.autoorder.model.dto.InterWarehouseRecommendationsFilterDTO;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.SpecialOrder;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.SpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Supplier;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Warehouse;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;
import ru.yandex.market.replenishment.autoorder.utils.Errors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentConstants.DISABLE_INTERWAREHOUSE_DEEPMIND_VALIDATION;

@MockBean(classes = {EnvironmentService.class})
public class WarehouseAvailabilityValidationServiceTest extends FunctionalTest {

    @Autowired
    private WarehouseAvailabilityValidationService service;

    @Autowired
    private EnvironmentService environmentService;

    @Before
    public void setMockService() throws ApiException {
        DeliveryParams mockParams = Mockito.mock(DeliveryParams.class);

        var message1 = MbocCommon.Message.newBuilder(MbocCommon.Message.getDefaultInstance()).build();
        ReflectionTestUtils.setField(message1, "messageCode_", "deepminderror_1");

        var message2 = MbocCommon.Message.newBuilder(MbocCommon.Message.getDefaultInstance()).build();
        ReflectionTestUtils.setField(message2, "messageCode_", "deepminderror_2");

        var message3 = MbocCommon.Message.newBuilder(MbocCommon.Message.getDefaultInstance()).build();
        ReflectionTestUtils.setField(message3, "messageCode_", "deepminderror_3");

        Mockito.when(mockParams.searchFulfilmentSskuParams(any())).thenReturn(
            MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
                .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .addWarehouseAvailabilities(MboMappingsForDelivery.WarehouseAvailability.newBuilder()
                        .setAllowCargoType(true)
                        .setAllowInbound(true)
                        .setWarehouseId(145)
                        .build()
                    )
                    .setShopSku("12345")
                    .setSupplierId(111)
                    .build()
                )
                .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .addWarehouseAvailabilities(MboMappingsForDelivery.WarehouseAvailability.newBuilder()
                        .setAllowCargoType(false)
                        .setAllowInbound(true)
                        .setAllowCargoTypeComment(message1)
                        .setWarehouseId(145)
                        .build()
                    )
                    .setShopSku("54321")
                    .setSupplierId(111)
                    .build()
                )
                .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .addWarehouseAvailabilities(MboMappingsForDelivery.WarehouseAvailability.newBuilder()
                        .setAllowCargoType(false)
                        .setAllowInbound(false)
                        .setAllowCargoTypeComment(message2)
                        .setAllowInboundComment(message3)
                        .setWarehouseId(145)
                        .build()
                    )
                    .setShopSku("65432")
                    .setSupplierId(424242)
                    .build()
                )
                .build()
        );

        ReflectionTestUtils.setField(service, "deliveryParams", mockParams);

        Message blockMessage = new Message()
            .code("mboc.msku.error.supply-forbidden.end-of-life-msku")
            .text("Удалите товар из списка — склады маркетплейса больше не будут его принимать");
        Message blockMessageSkipInSlow = new Message()
            .code("mboc.msku.error.supply-forbidden.inactive-tmp-offer")
            .text("Товар временно заблокирован: его нет в наличии, нет закупочной цены или он ещё не одобрен для " +
                "продажи");
        BlockInfo blockInfo = new BlockInfo().message(blockMessage);
        BlockInfo blockInfoSkipInSlow = new BlockInfo().message(blockMessageSkipInSlow);

        AvailabilitiesApi availabilitiesApi = Mockito.mock(AvailabilitiesApi.class);
        AvailabilitiesByIntervalResponse availabilities1 = new AvailabilitiesByIntervalResponse();
        availabilities1.setKey(new ShopSkuKey().shopSku("111.111").supplierId(10264169));

        AvailabilityByIntervalInfo availabilityByIntervalInfo1 = new AvailabilityByIntervalInfo();
        availabilityByIntervalInfo1.setWarehouseId(147L);
        availabilityByIntervalInfo1.setAllowInbound(false);
        availabilityByIntervalInfo1.setDateFrom(LocalDate.parse("2022-02-01"));
        availabilityByIntervalInfo1.setDateTo(LocalDate.parse("2022-03-09"));
        availabilityByIntervalInfo1.infos(List.of(blockInfo));

        AvailabilityByIntervalInfo availabilityByIntervalInfo2 = new AvailabilityByIntervalInfo();
        availabilityByIntervalInfo2.setWarehouseId(147L);
        availabilityByIntervalInfo2.setAllowInbound(false);
        availabilityByIntervalInfo2.setDateFrom(LocalDate.parse("2022-03-25"));
        availabilityByIntervalInfo2.setDateTo(LocalDate.parse("2022-03-30"));
        availabilityByIntervalInfo2.infos(List.of(blockInfoSkipInSlow));

        availabilities1.setAvailabilities(List.of(availabilityByIntervalInfo1, availabilityByIntervalInfo2));

        AvailabilitiesByIntervalResponse availabilities2 = new AvailabilitiesByIntervalResponse();
        availabilities2.setKey(new ShopSkuKey().shopSku("222.222").supplierId(10264169));

        AvailabilityByIntervalInfo availabilityByIntervalInfo3 = new AvailabilityByIntervalInfo();
        availabilityByIntervalInfo3.setWarehouseId(145L);
        availabilityByIntervalInfo3.setAllowInbound(false);
        availabilityByIntervalInfo3.setDateFrom(LocalDate.parse("2022-04-18"));
        availabilityByIntervalInfo3.setDateTo(LocalDate.parse("2022-04-18"));
        availabilityByIntervalInfo3.setInfos(List.of(blockInfo));

        AvailabilityByIntervalInfo availabilityByIntervalInfo4 = new AvailabilityByIntervalInfo();
        availabilityByIntervalInfo4.setWarehouseId(145L);
        availabilityByIntervalInfo4.setAllowInbound(false);
        availabilityByIntervalInfo4.setDateFrom(LocalDate.parse("2022-04-20"));
        availabilityByIntervalInfo4.setDateTo(LocalDate.parse("2022-04-25"));
        availabilityByIntervalInfo4.setInfos(List.of(blockInfo));

        availabilities2.setAvailabilities(List.of(availabilityByIntervalInfo3, availabilityByIntervalInfo4));

        AvailabilitiesByIntervalResponse availabilities3 = new AvailabilitiesByIntervalResponse();
        availabilities3.setKey(new ShopSkuKey().shopSku("333.333").supplierId(10264169));

        AvailabilityByIntervalInfo availabilityByIntervalInfo5 = new AvailabilityByIntervalInfo();
        availabilityByIntervalInfo5.setWarehouseId(300L);
        availabilityByIntervalInfo5.setAllowInbound(false);
        availabilityByIntervalInfo5.setDateFrom(LocalDate.parse("2022-04-08"));
        availabilityByIntervalInfo5.setDateTo(LocalDate.parse("2022-04-09"));
        availabilityByIntervalInfo5.infos(List.of(blockInfo));

        AvailabilityByIntervalInfo availabilityByIntervalInfo6 = new AvailabilityByIntervalInfo();
        availabilityByIntervalInfo6.setWarehouseId(300L);
        availabilityByIntervalInfo6.setAllowInbound(false);
        availabilityByIntervalInfo6.setDateFrom(LocalDate.parse("2022-05-08"));
        availabilityByIntervalInfo6.setDateTo(LocalDate.parse("2022-05-10"));
        availabilityByIntervalInfo6.infos(List.of(blockInfoSkipInSlow));

        availabilities3.setAvailabilities(List.of(availabilityByIntervalInfo5, availabilityByIntervalInfo6));


        Mockito.doReturn(List.of(availabilities1, availabilities2, availabilities3))
            .when(availabilitiesApi)
            .getBySskuOnInterval(any());
        ReflectionTestUtils.setField(service, "availabilitiesApi", availabilitiesApi);
    }

    @Test
    public void testNoWarehouseValidationError() {
        SpecialOrder order = new SpecialOrder();

        Warehouse warehouse = new Warehouse(145L, "the name", null, null, null);

        Supplier supplier = new Supplier();
        supplier.setId(111);

        order.setSsku("000111.12345");
        order.setWarehouse(warehouse);
        order.setSupplier(supplier);

        assertEquals(0, service.validateWarehouseAvailability(Collections.singletonList(order)).size());
    }

    @Test
    public void testWarehouseValidationError() {
        SpecialOrder order = new SpecialOrder();

        Supplier supplier = new Supplier();
        supplier.setId(111);

        order.setSsku("000111.54321");
        order.setWarehouse(new Warehouse(145L, null, null, null, null));
        order.setSupplier(supplier);

        var result = service.validateWarehouseAvailability(Collections.singletonList(order));

        assertEquals(1, result.size());
        assertEquals(
            "Согласно матрице блокировок следующие SSKU нельзя возить на выбранный склад: 000111.54321",
            result.toArray()[0]
        );
    }

    @Test
    public void testValidateWarehouseAvailabilityInFastPipeWithBlock() throws ApiException {
        Errors<SpecialOrder> errors =
            service.validateStrongWarehouseAvailability(getOrdersWithBlocks(), timeService.getNowDate());
        var result = errors.onlyStrings();

        assertEquals(3, errors.size());
        Assertions.assertThat(result)
            .containsExactlyInAnyOrder(
                "Согласно матрице блокировок SSKU 111.111 нельзя возить на выбранный склад: 147 в даты с 2022-03-25 " +
                    "по 2022-03-30, причина:" +
                    " Товар временно заблокирован: его нет в наличии, нет закупочной цены или он ещё не одобрен для " +
                    "продажи",
                "Согласно матрице блокировок SSKU 333.333 нельзя возить на выбранный склад: 300 в даты с 2022-05-08 " +
                    "по 2022-05-10, причина:" +
                    " Товар временно заблокирован: его нет в наличии, нет закупочной цены или он ещё не одобрен для " +
                    "продажи",
                "Согласно матрице блокировок SSKU 111.111 нельзя возить на выбранный склад: 147 в даты с 2022-02-01 " +
                    "по 2022-03-09, причина:" +
                    " Удалите товар из списка — склады маркетплейса больше не будут его принимать"
            );
    }

    @Test
    public void testValidateWarehouseAvailabilityInFastPipeWithoutBlock() throws ApiException {
        Errors<SpecialOrder> errors =
            service.validateStrongWarehouseAvailability(getOrdersWithoutBlocks(), timeService.getNowDate());

        assertEquals(0, errors.size());
    }

    @Test
    public void testValidateWarehouseAvailabilityInSlowPipeWithBlock() throws ApiException {
        Errors<SpecialOrder> errors = service.validateForAssortmentCommitteeWarehouseAvailability(
            getOrdersWithBlocks(), timeService.getNowDate());
        var result = errors.onlyStrings();

        assertEquals(1, result.size());
        Assertions.assertThat(result)
            .containsExactlyInAnyOrder(
                "Согласно матрице блокировок SSKU 111.111 нельзя возить на выбранный склад: 147 в даты с 2022-02-01 " +
                    "по 2022-03-09," +
                    " причина: Удалите товар из списка — склады маркетплейса больше не будут его принимать"
            );
    }

    @Test
    public void testValidateWarehouseAvailabilityInSlowPipeWithoutBlock() throws ApiException {
        Errors<SpecialOrder> errors = service.validateForAssortmentCommitteeWarehouseAvailability(
            getOrdersWithoutBlocks(), timeService.getNowDate());
        assertEquals(0, errors.size());
    }

    @Test
    @DbUnitDataSet(before = "WarehouseAvailabilityValidationServiceTest_deepmindErrors.before.csv",
        after = "WarehouseAvailabilityValidationServiceTest_deepmindErrors.after.csv")
    public void testValidateInterWarehouseMovementAvailability() {
        Mockito.when(environmentService.getBooleanWithDefault(
            DISABLE_INTERWAREHOUSE_DEEPMIND_VALIDATION, false)).thenReturn(true);
        InterWarehouseRecommendationsFilterDTO filter = new InterWarehouseRecommendationsFilterDTO(
            "2020-12-01",
            "2020-12-20",
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            null,
            null
        );
        service.validateInterWarehouseMovementAvailability(filter);
    }

    private List<SpecialOrder> getOrdersWithBlocks() {
        SpecialOrderItem specialOrderItem11 = new SpecialOrderItem();
        specialOrderItem11.setWeekStartDate(LocalDate.parse("2022-03-01"));
        specialOrderItem11.setOrderDateType(SpecialOrderDateType.LOG_PARAM);

        SpecialOrderItem specialOrderItem12 = new SpecialOrderItem();
        specialOrderItem12.setWeekStartDate(LocalDate.parse("2022-03-20"));

        SpecialOrderItem specialOrderItem21 = new SpecialOrderItem();
        specialOrderItem21.setWeekStartDate(LocalDate.parse("2022-04-01"));
        specialOrderItem21.setOrderDateType(SpecialOrderDateType.LOG_PARAM);

        SpecialOrderItem specialOrderItem22 = new SpecialOrderItem();
        specialOrderItem22.setWeekStartDate(LocalDate.parse("2022-04-10"));

        SpecialOrderItem specialOrderItem31 = new SpecialOrderItem();
        specialOrderItem31.setWeekStartDate(LocalDate.parse("2022-05-01"));
        specialOrderItem31.setOrderDateType(SpecialOrderDateType.LOG_PARAM);

        SpecialOrderItem specialOrderItem32 = new SpecialOrderItem();
        specialOrderItem32.setWeekStartDate(LocalDate.parse("2022-05-10"));

        List<SpecialOrderItem> specialOrderItemList1 = new ArrayList<>();
        specialOrderItemList1.add(specialOrderItem11);
        specialOrderItemList1.add(specialOrderItem12);

        List<SpecialOrderItem> specialOrderItemList2 = new ArrayList<>();
        specialOrderItemList2.add(specialOrderItem21);
        specialOrderItemList2.add(specialOrderItem22);

        List<SpecialOrderItem> specialOrderItemList3 = new ArrayList<>();
        specialOrderItemList3.add(specialOrderItem31);
        specialOrderItemList3.add(specialOrderItem32);

        Supplier supplier = new Supplier();
        supplier.setId(10264169);

        SpecialOrder order1 = new SpecialOrder();
        order1.setItems(specialOrderItemList1);
        order1.setSsku("111.111");
        order1.setWarehouse(new Warehouse(147L, null, null, null, null));
        order1.setSupplier(supplier);

        SpecialOrder order2 = new SpecialOrder();
        order2.setItems(specialOrderItemList2);
        order2.setSsku("222.222");
        order2.setWarehouse(new Warehouse(145L, null, null, null, null));
        order2.setSupplier(supplier);

        SpecialOrder order3 = new SpecialOrder();
        order3.setItems(specialOrderItemList3);
        order3.setSsku("333.333");
        order3.setWarehouse(new Warehouse(300L, null, null, null, null));
        order3.setSupplier(supplier);

        return List.of(order1, order2, order3);
    }

    private List<SpecialOrder> getOrdersWithoutBlocks() {
        SpecialOrderItem specialOrderItem11 = new SpecialOrderItem();
        specialOrderItem11.setWeekStartDate(LocalDate.parse("2025-03-01"));
        specialOrderItem11.setOrderDateType(SpecialOrderDateType.LOG_PARAM);

        SpecialOrderItem specialOrderItem12 = new SpecialOrderItem();
        specialOrderItem12.setWeekStartDate(LocalDate.parse("2025-03-20"));

        SpecialOrderItem specialOrderItem21 = new SpecialOrderItem();
        specialOrderItem21.setWeekStartDate(LocalDate.parse("2022-04-01"));
        specialOrderItem21.setOrderDateType(SpecialOrderDateType.LOG_PARAM);

        SpecialOrderItem specialOrderItem22 = new SpecialOrderItem();
        specialOrderItem22.setWeekStartDate(LocalDate.parse("2022-04-10"));

        SpecialOrderItem specialOrderItem31 = new SpecialOrderItem();
        specialOrderItem31.setWeekStartDate(LocalDate.parse("2025-05-01"));
        specialOrderItem31.setOrderDateType(SpecialOrderDateType.LOG_PARAM);

        SpecialOrderItem specialOrderItem32 = new SpecialOrderItem();
        specialOrderItem32.setWeekStartDate(LocalDate.parse("2025-05-10"));

        List<SpecialOrderItem> specialOrderItemList1 = new ArrayList<>();
        specialOrderItemList1.add(specialOrderItem11);
        specialOrderItemList1.add(specialOrderItem12);

        List<SpecialOrderItem> specialOrderItemList2 = new ArrayList<>();
        specialOrderItemList2.add(specialOrderItem21);
        specialOrderItemList2.add(specialOrderItem22);

        List<SpecialOrderItem> specialOrderItemList3 = new ArrayList<>();
        specialOrderItemList3.add(specialOrderItem31);
        specialOrderItemList3.add(specialOrderItem32);

        Supplier supplier = new Supplier();
        supplier.setId(10264169);

        SpecialOrder order1 = new SpecialOrder();
        order1.setItems(specialOrderItemList1);
        order1.setSsku("111.111");
        order1.setWarehouse(new Warehouse(147L, null, null, null, null));
        order1.setSupplier(supplier);

        SpecialOrder order2 = new SpecialOrder();
        order2.setItems(specialOrderItemList2);
        order2.setSsku("222.222");
        order2.setWarehouse(new Warehouse(301L, null, null, null, null));
        order2.setSupplier(supplier);

        SpecialOrder order3 = new SpecialOrder();
        order3.setItems(specialOrderItemList3);
        order3.setSsku("444.444");
        order3.setWarehouse(new Warehouse(300L, null, null, null, null));
        order3.setSupplier(supplier);

        return List.of(order1, order2, order3);
    }
}
