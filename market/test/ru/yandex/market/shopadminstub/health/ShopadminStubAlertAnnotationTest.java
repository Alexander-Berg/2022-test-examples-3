package ru.yandex.market.shopadminstub.health;

import ru.yandex.market.checkout.checkouter.health.AlertAnnotationsTestBase;
import ru.yandex.market.shopadminstub.controller.MonitoringController;
import ru.yandex.market.shopadminstub.controller.ShopAdminSupportController;
import ru.yandex.market.shopadminstub.controller.SundryController;
import ru.yandex.market.shopadminstub.controller.pushapi.AquaPushApiController;
import ru.yandex.market.shopadminstub.controller.pushapi.AutoPushApiController;
import ru.yandex.market.shopadminstub.controller.pushapi.SvnPushApiController;

import java.util.Set;

public class ShopadminStubAlertAnnotationTest extends AlertAnnotationsTestBase {

    public ShopadminStubAlertAnnotationTest() {
        super(Set.of(
                SundryController.class,
                MonitoringController.class,
                AquaPushApiController.class,
                AutoPushApiController.class,
                SvnPushApiController.class,
                ShopAdminSupportController.class
        ), "ru.yandex.market.shopadminstub.controller");
    }
}
