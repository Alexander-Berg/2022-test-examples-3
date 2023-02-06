package ru.yandex.market.deliverycalculator.indexer.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.indexer.task.UpdateShopTariffsCacheTask;
import ru.yandex.market.deliverycalculator.indexerclient.DeliveryCalculatorIndexerClient;
import ru.yandex.market.deliverycalculator.indexerclient.HttpDeliveryCalculatorIndexerClient;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.ShopDeliveryCostRequest;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.ShopDeliveryCostResponse;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffProgramType;
import ru.yandex.market.deliverycalculator.storage.StorageTestUtils;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.tariff.CargoTypeRestrictionsDto;
import ru.yandex.market.logistics.management.entity.response.tariff.TariffLocationCargoTypeDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.deliverycalculator.indexerclient.modelv2.DeliveryType.PICKUP;

/**
 * Тесты для {@link RuleFilterController}
 */
@DbUnitDataSet(before = "data/db/ruleFilterController.before.csv")
class RuleFilterControllerClientTest extends FunctionalTest {
    private DeliveryCalculatorIndexerClient client;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private TariffInfoProvider tariffInfoProvider;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void init() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "tariff_1948.xml", getClass());
        client = new HttpDeliveryCalculatorIndexerClient(baseUrl);
        runTasks(UpdateShopTariffsCacheTask.class);
    }

    private void mockLms() {
        CargoTypeRestrictionsDto firsRestriction = new CargoTypeRestrictionsDto(
                1948L,
                Arrays.asList(17, 18, 19),
                Arrays.asList(
                        new TariffLocationCargoTypeDto(11, Arrays.asList(11, 12, 13)),
                        new TariffLocationCargoTypeDto(12, Arrays.asList(14, 15, 16)))
        );
        Mockito.when(lmsClient.getCargoTypesByTariffId(eq(1948L))).thenReturn(firsRestriction);
    }

    private static Stream<Arguments> baseTestArguments() {
        return Stream.of(
                Arguments.of(
                        "Физический вес удовлетворяет и тарифному ограничению, и локационному",
                        createShopDeliveryCostRequest(3, 20, 30, 15, 39, null, 50, 11008),
                        BigDecimal.valueOf(19400),
                        BigDecimal.valueOf(135800, 1),
                        BigDecimal.valueOf(5000, 3),
                        3
                ),
                Arguments.of(
                        "Физический вес удовлетворяет тарифному ограничению, а расчетный - нет. но удовлетворяет локационному",
                        createShopDeliveryCostRequest(30, 59, 59, 59, 39, null, 50, 11008),
                        BigDecimal.valueOf(20800),
                        BigDecimal.valueOf(145600, 1),
                        BigDecimal.valueOf(5000, 3),
                        41.0758
                ),
                Arguments.of(
                        "Физический вес удовлетворяет тарифному ограничению, но правило выбирается по объемному весу, т к он убольше",
                        createShopDeliveryCostRequest(10, 39, 55, 61, 39, null, 50, 11008),
                        BigDecimal.valueOf(20800),
                        BigDecimal.valueOf(145600, 1),
                        BigDecimal.valueOf(5000, 3),
                        26.169
                ),
                Arguments.of(
                        "Работает разворачивание шагов",
                        createShopDeliveryCostRequest(47, 11, 11, 11, 39, null, 50, 11008),
                        BigDecimal.valueOf(60000),
                        BigDecimal.valueOf(420000, 1),
                        BigDecimal.valueOf(5000, 3),
                        47
                ),
                Arguments.of(
                        "Работает поднятие вверх по дереву регионов",
                        createShopDeliveryCostRequest(47, 11, 11, 11, 39, null, 50, 11011),
                        BigDecimal.valueOf(60000),
                        BigDecimal.valueOf(420000, 1),
                        BigDecimal.valueOf(5000, 3),
                        47
                ),
                Arguments.of(
                        "Работает поднятие вверх по дереву регионов, один из регионов имеет большую гранулярность",
                        createShopDeliveryCostRequest(47, 11, 11, 11, 39, null, 50, 11013),
                        BigDecimal.valueOf(60000),
                        BigDecimal.valueOf(420000, 1),
                        BigDecimal.valueOf(5000, 3),
                        47
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("baseTestArguments")
    @DisplayName("Базовые тесты на ручку getShopDeliveryCost")
    void baseTest(
            String description,
            ShopDeliveryCostRequest shopDeliveryCostRequest,
            BigDecimal expectedDeliveryCost,
            BigDecimal expectedReturnCost,
            BigDecimal expectedInsuranceCost,
            double expectedCalculatedWeight
    ) {
        mockLms();
        ShopDeliveryCostResponse response = client.getShopDeliveryCost(shopDeliveryCostRequest);

        Assertions.assertEquals(expectedDeliveryCost, response.getDeliveryCost());
        Assertions.assertEquals(expectedReturnCost, response.getReturnCost());
        Assertions.assertEquals(expectedInsuranceCost, response.getInsuranceCost());
        Assertions.assertEquals(expectedCalculatedWeight, response.getCalculatedWeight());

        verify(mdsS3Client, atMost(1)).download(any(), any());
    }

    @Test
    @DisplayName("Физический вес не удовлетворяет тарифному ограничению")
    void physicalWeightDoesNotMeetTariffLimit() {
        ShopDeliveryCostResponse response = client.getShopDeliveryCost(createShopDeliveryCostRequest(50, 10, 10, 10, 39, null, 50, 11008));

        Assertions.assertNull(response.getDeliveryCost());
        Assertions.assertEquals(0, response.getCalculatedWeight());
        Assertions.assertNull(response.getInsuranceCost());

        verify(mdsS3Client, atMost(1)).download(any(), any());
    }

    @Test
    @DisplayName("BERU_CROSSDOCK тарифы")
    void beruCrossdock() {
        ShopDeliveryCostResponse response = client.getShopDeliveryCost(createShopDeliveryCostRequest(1, 1, 1, 1, 39, null, 51, 11008));

        Assertions.assertNull(response.getDeliveryCost());
        Assertions.assertEquals(0, response.getCalculatedWeight());
        Assertions.assertNull(response.getInsuranceCost());

        verify(mdsS3Client, atMost(1)).download(any(), any());
    }

    private static Stream<Arguments> warehouseIdTestArguments() {
        return Stream.of(
                Arguments.of(
                        createShopDeliveryCostRequest(3, 20, 30, 15, 0, 0L, 50, 11008),
                        BigDecimal.valueOf(30000),
                        BigDecimal.valueOf(150000, 1)
                ),
                Arguments.of(
                        createShopDeliveryCostRequest(3, 20, 30, 15, 0, 145L, 50, 11008),
                        BigDecimal.valueOf(30000),
                        BigDecimal.valueOf(150000, 1)
                ),
                Arguments.of(
                        createShopDeliveryCostRequest(3, 20, 30, 15, 0, 147L, 50, 11008),
                        BigDecimal.valueOf(19400),
                        BigDecimal.valueOf(135800, 1)
                ),
                Arguments.of(
                        createShopDeliveryCostRequest(3, 20, 30, 15, 0, 171L, 50, 11008),
                        BigDecimal.valueOf(30000),
                        BigDecimal.valueOf(150000, 1)
                ),
                Arguments.of(
                        createShopDeliveryCostRequest(3, 20, 30, 15, 0, 172L, 50, 11008),
                        BigDecimal.valueOf(30000),
                        BigDecimal.valueOf(150000, 1)
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("warehouseIdTestArguments")
    @DisplayName("Если есть warehouse-id, то берём регион отправления из локации склада и игнорируем region-from")
    void warehouseIdTest(
            ShopDeliveryCostRequest shopDeliveryCostRequest,
            BigDecimal expectedDeliveryCost,
            BigDecimal expectedReturnCost
    ) {
        mockLms();

        ShopDeliveryCostResponse response = client.getShopDeliveryCost(shopDeliveryCostRequest);
        Assertions.assertEquals(expectedDeliveryCost, response.getDeliveryCost());
        Assertions.assertEquals(expectedReturnCost, response.getReturnCost());

        verify(mdsS3Client, atMost(1)).download(any(), any());
    }

    @Test
    void testWithoutLms() {
        ShopDeliveryCostResponse response = client.getShopDeliveryCost(createShopDeliveryCostRequest(3, 20, 30, 15, 39, null, 50, 11008));

        Assertions.assertEquals(BigDecimal.valueOf(19400), response.getDeliveryCost());
        Assertions.assertEquals(3, response.getCalculatedWeight());
        Assertions.assertEquals(BigDecimal.valueOf(5000, 3), response.getInsuranceCost());
        response = client.getShopDeliveryCost(createShopDeliveryCostRequest(3, 20, 30, 15, 0, 0L, 50, 11008));
        Assertions.assertEquals(BigDecimal.valueOf(30000), response.getDeliveryCost());
    }

    private static ShopDeliveryCostRequest createShopDeliveryCostRequest(
            double weight,
            double height,
            double length,
            double width,
            int regionFrom,
            Long warehouseId,
            int deliveryServiceId,
            int targetRegionId
    ) {
        ShopDeliveryCostRequest request = new ShopDeliveryCostRequest();
        request.setDeliveryProgram(DeliveryTariffProgramType.MARKET_DELIVERY);
        request.setDeliveryType(PICKUP);
        request.setDeliveryServiceId(deliveryServiceId);
        request.setWeight(weight);
        request.setHeight(height);
        request.setLength(length);
        request.setWidth(width);
        request.setRegionFrom(regionFrom);
        request.setWarehouseId(warehouseId);
        request.setRegionTo(targetRegionId);
        request.setAccessedValue(BigDecimal.valueOf(1000));
        return request;
    }
}
