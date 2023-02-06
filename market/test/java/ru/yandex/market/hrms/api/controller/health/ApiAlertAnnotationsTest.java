package ru.yandex.market.hrms.api.controller.health;

import java.util.Set;

import ru.yandex.market.hrms.core.health.AlertAnnotationsTestBase;

public class ApiAlertAnnotationsTest extends AlertAnnotationsTestBase {
    public ApiAlertAnnotationsTest() {
        super(Set.of(), "ru.yandex.market.hrms.api.controller");
    }
}
