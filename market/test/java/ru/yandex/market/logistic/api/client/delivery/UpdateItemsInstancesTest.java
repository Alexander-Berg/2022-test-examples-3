package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.model.delivery.ItemInstances;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.UpdateItemsInstancesResponse;
import ru.yandex.market.logistic.api.utils.delivery.DtoFactory;

public class UpdateItemsInstancesTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    @DisplayName("Обновление инстансов айтемов: успешное")
    void testUpdateItemsInstancesSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_update_items_instances", PARTNER_URL);

        ResourceId orderId = DtoFactory.createOrderId();

        ItemInstances itemInstances =
            getObjectFromXml("fixture/entities/delivery/items_instances.xml", ItemInstances.class);

        UpdateItemsInstancesResponse response = deliveryServiceClient.updateItemsInstances(
            orderId,
            java.util.Collections.singletonList(
                itemInstances
            ),
            getPartnerProperties()
        );

        assertions.assertThat(response)
            .as("Asserting that response is not null")
            .isNotNull();
    }
}
