package ru.yandex.market.logistics.nesu.controller.internal;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.type.StockUnfreezingStrategyType;
import ru.yandex.market.fulfillment.stockstorage.client.entity.type.StockUpdatingStrategyType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/partner/set-stock-sync-strategy/setup.xml")
@DisplayName("Установка стратегии синхронизации стоков")
public class InternalPartnerControllerTest extends AbstractContextualTest {

    @Autowired
    private StockStorageOrderClient stockStorageOrderClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(stockStorageOrderClient);
    }

    @Test
    @DisplayName("Установка стратегии фидов Dropship")
    void dropshipSuccessFeed() throws Exception {
        setStockSyncStrategy(1, 1, true)
            .andExpect(status().isOk())
            .andExpect(noContent());

        verifySetFeedStrategy(1);
    }

    @Test
    @DisplayName("Установка стратегии по умолчанию Dropship")
    void dropshipSuccessDefault() throws Exception {
        setStockSyncStrategy(1, 1, false)
            .andExpect(status().isOk())
            .andExpect(noContent());

        verifyUnsetFeedStrategy(1);
    }

    @Test
    @DisplayName("Установка стратегии фидов ненастроенному Dropship by Seller")
    void drophipBySellerNeedSettingsSuccessFeed() throws Exception {
        setStockSyncStrategy(2, 2, true)
            .andExpect(status().isOk())
            .andExpect(noContent());

        verifySetFeedStrategy(2);
    }

    @Test
    @DisplayName("Установка стратегии по умолчанию ненастроенному Dropship by Seller")
    void drophipBySellerNeedSettingsSuccessDefault() throws Exception {
        setStockSyncStrategy(2, 2, false)
            .andExpect(status().isOk())
            .andExpect(noContent());

        verifyUnsetFeedStrategy(2);
    }

    @Test
    @DisplayName("Партнёр не принадлежит магазину")
    void noRelation() throws Exception {
        setStockSyncStrategy(2, 1, true)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("There is no relation between partner 2 and shop 1"));
    }

    @Test
    @DisplayName("Магазин не существует")
    void shopNotExists() throws Exception {
        setStockSyncStrategy(1, 3, true)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("There is no relation between partner 1 and shop 3"));
    }

    @Nonnull
    private ResultActions setStockSyncStrategy(long partnerId, long shopId, boolean isFeedStrategy) throws Exception {
        return mockMvc.perform(
            post("/internal/partner/{partnerId}/set-stock-sync-strategy", partnerId)
                .param("isFeedStrategy", String.valueOf(isFeedStrategy))
                .param("shopId", String.valueOf(shopId))
        );
    }

    private void verifySetFeedStrategy(int partnerId) {
        verify(stockStorageOrderClient)
            .setStockUnfreezingStrategy(partnerId, StockUnfreezingStrategyType.CHECK_STOCK_WAS_UPDATED);
        verify(stockStorageOrderClient)
            .setStockUpdatingStrategy(partnerId, StockUpdatingStrategyType.CHECK_ONLY_DATE);
    }

    private void verifyUnsetFeedStrategy(int partnerId) {
        verify(stockStorageOrderClient)
            .setStockUnfreezingStrategy(partnerId, StockUnfreezingStrategyType.DEFAULT);
        verify(stockStorageOrderClient)
            .setStockUpdatingStrategy(partnerId, StockUpdatingStrategyType.CHECK_ONLY_STOCK_AMOUNT);
    }
}
