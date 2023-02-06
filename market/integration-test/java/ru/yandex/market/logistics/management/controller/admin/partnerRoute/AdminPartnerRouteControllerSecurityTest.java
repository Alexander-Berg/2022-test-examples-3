package ru.yandex.market.logistics.management.controller.admin.partnerRoute;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.RequestBuilder;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.ActionDto;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.create;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.delete;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.deleteMultiple;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.downloadTemplate;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.getDetail;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.getGrid;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.getNew;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.partnerRouteDetailDto;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.partnerRouteNewDto;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.toContent;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.update;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.uploadUpsert;

@DisplayName("Безопасность ручек контроллера расписания магистралей партнеров")
@DatabaseSetup("/data/controller/admin/partnerRoute/before/prepare_data.xml")
class AdminPartnerRouteControllerSecurityTest extends AbstractContextualTest {
    @DisplayName("Неавторизованный пользователь")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getArguments")
    void requestUnauthorized(@SuppressWarnings("unused") String caseName, RequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @DisplayName("Недостаточно прав")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void requestForbidden(@SuppressWarnings("unused") String caseName, RequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Nonnull
    private static Stream<Arguments> getArguments() {
        return Stream.of(
            Arguments.of("getGrid", getGrid()),
            Arguments.of("getDetail", getDetail(1)),
            Arguments.of("getNew", getNew()),
            Arguments.of("create", create().content(toContent(partnerRouteNewDto().build()))),
            Arguments.of("update", update(1).content(toContent(partnerRouteDetailDto().build()))),
            Arguments.of("delete", delete(1)),
            Arguments.of("deleteMultiple", deleteMultiple().content(toContent(new ActionDto().setIds(Set.of(1L, 2L))))),
            Arguments.of("download/template", downloadTemplate()),
            Arguments.of("upload/upsert", uploadUpsert().file(Helper.file()))
        );
    }
}
