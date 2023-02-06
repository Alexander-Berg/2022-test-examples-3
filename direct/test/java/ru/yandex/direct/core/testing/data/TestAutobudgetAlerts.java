package ru.yandex.direct.core.testing.data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetCommonAlertStatus;
import ru.yandex.direct.core.entity.autobudget.model.AutobudgetHourlyProblem;
import ru.yandex.direct.core.entity.autobudget.model.HourlyAutobudgetAlert;

public final class TestAutobudgetAlerts {
    public static HourlyAutobudgetAlert defaultActiveHourlyAlert(Long campaignId) {
        return new HourlyAutobudgetAlert()
                .withCid(campaignId)
                .withLastUpdate(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .withOverdraft(1234L)
                .withProblems(EnumSet.of(AutobudgetHourlyProblem.MAX_BID_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);
    }
}
