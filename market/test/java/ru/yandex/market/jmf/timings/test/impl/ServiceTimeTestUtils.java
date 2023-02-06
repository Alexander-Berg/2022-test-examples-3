package ru.yandex.market.jmf.timings.test.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.ServiceTimeDayOfWeekPeriodEntity;
import ru.yandex.market.jmf.timings.ServiceTimeExceptionPeriodEntity;

@Component
public class ServiceTimeTestUtils {

    private final BcpService bcpService;
    private final EntityService entityService;
    private final DbService dbService;

    public ServiceTimeTestUtils(BcpService bcpService,
                                EntityService entityService,
                                DbService dbService) {
        this.bcpService = bcpService;
        this.entityService = entityService;
        this.dbService = dbService;
    }

    public ServiceTime createServiceTime() {
        return createServiceTimeWithCode(Randoms.string());
    }

    public ServiceTime createServiceTimeWithCode(String code) {
        return bcpService.create(ServiceTime.FQN, ImmutableMap.of(
                ServiceTime.CODE, code,
                ServiceTime.TITLE, Randoms.string()
        ));
    }

    public ServiceTime createDefaultServiceTimeWithPeriodByCode(String code) {
        ServiceTime st = dbService.getByNaturalId(ServiceTime.FQN, CatalogItem.CODE, code);
        createPeriod(st, "monday", "09:00", "21:00");
        return st;
    }

    public ServiceTime createServiceTime8x7() {
        ServiceTime st = createServiceTime();

        entityService.setAttribute(st, "periods", Set.of(
                createPeriod(st, "monday", "09:00", "13:00"),
                createPeriod(st, "monday", "14:00", "18:00"),
                createPeriod(st, "tuesday", "09:00", "13:00"),
                createPeriod(st, "tuesday", "14:00", "18:00"),
                createPeriod(st, "wednesday", "09:00", "13:00"),
                createPeriod(st, "wednesday", "14:00", "18:00"),
                createPeriod(st, "thursday", "09:00", "13:00"),
                createPeriod(st, "thursday", "14:00", "18:00"),
                createPeriod(st, "friday", "09:00", "13:00"),
                createPeriod(st, "friday", "14:00", "18:00"),
                createPeriod(st, "saturday", "09:00", "13:00"),
                createPeriod(st, "saturday", "14:00", "18:00"),
                createPeriod(st, "sunday", "09:00", "13:00"),
                createPeriod(st, "sunday", "14:00", "18:00")
        ));
        return st;
    }

    public ServiceTime createServiceTime24x7() {
        ServiceTime st = createServiceTime();

        entityService.setAttribute(st, "periods", Set.of(
                createPeriod(st, "monday", "00:00", "23:59:59"),
                createPeriod(st, "tuesday", "00:00", "23:59:59"),
                createPeriod(st, "wednesday", "00:00", "23:59:59"),
                createPeriod(st, "thursday", "00:00", "23:59:59"),
                createPeriod(st, "friday", "00:00", "23:59:59"),
                createPeriod(st, "saturday", "00:00", "23:59:59"),
                createPeriod(st, "sunday", "00:00", "23:59:59")
        ));
        return st;
    }

    public ServiceTimeDayOfWeekPeriodEntity createPeriod(Entity st, String dayOfWeek,
                                                         String startTime, String endTime) {
        return bcpService.create(ServiceTimeDayOfWeekPeriodEntity.FQN, ImmutableMap.of(
                ServiceTimeDayOfWeekPeriodEntity.SERVICE_TIME, st,
                ServiceTimeDayOfWeekPeriodEntity.DAY_OF_WEEK, dayOfWeek,
                ServiceTimeDayOfWeekPeriodEntity.START_TIME, startTime,
                ServiceTimeDayOfWeekPeriodEntity.END_TIME, endTime
        ));
    }

    public Entity createException(Entity st, String day, String startTime, String endTime) {
        return bcpService.create(ServiceTimeExceptionPeriodEntity.FQN, ImmutableMap.of(
                ServiceTimeExceptionPeriodEntity.SERVICE_TIME, st,
                ServiceTimeExceptionPeriodEntity.DAY, day,
                ServiceTimeExceptionPeriodEntity.START_TIME, startTime,
                ServiceTimeExceptionPeriodEntity.END_TIME, endTime
        ));
    }

    public ServiceTime createNonWorkingNowServiceTime() {
        ServiceTime st = createServiceTime();
        String dayOfWeek = Now.localDate().minusDays(1).getDayOfWeek().name().toLowerCase();
        entityService.setAttribute(st, "periods", Set.of(
                createPeriod(st, dayOfWeek, "00:00", "01:00")
        ));
        return st;
    }

    public ServiceTimeDayOfWeekPeriodEntity createSingleDayPeriod(ServiceTime st,
                                                                  LocalDateTime startOfPeriod,
                                                                  LocalDateTime endOfPeriod) {
        checkPeriodDay(startOfPeriod, endOfPeriod);
        var dayOfWeek = startOfPeriod.getDayOfWeek().toString().toLowerCase();

        ImmutableMap<String, Object> properties = ImmutableMap.of(
                ServiceTimeDayOfWeekPeriodEntity.SERVICE_TIME, st,
                ServiceTimeDayOfWeekPeriodEntity.DAY_OF_WEEK, dayOfWeek,
                ServiceTimeDayOfWeekPeriodEntity.START_TIME,
                startOfPeriod.toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString(),
                ServiceTimeDayOfWeekPeriodEntity.END_TIME,
                endOfPeriod.toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString()
        );
        return bcpService.create(ServiceTimeDayOfWeekPeriodEntity.FQN, properties);
    }

    private void checkPeriodDay(LocalDateTime start,
                                LocalDateTime end) {

        if (!start.isBefore(end) || !start.truncatedTo(ChronoUnit.DAYS).isEqual(end.truncatedTo(ChronoUnit.DAYS))) {
            throw new IllegalArgumentException("Время должно быть в рамках одного дня и начало должно быть раньше " +
                    "конца периода");
        }
    }
}
