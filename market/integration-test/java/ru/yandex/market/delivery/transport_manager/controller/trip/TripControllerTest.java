package ru.yandex.market.delivery.transport_manager.controller.trip;

import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;

class TripControllerTest extends AbstractContextualTest {
    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/new_units_1_to_2.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/after/insert_transportation_non_existing_direction.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    @SneakyThrows
    void modifyIncludedOutbouds() {
        mockMvc.perform(
                post("/trip/TMT1/included-outbounds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent("controller/trip/modify_included_outbounds.json"))
            )
            .andExpect(status().isOk())
            .andExpect(noContent());
    }
    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/new_units_1_to_2_overflow.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/insert_transportation_full_example.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    @SneakyThrows
    void modifyIncludedOutboudOverflow() {
        mockMvc.perform(
                post("/trip/TMT1/included-outbounds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent("controller/trip/modify_included_outbounds.json"))
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().string(
                "{\"message\":\"Не могу добавить поставку к рейсу 1: превышена паллетовместимость "
                    + "машины в точке 3 (TMU3)! Максимум 5, пытаемся положить 8.\"}"
            ));
    }


    @ParameterizedTest
    @MethodSource("modifyIncludedOutboudsBadRequests")
    @SneakyThrows
    void modifyIncludedOutboudsNullFields(String requestJson) {
        mockMvc.perform(
                post("/trip/TMT1/included-outbounds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent(requestJson))
            )
            .andExpect(status().isBadRequest());
    }

    public static Stream<Arguments> modifyIncludedOutboudsBadRequests() {
        return Stream.of(
            Arguments.of("controller/trip/modify_included_outbounds_null_trip.json"),
            Arguments.of("controller/trip/modify_included_outbounds_null_outbound_ids.json")
        );
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/insert_transportation_registry_example.xml",
    })
    @Test
    @SneakyThrows
    void getOutboundIdsInTrips() {
        mockMvc.perform(
            post("/trip/trip-ids-by-cargo-units")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/trip/request/trip_id_by_cargo_units.json"))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/trip/response/trip_id_by_cargo_units.json"));
    }
}
