package ru.yandex.market.logistics.management.controller.admin.korobyteRestriction;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.dto.filter.admin.AdminKorobyteRestrictionFilter;
import ru.yandex.market.logistics.management.domain.dto.filter.admin.AdminKorobyteRestrictionFilter.AdminKorobyteRestrictionFilterBuilder;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.getGrid;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/data/controller/admin/korobyteRestrictions/before/setup.xml")
class AdminKorobyteRestrictionControllerGetGridTest extends AbstractContextualAspectValidationTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getGridArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_ONLY)
    void shouldGetGridWithViewMode(
        @SuppressWarnings("unused") String displayName,
        AdminKorobyteRestrictionFilterBuilder filter,
        String responsePathViewMode,
        @SuppressWarnings("unused") String responsePathEditMode
    ) throws Exception {
        performGetGrid(filter.build())
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePathViewMode));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getGridArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void shouldGetGridWithEditMode(
        @SuppressWarnings("unused") String displayName,
        AdminKorobyteRestrictionFilterBuilder filter,
        @SuppressWarnings("unused") String responsePathViewMode,
        String responsePathEditMode
    ) throws Exception {
        performGetGrid(filter.build())
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePathEditMode));
    }

    @Nonnull
    private static Stream<Arguments> getGridArguments() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                emptyFilter(),
                "data/controller/admin/korobyteRestrictions/response/grid_all.json",
                "data/controller/admin/korobyteRestrictions/response/grid_all_edit.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору",
                emptyFilter().korobyteRestrictionId(102L),
                "data/controller/admin/korobyteRestrictions/response/grid_2.json",
                "data/controller/admin/korobyteRestrictions/response/grid_2_edit.json"
            ),
            Arguments.of(
                "Фильтр по уникальному ключу",
                emptyFilter().key("101"),
                "data/controller/admin/korobyteRestrictions/response/grid_1.json",
                "data/controller/admin/korobyteRestrictions/response/grid_1_edit.json"
            ),
            Arguments.of(
                "Фильтр по описанию",
                emptyFilter().description("tHiRd"),
                "data/controller/admin/korobyteRestrictions/response/grid_3.json",
                "data/controller/admin/korobyteRestrictions/response/grid_3_edit.json"
            ),
            Arguments.of(
                "По поисковому запросу (идентификатор)",
                emptyFilter().searchQuery("102"),
                "data/controller/admin/korobyteRestrictions/response/grid_2.json",
                "data/controller/admin/korobyteRestrictions/response/grid_2_edit.json"
            ),
            Arguments.of(
                "По поисковому запросу (ключ)",
                emptyFilter().searchQuery("key_103"),
                "data/controller/admin/korobyteRestrictions/response/grid_3.json",
                "data/controller/admin/korobyteRestrictions/response/grid_3_edit.json"
            )
        );
    }

    @Nonnull
    private static AdminKorobyteRestrictionFilterBuilder emptyFilter() {
        return AdminKorobyteRestrictionFilter.builder();
    }

    @Nonnull
    private ResultActions performGetGrid(@Nullable AdminKorobyteRestrictionFilter filter) throws Exception {
        return mockMvc.perform(getGrid().params(toParams(filter)));
    }
}
