package ru.yandex.market.tpl.internal.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.tpl.internal.controller.dropoff.DropoffCargoController;
import ru.yandex.market.tpl.internal.controller.internal.LogisticApiController;
import ru.yandex.market.tpl.internal.controller.internal.TrackingController;
import ru.yandex.market.tpl.internal.controller.manual.ManualRoutingController;
import ru.yandex.market.tpl.internal.controller.partner.PartnerDropshipController;
import ru.yandex.market.tpl.internal.controller.partner.PartnerFilterController;
import ru.yandex.market.tpl.internal.controller.partner.PartnerOrderController;
import ru.yandex.market.tpl.internal.controller.partner.PartnerVehicleController;

@Configuration
@Import({
        ManualRoutingController.class,
        DropoffCargoController.class,
        TrackingController.class,
        PartnerFilterController.class,
        PartnerDropshipController.class,
        PartnerOrderController.class,
        PartnerVehicleController.class,
        LogisticApiController.class,
})
@ComponentScan(basePackages = {
        "ru.yandex.market.tpl.core.mvc",
})
public class TplTestIntWebConfiguration {
}
