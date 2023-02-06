package ru.yandex.market.logistics.management.controller.admin.partnerRoute;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.getGrid;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Получение таблицы магистралей партнеров")
@DatabaseSetup("/data/controller/admin/partnerRoute/before/prepare_data.xml")
class AdminPartnerRouteControllerGetGridTest extends AbstractContextualTest {

    @DisplayName("ReadOnly mode - Успешно")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getGridArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_ONLY)
    void getGridSuccessReadOnly(
        @SuppressWarnings("unused") String caseName,
        Map<String, String> queryParams,
        String responsePathReadOnlyMode,
        @SuppressWarnings("unused") String responsePathReadWriteMode
    ) throws Exception {
        mockMvc.perform(getGrid().params(toParams(queryParams)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePathReadOnlyMode));
    }

    @DisplayName("ReadWrite mode - Успешно")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getGridArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void getGridSuccessReadWrite(
        @SuppressWarnings("unused") String caseName,
        Map<String, String> queryParams,
        @SuppressWarnings("unused") String responsePathReadOnlyMode,
        String responsePathReadWriteMode
    ) throws Exception {
        mockMvc.perform(getGrid().param("entityId", "1").params(toParams(queryParams)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePathReadWriteMode));
    }

    @Nonnull
    private static Stream<Arguments> getGridArguments() {
        return Stream.of(
            Arguments.of(
                "Без фильтрации",
                Map.of(),
                "data/controller/admin/partnerRoute/response/search_no_filter.json",
                "data/controller/admin/partnerRoute/response/search_no_filter_edit.json"
            ),
            Arguments.of(
                "По идентификатору партнера",
                Map.of("partner", "3000"),
                "data/controller/admin/partnerRoute/response/search_by_partner.json",
                "data/controller/admin/partnerRoute/response/search_by_partner_edit.json"
            ),
            Arguments.of(
                "По региону отправления",
                Map.of("locationFrom", "162"),
                "data/controller/admin/partnerRoute/response/search_by_location_from.json",
                "data/controller/admin/partnerRoute/response/search_by_location_from_edit.json"
            ),
            Arguments.of(
                "По региону прибытия",
                Map.of("locationTo", "164"),
                "data/controller/admin/partnerRoute/response/search_by_location_to.json",
                "data/controller/admin/partnerRoute/response/search_by_location_to_edit.json"
            ),
            Arguments.of(
                "По ограничению ВГХ",
                Map.of("korobyteRestriction", "1"),
                "data/controller/admin/partnerRoute/response/search_by_korobyte_restriction.json",
                "data/controller/admin/partnerRoute/response/search_by_korobyte_restriction_edit.json"
            )
        );
    }
}
