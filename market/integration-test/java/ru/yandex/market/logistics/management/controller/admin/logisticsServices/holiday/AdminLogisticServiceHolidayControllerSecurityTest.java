package ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.RequestBuilder;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday.LogisticsServicesHolidaysHelper.create;
import static ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday.LogisticsServicesHolidaysHelper.delete;
import static ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday.LogisticsServicesHolidaysHelper.deleteMultiple;
import static ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday.LogisticsServicesHolidaysHelper.getTemplate;
import static ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday.LogisticsServicesHolidaysHelper.uploadFileForCreate;
import static ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday.LogisticsServicesHolidaysHelper.uploadFileForDelete;

@DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_holidays.xml")
public class AdminLogisticServiceHolidayControllerSecurityTest extends AbstractContextualAspectValidationTest {

    private static final String HOLIDAY_TO_CREATE = "{\"day\": \"2020-04-11\"}";
    private static final String HOLIDAYS_TO_DELETE = "{\"ids\": [11, 33]}";

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
            Arguments.of("create", create(25L).content(HOLIDAY_TO_CREATE)),
            Arguments.of("delete", delete(22L, 25L)),
            Arguments.of("deleteMultiple", deleteMultiple(25L).content(HOLIDAYS_TO_DELETE)),
            Arguments.of("getTemplate", getTemplate()),
            Arguments.of("uploadFileForDelete", uploadFileForDelete("")),
            Arguments.of("uploadFileForCreate", uploadFileForCreate(""))
        );
    }
}
