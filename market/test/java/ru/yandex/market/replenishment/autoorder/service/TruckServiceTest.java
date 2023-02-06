package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException;
import ru.yandex.market.replenishment.autoorder.model.dto.InterWarehouseRecommendation;
import ru.yandex.market.replenishment.autoorder.model.dto.InterWarehouseTruckStatisticsDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.TruckDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.TruckInfoDTO;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
public class TruckServiceTest extends FunctionalTest {

    @Autowired
    private TruckService truckService;


    @Test
    @DbUnitDataSet(before = "../repository/InterWarehouseRecommendationRepository.before.csv")
    public void getTruckInfo() {
        TruckInfoDTO truckInfoDTO  = truckService.getTruckInfo(6L);

        List<InterWarehouseRecommendation> recommendations = truckInfoDTO.getInterWarehouseRecommendations();
        assertEquals(1, recommendations.size());

        InterWarehouseRecommendation recommendation = recommendations.get(0);
        assertNotNull(recommendation);
        assertEquals(100L, recommendation.getMsku());
        assertNotNull(recommendation.getTruckQuantity());
        assertEquals(3L, recommendation.getTruckQuantity().longValue());
        assertNotNull(recommendation.getExportedQty());
        assertEquals(7L, recommendation.getExportedQty().longValue());

        TruckDTO truckD = truckInfoDTO.getTruck();
        assertNotNull(truckD.getId());
        assertEquals(6L, truckD.getId().longValue());
        assertNotNull(truckD.getCode());
        assertEquals(102L, truckD.getCode().longValue());

        List<String> orderIds = truckD.getAxOrderIds();
        assertNotNull(orderIds);
        assertEquals(1, orderIds.size());
        assertTrue(truckD.getAxOrderIds().contains("abc123"));

        Map<Integer, String> expectedErrors = new HashMap<>(){
            {
                put(1, "some_error");
                put(2, "another_error");
            }
        };
        assertEquals(expectedErrors, truckD.getErrors());
    }

    @Test
    @DbUnitDataSet(before = "../repository/InterWarehouseRecommendationRepository.before.csv")
    public void getRecommendations_ByNewTruckId() {
        List<InterWarehouseRecommendation> recommendations  = truckService.getTruckRecommendations(6L);

        assertNotNull(recommendations);
        assertEquals(1, recommendations.size());

        InterWarehouseRecommendation recommendation = recommendations.get(0);
        assertNotNull(recommendation);
        assertEquals(100L, recommendation.getMsku());
        assertNotNull(recommendation.getTruckQuantity());
        assertEquals(3L, recommendation.getTruckQuantity().longValue());
        assertNotNull(recommendation.getExportedQty());
        assertEquals(7L, recommendation.getExportedQty().longValue());
    }

    @Test
    @DbUnitDataSet(before = "../repository/InterWarehouseRecommendationRepository.before.csv")
    public void getRecommendations_ByAvoidTruckId() {
        UserWarningException exception = assertThrows(
                UserWarningException.class,
                () -> truckService.getTruckRecommendations(100500L)
        );
        assertEquals("Трака с ID #100500 не существует", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "TruckServiceTest.before.csv")
    public void getTrucksTest() {
        LocalDate curDate = TestUtils.parseISOLocalDate("2019-10-15");
        LocalDate dateFrom = curDate.minusDays(6);

        List<TruckDTO> actual = truckService.getTrucks(curDate, dateFrom, curDate);
        assertEquals(4, actual.size());

        actual.sort(Comparator.comparing(TruckDTO::getId));

        // Truck #1
        TruckDTO truck = actual.get(0);
        assertNotNull(truck.getId());
        assertEquals(1002L, truck.getId().longValue());

        List<String> orderIds = truck.getAxOrderIds();
        assertNotNull(orderIds);
        assertEquals(2, orderIds.size());
        assertTrue(orderIds.contains("abc_21"));
        assertTrue(orderIds.contains("abc_22"));

        Map<Integer, String> errors = truck.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        assertEquals("Не найден продукт с MSKU '100600'", errors.get(1));

        // Truck #2
        truck = actual.get(1);
        assertNotNull(truck.getId());
        assertEquals(1003L, truck.getId().longValue());

        orderIds = truck.getAxOrderIds();
        assertNotNull(orderIds);
        assertEquals(1, orderIds.size());
        assertEquals("abc_31", orderIds.get(0));

        errors = truck.getErrors();
        assertNotNull(errors);
        assertEquals(3, errors.size());
        assertEquals("Не найден продукт с MSKU '100501'", errors.get(1));
        assertEquals("Не найден продукт с MSKU '100502'", errors.get(2));
        assertEquals("Не найден перевозчик с кодом '207'", errors.get(3));

        // Truck #3
        truck = actual.get(2);
        assertNotNull(truck.getId());
        assertEquals(1004L, truck.getId().longValue());

        orderIds = truck.getAxOrderIds();
        assertNotNull(orderIds);
        assertEquals(1, orderIds.size());
        assertEquals("abc_41", orderIds.get(0));

        errors = truck.getErrors();
        assertNotNull(errors);
        assertEquals(0, errors.size());

        // Truck #4
        truck = actual.get(3);
        assertNotNull(truck.getId());
        assertEquals(1005L, truck.getId().longValue());

        orderIds = truck.getAxOrderIds();
        assertNotNull(orderIds);
        assertEquals(0, orderIds.size());

        errors = truck.getErrors();
        assertNotNull(errors);
        assertEquals(0, errors.size());

        InterWarehouseTruckStatisticsDTO stat = truck.getStatistics();
        assertNotNull(stat);
        assertEquals(3, stat.getItems());
        assertEquals(1, stat.getSskus());
        assertEquals(3, stat.getWeight());
        assertEquals(3, stat.getVolume());
    }

    @Test
    @DbUnitDataSet(before = "TruckServiceTest.before.csv")
    public void getTrucksTest_EmptyResult() {
        LocalDate curDate = TestUtils.parseISOLocalDate("2000-01-15");
        LocalDate dateFrom = curDate.minusDays(15);
        LocalDate dateTo = curDate.minusDays(10);

        List<TruckDTO> actual = truckService.getTrucks(curDate, dateFrom, dateTo);
        assertEquals(0, actual.size());
    }
}
