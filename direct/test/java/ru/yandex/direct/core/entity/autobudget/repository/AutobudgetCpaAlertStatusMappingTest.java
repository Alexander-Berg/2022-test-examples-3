package ru.yandex.direct.core.entity.autobudget.repository;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetCommonAlertStatus;
import ru.yandex.direct.dbschema.ppc.enums.AutobudgetCpaAlertsStatus;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class AutobudgetCpaAlertStatusMappingTest {

    @Parameterized.Parameter
    public AutobudgetCpaAlertsStatus cpaAlertStatus;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<AutobudgetCpaAlertsStatus> data() {
        return Arrays.asList(AutobudgetCpaAlertsStatus.values());
    }

    @Test
    public void checkCpaAlertsStatusFromDbConvertToCommonStatus() {
        AutobudgetCommonAlertStatus commonAlertStatus =
                AutobudgetCommonAlertStatus.valueOf(cpaAlertStatus.getLiteral().toUpperCase());
        assertThat("Конвертация статуса из формата базы должна быть однозначной",
                AutobudgetMapping.cpaAlertsStatusFromDb(cpaAlertStatus), equalTo(commonAlertStatus));
    }
}
