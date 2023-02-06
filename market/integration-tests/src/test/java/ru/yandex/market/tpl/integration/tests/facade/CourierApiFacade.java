package ru.yandex.market.tpl.integration.tests.facade;

import java.math.BigDecimal;

import io.qameta.allure.Step;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.user.partner.PartnerUserDto;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserRoutingPropertiesDto;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.client.PartnerApiClient;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.service.Courier;
import ru.yandex.market.tpl.integration.tests.service.CourierPool;

@Component
@RequiredArgsConstructor
public class CourierApiFacade extends BaseFacade {
    private final PartnerApiClient partnerApi;
    private final ManualApiClient manualApiClient;
    private final CourierPool courierPool;

    public void createCourierWithSchedule() {
        createCourierWithSchedule(getConfig().isRecipientCallEnabled(), getConfig().isDeliveryPhotoEnabled());
    }

    @Step("Создание курьера и расписания")
    public synchronized void createCourierWithSchedule(boolean recipientCallEnabled, boolean deliveryPhotoEnabled) {
        manualApiClient.updateCaches();
        Courier courier = courierPool.resolveFreeCourier();
        getContext().setCourierTkn(courier.getTkn());
        PartnerUserRoutingPropertiesDto routingProperties = new PartnerUserRoutingPropertiesDto();
        routingProperties.setDeliveryPhotoEnabled(deliveryPhotoEnabled);
        routingProperties.setRerouteEnabled(false);

        routingProperties.setFootMultiplier(BigDecimal.ONE);
        routingProperties.setFootSharedServiceMultiplier(BigDecimal.ONE);
        routingProperties.setFootServiceMultiplier(BigDecimal.ONE);
        routingProperties.setFootEnabled(true);

        routingProperties.setCarMultiplier(BigDecimal.ONE);
        routingProperties.setCarSharedServiceMultiplier(BigDecimal.ONE);
        routingProperties.setCarServiceMultiplier(BigDecimal.ONE);
        routingProperties.setCarEnabled(true);

        PartnerUserDto partnerUserDto = partnerApi.createCourier(courier.getEmail(), recipientCallEnabled,
                routingProperties);
        AutoTestContextHolder.getContext().setUserId(partnerUserDto.getId());
        AutoTestContextHolder.getContext().setUid(partnerUserDto.getUid());
        partnerApi.createCourierSchedule(partnerUserDto.getId());
    }
}
