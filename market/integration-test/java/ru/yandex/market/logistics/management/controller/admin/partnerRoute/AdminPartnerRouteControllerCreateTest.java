package ru.yandex.market.logistics.management.controller.admin.partnerRoute;

import java.time.LocalTime;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.ScheduleDto;
import ru.yandex.market.logistics.management.domain.dto.front.partnerRoute.PartnerRouteNewDto.PartnerRouteNewDtoBuilder;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.create;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.partnerRouteNewDto;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.toContent;

@DisplayName("Создание расписания магистрали партнера")
@DatabaseSetup("/data/controller/admin/partnerRoute/before/prepare_data.xml")
class AdminPartnerRouteControllerCreateTest extends AbstractContextualTest {

    @DisplayName("Ошибки валидации dto")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getBadRequestDtos")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void createErrorValidation(
        @SuppressWarnings("unused") String caseName,
        PartnerRouteNewDtoBuilder<?, ?> partnerRoute
    ) throws Exception {
        mockMvc.perform(create().content(toContent(partnerRoute.build()))).andExpect(status().isBadRequest());
    }

    @Nonnull
    private static Stream<Arguments> getBadRequestDtos() {
        return Stream.of(
            Arguments.of("locationFrom = null", partnerRouteNewDto().locationFrom(null)),
            Arguments.of("locationTo = null", partnerRouteNewDto().locationTo(null)),
            Arguments.of("partner = null", partnerRouteNewDto().partner(null))
        );
    }

    @Test
    @DisplayName("Регион не найден")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void createErrorRegionNotFound() throws Exception {
        mockMvc.perform(create().content(toContent(partnerRouteNewDto().locationFrom(1).build())))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Партнер не найден")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void createErrorPartnerNotFound() throws Exception {
        mockMvc.perform(create().content(toContent(partnerRouteNewDto().partner(1L).build())))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Ограничение ВГХ не найдено")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void createErrorKorobyteRestrictionNotFound() throws Exception {
        mockMvc.perform(create().content(toContent(partnerRouteNewDto().korobyteRestrictionId(9999L).build())))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Успешно")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRoute/after/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSuccess() throws Exception {
        var partnerRouteNewDto = partnerRouteNewDto()
            .korobyteRestrictionId(1L)
            .pickupInboundSchedule(
                new ScheduleDto()
                    .setMondayFrom(LocalTime.of(1, 0))
                    .setMondayTo(LocalTime.of(2, 0))
            )
            .saturday(true)
            .sunday(true)
            .build();
        var content = objectMapper.writeValueAsString(partnerRouteNewDto);
        mockMvc.perform(create().content(content))
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl("http://localhost/admin/lms/partner-route/1"));
    }
}
