package ru.yandex.market.logistics.lom.controller.shipment.processing;

import javax.annotation.Nonnull;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.jobs.model.QueueType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class FulfillmentShipmentAsyncResultSuccessControllerTest extends AbstractShipmentAsyncResultSuccessControllerTest {
    @Nonnull
    ResultActions performCall(@Nonnull String method, String requestFileName) throws Exception {
        return mockMvc.perform(
            put("/shipments/ff/" + method)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestFileName))
        );
    }

    @Nonnull
    @Override
    QueueType getQueueType() {
        return QueueType.FULFILLMENT_SHIPMENT_CREATION;
    }
}
