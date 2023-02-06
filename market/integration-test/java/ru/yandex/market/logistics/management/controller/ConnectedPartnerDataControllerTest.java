package ru.yandex.market.logistics.management.controller;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.partner.ConnectedPartnerDataRequest;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DatabaseSetup("/data/controller/partner/connectedPartnerData/prepare.xml")
public class ConnectedPartnerDataControllerTest extends AbstractContextualTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Получение информации о количестве магазинов, подключенных к точкам сдачи")
    void getConnectedPartnersData() throws Exception {
        mockMvc.perform(
            put("/externalApi/connected-partner-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConnectedPartnerDataRequest(
                    Set.of(1L, 2L, 4L, 5L),
                    Set.of(PartnerType.DROPSHIP)
                )))
        )
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/connectedPartnerData/response/get_connected_partner_data.json"
            ));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("invalidArguments")
    @DisplayName("Невалидный запрос для получения информации о количестве магазинов, подключенных к точкам сдачи")
    void getConnectedPartnersDataInvalid(
        @SuppressWarnings("unused") String name,
        ConnectedPartnerDataRequest request,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
            put("/externalApi/connected-partner-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest())
            .andExpect(testJson(responsePath, Option.IGNORING_EXTRA_FIELDS));
    }

    private static Stream<Arguments> invalidArguments() {
        return Stream.of(
            Arguments.of(
                "Нет коллекции идентификаторов",
                new ConnectedPartnerDataRequest(null, Set.of(PartnerType.DROPSHIP)),
                "data/controller/connectedPartnerData/response/get_connected_partner_data_null_point_ids.json"
            ),
            Arguments.of(
                "Нет коллекции типов партнеров",
                new ConnectedPartnerDataRequest(Set.of(), null),
                "data/controller/connectedPartnerData/response/get_connected_partner_data_null_partner_types.json"
            ),
            Arguments.of(
                "Пустая коллекция типов партнеров",
                new ConnectedPartnerDataRequest(Set.of(1L, 2L), Set.of()),
                "data/controller/connectedPartnerData/response/get_connected_partner_data_empty_partner_types.json"
            ),
            Arguments.of(
                "В коллекции идентификаторов есть null",
                new ConnectedPartnerDataRequest(Collections.singleton(null), Set.of(PartnerType.DROPSHIP)),
                "data/controller/connectedPartnerData/response/get_connected_partner_data_null_in_point_ids.json"
            ),
            Arguments.of(
                "В коллекции типов есть null",
                new ConnectedPartnerDataRequest(Set.of(1L, 2L), Collections.singleton(null)),
                "data/controller/connectedPartnerData/response/get_connected_partner_data_null_in_partner_types.json"
            )
        );
    }
}
