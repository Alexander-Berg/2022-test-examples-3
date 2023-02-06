package ru.yandex.direct.core.entity.autobudget.repository;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetCommonAlertStatus;
import ru.yandex.direct.dbschema.ppc.enums.AutobudgetAlertsStatus;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class AutobudgetHourlyAlertStatusMappingTest {

    @Parameterized.Parameter
    public AutobudgetAlertsStatus hourlyAlertStatus;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<AutobudgetAlertsStatus> data() {
        return Arrays.asList(AutobudgetAlertsStatus.values());
    }

    @Test
    public void checkHourlyAlertsStatusFromDbConvertToCommonStatus() {
        AutobudgetCommonAlertStatus commonAlertStatus =
                AutobudgetCommonAlertStatus.valueOf(hourlyAlertStatus.getLiteral().toUpperCase());
        assertThat("Конвертация статуса из формата базы должна быть однозначной",
                AutobudgetMapping.hourlyAlertsStatusFromDb(hourlyAlertStatus), equalTo(commonAlertStatus));
    }
}
