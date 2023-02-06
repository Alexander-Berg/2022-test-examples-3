package ru.yandex.market.logistics.nesu.controller.internal.registration;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.delivery.trust.client.TrustClient;
import ru.yandex.market.delivery.trust.client.model.request.CreateProductRequest;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Ручное создание продукта магазина в Балансе")
class ShopAssignTrustProductTest extends AbstractContextualTest {

    @Autowired
    private TrustClient trustClient;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(trustClient);
    }

    @Test
    @DisplayName("Ручное создания продукта DaaS магазина")
    @DatabaseSetup("/controller/shop-registration/after_daas_registration.xml")
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_create_product.xml",
        assertionMode = NON_STRICT
    )
    void successDaas() throws Exception {
        createProduct()
            .andExpect(status().isOk())
            .andExpect(noContent());

        verify(trustClient).createProduct(
            "test-token",
            CreateProductRequest.builder()
                .productId("daas_1")
                .name("Перечисление выручки магазину")
                .partnerId(255L)
                .build()
        );
    }

    @Test
    @DisplayName("Попытка создания для Dropshop магазина, ничего не происходит")
    @DatabaseSetup("/controller/shop-registration/after_dropship_registration.xml")
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_dropship_registration.xml",
        assertionMode = NON_STRICT
    )
    void tryDropship() throws Exception {
        createProduct()
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Test
    @DisplayName("Магазин не существует")
    void shopNotFound() throws Exception {
        createProduct()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [1]"));
    }

    @Nonnull
    private ResultActions createProduct() throws Exception {
        return mockMvc.perform(post("/internal/shops/1/create-product"));
    }
}
