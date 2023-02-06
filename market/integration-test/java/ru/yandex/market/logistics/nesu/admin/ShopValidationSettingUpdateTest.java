package ru.yandex.market.logistics.nesu.admin;

import java.time.LocalTime;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.response.ShopValidationSettingDetailDto;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/validation/default_validation_settings.xml")
class ShopValidationSettingUpdateTest extends AbstractContextualTest {

    @Test
    @DisplayName("Обновление настроек валидации с админки")
    @ExpectedDatabase(
        value = "/repository/validation/expected_validation_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void update() throws Exception {
        assertSuccessfulUpdateValidationSettingsResponse(
            2L,
            ShopValidationSettingDetailDto.builder()
                .dailyCapacity(99)
                .shipmentWorkingDays(4)
                .warehouseWorkingDays(4)
                .nextDayCutoffTime(LocalTime.NOON)
                .nextDayShipmentInterval(LocalTime.NOON)
                .sameDayShipmentInterval(LocalTime.NOON)
                .sameDayCutoffTimeShift(1)
                .build(),
            "controller/admin/validation/updated_supplier_settings.json"
        );
    }

    @Test
    @DisplayName("Обновление настроек валидации на null")
    @ExpectedDatabase(
        value = "/repository/validation/nullable_validation_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateSettingsWithNulls() throws Exception {
        ShopValidationSettingDetailDto dto = ShopValidationSettingDetailDto.builder().build();

        assertSuccessfulUpdateValidationSettingsResponse(
            2L,
            dto,
            "controller/admin/validation/supplier_empty_settings_for_update.json"
        );
        assertSuccessfulUpdateValidationSettingsResponse(
            1L,
            dto,
            "controller/admin/validation/dropship_empty_settings_for_update.json"
        );
    }

    private void assertSuccessfulUpdateValidationSettingsResponse(
        Long roleId,
        ShopValidationSettingDetailDto detailDto,
        String jsonContentPath
    ) throws Exception {
        updateValidationSettings(roleId, detailDto)
            .andExpect(status().isOk())
            .andExpect(jsonContent(jsonContentPath));
    }

    @Nonnull
    private ResultActions updateValidationSettings(Long roleId, ShopValidationSettingDetailDto detailDto)
        throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/admin/validation/settings/" + roleId, detailDto));
    }

}
