package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;

import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.XdocSchedule;
import ru.yandex.market.replenishment.autoorder.repository.postgres.XdocScheduleRepository;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.replenishment.autoorder.utils.Constants.PEK_WAREHOUSE_ID;

;
public class XdocScheduleServiceTest extends FunctionalTest {

    @Autowired
    XdocScheduleService xdocScheduleService;

    @Autowired
    SqlSession batchSqlSession;

    @Test
    public void testGetSchedule() {
        fillXdocSchedule();
        Integer[] schedule = xdocScheduleService.getSchedule(PEK_WAREHOUSE_ID, 147);
        assertArrayEquals(new Integer[]{5, 4, 4, 3, 8, null, null}, schedule);
    }

    @Test
    public void testGetXdocDeliveryDate_MondayToSaturday() {
        fillXdocSchedule();
        LocalDate deliveryDateFriday = LocalDate.of(2021, 8, 16);
        assertEquals(LocalDate.of(2021, 8, 22),
                xdocScheduleService.getXdocDeliveryDate(deliveryDateFriday, PEK_WAREHOUSE_ID, 147));
    }

    @Test
    public void testGetXdocDeliveryDate_TuesdayToSunday() {
        fillXdocSchedule();
        LocalDate deliveryDateFriday = LocalDate.of(2021, 8, 17);
        assertEquals(LocalDate.of(2021, 8, 22),
                xdocScheduleService.getXdocDeliveryDate(deliveryDateFriday, PEK_WAREHOUSE_ID, 147));
    }

    @Test
    public void testGetXdocDeliveryDate_WednesdayToSunday() {
        fillXdocSchedule();
        LocalDate deliveryDateFriday = LocalDate.of(2021, 8, 18);
        assertEquals(LocalDate.of(2021, 8, 22),
                xdocScheduleService.getXdocDeliveryDate(deliveryDateFriday, PEK_WAREHOUSE_ID, 147));
    }

    @Test
    public void testGetXdocDeliveryDate_ThursdayToSunday() {
        fillXdocSchedule();
        LocalDate deliveryDateFriday = LocalDate.of(2021, 8, 19);
        assertEquals(LocalDate.of(2021, 8, 22),
                xdocScheduleService.getXdocDeliveryDate(deliveryDateFriday, PEK_WAREHOUSE_ID, 147));
    }

    @Test
    public void testGetXdocDeliveryDate_FridayToNextSunday() {
        fillXdocSchedule();
        LocalDate deliveryDateFriday = LocalDate.of(2021, 8, 20);
        assertEquals(LocalDate.of(2021, 8, 29),
                xdocScheduleService.getXdocDeliveryDate(deliveryDateFriday, PEK_WAREHOUSE_ID, 147));
    }

    @Test
    public void testGetXdocDeliveryDate_SaturdayToNextSunday() {
        fillXdocSchedule();
        LocalDate deliveryDateFriday = LocalDate.of(2021, 8, 21);
        assertEquals(LocalDate.of(2021, 8, 29),
                xdocScheduleService.getXdocDeliveryDate(deliveryDateFriday, PEK_WAREHOUSE_ID, 147));
    }

    @Test
    public void testGetXdocDeliveryDate_ScheduleIsAbsent() {
        LocalDate deliveryDateFriday = LocalDate.of(2021, 8, 21);
        assertThrows(IllegalStateException.class,
                () -> xdocScheduleService.getXdocDeliveryDate(deliveryDateFriday, PEK_WAREHOUSE_ID, 147),
                "Transit time wasn't found for warehouse id 147 and day of week SATURDAY"
        );
    }

    private void fillXdocSchedule() {
        fillXdocSchedule(new Integer[]{5, 4, 4, 3, 8, null, null});
    }

    private void fillXdocSchedule(Integer[] schedule) {
        final XdocScheduleRepository xdocScheduleRepository = batchSqlSession.getMapper(XdocScheduleRepository.class);
        xdocScheduleRepository.upsert(new XdocSchedule(PEK_WAREHOUSE_ID, 147L, schedule));
        xdocScheduleRepository.upsert(new XdocSchedule(PEK_WAREHOUSE_ID, 301L, schedule));
        xdocScheduleRepository.upsert(new XdocSchedule(PEK_WAREHOUSE_ID, 302L, schedule));
        xdocScheduleRepository.upsert(new XdocSchedule(PEK_WAREHOUSE_ID, 304L, schedule));
        xdocScheduleRepository.upsert(new XdocSchedule(PEK_WAREHOUSE_ID, 147L, schedule));
    }
}
