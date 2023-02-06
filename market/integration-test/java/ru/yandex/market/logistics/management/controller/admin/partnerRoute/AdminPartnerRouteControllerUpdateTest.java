package ru.yandex.market.logistics.management.controller.admin.partnerRoute;

import java.time.LocalTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.ScheduleDto;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.partnerRouteDetailDto;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.toContent;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.update;

@DisplayName("Обновление расписания магистрали партнера")
@DatabaseSetup("/data/controller/admin/partnerRoute/before/prepare_data.xml")
class AdminPartnerRouteControllerUpdateTest extends AbstractContextualTest {

    private static final long PARTNER_ROUTE_ID = 4000L;
    private static final long NON_EXISTENT_KOROBYTE_RESTRICTION_ID = 9999L;

    @Test
    @DisplayName("Расписание магистрали не найдено")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void updateErrorPartnerRouteNotFound() throws Exception {
        mockMvc.perform(update(1L).content(toContent(partnerRouteDetailDto().build())))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Ограничения ВГХ не найдены")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void updateErrorKorobyteRestrictionNotFound() throws Exception {
        var partnerRouteDetailDto =
            partnerRouteDetailDto().korobyteRestrictionId(NON_EXISTENT_KOROBYTE_RESTRICTION_ID).build();
        mockMvc.perform(update(PARTNER_ROUTE_ID).content(toContent(partnerRouteDetailDto)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Успешно")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRoute/after/update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSuccess() throws Exception {
        var partnerRouteDetailDto = partnerRouteDetailDto()
            .korobyteRestrictionId(3L)
            .pickupInboundSchedule(
                new ScheduleDto()
                    .setMondayFrom(LocalTime.of(1, 0))
                    .setMondayTo(LocalTime.of(2, 0))
            )
            .saturday(true)
            .sunday(true)
            .build();
        var content = objectMapper.writeValueAsString(partnerRouteDetailDto);
        mockMvc.perform(update(PARTNER_ROUTE_ID).content(content))
            .andExpect(status().isOk());

    }
}
