package ru.yandex.market.wms.shippingsorter.sorting.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.core.client.CoreClient;
import ru.yandex.market.wms.shared.libs.configproperties.dao.GlobalConfigurationDao;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.domain.SorterOrderStatus;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxId;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxInfo;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.LocationId;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.PackStationId;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.dto.ConfigDto;
import ru.yandex.market.wms.shippingsorter.sorting.entity.SorterOrderEntity;
import ru.yandex.market.wms.shippingsorter.sorting.entity.id.SorterExitId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

@Import(ShippingSorterSecurityTestConfiguration.class)
@RequiredArgsConstructor
public class SorterOrderCalculationServiceTest extends IntegrationTest {

    @Autowired
    @MockBean
    protected CoreClient coreClient;

    @MockBean
    @Autowired
    private Clock clock;

    @MockBean
    @Autowired
    @Qualifier("configPropertyPostgreSqlDao")
    private GlobalConfigurationDao configPropertyPostgreSqlDao;

    @Autowired
    private SorterOrderCalculationService calculationService;

    @BeforeEach
    protected void reset() {
        Mockito.reset(coreClient);
    }

    void clockSetup(String date) {
        Instant parse = Instant.parse(date);
        doReturn(Clock.fixed(parse, ZoneOffset.UTC).instant()).when(this.clock).instant();
        doReturn(Clock.fixed(parse, ZoneOffset.UTC).getZone()).when(this.clock).getZone();
    }

    void clockSetup() {
        clockSetup("2020-01-01T05:00:00.000Z");
    }

    /**
     * Простой выбор, happy path
     */
    @Test
    @DatabaseSetup("/sorting/service/sorter-order/calc/1/before.xml")
    void checkSimpleCalc() {
        mockConfig(
                ConfigDto.builder()
                        .maxWeight("30000.0")
                        .minWeight("50.0")
                        .upperBoxWeightLimitForRound("31000.0")
                        .build()
        );

        clockSetup();
        BoxId boxId = BoxId.builder().id("P123456789").build();
        PackStationId packStationId = PackStationId.builder().id("PACK-TAB03").build();

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(1750)
                .carrierCode("123456")
                .carrierName("DPD")
                .operationDayId(18262L)
                .build();

        SorterOrderEntity expected = SorterOrderEntity.builder()
                .packStationId(packStationId)
                .sorterExitId(SorterExitId.builder().id("SORTEXIT1").build())
                .alternateSorterExitId(SorterExitId.builder().id("SORTEXIT1").build())
                .errorSorterExitId(SorterExitId.builder().id("ESORTEXIT").build())
                .actualLocationId(LocationId.of(packStationId.getId()))
                .boxId(boxId)
                .assignee("conveyor")
                .weightMin(1663)
                .weightMax(1837)
                .status(SorterOrderStatus.ASSIGNED)
                .addWho("ShippingSorter")
                .editWho("ShippingSorter")
                .build();

        SorterOrderEntity received = calculationService
                .calcSorterOrder(boxId, boxInfo, packStationId, "SSORT_ZONE");

        assertEquals(expected, received);
    }

    /**
     * Проверяем выбор выхода в зависимости от загруженности
     */
    @Test
    @DatabaseSetup("/sorting/service/sorter-order/calc/2/before.xml")
    void checkCalcByLoad() {
        clockSetup();
        mockConfig(
                ConfigDto.builder()
                        .maxWeight("30000.0")
                        .minWeight("50.0")
                        .upperBoxWeightLimitForRound("31000.0")
                        .build()
        );

        BoxId boxId = BoxId.builder().id("P123456789").build();
        PackStationId packStationId = PackStationId.builder().id("PACK-TAB03").build();

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(15500)
                .carrierCode("123456")
                .carrierName("DPD")
                .operationDayId(18262L)
                .build();

        SorterOrderEntity expected = SorterOrderEntity.builder()
                .packStationId(packStationId)
                .sorterExitId(SorterExitId.builder().id("SORT_01-02").build())
                .alternateSorterExitId(SorterExitId.builder().id("SORT_01-02").build())
                .errorSorterExitId(SorterExitId.builder().id("ESORTEXIT").build())
                .actualLocationId(LocationId.of(packStationId.getId()))
                .boxId(boxId)
                .assignee("conveyor")
                .weightMin(14725)
                .weightMax(16275)
                .status(SorterOrderStatus.ASSIGNED)
                .addWho("ShippingSorter")
                .editWho("ShippingSorter")
                .build();

        SorterOrderEntity received = calculationService
                .calcSorterOrder(boxId, boxInfo, packStationId, "SSORT_ZONE");

        assertEquals(expected, received);
    }

