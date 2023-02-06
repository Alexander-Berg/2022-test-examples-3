package ru.yandex.market.logistics.nesu.api.warehouse;

import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.api.model.warehouse.ApiShopWarehouseCreateRequest;
import ru.yandex.market.logistics.nesu.base.AbstractShopWarehouseCreateTest;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание склада через OpenApi")
public class ApiShopWarehouseCreateTest extends AbstractShopWarehouseCreateTest<ApiShopWarehouseCreateRequest> {
    @Autowired
    protected MbiApiClient mbiApiClient;

    @Autowired
    private FeatureProperties featureProperties;

    @Autowired
    private BlackboxService blackboxService;

    protected ApiAuthHolder authHolder;

    @BeforeEach
    void setupAuth() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);
        authHolder.mockAccess(mbiApiClient, 1L);
        authHolder.mockAccess(mbiApiClient, 3L);
        authHolder.mockAccess(mbiApiClient, 4L);
        authHolder.mockAccess(mbiApiClient, 10L);
        when(featureProperties.getShopIdsForExtendedNeeds()).thenReturn(Set.of(1L, 2L, 3L, 4L, 10L));
    }

    @Test
    @DisplayName("Создание склада, идентификатор кабинета не указан")
    void noCabinetId() throws Exception {
        createWarehouse(null, createRequest())
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(applicationFieldError()));
    }

    @Test
    @DisplayName("Создание склада, магазин недоступен")
    void inaccessibleShop() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, 1L);

        createWarehouse(1L, createRequest())
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Создание склада, shopId не в списке разрешенных")
    void notAllowedShop() throws Exception {
        createWarehouse(100L, createRequest())
            .andExpect(status().isNotFound())
            .andExpect(jsonContent(
                "controller/warehouse/response/not_found.json",
                JSONCompareMode.STRICT_ORDER,
                "timestamp"
            ));
    }

    @Test
    @DisplayName("Создание склада, пустой список разрешенных shopId")
    void emptyAllowedShopList() throws Exception {
        when(featureProperties.getShopIdsForExtendedNeeds()).thenReturn(Set.of());
        createWarehouse(100L, createRequest())
            .andExpect(status().isNotFound())
            .andExpect(jsonContent(
                "controller/warehouse/response/not_found.json",
                JSONCompareMode.STRICT_ORDER,
                "timestamp"
            ));
    }

    @Nonnull
    protected ValidationErrorData applicationFieldError() {
        return ValidationErrorData.fieldError("cabinetId", "must not be null", getObjectName(), "NotNull");
    }

    @Override
    @Nonnull
    protected ResultActions createWarehouse(Long shopId, ApiShopWarehouseCreateRequest request) throws Exception {
        request.setCabinetId(shopId);
        return mockMvc.perform(request(HttpMethod.POST, "/api/warehouses", request)
            .headers(authHolder.authHeaders())
            .param("userId", "19216801")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
    }

    @Nonnull
    @Override
    protected ApiShopWarehouseCreateRequest createMinimalRequest() {
        return new ApiShopWarehouseCreateRequest();
    }

    @Nonnull
    @Override
    protected String getObjectName() {
        return "apiShopWarehouseCreateRequest";
    }
}
