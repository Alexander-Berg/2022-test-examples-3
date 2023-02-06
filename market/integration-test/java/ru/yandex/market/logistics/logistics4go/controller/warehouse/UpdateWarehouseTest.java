package ru.yandex.market.logistics.logistics4go.controller.warehouse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4go.TestUtils;
import ru.yandex.market.logistics.logistics4go.client.api.WarehousesApi;
import ru.yandex.market.logistics.logistics4go.client.model.ErrorType;
import ru.yandex.market.logistics.logistics4go.client.model.NotFoundError;
import ru.yandex.market.logistics.logistics4go.client.model.ResourceType;
import ru.yandex.market.logistics.logistics4go.client.model.UpdateWarehouseRequest;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationError;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationViolation;
import ru.yandex.market.logistics.logistics4go.client.model.WarehouseResponse;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointUpdateRequest;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Обновление склада по его идентификатору")
@DatabaseSetup("/controller/warehouse/common/sender.xml")
@ParametersAreNonnullByDefault
class UpdateWarehouseTest extends AbstractIntegrationTest {
    private static final String WAREHOUSE_EXTERNAL_ID = "external-1000";
    private static final long PARTNER_ID = 100;
    private static final long LOGISTICS_POINT_ID = 101;
    private static final String FACTORY_SUFFIX = "1";

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Обновление склада, только необходимые поля заполнены")
    void updateRequiredFields() throws Exception {
        UpdateWarehouseFactory warehouseFactory = new UpdateWarehouseFactory(
            WAREHOUSE_EXTERNAL_ID,
            PARTNER_ID,
            FACTORY_SUFFIX,
            true
        );

        try (AutoCloseable verify = mockLms(warehouseFactory)) {
            WarehouseResponse response = updateWarehouse(warehouseFactory.updateWarehouseRequest())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            assertResponse(warehouseFactory, response);
        }
    }

    @Test
    @DisplayName("Обновление склада, все поля заполнены")
    void createAllFields() throws Exception {
        UpdateWarehouseFactory warehouseFactory = new UpdateWarehouseFactory(
            WAREHOUSE_EXTERNAL_ID,
            PARTNER_ID,
            FACTORY_SUFFIX,
            false
        );

        try (AutoCloseable verify = mockLms(warehouseFactory)) {
            WarehouseResponse response = updateWarehouse(warehouseFactory.updateWarehouseRequest())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            assertResponse(warehouseFactory, response);
        }
    }

    @Test
    @DisplayName("Обновление несуществующего в LMS склада")
    void warehouseNotFound() {
        UpdateWarehouseFactory warehouseFactory = new UpdateWarehouseFactory(
            WAREHOUSE_EXTERNAL_ID,
            PARTNER_ID,
            FACTORY_SUFFIX,
            false
        );

        when(lmsClient.getLogisticsPoint(LOGISTICS_POINT_ID)).thenReturn(Optional.empty());

        NotFoundError error = updateWarehouse(warehouseFactory.updateWarehouseRequest())
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error).isEqualTo(
            new NotFoundError()
                .code(ErrorType.RESOURCE_NOT_FOUND)
                .addIdsItem(LOGISTICS_POINT_ID)
                .resourceType(ResourceType.WAREHOUSE)
                .message("Failed to find WAREHOUSE with ids [" + LOGISTICS_POINT_ID + "]")
        );