    /**
     * Проверяем выбор альтернативного выхода если не найден подходящий
     */
    @Test
    @DatabaseSetup("/sorting/service/sorter-order/calc/3/before.xml")
    void checkCalcAlternateExit() {
        mockConfig(
                ConfigDto.builder()
                        .maxWeight("30000.0")
                        .minWeight("50.0")
                        .upperBoxWeightLimitForRound("31000.0")
                        .build()
        );

        clockSetup();
        BoxId boxId = BoxId.builder().id("P123456789").build();
        PackStationId packStationId = PackStationId.builder().id("PACK-TAB03").build();

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(15500)
                .carrierCode("123456")
                .carrierName("DPD")
                .operationDayId(18263L)
                .build();

        SorterOrderEntity expected = SorterOrderEntity.builder()
                .packStationId(packStationId)
                .sorterExitId(SorterExitId.builder().id("ASORTEXIT").build())
                .alternateSorterExitId(SorterExitId.builder().id("ASORTEXIT").build())
                .errorSorterExitId(SorterExitId.builder().id("ESORTEXIT").build())
                .actualLocationId(LocationId.of(packStationId.getId()))
                .boxId(boxId)
                .assignee("conveyor")
                .weightMin(14725)
                .weightMax(16275)
                .status(SorterOrderStatus.ASSIGNED)
                .addWho("ShippingSorter")
                .editWho("ShippingSorter")
                .build();

        SorterOrderEntity received = calculationService
                .calcSorterOrder(boxId, boxInfo, packStationId, "SSORT_ZONE");

        assertEquals(expected, received);
    }

    /**
     * Проверяем выбор выхода по привязке к СД
     */
    @Test
    @DatabaseSetup("/sorting/service/sorter-order/calc/4/before.xml")
    void checkCalcByCarrier() {
        mockConfig(
                ConfigDto.builder()
                        .maxWeight("30000.0")
                        .minWeight("50.0")
                        .upperBoxWeightLimitForRound("31000.0")
                        .build()
        );

        clockSetup();
        BoxId boxId = BoxId.builder().id("P123456789").build();
        PackStationId packStationId = PackStationId.builder().id("PACK-TAB03").build();

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(15500)
                .carrierCode("123456")
                .carrierName("DPD")
                .operationDayId(18262L)
                .build();

        SorterOrderEntity expected = SorterOrderEntity.builder()
                .packStationId(packStationId)
                .sorterExitId(SorterExitId.builder().id("SORTEXIT2").build())
                .alternateSorterExitId(SorterExitId.builder().id("SORTEXIT2").build())
                .errorSorterExitId(SorterExitId.builder().id("ESORTEXIT").build())
                .actualLocationId(LocationId.of(packStationId.getId()))
                .boxId(boxId)
                .assignee("conveyor")
                .weightMin(14725)
                .weightMax(16275)
                .status(SorterOrderStatus.ASSIGNED)
                .addWho("ShippingSorter")
                .editWho("ShippingSorter")
                .build();

        SorterOrderEntity received = calculationService
                .calcSorterOrder(boxId, boxInfo, packStationId, "SSORT_ZONE");

        assertEquals(expected, received);
    }

