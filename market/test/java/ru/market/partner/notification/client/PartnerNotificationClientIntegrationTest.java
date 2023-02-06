package ru.market.partner.notification.client;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.partner.notification.client.model.DestinationDTO;
import ru.yandex.market.partner.notification.client.model.SendNotificationRequest;


@Disabled
class PartnerNotificationClientIntegrationTest {

    @Test
    void sendNotification() {
        var client = PartnerNotificationClient.newBuilder()
                .baseUrl("http://localhost:8080")
                .build();

        client.sendNotification(
                new SendNotificationRequest()
                        .data("<![CDATA[<shop-name>The Shop</shop-name>]]>")
                        .typeId(1L)
                        .destination(new DestinationDTO().shopId(1L))
        );
    }
}
