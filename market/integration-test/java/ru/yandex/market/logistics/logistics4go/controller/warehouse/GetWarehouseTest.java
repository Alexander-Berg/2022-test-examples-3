package ru.yandex.market.logistics.logistics4go.controller.warehouse;

import java.util.Optional;

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
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Получение склада по его идентификатору")
@DatabaseSetup("/controller/warehouse/common/sender.xml")
@ParametersAreNonnullByDefault
class GetWarehouseTest extends AbstractIntegrationTest {
    private static final String WAREHOUSE_EXTERNAL_ID = "external-1000";
    private static final long PARTNER_ID = 100;
    private static final long LOGISTICS_POINT_ID = 101;
    private static final String FACTORY_SUFFIX = "1";

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verify(lmsClient).getLogisticsPoint(LOGISTICS_POINT_ID);
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Получение склада")
    void success() {
        WarehouseFactory warehouseFactory = new WarehouseFactory(
            WAREHOUSE_EXTERNAL_ID,
            PARTNER_ID,
            FACTORY_SUFFIX,
            true
        );

        LogisticsPointResponse logisticsPointResponse = warehouseFactory.logisticsPointResponse(LOGISTICS_POINT_ID);
        when(lmsClient.getLogisticsPoint(LOGISTICS_POINT_ID)).thenReturn(Optional.of(logisticsPointResponse));

        WarehouseResponse response = getWarehouse()
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertResponse(warehouseFactory, response);
    }

    @Test
    @DisplayName("Получение несуществующего в LMS склада")
    void warehouseNotFound() {
        when(lmsClient.getLogisticsPoint(LOGISTICS_POINT_ID)).thenReturn(Optional.empty());

        NotFoundError error = getWarehouse()
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error).isEqualTo(
            new NotFoundError()
                .code(ErrorType.RESOURCE_NOT_FOUND)
                .addIdsItem(LOGISTICS_POINT_ID)
                .resourceType(ResourceType.WAREHOUSE)
                .message("Failed to find WAREHOUSE with ids [" + LOGISTICS_POINT_ID + "]")
        );
    }

    @Test
    @DisplayName("Получение склада, для которого нет sender")
    void senderNotFound() {
        WarehouseFactory warehouseFactory = new WarehouseFactory(
            WAREHOUSE_EXTERNAL_ID,
            PARTNER_ID + 1,
            FACTORY_SUFFIX,
            true
        );

        LogisticsPointResponse logisticsPointResponse = warehouseFactory.logisticsPointResponse(LOGISTICS_POINT_ID);
        when(lmsClient.getLogisticsPoint(LOGISTICS_POINT_ID)).thenReturn(Optional.of(logisticsPointResponse));

        NotFoundError error = getWarehouse()
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error).isEqualTo(
            new NotFoundError()
                .code(ErrorType.RESOURCE_NOT_FOUND)
                .addIdsItem(LOGISTICS_POINT_ID)
                .resourceType(ResourceType.WAREHOUSE)
                .message("Failed to find WAREHOUSE with ids [" + LOGISTICS_POINT_ID + "]")
        );
    }

    private WarehousesApi.GetWarehouseOper getWarehouse() {
        return apiClient.warehouses().getWarehouse().warehouseIdPath(LOGISTICS_POINT_ID);
    }

    private void assertResponse(WarehouseFactory warehouseFactory, WarehouseResponse response) {
        WarehouseResponse expected = warehouseFactory.warehouseResponse(LOGISTICS_POINT_ID);
        softly.assertThat(response)
            .isNotNull()
            .isEqualTo(expected);
    }
}