    /**
     * Проверяем выбор выхода в зависимости от расписания
     */
    @Test
    @DatabaseSetup("/sorting/service/sorter-order/calc/5/before.xml")
    void checkCalcBySchedule() {
        mockConfig(
                ConfigDto.builder()
                        .maxWeight("30000.0")
                        .minWeight("50.0")
                        .upperBoxWeightLimitForRound("31000.0")
                        .build()
        );

        clockSetup();
        BoxId boxId = BoxId.builder().id("P123456789").build();
        PackStationId packStationId = PackStationId.builder().id("PACK-TAB03").build();

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(28000)
                .carrierCode("123456")
                .carrierName("DPD")
                .operationDayId(18264L)
                .build();

        SorterOrderEntity expected = SorterOrderEntity.builder()
                .packStationId(packStationId)
                .sorterExitId(SorterExitId.builder().id("SORTEXIT3").build())
                .alternateSorterExitId(SorterExitId.builder().id("SORTEXIT3").build())
                .errorSorterExitId(SorterExitId.builder().id("ESORTEXIT").build())
                .actualLocationId(LocationId.of(packStationId.getId()))
                .boxId(boxId)
                .assignee("conveyor")
                .weightMin(25200)
                .weightMax(30000)
                .status(SorterOrderStatus.ASSIGNED)
                .addWho("ShippingSorter")
                .editWho("ShippingSorter")
                .build();

        SorterOrderEntity received = calculationService
                .calcSorterOrder(boxId, boxInfo, packStationId, "SSORT_ZONE");

        assertEquals(expected, received);
    }

    /**
     * Проверяем errorSorterExit и alternateSorterExitId при включенной настройке "Сломанные весы" (ISBROKENSCALES)
     */
    @Test
    @DatabaseSetup("/sorting/service/sorter-order/calc/6/before.xml")
    void checkCalcWithIsBrokenScalesOn() {
        mockConfig(
                ConfigDto.builder()
                        .maxWeight("30000.0")
                        .minWeight("50.0")
                        .upperBoxWeightLimitForRound("31000.0")
                        .isBrokenScales("1")
                        .build()
        );

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(15500)
                .carrierCode("123456")
                .carrierName("DPD")
                .operationDayId(18264L)
                .build();

        clockSetup();

        BoxId boxId = BoxId.builder().id("P123456789").build();
        PackStationId packStationId = PackStationId.builder().id("PACK-TAB03").build();

        SorterOrderEntity expected = SorterOrderEntity.builder()
                .packStationId(packStationId)
                .sorterExitId(SorterExitId.builder().id("SORTEXIT1").build())
                .alternateSorterExitId(SorterExitId.builder().id("SORTEXIT1").build())
                .errorSorterExitId(SorterExitId.builder().id("SORTEXIT1").build())
                .actualLocationId(LocationId.of(packStationId.getId()))
                .boxId(boxId)
                .assignee("conveyor")
                .weightMin(14725)
                .weightMax(16275)
                .status(SorterOrderStatus.ASSIGNED)
                .addWho("ShippingSorter")
                .editWho("ShippingSorter")
                .build();

        SorterOrderEntity received = calculationService
                .calcSorterOrder(boxId, boxInfo, packStationId, "SSORT_ZONE");

        assertEquals(expected, received);
    }

    /**
     * Проверяем выбор выхода в зависимости от загруженности, если задания на сортировку отсутствуют
     */
    @Test
    @DatabaseSetup("/sorting/service/sorter-order/calc/7/before.xml")
    void checkCalcByLoadWithoutSorterOrders() {
        mockConfig(
                ConfigDto.builder()
                        .maxWeight("30000.0")
                        .minWeight("50.0")
                        .upperBoxWeightLimitForRound("31000.0")
                        .build()
        );

        clockSetup();
        BoxId boxId = BoxId.builder().id("P123456789").build();
        PackStationId packStationId = PackStationId.builder().id("PACK-TAB03").build();

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(15500)
                .carrierCode("123456")
                .carrierName("DPD")
                .operationDayId(18262L)
                .build();

        SorterOrderEntity expected = SorterOrderEntity.builder()
                .packStationId(packStationId)
                .sorterExitId(SorterExitId.builder().id("SORT_01-01").build())
                .alternateSorterExitId(SorterExitId.builder().id("SORT_01-01").build())
                .errorSorterExitId(SorterExitId.builder().id("ESORTEXIT").build())
                .actualLocationId(LocationId.of(packStationId.getId()))
                .boxId(boxId)
                .assignee("conveyor")
                .weightMin(14725)
                .weightMax(16275)
                .status(SorterOrderStatus.ASSIGNED)
                .addWho("ShippingSorter")
                .editWho("ShippingSorter")
                .build();

        SorterOrderEntity received = calculationService
                .calcSorterOrder(boxId, boxInfo, packStationId, "SSORT_ZONE");

        assertEquals(expected, received);
    }

