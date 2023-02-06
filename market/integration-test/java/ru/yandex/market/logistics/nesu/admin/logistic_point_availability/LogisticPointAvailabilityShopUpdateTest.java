package ru.yandex.market.logistics.nesu.admin.logistic_point_availability;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.response.LogisticPointAvailabilityShopDto;
import ru.yandex.market.logistics.nesu.admin.utils.AdminValidationUtils;

import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/logistic-point-availability/before/prepare_data.xml")
class LogisticPointAvailabilityShopUpdateTest extends AbstractContextualTest {
    @Test
    @DisplayName("Редактирование настроек доступа магазинов к несуществующей конфигурации доступности склада")
    void updateLogisticPointAvailabilityNotFound() throws Exception {
        update(10L, List.of())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTIC_POINT_AVAILABILITY] with ids [10]"));
    }

    @Test
    @DisplayName("Очистка списка магазинов, имеющих доступ к конфигурации доступности склада")
    @ExpectedDatabase(
        value = "/repository/logistic-point-availability/after/shop_update_result_empty_list.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void clearShopIds() throws Exception {
        update(1L, List.of())
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/admin/logistic-point-availability/shops/update_result_empty_shops_list.json"
            ));
    }

    @Test
    @DisplayName("Обновление списка магазинов, имеющих доступ к конфигурации доступности склада")
    @ExpectedDatabase(
        value = "/repository/logistic-point-availability/after/shop_update_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void update() throws Exception {
        update(1L, List.of(11L, 12L))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/admin/logistic-point-availability/shops/update_result.json"
            ));
    }

    @Test
    @DisplayName("Переданный список магазинов, имеющих доступ к конфигурации доступности склада, null")
    void updateShopIdsIsNull() throws Exception {
        update(1L, null)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                AdminValidationUtils.createNullFieldError("shopIds", "logisticPointAvailabilityShopUpdateDto")
            ));
    }

    @Test
    @DisplayName("Переданный список магазинов, имеющих доступ к конфигурации доступности склада, содержит null")
    void updateShopIdsContainsNull() throws Exception {
        update(1L, Lists.newArrayList(10L, null))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                fieldError("shopIds[]", "must not be null", "logisticPointAvailabilityShopUpdateDto", "NotNull")
            ));
    }

    @Nonnull
    private ResultActions update(long logisticPointAvailabilityId, @Nullable List<Long> shopIds) throws Exception {
        return mockMvc.perform(request(
            HttpMethod.PUT,
            String.format("/admin/logistic-point-availability/%d/shop", logisticPointAvailabilityId),
            LogisticPointAvailabilityShopDto.builder().shopIds(shopIds).build()
        ));
    }
}
