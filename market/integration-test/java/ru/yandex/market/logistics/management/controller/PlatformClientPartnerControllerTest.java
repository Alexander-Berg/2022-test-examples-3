package ru.yandex.market.logistics.management.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.partner.PlatformClientPartnerFilter;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CleanDatabase
@DatabaseSetup("/data/controller/partner/shipmentSettings/prepare_data.xml")
public class PlatformClientPartnerControllerTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("Успешный поиск настроек отгрузки")
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("findShipmentSettingsSource")
    void findShipmentSettingsSuccess(
        @SuppressWarnings("unused") String name,
        PlatformClientPartnerFilter filter,
        String response
    ) throws Exception {
        findShipmentSettings(filter)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(response));
    }

    @Nonnull
    private static Stream<Arguments> findShipmentSettingsSource() {
        return Stream.of(
            Triple.of(
                "Фильтр по partnerIds",
                PlatformClientPartnerFilter.newBuilder().partnerIds(Set.of(4L)).build(),
                "data/controller/shipmentSettings/filter_by_partner_ids.json"
            ),
            Triple.of(
                "Фильтр по всем параметрам",
                PlatformClientPartnerFilter.newBuilder()
                    .partnerIds(Set.of(4L))
                    .platformClientIds(Set.of(3903L))
                    .build(),
                "data/controller/shipmentSettings/filter_by_all.json"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @DisplayName("Невалидный фильтр")
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("invalidFilter")
    void filterValidation(
        @SuppressWarnings("unused") String name,
        PlatformClientPartnerFilter filter,
        String response
    ) throws Exception {
        findShipmentSettings(filter)
            .andExpect(status().isBadRequest())
            .andExpect(TestUtil.testJson(response, Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS));
    }

    @Nonnull
    private static Stream<Arguments> invalidFilter() {
        return Stream.of(
            Triple.of(
                "Пустой фильтр",
                PlatformClientPartnerFilter.newBuilder().build(),
                "data/controller/shipmentSettings/empty_filter.json"
            ),
            Triple.of(
                "Фильтр с null",
                PlatformClientPartnerFilter.newBuilder()
                    .partnerIds(new HashSet<>(Collections.singletonList(null)))
                    .platformClientIds(new HashSet<>(Collections.singletonList(null)))
                    .build(),
                "data/controller/shipmentSettings/filter_with_null.json"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @Nonnull
    private ResultActions findShipmentSettings(PlatformClientPartnerFilter filter) throws Exception {
        return mockMvc.perform(
            put("/externalApi/partner-platform-settings/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(filter))
        );
    }
}
