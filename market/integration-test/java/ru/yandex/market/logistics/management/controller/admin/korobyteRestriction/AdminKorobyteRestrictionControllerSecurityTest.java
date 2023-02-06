package ru.yandex.market.logistics.management.controller.admin.korobyteRestriction;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.RequestBuilder;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.KOROBYTE_RESTRICTION_ID_1;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.KOROBYTE_RESTRICTION_ID_2;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.KOROBYTE_RESTRICTION_ID_3;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.create;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.delete;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.getDetail;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.getGrid;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.getNew;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.newDto;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.update;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.updateDto;

class AdminKorobyteRestrictionControllerSecurityTest extends AbstractContextualAspectValidationTest {

    @DisplayName("Неавторизованный пользователь")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("securityArguments")
    void shouldGetUnauthorized(@SuppressWarnings("unused") String caseName, RequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @DisplayName("Недостаточно прав")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("securityArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void shouldGetForbidden(@SuppressWarnings("unused") String caseName, RequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Nonnull
    private static Stream<Arguments> securityArguments() {
        return Stream.of(
            Arguments.of("getGrid", getGrid()),
            Arguments.of("getDetail", getDetail(KOROBYTE_RESTRICTION_ID_1)),
            Arguments.of("getNew", getNew()),
            Arguments.of("create", create(newDto())),
            Arguments.of("update", update(KOROBYTE_RESTRICTION_ID_2, updateDto())),
            Arguments.of("delete", delete(KOROBYTE_RESTRICTION_ID_3))
        );
    }
}
