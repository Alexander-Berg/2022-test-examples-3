package ru.yandex.market.logistics.management.controller.admin.logisticsPoint;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.ActionDto;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_EDIT;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.SLUG_LOGISTICS_POINT_ACTIVATE;

@DisplayName("Активация логистических точек")
@ParametersAreNonnullByDefault
@DatabaseSetup("/data/controller/admin/logisticsPoint/before/prepare_data.xml")
class AdminLogisticsPointActivateTest extends AbstractContextualTest {
    @DisplayName("Неавторизованный пользователь")
    @Test
    void activateLogisticsPointsUnauthorized() throws Exception {
        activate().andExpect(status().isUnauthorized());
    }

    @DisplayName("У пользователя нет прав на вызов метода")
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void activateLogisticsPointsForbidden() throws Exception {
        activate().andExpect(status().isForbidden());
    }

    @DisplayName("Успешная активация логистических точек")
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsPoint/after/activate.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateLogisticsPoints() throws Exception {
        activate().andExpect(status().isOk());
        checkBuildWarehouseSegmentTask(1L);
    }

    @DisplayName("Ошибка при активации заменённой точки")
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsPoint/after/activate.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateChangedLogisticsPoint() throws Exception {
        activate(Set.of(1L, 2L, 5L, 6L))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(
                "Точки [5, 6] не могут быть активированы, так как были заменены. Информация о заменах: {5=7, 6=7}"
            ));
        checkBuildWarehouseSegmentTask(1L);
    }

    @DisplayName("Ошибки валидации")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    @MethodSource("validationSource")
    void validation(
        @SuppressWarnings("unused") String displayName,
        @Nullable Set<Long> ids,
        String objectName,
        String field,
        String code
    ) throws Exception {
        activate(ids)
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
                "actionDto",
                "ids",
                "NotEmpty"
            ),
            Arguments.of(
                "Пустая коллекция идентификаторов",
                Set.of(),
                "actionDto",
                "ids",
                "NotEmpty"
            ),
            Arguments.of(
                "Коллекция идентификаторов содержит null",
                Sets.newHashSet(1L, null),
                "actionDto",
                "ids[]",
                "NotNull"
            )
        );
    }

    @Nonnull
    private ResultActions activate() throws Exception {
        return activate(Set.of(1L, 2L));
    }

    @Nonnull
    private ResultActions activate(@Nullable Set<Long> ids) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/logistics-point" + SLUG_LOGISTICS_POINT_ACTIVATE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ActionDto().setIds(ids)))
        );
    }
}
