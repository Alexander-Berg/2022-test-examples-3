package ru.yandex.market.replenishment.autoorder.repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.AlertType;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Alert;
import ru.yandex.market.replenishment.autoorder.repository.postgres.AlertsRepository;
public class AlertsRepositoryTest extends FunctionalTest {

    @Autowired
    private AlertsRepository alertsRepository;

    @Test
    @DbUnitDataSet(
            before = "AlertsRepositoryTest_upsert.before.csv",
            after = "AlertsRepositoryTest_upsert.after.csv")
    public void testUpsert() {
        final LocalDateTime updateTime = LocalDateTime.of(2021, 6, 4, 12, 12, 12);
        Arrays.stream(AlertType.values())
                .forEach(alertType ->
                        alertsRepository.upsert(alertType, "message2", updateTime));
    }

    @Test
    @DbUnitDataSet(before = "AlertsRepositoryTest_upsert.before.csv")
    public void testGetAll() {
        final Map<AlertType, Alert> alerts = alertsRepository.getAll(true)
                .stream()
                .collect(Collectors.toMap(Alert::getAlertType, Function.identity()));
        Assert.assertEquals(AlertType.values().length, alerts.size());

        Arrays.stream(AlertType.values())
                .forEach(alertType -> {
                    final Alert alert = alerts.get(alertType);
                    Assert.assertEquals(alertType, alert.getAlertType());
                    Assert.assertEquals("message1", alert.getMessage());
                    Assert.assertEquals(LocalDateTime.of(2021, 6, 4, 3, 54, 27),
                            alert.getUpdateTimestamp());
                });
    }

}
