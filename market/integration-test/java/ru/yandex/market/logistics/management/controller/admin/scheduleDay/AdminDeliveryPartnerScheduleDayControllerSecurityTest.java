package ru.yandex.market.logistics.management.controller.admin.scheduleDay;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.RequestBuilder;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.getDetail;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.getGrid;

@DisplayName("Безопасность ручек контроллера интервалов доставки в конечных точках")
@DatabaseSetup("/data/controller/admin/partnerRoute/before/prepare_data.xml")
public class AdminDeliveryPartnerScheduleDayControllerSecurityTest extends AbstractContextualTest {
    @DisplayName("Неавторизованный пользователь")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getArguments")
    void requestUnauthorized(@SuppressWarnings("unused") String caseName, RequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @DisplayName("Недостаточно прав")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void requestForbidden(@SuppressWarnings("unused") String caseName, RequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Nonnull
    private static Stream<Arguments> getArguments() {
        return Stream.of(
            Arguments.of("getGrid", getGrid()),
            Arguments.of("getDetail", getDetail(1))
        );
    }
}
