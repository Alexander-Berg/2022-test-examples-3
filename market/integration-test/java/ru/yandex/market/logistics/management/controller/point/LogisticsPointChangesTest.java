package ru.yandex.market.logistics.management.controller.point;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointChangesFilter;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Замены логистических точек")
@DatabaseSetup("/data/controller/point/before/logistics_point_changes_prepare_data.xml")
class LogisticsPointChangesTest extends AbstractContextualTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Получение мапы замен логистических точек")
    void getLogisticsPointChanges() throws Exception {
        getLogisticsPointChanges(Set.of(1L, 2L, 5L))
            .andExpect(status().isOk())
            .andExpect(json().isEqualTo(Map.of(1L, 3L, 2L, 3L)));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validationSource")
    @DisplayName("Ошибки валидации")
    void validation(
        String displayName,
        Set<Long> oldLogisticsPointIds,
        String objectName,
        String field,
        String code
    ) throws Exception {
        getLogisticsPointChanges(oldLogisticsPointIds)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].objectName").value(objectName))
            .andExpect(jsonPath("errors[0].field").value(field))
            .andExpect(jsonPath("errors[0].code").value(code));
    }

    @Nonnull
    private static Stream<Arguments> validationSource() {
        return Stream.of(
            Arguments.of(
                "Коллекция идентификаторов null",
                null,
                "logisticsPointChangesFilter",
                "oldLogisticsPointIds",
                "NotEmpty"
            ),
            Arguments.of(
                "Пустая коллекция идентификаторов",
                Set.of(),
                "logisticsPointChangesFilter",
                "oldLogisticsPointIds",
                "NotEmpty"
            ),
            Arguments.of(
                "Коллекция идентификаторов содержит null",
                Sets.newHashSet(1L, null),
                "logisticsPointChangesFilter",
                "oldLogisticsPointIds[]",
                "NotNull"
            )
        );
    }

    @Nonnull
    private ResultActions getLogisticsPointChanges(Set<Long> oldLogisticsPointIds) throws Exception {
        LogisticsPointChangesFilter filter = LogisticsPointChangesFilter.builder()
            .oldLogisticsPointIds(oldLogisticsPointIds)
            .build();

        return mockMvc.perform(
            put("/externalApi/logisticsPoints/changes")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsBytes(filter))
        );
    }
}
