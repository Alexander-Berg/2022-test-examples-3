package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.AlertType;
public class AlertsServiceTest extends FunctionalTest {

    @Autowired
    private AlertsService alertsService;

    @Test
    @DbUnitDataSet(before = "AlertsServiceTest.before.csv")
    public void testGetCurrentAlerts() {
        final Optional<String> result = alertsService.setAlertsHandledAndGet();
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("SUPPLIER_NOTIFICATION: message (last at 2021-06-04T03:54:27); " +
                "EMAIL_NOTIFICATION: message (last at 2021-06-04T03:54:27); " + "NOT_CORRECT_RECOMMENDATIONS: message" +
                " (last at 2021-06-04T03:54:27)", result.get());
    }

    @Test
    @DbUnitDataSet(before = "AlertsServiceTest.before.csv", after = "AlertsServiceTest.after.csv")
    public void testPushAlerts() {
       setTestTime(LocalDateTime.of(2021, 6, 7, 0, 0));
        Arrays.stream(AlertType.values())
                .forEach(alertType -> alertsService.pushAlert(alertType, "error"));
    }

}