    /**
     * Проверяем выбор выхода в зависимости от загруженности, если активные задания на сортировку существуют
     */
    @Test
    @DatabaseSetup("/sorting/service/sorter-order/calc/8/before.xml")
    void checkCalcByLoadWithSorterOrders() {
        mockConfig(
                ConfigDto.builder()
                        .maxWeight("30000.0")
                        .minWeight("50.0")
                        .upperBoxWeightLimitForRound("31000.0")
                        .build()
        );

        clockSetup();
        BoxId boxId = BoxId.builder().id("P123456789").build();
        PackStationId packStationId = PackStationId.builder().id("PACK-TAB03").build();

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(15500)
                .carrierCode("123456")
                .carrierName("DPD")
                .operationDayId(18262L)
                .build();

        SorterOrderEntity expected = SorterOrderEntity.builder()
                .packStationId(packStationId)
                .sorterExitId(SorterExitId.builder().id("SORT_01-02").build())
                .alternateSorterExitId(SorterExitId.builder().id("SORT_01-02").build())
                .errorSorterExitId(SorterExitId.builder().id("ESORTEXIT").build())
                .actualLocationId(LocationId.of(packStationId.getId()))
                .boxId(boxId)
                .assignee("conveyor")
                .weightMin(14725)
                .weightMax(16275)
                .status(SorterOrderStatus.ASSIGNED)
                .addWho("ShippingSorter")
                .editWho("ShippingSorter")
                .build();

        SorterOrderEntity received = calculationService
                .calcSorterOrder(boxId, boxInfo, packStationId, "SSORT_ZONE");

        assertEquals(expected, received);
    }

    /**
     * Проверяем выбор выхода в зависимости от расписания
     */
    @Test
    @DatabaseSetup("/sorting/service/sorter-order/calc/9/before.xml")
    void checkCalcByLoadAndSchedule() {
        mockConfig(
                ConfigDto.builder()
                        .maxWeight("30000.0")
                        .minWeight("50.0")
                        .upperBoxWeightLimitForRound("31000.0")
                        .build()
        );

        clockSetup();

        BoxId boxId = BoxId.builder().id("P123456789").build();
        PackStationId packStationId = PackStationId.builder().id("PACK-TAB03").build();

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(2000)
                .carrierCode("123456")
                .carrierName("DPD")
                .operationDayId(18262L)
                .build();

        SorterOrderEntity expected = SorterOrderEntity.builder()
                .packStationId(packStationId)
                .sorterExitId(SorterExitId.builder().id("SORT_01-02").build())
                .alternateSorterExitId(SorterExitId.builder().id("SORT_01-02").build())
                .errorSorterExitId(SorterExitId.builder().id("ESORTEXIT").build())
                .actualLocationId(LocationId.of(packStationId.getId()))
                .boxId(boxId)
                .assignee("conveyor")
                .weightMin(1900)
                .weightMax(2100)
                .status(SorterOrderStatus.ASSIGNED)
                .addWho("ShippingSorter")
                .editWho("ShippingSorter")
                .build();

        SorterOrderEntity received = calculationService
                .calcSorterOrder(boxId, boxInfo, packStationId, "SSORT_ZONE");

        assertEquals(expected, received);
    }

    /**
     * Проверяем выбор выхода при условии, что текущий день - операционный, а operationDayId посылки < operationDayId
     * выхода
     */
    @Test
    @DatabaseSetup("/sorting/service/sorter-order/calc/10/before.xml")
    void checkCalcByOperationDay() {
        mockConfig(
                ConfigDto.builder()
                        .maxWeight("30000.0")
                        .minWeight("50.0")
                        .upperBoxWeightLimitForRound("31000.0")
                        .build()
        );

        clockSetup("2020-01-02T05:00:00.000Z");
        BoxId boxId = BoxId.builder().id("P123456789").build();
        PackStationId packStationId = PackStationId.builder().id("PACK-TAB03").build();

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(1750)
                .carrierCode("123456")
                .carrierName("DPD")
                .operationDayId(18262L)
                .build();

        SorterOrderEntity expected = SorterOrderEntity.builder()
                .packStationId(packStationId)
                .sorterExitId(SorterExitId.builder().id("SORTEXIT1").build())
                .alternateSorterExitId(SorterExitId.builder().id("SORTEXIT1").build())
                .errorSorterExitId(SorterExitId.builder().id("ESORTEXIT").build())
                .actualLocationId(LocationId.of(packStationId.getId()))
                .boxId(boxId)
                .assignee("conveyor")
                .weightMin(1663)
                .weightMax(1837)
                .status(SorterOrderStatus.ASSIGNED)
                .addWho("ShippingSorter")
                .editWho("ShippingSorter")
                .build();

        SorterOrderEntity received = calculationService
                .calcSorterOrder(boxId, boxInfo, packStationId, "SSORT_ZONE");

        assertEquals(expected, received);
    }

