package ru.yandex.market.tpl.integration.tests.facade;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.order.partner.PartnerRecipientDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerUpdateOrderRequestDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.client.PartnerApiClient;

@Component
@RequiredArgsConstructor
public class PartnerApiFacade extends BaseFacade {
    private final PartnerApiClient partnerApiClient;
    private final ManualApiClient manualApiClient;
    private final PublicApiFacade publicApiFacade;

    public void updateRecipientData() {
        var deliveryTask = publicApiFacade.getDeliveryTask();
        var externalOrderId = deliveryTask.getOrder().getExternalOrderId();
        var orderId = manualApiClient.getOrderId(externalOrderId).getOrderId();
        partnerApiClient.updateOrder(orderId,
                PartnerUpdateOrderRequestDto.builder()
                        .recipient(PartnerRecipientDto.builder()
                                .phone("+79991234567")
                                .name("Test Name")
                                .forename("Name")
                                .surname("Test")
                                .build())
                        .deliveryDate(LocalDate.now(DateTimeUtil.DEFAULT_ZONE_ID))
                        .build());
    }
}
