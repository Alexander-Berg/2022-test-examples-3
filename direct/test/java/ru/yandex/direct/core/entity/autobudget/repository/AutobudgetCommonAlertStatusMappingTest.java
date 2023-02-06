package ru.yandex.direct.core.entity.autobudget.repository;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetCommonAlertStatus;
import ru.yandex.direct.dbschema.ppc.enums.AutobudgetAlertsStatus;
import ru.yandex.direct.dbschema.ppc.enums.AutobudgetCpaAlertsStatus;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class AutobudgetCommonAlertStatusMappingTest {

    @Parameterized.Parameter
    public AutobudgetCommonAlertStatus commonAlertStatus;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<AutobudgetCommonAlertStatus> data() {
        return Arrays.asList(AutobudgetCommonAlertStatus.values());
    }


    @Test
    public void checkCommonStatusConvertToDbHourlyAlertsStatus() {
        AutobudgetAlertsStatus dbStatus = AutobudgetAlertsStatus.valueOf(commonAlertStatus.name().toLowerCase());
        assertThat("Конвертация статуса в формат базы должна быть однозначной",
                AutobudgetMapping.hourlyAlertsStatusToDb(commonAlertStatus), equalTo(dbStatus));
    }

    @Test
    public void checkCommonStatusConvertToDbCpaAlertsStatus() {
        AutobudgetCpaAlertsStatus dbStatus = AutobudgetCpaAlertsStatus.valueOf(commonAlertStatus.name().toLowerCase());
        assertThat("Конвертация статуса в формат базы должна быть однозначной",
                AutobudgetMapping.cpaAlertsStatusToDb(commonAlertStatus), equalTo(dbStatus));
    }
}