    /**
     * Проверяем выбор альтернативного выхода при условии, что текущий день - не операционный, а operationDayId
     * посылки < operationDayId выхода
     */
    @Test
    @DatabaseSetup("/sorting/service/sorter-order/calc/11/before.xml")
    void checkCalcAlternateExitByOperationDay() {
        mockConfig(
                ConfigDto.builder()
                        .maxWeight("30000.0")
                        .minWeight("50.0")
                        .upperBoxWeightLimitForRound("31000.0")
                        .build()
        );

        clockSetup("2020-01-03T05:00:00.000Z");
        BoxId boxId = BoxId.builder().id("P123456789").build();
        PackStationId packStationId = PackStationId.builder().id("PACK-TAB03").build();

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(1750)
                .carrierCode("123456")
                .carrierName("DPD")
                .operationDayId(18262L)
                .build();

        SorterOrderEntity expected = SorterOrderEntity.builder()
                .packStationId(packStationId)
                .sorterExitId(SorterExitId.builder().id("ASORTEXIT").build())
                .alternateSorterExitId(SorterExitId.builder().id("ASORTEXIT").build())
                .errorSorterExitId(SorterExitId.builder().id("ESORTEXIT").build())
                .actualLocationId(LocationId.of(packStationId.getId()))
                .boxId(boxId)
                .assignee("conveyor")
                .weightMin(1663)
                .weightMax(1837)
                .status(SorterOrderStatus.ASSIGNED)
                .addWho("ShippingSorter")
                .editWho("ShippingSorter")
                .build();

        SorterOrderEntity received = calculationService
                .calcSorterOrder(boxId, boxInfo, packStationId, "SSORT_ZONE");

        assertEquals(expected, received);
    }

    @Test
    @DatabaseSetup("/sorting/service/sorter-order/calc/12/before.xml")
    void checkCalcWithoutScheduledShipDate() {
        mockConfig(
                ConfigDto.builder()
                        .maxWeight("30000.0")
                        .minWeight("50.0")
                        .upperBoxWeightLimitForRound("31000.0")
                        .build()
        );

        clockSetup("2020-01-03T05:00:00.000Z");
        BoxId boxId = BoxId.builder().id("P123456789").build();
        PackStationId packStationId = PackStationId.builder().id("PACK-TAB03").build();
        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(1750)
                .carrierCode("123456")
                .carrierName("DPD")
                .operationDayId(18262L)
                .build();

        SorterOrderEntity expected = SorterOrderEntity.builder()
                .packStationId(packStationId)
                .sorterExitId(SorterExitId.builder().id("SORTEXIT1").build())
                .alternateSorterExitId(SorterExitId.builder().id("SORTEXIT1").build())
                .errorSorterExitId(SorterExitId.builder().id("ESORTEXIT").build())
                .actualLocationId(LocationId.of(packStationId.getId()))
                .boxId(boxId)
                .assignee("conveyor")
                .weightMin(1663)
                .weightMax(1837)
                .status(SorterOrderStatus.ASSIGNED)
                .addWho("ShippingSorter")
                .editWho("ShippingSorter")
                .build();

        SorterOrderEntity received = calculationService
                .calcSorterOrder(boxId, boxInfo, packStationId, "SSORT_ZONE");

        assertEquals(expected, received);
    }

    private void mockConfig(ConfigDto dto) {
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_WEIGHT_GRAMS"))
                .thenReturn(dto.getMaxWeight());
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MIN_WEIGHT_GRAMS"))
                .thenReturn(dto.getMinWeight());
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("WEIGHT_LIMIT_FOR_ROUND_GRAMS"))
                .thenReturn(dto.getUpperBoxWeightLimitForRound());
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("ISBROKENSCALES"))
                .thenReturn(dto.getIsBrokenScales());
    }
}
