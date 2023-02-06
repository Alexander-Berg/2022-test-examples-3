package ru.yandex.market.jmf.timings.test;

import java.time.Duration;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.ServiceTimeDayOfWeekPeriodEntity;
import ru.yandex.market.jmf.timings.ServiceTimeExceptionPeriodEntity;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig(InternalTimingTestConfiguration.class)
public class ServiceTimeCatalogTest {

    @Inject
    TxService txService;
    @Inject
    BcpService bcpService;
    @Inject
    ServiceTimeTestUtils utils;
    @Inject
    DbService dbService;

    /**
     * Проверяем, что справочник создается. Без этого остальные тесты не имеют смысла.
     */
    @Test
    public void create() {
        create8x5();
    }

    /**
     * Проверяем, что справочник создается. Без этого остальные тесты не имеют смысла.
     */
    @Test
    public void weeklyDuration_8x5() {
        Entity entity = create8x5();

        Object result = entity.getAttribute(ServiceTime.WEEKLY_DURATION);
        assertEquals(Duration.ofHours(40), result, "Настроили сорокочасовую рабочую неделю");
    }

    /**
     * Проверяем, что справочник создается. Без этого остальные тесты не имеют смысла.
     */
    @Test
    public void weeklyDuration_removeInterval() {
        // настройка системы
        ServiceTimeDayOfWeekPeriodEntity period = txService.doInNewTx(() -> {
            Entity st = utils.createServiceTime();
            return utils.createPeriod(st, "monday", "08:30", "09:30");
        });

        String gid = period.getServiceTime().getGid();

        // вызов системы
        txService.runInNewTx(() -> bcpService.delete(period));

        // проверка утверждений
        Entity currentEntity = txService.doInNewTx(() -> dbService.get(gid));

        Object result = currentEntity.getAttribute(ServiceTime.WEEKLY_DURATION);
        assertEquals(Duration.ofHours(0), result, "Настроили сорокочасовую рабочую неделю");
    }

    /**
     * Проверяем, что справочник создается. Без этого остальные тесты не имеют смысла.
     */
    @Test
    public void exception_removeInterval() {
        // настройка системы
        ServiceTimeExceptionPeriodEntity period = txService.doInNewTx(() -> {
            Entity st = utils.createServiceTime();
            return (ServiceTimeExceptionPeriodEntity) utils.createException(st, "2019-02-16", "08:30", "09:30");
        });

        String gid = period.getServiceTime().getGid();

        // вызов системы
        txService.doInNewTx(() -> {
            bcpService.delete(period);
            return null;
        });

        // проверка утверждений
        Entity currentEntity = txService.doInNewTx(() -> dbService.get(gid));
        // проверяем отсутствие исключения
    }

    /**
     * Проверяем, что не позволяем создавать пересекающиеся интервалы.
     */
    @Test()
    @Transactional
    public void create_overlap() {
        Entity st = utils.createServiceTime();
        utils.createPeriod(st, "monday", "09:00", "13:00");

        assertThrows(ValidationException.class, () -> {
            utils.createPeriod(st, "monday", "10:00", "14:00");
        });
    }

    /**
     * Проверяем, что не позволяем создавать пересекающиеся интервалы.
     */
    @Test()
    @Transactional
    public void create_overlap2() {
        assertThrows(ValidationException.class, () -> {
            bcpService.create(ServiceTime.FQN, Maps.of(
                    "code", Randoms.string(),
                    "title", Randoms.string(),
                    "periods", List.of(
                            Maps.of(
                                    "dayOfWeek", "monday",
                                    "startTime", "09:00",
                                    "endTime", "13:00"
                            ),
                            Maps.of(
                                    "dayOfWeek", "monday",
                                    "startTime", "10:00",
                                    "endTime", "14:00"
                            )
                    )
            ));
        });
    }

