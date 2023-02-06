package ru.yandex.market.logistics.nesu.admin.logistic_point_availability;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.admin.model.enums.AdminShopRole;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminShopShipmentType;
import ru.yandex.market.logistics.nesu.admin.model.response.LogisticPointAvailabilityNewDto;
import ru.yandex.market.logistics.nesu.admin.utils.AdminValidationUtils;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/logistic-point-availability/before/prepare_data.xml")
class LogisticPointAvailabilityCreateTest extends BaseLogisticPointAvailabilityTest {
    @Test
    @DisplayName("Получение формы создания новых конфигураций доступности складов для магазинов")
    void getCreationForm() throws Exception {
        mockMvc.perform(get("/admin/logistic-point-availability/new"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/logistic-point-availability/creation_form.json"));
    }

    @Test
    @DisplayName("Создание новой конфигурации доступности складов для магазинов")
    @ExpectedDatabase(
        value = "/repository/logistic-point-availability/after/creation_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLogisticPointAvailability() throws Exception {
        create(defaultRequest())
            .andExpect(status().isOk())
            .andExpect(content().string("4"));

        verifyGetLogisticPoint(LOGISTIC_POINT_1_ID);
    }

    @Test
    @DisplayName("Создание новой конфигурации доступности складов с незаполненными необязательными полями")
    @ExpectedDatabase(
        value = "/repository/logistic-point-availability/after/creation_result_only_required_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLogisticPointAvailabilityOnlyRequiredFields() throws Exception {
        create(
            new LogisticPointAvailabilityNewDto()
                .setLogisticPoint(2L)
                .setLocationId(21652)
                .setShipmentType(AdminShopShipmentType.IMPORT)
                .setShopRole(AdminShopRole.SUPPLIER)
        )
            .andExpect(status().isOk())
            .andExpect(content().string("4"));

        verifyGetLogisticPoint(LOGISTIC_POINT_2_ID);
    }

    @Test
    @DisplayName("Создание новой конфигурации с новыми полями ограничений")
    @ExpectedDatabase(
        value = "/repository/logistic-point-availability/after/creation_result_new_restrictions.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createNewRestrictionFields() throws Exception {
        create(
            defaultRequest()
                .setOrdersPerPartnerLimit(50)
        )
            .andExpect(status().isOk())
            .andExpect(content().string("4"));

        verifyGetLogisticPoint(LOGISTIC_POINT_1_ID);
    }

    @Test
    @DisplayName("Попытка создать новую конфигурацию доступности складов для несуществующей логистической точки")
    void createLogisticPointAvailabilityLogisticPointNotFound() throws Exception {
        when(lmsClient.getLogisticsPoint(LOGISTIC_POINT_1_ID)).thenReturn(Optional.empty());

        create(defaultRequest())
            .andExpect(status().isNotFound());

        verifyGetLogisticPoint(LOGISTIC_POINT_1_ID);
    }

    @Test
    @DisplayName("Попытка создать новую конфигурацию доступности складов с незаполненными обязательными полями")
    void createLogisticPointAvailabilityBadRequest() throws Exception {
        create(new LogisticPointAvailabilityNewDto())
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                createNullFieldError("locationId"),
                createNullFieldError("logisticPoint"),
                createNullFieldError("shipmentType"),
                createNullFieldError("shopRole")
            )));
    }

    @Test
    @DisplayName("Создание новой конфигурации доступности складов для магазинов "
        + "со значениями полей, совпадающими с уже существующей конфигурацией")
    void createLogisticPointAvailabilityNotUnique() throws Exception {
        create(defaultRequest().setLocationId(21651))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Конфигурация для логистической точки с идентификатором 1, локации 21651, "
                + "типа отгрузки \"Забор\" и типа партнёра \"DropShip\" уже существует."));
    }

    @Test
    @DisplayName("Создание конфигурации DAAS + WITHDRAW_EXPRESS")
    void createDaasAndWithdrawExpressLogisticPointAvailability() throws Exception {
        create(
            new LogisticPointAvailabilityNewDto()
                .setLogisticPoint(2L)
                .setLocationId(21651)
                .setShipmentType(AdminShopShipmentType.WITHDRAW_EXPRESS)
                .setShopRole(AdminShopRole.DAAS)
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Invalid combination of DAAS and WITHDRAW_EXPRESS"));
    }

    @Nonnull
    private ValidationErrorData createNullFieldError(String field) {
        return AdminValidationUtils.createNullFieldError(field, "logisticPointAvailabilityNewDto");
    }

    @Nonnull
    private LogisticPointAvailabilityNewDto defaultRequest() {
        return new LogisticPointAvailabilityNewDto()
            .setLogisticPoint(1L)
            .setLocationId(11474)
            .setShipmentType(AdminShopShipmentType.WITHDRAW)
            .setShopRole(AdminShopRole.DROPSHIP)
            .setPartnerLimit(10)
            .setEnabled(true);
    }

    @Nonnull
    private ResultActions create(LogisticPointAvailabilityNewDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.POST, "/admin/logistic-point-availability", request));
    }

}
