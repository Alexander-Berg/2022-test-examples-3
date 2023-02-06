package ru.yandex.autotests.market.stat.conductor;

import ru.yandex.autotests.conductor.PackageInfoProvider;
import ru.yandex.qatools.allure.annotations.Step;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


/**
 * Created by jkt on 02.07.14.
 */
public class ConductorClient {

    @Step("Получаем время последней раскладки пакета {0}")
    public LocalDateTime getLastDeployTimeForPackage(String packageName) {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(PackageInfoProvider.getLastDeployDateOfPackage(packageName, "testing").getMillis()), ZoneId.systemDefault());
    }

}