    /**
     * Проверяем, что не позволяем создавать пересекающиеся интервалы.
     */
    @Test()
    @Transactional
    public void create_exception_overlap() {
        Entity st = utils.createServiceTime();
        utils.createException(st, "2019-02-16", "09:00", "13:00");
        assertThrows(ValidationException.class, () -> {
            utils.createException(st, "2019-02-16", "10:00", "14:00");
        });

    }

    /**
     * Проверяем, что не позволяем создавать пересекающиеся интервалы.
     */
    @Test()
    @Transactional
    public void create_exception_overlap2() {
        assertThrows(ValidationException.class, () -> {
            bcpService.create(ServiceTime.FQN, Maps.of(
                    "code", Randoms.string(),
                    "title", Randoms.string(),
                    "exceptions", List.of(
                            Maps.of(
                                    "day", "2019-02-16",
                                    "startTime", "09:00",
                                    "endTime", "13:00"
                            ),
                            Maps.of(
                                    "day", "2019-02-16",
                                    "startTime", "10:00",
                                    "endTime", "14:00"
                            )
                    )
            ));
        });
    }

    /**
     * Проверяем, что не позволяем создавать пересекающиеся интервалы.
     */
    @Test()
    @Transactional
    public void create_equals() {
        Entity st = utils.createServiceTime();
        utils.createPeriod(st, "monday", "09:00", "13:00");

        assertThrows(ValidationException.class, () -> {
            utils.createPeriod(st, "monday", "09:00", "13:00");
        });
    }

    /**
     * Проверяем, что не позволяем создавать пересекающиеся интервалы.
     */
    @Test()
    @Transactional
    public void create_exception_equals() {
        Entity st = utils.createServiceTime();
        utils.createException(st, "2019-02-16", "09:00", "13:00");

        assertThrows(ValidationException.class, () -> {
            utils.createException(st, "2019-02-16", "09:00", "13:00");
        });
    }

    /**
     * Проверяем, что можем создавть смежные интервалы (время начало одного совпадает с временем окончания другого)
     */
    @Test
    @Transactional
    public void create_adjacent() {
        Entity st = utils.createServiceTime();
        utils.createPeriod(st, "monday", "09:00", "13:00");
        utils.createPeriod(st, "monday", "13:00", "14:00");
    }

    /**
     * Проверяем, что можем создавть смежные интервалы (время начало одного совпадает с временем окончания другого)
     */
    @Test
    @Transactional
    public void create_exception_adjacent() {
        Entity st = utils.createServiceTime();
        utils.createException(st, "2019-02-16", "09:00", "13:00");
        utils.createException(st, "2019-02-16", "13:00", "14:00");
    }

    /**
     * Проверяем, что не можем создавть пустые интервалы
     */
    @Test()
    @Transactional
    public void create_empty() {
        Entity st = utils.createServiceTime();
        assertThrows(ValidationException.class, () -> {
            utils.createPeriod(st, "monday", "09:00", "09:00");
        });
    }

    /**
     * Проверяем, что не можем создавть интервалы у которых время окончания раньше времени начала
     */
    @Test()
    @Transactional
    public void create_reverse() {
        Entity st = utils.createServiceTime();

        assertThrows(ValidationException.class, () -> {
            utils.createPeriod(st, "monday", "19:00", "09:00");
        });
    }

    private Entity create8x5() {
        return txService.doInNewTx(() -> {
            Entity st = utils.createServiceTime();
            utils.createPeriod(st, "monday", "09:00", "13:00");
            utils.createPeriod(st, "monday", "14:00", "18:00");
            utils.createPeriod(st, "tuesday", "09:00", "13:00");
            utils.createPeriod(st, "tuesday", "14:00", "18:00");
            utils.createPeriod(st, "wednesday", "09:00", "13:00");
            utils.createPeriod(st, "wednesday", "14:00", "18:00");
            utils.createPeriod(st, "thursday", "09:00", "13:00");
            utils.createPeriod(st, "thursday", "14:00", "18:00");
            utils.createPeriod(st, "friday", "09:00", "13:00");
            utils.createPeriod(st, "friday", "14:00", "18:00");
            return st;
        });
    }
}
