package ru.yandex.market.logistics.nesu.api.warehouse;

import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.api.model.warehouse.ApiShopWarehouseUpdateRequest;
import ru.yandex.market.logistics.nesu.base.AbstractShopWarehouseReplaceTest;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.scheduleDay;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Замена склада через OpenApi")
public class ApiShopWarehouseReplaceTest extends AbstractShopWarehouseReplaceTest<ApiShopWarehouseUpdateRequest> {
    @Autowired
    private FeatureProperties featureProperties;

    @Autowired
    private BlackboxService blackboxService;

    private ApiAuthHolder authHolder;

    @BeforeEach
    void setupAuth() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);
        when(featureProperties.getShopIdsForExtendedNeeds()).thenReturn(Set.of(1L, 2L, 3L, 4L, 10L));
    }

    @Test
    @DisplayName("Обновление склада, идентификатор кабинета не указан")
    void noCabinetId() throws Exception {
        updateWarehouse(null, createMinimalValidRequest())
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(applicationFieldError()));
    }

    @Test
    @DisplayName("Обновление склада, магазин недоступен")
    void inaccessibleShop() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, 1L);

        updateWarehouse(1L, createRequest())
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Обновление склада, shopId не в списке разрешенных")
    void notAllowedShop() throws Exception {
        updateWarehouse(100L, createRequest())
            .andExpect(status().isNotFound())
            .andExpect(jsonContent(
                "controller/warehouse/response/update_not_found.json",
                JSONCompareMode.STRICT_ORDER,
                "timestamp"
            ));
    }

    @Test
    @DisplayName("Обновление склада, пустой список разрешенных shopId")
    void emptyAllowedShopList() throws Exception {
        when(featureProperties.getShopIdsForExtendedNeeds()).thenReturn(Set.of());
        updateWarehouse(100L, createRequest())
            .andExpect(status().isNotFound())
            .andExpect(jsonContent(
                "controller/warehouse/response/update_not_found.json",
                JSONCompareMode.STRICT_ORDER,
                "timestamp"
            ));
    }

    @Nonnull
    @Override
    protected ApiShopWarehouseUpdateRequest createMinimalValidRequest() {
        ApiShopWarehouseUpdateRequest request = new ApiShopWarehouseUpdateRequest();
        request.setSchedule(Set.of(scheduleDay()));
        return request;
    }

    @Nonnull
    @Override
    protected ResultActions updateWarehouse(Long shopId, ApiShopWarehouseUpdateRequest request) throws Exception {
        request.setCabinetId(shopId);
        return mockMvc.perform(request(HttpMethod.PUT, "/api/warehouses/1", request)
            .headers(authHolder.authHeaders())
            .param("userId", "19216801")
        );
    }

    @Nonnull
    protected ValidationErrorData applicationFieldError() {
        return ValidationErrorData.fieldError("cabinetId", "must not be null", getObjectName(), "NotNull");
    }

    @Nonnull
    @Override
    protected String getObjectName() {
        return "apiShopWarehouseUpdateRequest";
    }

    @Override
    protected void mockAccess(Long allowedId) {
        authHolder.mockAccess(mbiApiClient, allowedId);
    }

}
