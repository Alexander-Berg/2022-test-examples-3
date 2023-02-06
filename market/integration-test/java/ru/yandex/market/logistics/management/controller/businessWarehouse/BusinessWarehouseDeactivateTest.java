package ru.yandex.market.logistics.management.controller.businessWarehouse;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.logbroker.EventDto;
import ru.yandex.market.logistics.management.queue.producer.LogbrokerEventTaskProducer;
import ru.yandex.market.logistics.management.util.TestableClock;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;

@DisplayName("Деактивация бизнес-склада")
@DatabaseSetup("/data/controller/businessWarehouse/deactivate/prepare.xml")
class BusinessWarehouseDeactivateTest extends AbstractContextualTest {
    @Autowired
    private LogbrokerEventTaskProducer logbrokerEventTaskProducer;
    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        doNothing().when(logbrokerEventTaskProducer).produceTask(any());
        clock.setFixed(Instant.parse("2022-03-05T13:45:00Z"), ZoneId.systemDefault());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/deactivate/after/active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех - активный склад")
    void deactivateActiveBusinessWarehouseWithSettings() throws Exception {
        performDeactivate(1L).andExpect(status().isOk());

        checkLogbrokerEvent("data/controller/businessWarehouse/logbrokerEvent/deactivate_active_warehouse.json");
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/deactivate/after/inactive.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех - неактивный склад")
    void deactivateInactiveBusinessWarehouseWithSettings() throws Exception {
        performDeactivate(2L).andExpect(status().isOk());

        checkLogbrokerEvent("data/controller/businessWarehouse/logbrokerEvent/deactivate_inactive_warehouse.json");
        checkBuildWarehouseSegmentTask(2L);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/deactivate/after/minimal_active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех - активный склад без связок")
    void deactivateActiveBusinessWarehouseWithoutSettings() throws Exception {
        performDeactivate(3L).andExpect(status().isOk());

        checkLogbrokerEvent(
            "data/controller/businessWarehouse/logbrokerEvent/deactivate_active_warehouse_without_settings.json"
        );
        checkBuildWarehouseSegmentTask(3L);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/businessWarehouse/deactivate/after/minimal_inactive.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успех - неактивный склад без связок")
    void deactivateInactiveBusinessWarehouseWithoutSettings() throws Exception {
        performDeactivate(4L).andExpect(status().isOk());

        checkLogbrokerEvent(
            "data/controller/businessWarehouse/logbrokerEvent/deactivate_inactive_warehouse_without_settings.json"
        );
        checkBuildWarehouseSegmentTask(4L);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Попытка деактивировать невалидный склад")
    void invalid(
        @SuppressWarnings("unused") String name,
        Long partnerId,
        String message,
        HttpStatus expectedStatus
    ) throws Exception {
        performDeactivate(partnerId).andExpect(status().is(expectedStatus.value())).andExpect(status().reason(message));
    }

    @Nonnull
    private static Stream<Arguments> invalid() {
        return Stream.of(
            Arguments.of(
                "У партнера нет активного склада",
                5L,
                "Can not find warehouse for partner 5",
                HttpStatus.NOT_FOUND
            ),
            Arguments.of(
                "У партнера нет складов",
                6L,
                "Can not find warehouse for partner 6",
                HttpStatus.NOT_FOUND
            ),
            Arguments.of(
                "Партнер невалидного типа",
                7L,
                "Invalid typ XDOC of partner with id 7",
                HttpStatus.BAD_REQUEST
            ),
            Arguments.of(
                "У партнера несколько активных складов",
                8L,
                "Partner 8 has more than one active warehouse",
                HttpStatus.BAD_REQUEST
            ),
            Arguments.of(
                "Партнер не существует",
                9L,
                "Can't find Partner with id=9",
                HttpStatus.NOT_FOUND
            )
        );
    }

    @Nonnull
    private ResultActions performDeactivate(Long partnerId) throws Exception {
        return mockMvc.perform(
            put("/externalApi/business-warehouse/{partnerId}/deactivate", partnerId)
        );
    }

    private void checkLogbrokerEvent(String jsonPath) throws IOException {
        ArgumentCaptor<EventDto> argumentCaptor =
            ArgumentCaptor.forClass(EventDto.class);
        verify(logbrokerEventTaskProducer).produceTask(argumentCaptor.capture());
        assertThatJson(argumentCaptor.getValue())
            .isEqualTo(objectMapper.readValue(pathToJson(jsonPath), EventDto.class));
    }
}
