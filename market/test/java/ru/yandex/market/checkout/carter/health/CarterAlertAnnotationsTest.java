package ru.yandex.market.checkout.carter.health;

import java.util.Set;

import ru.yandex.market.checkout.carter.web.CarterTaskHealthController;
import ru.yandex.market.checkout.checkouter.health.AlertAnnotationsTestBase;

public class CarterAlertAnnotationsTest extends AlertAnnotationsTestBase {

    public CarterAlertAnnotationsTest() {
        super(Set.of(CarterTaskHealthController.class), "ru.yandex.market.checkout.carter.web");
    }
}
