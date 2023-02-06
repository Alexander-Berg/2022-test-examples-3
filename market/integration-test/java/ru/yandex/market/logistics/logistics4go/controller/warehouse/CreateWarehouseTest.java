package ru.yandex.market.logistics.logistics4go.controller.warehouse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
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
import ru.yandex.market.logistics.logistics4go.client.model.ValidationError;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationViolation;
import ru.yandex.market.logistics.logistics4go.client.model.WarehouseDto;
import ru.yandex.market.logistics.logistics4go.client.model.WarehouseResponse;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.CreateWarehouseSegmentRequest;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointCreateRequest;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Создание склада магазина")
@DatabaseSetup("/controller/warehouse/common/sender.xml")
@ParametersAreNonnullByDefault
class CreateWarehouseTest extends AbstractIntegrationTest {
    private static final String WAREHOUSE_EXTERNAL_ID = "external-1000";
    private static final long SENDER_ID = 1;
    private static final long PARTNER_ID = 100;
    private static final long LOGISTICS_POINT_ID = 101;
    private static final long SEGMENT_ID = 11;
    private static final String FACTORY_SUFFIX = "1";

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Создание склада магазина, только необходимые поля заполнены")
    @ExpectedDatabase(
        value = "/controller/warehouse/create/after/sender_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createRequiredFields() throws Exception {
        WarehouseFactory warehouseFactory = new WarehouseFactory(
            WAREHOUSE_EXTERNAL_ID,
            PARTNER_ID,
            FACTORY_SUFFIX,
            true
        );

        try (AutoCloseable verify = mockLms(warehouseFactory)) {
            WarehouseResponse response = createWarehouse(warehouseFactory.createWarehouseRequest())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            assertResponse(warehouseFactory, response);
        }
    }

    @Test
    @DisplayName("Создание склада магазина, все поля заполнены")
    @ExpectedDatabase(
        value = "/controller/warehouse/create/after/sender_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createAllFields() throws Exception {
        WarehouseFactory warehouseFactory = new WarehouseFactory(
            WAREHOUSE_EXTERNAL_ID,
            PARTNER_ID,
            FACTORY_SUFFIX,
            false
        );

        try (AutoCloseable verify = mockLms(warehouseFactory)) {
            WarehouseResponse response = createWarehouse(warehouseFactory.createWarehouseRequest())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            assertResponse(warehouseFactory, response);
        }
    }

    @Test
    @DisplayName("Создание склада магазина с существующим externalId")
    @ExpectedDatabase(
        value = "/controller/warehouse/common/sender.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void existedExternalId() throws Exception {
        WarehouseFactory warehouseFactory = new WarehouseFactory(
            "external-100",
            PARTNER_ID,
            FACTORY_SUFFIX,
            false
        );

        long existingWarehouseId = 100;
        LogisticsPointResponse logisticsPointResponse = warehouseFactory.logisticsPointResponse(existingWarehouseId);
        when(lmsClient.getLogisticsPoint(existingWarehouseId)).thenReturn(Optional.of(logisticsPointResponse));

        WarehouseResponse response = createWarehouse(warehouseFactory.createWarehouseRequest())
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response).isNotNull().isEqualTo(warehouseFactory.warehouseResponse(existingWarehouseId));
        verify(lmsClient).getLogisticsPoint(existingWarehouseId);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса на создание")
    @ExpectedDatabase(
        value = "/controller/warehouse/common/sender.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requestValidation(
        @SuppressWarnings("unused") String displayName,
        WarehouseDto request,
        ValidationViolation violation
    ) {
        ValidationError response = createWarehouse(request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(response.getErrors())
            .isEqualTo(List.of(violation));
    }

    @Nonnull
    static Stream<Arguments> requestValidation() {
        WarehouseFactory warehouseFactory = new WarehouseFactory(
            WAREHOUSE_EXTERNAL_ID,
            PARTNER_ID,
            FACTORY_SUFFIX,
            false
        );

        return Stream.of(
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "externalId"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "name"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "address"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "schedule"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "contact.name"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "contact.name.firstName"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "contact.name.lastName"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "contact.phone"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "contact.phone.number"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "address.geoId"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "address.region"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "address.locality"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "address.street"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "address.house"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "address.postalCode"),
            TestUtils.notNullValidation(warehouseFactory.createWarehouseRequest(), "address.geoId"),
            TestUtils.getValidationViolationArguments(
                "Пустое расписание",
                warehouseFactory.createWarehouseRequest(),
                "schedule",
                List.of(),
                "size must be between 1 and 100"
            ),
            getScheduleArguments(
                "Некорректный номер дня",
                warehouseFactory.createWarehouseRequest(),
                List.of(WarehouseFactory.scheduleDay(0)),
                "day",
                "must be greater than or equal to 1"
            ),
            getScheduleArguments(
                "Отсутствует schedule[0].timeFrom",
                warehouseFactory.createWarehouseRequest(),
                List.of(WarehouseFactory.scheduleDay(1).timeFrom(null)),
                "timeFrom",
                "must not be null"
            ),
            getScheduleArguments(
                "Отсутствует schedule[0].timeTo",
                warehouseFactory.createWarehouseRequest(),
                List.of(WarehouseFactory.scheduleDay(1).timeTo(null)),
                "timeTo",
                "must not be null"
            )
        );
    }

    static Arguments getScheduleArguments(
        String displayName,
        WarehouseDto source,
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
    private AutoCloseable mockLms(WarehouseFactory warehouseFactory) {
        LogisticsPointCreateRequest logisticsPointCreateRequest = warehouseFactory.logisticsPointCreateRequest();
        CreateWarehouseSegmentRequest expectedCreateSegmentRequest = CreateWarehouseSegmentRequest.builder()
            .logisticPointId(LOGISTICS_POINT_ID)
            .build();
        LogisticsPointResponse logisticsPointResponse = warehouseFactory.logisticsPointResponse(LOGISTICS_POINT_ID);
        doReturn(logisticsPointResponse).when(lmsClient).createLogisticsPoint(logisticsPointCreateRequest);
        doReturn(new LogisticSegmentDto().setId(SEGMENT_ID))
            .when(lmsClient).createWarehouseLogisticSegment(expectedCreateSegmentRequest);

        return () -> {
            verify(lmsClient).createLogisticsPoint(logisticsPointCreateRequest);
            verify(lmsClient).createWarehouseLogisticSegment(expectedCreateSegmentRequest);
        };
    }

    @Nonnull
    private WarehousesApi.CreateWarehouseOper createWarehouse(WarehouseDto request) {
        return apiClient.warehouses().createWarehouse().senderIdPath(SENDER_ID).body(request);
    }

    private void assertResponse(WarehouseFactory warehouseFactory, WarehouseResponse response) {
        WarehouseResponse expected = warehouseFactory.warehouseResponse(LOGISTICS_POINT_ID);
        softly.assertThat(response)
            .isNotNull()
            .isEqualTo(expected);
    }
}
