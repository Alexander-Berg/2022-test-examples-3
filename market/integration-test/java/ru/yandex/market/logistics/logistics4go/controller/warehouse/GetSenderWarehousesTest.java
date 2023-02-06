package ru.yandex.market.logistics.logistics4go.controller.warehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4go.client.api.WarehousesApi;
import ru.yandex.market.logistics.logistics4go.client.model.ErrorType;
import ru.yandex.market.logistics.logistics4go.client.model.NotFoundError;
import ru.yandex.market.logistics.logistics4go.client.model.ResourceType;
import ru.yandex.market.logistics.logistics4go.client.model.WarehouseResponse;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;

@DatabaseSetup("/controller/warehouse/common/sender.xml")
@DisplayName("Получение всех складов магазина по его идентификатору")
@ParametersAreNonnullByDefault
class GetSenderWarehousesTest extends AbstractIntegrationTest {
    private static final long SENDER_ID = 1;
    private static final long MISSING_SENDER_ID = 2;
    private static final long WAREHOUSE_EXTERNAL_ID = 1000;
    private static final long PARTNER_ID = 100;
    private static final long LOGISTICS_POINT_ID = 101;
    private static final int FACTORY_SUFFIX = 1;

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Получение всех складов магазина")
    void success() throws Exception {
        try (AutoCloseable verify = mockLms()) {
            List<WarehouseResponse> response = getSenderWarehouses(SENDER_ID)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(response)
                .isNotNull()
                .isEqualTo(warehouses());
        }
    }

    @Test
    @DisplayName("Магазин не найден")
    void senderNotFound() {
        NotFoundError error = getSenderWarehouses(MISSING_SENDER_ID)
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error).isEqualTo(
            new NotFoundError()
                .code(ErrorType.RESOURCE_NOT_FOUND)
                .addIdsItem(MISSING_SENDER_ID)
                .resourceType(ResourceType.SENDER)
                .message("Failed to find SENDER with ids [" + MISSING_SENDER_ID + "]")
        );
    }

    @Nonnull
    private AutoCloseable mockLms() {
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(PARTNER_ID))
            .build();
        when(lmsClient.getLogisticsPoints(filter)).thenReturn(logisticsPoints());

        return () -> verify(lmsClient).getLogisticsPoints(filter);
    }

    @Nonnull
    private WarehousesApi.GetSenderWarehousesOper getSenderWarehouses(long senderId) {
        return apiClient.warehouses().getSenderWarehouses().senderIdPath(senderId);
    }

    @Nonnull
    public List<LogisticsPointResponse> logisticsPoints() {
        int size = 3;
        List<LogisticsPointResponse> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(WarehouseFactory.logisticsPointResponse(
                LOGISTICS_POINT_ID + i,
                WAREHOUSE_EXTERNAL_ID + String.valueOf(i),
                PARTNER_ID,
                String.valueOf(FACTORY_SUFFIX + i),
                false
            ));
        }

        return result;
    }

    @Nonnull
    public List<WarehouseResponse> warehouses() {
        int size = 3;
        List<WarehouseResponse> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(WarehouseFactory.warehouseResponse(
                LOGISTICS_POINT_ID + i,
                WAREHOUSE_EXTERNAL_ID + String.valueOf(i),
                String.valueOf(FACTORY_SUFFIX + i),
                false
            ));
        }

        return result;
    }
}
