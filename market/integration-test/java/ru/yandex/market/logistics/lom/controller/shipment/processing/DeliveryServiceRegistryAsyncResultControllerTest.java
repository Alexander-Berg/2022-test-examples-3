package ru.yandex.market.logistics.lom.controller.shipment.processing;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.jobs.model.QueueType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@ParametersAreNonnullByDefault
@DisplayName("RegistryAsyncResultController пуш ответа от LGW")
class DeliveryServiceRegistryAsyncResultControllerTest extends AbstractRegistryAsyncResultControllerTest {
    @Nonnull
    @Override
    ResultActions performCall(String method, String requestFileName) throws Exception {
        return mockMvc.perform(
            put("/registries/ds/" + method)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestFileName))
        );
    }

    @Nonnull
    @Override
    QueueType getQueueType() {
        return QueueType.DELIVERY_SERVICE_CREATE_REGISTRY_EXTERNAL;
    }
}
