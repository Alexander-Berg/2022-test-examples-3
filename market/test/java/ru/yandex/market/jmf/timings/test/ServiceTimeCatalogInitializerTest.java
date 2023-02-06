package ru.yandex.market.jmf.timings.test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.ServiceTimeDayOfWeekPeriodEntity;
import ru.yandex.market.jmf.timings.ServiceTimeExceptionPeriodEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Transactional
@SpringJUnitConfig(InternalTimingTestConfiguration.class)
public class ServiceTimeCatalogInitializerTest {

    @Inject
    DbService dbService;

    @Test
    public void checkCreated() {
        ServiceTime serviceTime = getServiceTime("test1");
        assertNotNull(serviceTime, "Должны инициализировать время обслуживания из serviceTime.json");
    }

    @Test
    public void checkPeriods() {
        ServiceTime serviceTime = getServiceTime("test1");

        Set<ServiceTimeDayOfWeekPeriodEntity> periods = serviceTime.getPeriods();

        assertEquals(3, periods.size(), "В serviceTime.json содержится три периода обслуживания");
        ServiceTimeDayOfWeekPeriodEntity e = Iterables.find(periods,
                p -> "thursday".equals(p.getDayOfWeek().getCode()), null);
        assertNotNull(e);
        assertEquals(LocalTime.parse("10:00"), e.getStartTime());
        assertEquals(LocalTime.parse("17:00"), e.getEndTime());
    }

    @Test
    public void checkExceptions() {
        ServiceTime serviceTime = getServiceTime("test1");

        Set<ServiceTimeExceptionPeriodEntity> exceptions = serviceTime.getExceptions();

        assertEquals(1, exceptions.size(), "В serviceTime.json содержится одно исключение обсуживания");
        ServiceTimeExceptionPeriodEntity e = Iterables.getFirst(exceptions, null);
        assertEquals(LocalDate.parse("2020-02-19"), e.getDay());
        assertEquals(LocalTime.parse("13:00"), e.getStartTime());
        assertEquals(LocalTime.parse("15:00"), e.getEndTime());
    }


    private ServiceTime getServiceTime(String code) {
        return dbService.getByNaturalId(ServiceTime.FQN, ServiceTime.CODE, code);
    }
}
