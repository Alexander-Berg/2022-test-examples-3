package ru.yandex.market.checkout.pushapi.health;

import java.util.Set;

import ru.yandex.market.checkout.checkouter.health.AlertAnnotationsTestBase;
import ru.yandex.market.checkout.pushapi.controller.SvnPushApiController;

public class PushApiAlertAnnotationsTest extends AlertAnnotationsTestBase {

    public PushApiAlertAnnotationsTest() {
        super(Set.of(SvnPushApiController.class), "ru.yandex.market.checkout.pushapi" +
                ".controller");
    }
}
