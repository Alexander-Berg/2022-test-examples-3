package ru.yandex.market.replenishment.autoorder.repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.TruckStatus;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Truck;
import ru.yandex.market.replenishment.autoorder.repository.postgres.TruckRepository;

import static org.junit.Assert.assertEquals;
public class TruckRepositoryTest extends FunctionalTest {

    @Autowired
    private TruckRepository truckRepository;

    @Test
    @DbUnitDataSet(before = "TruckRepositoryTest.before.csv")
    public void testGetGeneratedTrucks() {
        List<Truck> trucks = truckRepository.getAllByStatus(TruckStatus.NEW);
        trucks.sort(Comparator.comparing(Truck::getId));

        assertEquals(3, trucks.size());
        assertEquals(Long.valueOf(6), trucks.get(0).getId());
        assertEquals(Long.valueOf(7), trucks.get(1).getId());
        assertEquals(Long.valueOf(8), trucks.get(2).getId());
    }

    @Test
    @DbUnitDataSet(before = "TruckRepositoryTest.before.csv")
    public void testGetTrucksForPeriod() {
        List<Truck> trucks = truckRepository.getAllByExportTsBetween(
                LocalDate.parse("2019-10-11"),
                LocalDate.parse("2019-10-16")
        ).stream()
                .filter(truck -> truck.getId() != null)
                .sorted(Comparator.comparing(Truck::getId))
                .collect(Collectors.toList());

        assertEquals(3, trucks.size());
        assertEquals(Long.valueOf(2), trucks.get(0).getId());
        assertEquals(Long.valueOf(3), trucks.get(1).getId());
        assertEquals(Long.valueOf(4), trucks.get(2).getId());
    }
}