        verify(lmsClient).getLogisticsPoint(LOGISTICS_POINT_ID);
    }

    @Test
    @DisplayName("Обновление склада, для которого нет sender")
    void senderNotFound() {
        UpdateWarehouseFactory warehouseFactory = new UpdateWarehouseFactory(
            WAREHOUSE_EXTERNAL_ID,
            PARTNER_ID + 1,
            FACTORY_SUFFIX,
            true
        );

        LogisticsPointResponse logisticsPointResponse = warehouseFactory.logisticsPointResponse(LOGISTICS_POINT_ID);
        when(lmsClient.getLogisticsPoint(LOGISTICS_POINT_ID)).thenReturn(Optional.of(logisticsPointResponse));

        NotFoundError error = updateWarehouse(warehouseFactory.updateWarehouseRequest())
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error).isEqualTo(
            new NotFoundError()
                .code(ErrorType.RESOURCE_NOT_FOUND)
                .addIdsItem(LOGISTICS_POINT_ID)
                .resourceType(ResourceType.WAREHOUSE)
                .message("Failed to find WAREHOUSE with ids [" + LOGISTICS_POINT_ID + "]")
        );

        verify(lmsClient).getLogisticsPoint(LOGISTICS_POINT_ID);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса на обновление")
    void requestValidation(
        @SuppressWarnings("unused") String displayName,
        UpdateWarehouseRequest request,
        ValidationViolation violation
    ) {
        ValidationError response = updateWarehouse(request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(response.getErrors())
            .isEqualTo(List.of(violation));
    }

    @Nonnull
    static Stream<Arguments> requestValidation() {
        UpdateWarehouseFactory warehouseFactory = new UpdateWarehouseFactory(
            WAREHOUSE_EXTERNAL_ID,
            PARTNER_ID,
            FACTORY_SUFFIX,
            false
        );

        return Stream.of(
            TestUtils.notNullValidation(warehouseFactory.updateWarehouseRequest(), "name"),
            TestUtils.notNullValidation(warehouseFactory.updateWarehouseRequest(), "schedule"),
            TestUtils.notNullValidation(warehouseFactory.updateWarehouseRequest(), "contact.name"),
            TestUtils.notNullValidation(warehouseFactory.updateWarehouseRequest(), "contact.name.firstName"),
            TestUtils.notNullValidation(warehouseFactory.updateWarehouseRequest(), "contact.name.lastName"),
            TestUtils.notNullValidation(warehouseFactory.updateWarehouseRequest(), "contact.phone"),
            TestUtils.notNullValidation(warehouseFactory.updateWarehouseRequest(), "contact.phone.number"),
            TestUtils.getValidationViolationArguments(
                "Пустое расписание",
                warehouseFactory.updateWarehouseRequest(),
                "schedule",
                List.of(),
                "size must be between 1 and 100"
            ),
            getScheduleArguments(
                "Некорректный номер дня",
                warehouseFactory.updateWarehouseRequest(),
                List.of(WarehouseFactory.scheduleDay(0)),
                "day",
                "must be greater than or equal to 1"
            ),
            getScheduleArguments(
                "Отсутствует schedule[0].timeFrom",
                warehouseFactory.updateWarehouseRequest(),
                List.of(WarehouseFactory.scheduleDay(1).timeFrom(null)),
                "timeFrom",
                "must not be null"
            ),
            getScheduleArguments(
                "Отсутствует schedule[0].timeTo",
                warehouseFactory.updateWarehouseRequest(),
                List.of(WarehouseFactory.scheduleDay(1).timeTo(null)),
                "timeTo",
                "must not be null"
            )
        );
    }

    static Arguments getScheduleArguments(
        String displayName,
        UpdateWarehouseRequest source,
        @Nullable Object value,
        String field,
        String message
    ) {
        TestUtils.setter("schedule", value)
            .accept(source);
        return Arguments.of(
            displayName,
            source,
            new ValidationViolation().field("schedule[0]." + field).message(message)
        );
    }

    @Nonnull
    private AutoCloseable mockLms(UpdateWarehouseFactory warehouseFactory) {
        LogisticsPointResponse logisticsPointResponse = warehouseFactory.logisticsPointResponse(LOGISTICS_POINT_ID);
        when(lmsClient.getLogisticsPoint(LOGISTICS_POINT_ID)).thenReturn(Optional.of(logisticsPointResponse));

        LogisticsPointUpdateRequest updateRequest = warehouseFactory.logisticsPointUpdateRequest();
        LogisticsPointResponse updatedResponse = warehouseFactory.logisticsPointResponse(LOGISTICS_POINT_ID);
        when(lmsClient.updateLogisticsPoint(LOGISTICS_POINT_ID, updateRequest))
            .thenReturn(updatedResponse);

        return () -> {
            verify(lmsClient).getLogisticsPoint(LOGISTICS_POINT_ID);
            verify(lmsClient).updateLogisticsPoint(LOGISTICS_POINT_ID, updateRequest);
        };
    }

    @Nonnull
    private WarehousesApi.UpdateWarehouseOper updateWarehouse(UpdateWarehouseRequest request) {
        return apiClient.warehouses().updateWarehouse().warehouseIdPath(LOGISTICS_POINT_ID).body(request);
    }

    private void assertResponse(UpdateWarehouseFactory warehouseFactory, WarehouseResponse response) {
        WarehouseResponse expected = warehouseFactory.warehouseResponse(LOGISTICS_POINT_ID);
        softly.assertThat(response)
            .isNotNull()
            .isEqualTo(expected);
    }
}
