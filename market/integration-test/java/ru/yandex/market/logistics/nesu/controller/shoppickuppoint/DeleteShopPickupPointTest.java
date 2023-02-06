package ru.yandex.market.logistics.nesu.controller.shoppickuppoint;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.nesu.jobs.producer.UpdateShopPickupPointProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.UpdateShopPickupPointTariffProducer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Удаление ПВЗ магазина и его тарифа")
class DeleteShopPickupPointTest extends AbstractShopPickupPointTest {

    @Autowired
    private UpdateShopPickupPointProducer updateShopPickupPointProducer;

    @Autowired
    private UpdateShopPickupPointTariffProducer updateShopPickupPointTariffProducer;

    @BeforeEach
    void setUp() {
        doNothing().when(updateShopPickupPointProducer).produceTask(anyLong());
        doNothing().when(updateShopPickupPointTariffProducer).produceTask(anyLong());
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(updateShopPickupPointProducer, updateShopPickupPointTariffProducer);
    }

    @Test
    @DisplayName("Успешное удаление")
    @ExpectedDatabase(
        value = "/controller/shop-pickup-points/after/deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteSuccess() throws Exception {
        mockMvc.perform(requestBuilder(200, 800))
            .andExpect(status().isOk())
            .andExpect(noContent());
        verify(updateShopPickupPointProducer).produceTask(800);
        verify(updateShopPickupPointTariffProducer).produceTask(800);
    }

    @Nonnull
    @Override
    protected MockHttpServletRequestBuilder requestBuilder(long shopId, long shopPickupPointMetaId) {
        return MockMvcRequestBuilders.request(
            HttpMethod.DELETE,
            String.format("/internal/shop/%d/pickup-point-meta/%d", shopId, shopPickupPointMetaId)
        );
    }
}
