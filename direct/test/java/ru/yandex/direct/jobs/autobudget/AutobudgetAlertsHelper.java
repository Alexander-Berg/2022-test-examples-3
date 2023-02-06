package ru.yandex.direct.jobs.autobudget;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetCommonAlertStatus;
import ru.yandex.direct.core.entity.autobudget.model.AutobudgetHourlyProblem;
import ru.yandex.direct.core.entity.autobudget.model.CpaAutobudgetAlert;
import ru.yandex.direct.core.entity.autobudget.model.HourlyAutobudgetAlert;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetCpaAlertRepository;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetHourlyAlertRepository;

import static ru.yandex.direct.core.entity.autobudget.model.AutobudgetAlertProperties.CPA;
import static ru.yandex.direct.core.entity.autobudget.model.AutobudgetAlertProperties.HOURLY;

/**
 * Тестовые методы для работы с алертами автобюджета. Используются только в тестах
 * Таблицы: ppc.autobudget_alerts, ppc.autobudget_cpa_alerts
 */
@ParametersAreNonnullByDefault
class AutobudgetAlertsHelper {

    private static final EnumSet<AutobudgetHourlyProblem> DEFAULT_PROBLEMS_VALUE =
            EnumSet.of(AutobudgetHourlyProblem.IN_ROTATION);
    private static final long DEFAULT_OVERDRAFT_VALUE = 0;

    private final AutobudgetHourlyAlertRepository hourlyAlertRepository;
    private final AutobudgetCpaAlertRepository cpaAlertRepository;

    AutobudgetAlertsHelper(
            AutobudgetHourlyAlertRepository hourlyAlertRepository,
            AutobudgetCpaAlertRepository cpaAlertRepository) {
        this.hourlyAlertRepository = hourlyAlertRepository;
        this.cpaAlertRepository = cpaAlertRepository;
    }

    /**
     * @param ttlForActiveAlerts время с момента последнего обновления активного алерта в течение, которого он не должен быть удален
     * @return время последнего обновления алерта, который может быть удален
     */
    private LocalDateTime getLastUpdateTimeForOutdatedAlert(Duration ttlForActiveAlerts) {
        return LocalDateTime.now().minus(ttlForActiveAlerts)
                //Отнимаем еще секунду, чтобы время было строго меньше borderDateTime в AutobudgetCpaAlertRepository.deleteActiveAlertsOlderThanDateTime
                .minusSeconds(1);
    }

    /**
     * Добавляет часовой алерт в базу
     */
    private void addHourlyAlert(int shard, HourlyAutobudgetAlert hourlyAlert) {
        if (hourlyAlert.getProblems() == null) {
            hourlyAlert.withProblems(DEFAULT_PROBLEMS_VALUE);
        }
        if (hourlyAlert.getOverdraft() == null) {
            hourlyAlert.withOverdraft(DEFAULT_OVERDRAFT_VALUE);
        }
        hourlyAlertRepository.addAlerts(shard, Collections.singletonList(hourlyAlert));
    }

    /**
     * Добавляет CPA-алерт в базу
     */
    private void addCpaAlert(int shard, CpaAutobudgetAlert cpaAlert) {
        cpaAlertRepository.addAlerts(shard, Collections.singletonList(cpaAlert));
    }

    /**
     * Добавляет в базу активные часовой и CPA алерты, которые должны быть удалены
     */
    void addOutdatedActiveAlerts(int shard, long cid) {
        HourlyAutobudgetAlert hourlyAlert = new HourlyAutobudgetAlert()
                .withCid(cid)
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                .withLastUpdate(getLastUpdateTimeForOutdatedAlert(HOURLY.getTtlForActiveAlerts()));
        addHourlyAlert(shard, hourlyAlert);

        CpaAutobudgetAlert cpaAlert = new CpaAutobudgetAlert()
                .withCid(cid)
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                .withApcDeviation(0L)
                .withCpaDeviation(0L)
                .withLastUpdate(getLastUpdateTimeForOutdatedAlert(CPA.getTtlForActiveAlerts()));
        addCpaAlert(shard, cpaAlert);
    }

    /**
     * Добавляет в базу активные часовой и CPA алерты, которые не должны быть удалены
     */
    void addNotOutdatedActiveAlerts(int shard, long cid) {
        HourlyAutobudgetAlert hourlyAlert = new HourlyAutobudgetAlert()
                .withCid(cid)
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                .withLastUpdate(LocalDateTime.now());
        addHourlyAlert(shard, hourlyAlert);

        CpaAutobudgetAlert cpaAlert = new CpaAutobudgetAlert()
                .withCid(cid)
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                .withApcDeviation(0L)
                .withCpaDeviation(0L)
                .withLastUpdate(LocalDateTime.now());
        addCpaAlert(shard, cpaAlert);
    }

    /**
     * Добавляет в базу не активные часовой и CPA алерты, которые не должны быть удалены
     */
    void addNotActiveOutdatedAlerts(int shard, long cid, AutobudgetCommonAlertStatus status) {
        if (status.equals(AutobudgetCommonAlertStatus.ACTIVE)) {
            throw new IllegalArgumentException("метод предназначен для не активного статуса");
        }

        HourlyAutobudgetAlert hourlyAlert = new HourlyAutobudgetAlert()
                .withCid(cid)
                .withStatus(status)
                .withLastUpdate(getLastUpdateTimeForOutdatedAlert(HOURLY.getTtlForActiveAlerts()));
        addHourlyAlert(shard, hourlyAlert);

        CpaAutobudgetAlert cpaAlert = new CpaAutobudgetAlert()
                .withCid(cid)
                .withStatus(status)
                .withApcDeviation(0L)
                .withCpaDeviation(0L)
                .withLastUpdate(getLastUpdateTimeForOutdatedAlert(CPA.getTtlForActiveAlerts()));
        addCpaAlert(shard, cpaAlert);
    }
}
