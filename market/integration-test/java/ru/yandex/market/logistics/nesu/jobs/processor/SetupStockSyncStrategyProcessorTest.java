package ru.yandex.market.logistics.nesu.jobs.processor;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.type.StockUnfreezingStrategyType;
import ru.yandex.market.fulfillment.stockstorage.client.entity.type.StockUpdatingStrategyType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.model.ShopIdPartnerIdPayload;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.MbiFactory.getShopFeatureListItems;

@DisplayName("Настройка стратегии стоков")
@DatabaseSetup("/jobs/processor/setup_stock_sync_strategy/prepare.xml")
public class SetupStockSyncStrategyProcessorTest extends AbstractContextualTest {
    @Autowired
    private SetupStockSyncStrategyProcessor setupStockSyncStrategyProcessor;
    @Autowired
    private StockStorageOrderClient orderClient;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private MbiApiClient mbiApiClient;

    @BeforeEach
    void setup() {
        doReturn(Optional.of(LmsFactory.createPartner(100L, PartnerType.DROPSHIP))).when(lmsClient).getPartner(100L);
        doReturn(Optional.of(LmsFactory.createPartner(400L, PartnerType.XDOC))).when(lmsClient).getPartner(400L);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(orderClient, lmsClient, mbiApiClient);
    }

    @Test
    @DisplayName("Успешное проставление дефолтной стратегии")
    void success() {
        setupStockSyncStrategyProcessor.processPayload(payload(100, 1));
        verify(orderClient).setStockUnfreezingStrategy(100, StockUnfreezingStrategyType.DEFAULT);
        verify(orderClient).setStockUpdatingStrategy(100, StockUpdatingStrategyType.CHECK_ONLY_STOCK_AMOUNT);
        verify(lmsClient).getPartner(100L);
        verify(mbiApiClient).getShopsWithFeature(FeatureType.DROPSHIP.getId());
    }

    @Test
    @DisplayName("Успешное проставление стратегии фида")
    void successFeedStrategy() {
        when(mbiApiClient.getShopsWithFeature(FeatureType.DROPSHIP.getId()))
            .thenReturn(getShopFeatureListItems(1, FeatureType.DROPSHIP, true));
        setupStockSyncStrategyProcessor.processPayload(payload(100, 1));
        verify(orderClient).setStockUnfreezingStrategy(100, StockUnfreezingStrategyType.CHECK_STOCK_WAS_UPDATED);
        verify(orderClient).setStockUpdatingStrategy(100, StockUpdatingStrategyType.CHECK_ONLY_DATE);
        verify(lmsClient).getPartner(100L);
        verify(mbiApiClient).getShopsWithFeature(FeatureType.DROPSHIP.getId());
    }

    @Test
    @DisplayName("Нет настройки для магазина и партнера")
    void noShopPartnerSetting() {
        softly.assertThatCode(() -> setupStockSyncStrategyProcessor.processPayload(payload(200, 1)))
            .hasMessage("There is no relation between partner 200 and shop 1");
    }

    @Test
    @DisplayName("Партнер не найден")
    void noPartner() {
        softly.assertThatCode(() -> setupStockSyncStrategyProcessor.processPayload(payload(300, 1)))
            .hasMessage("Failed to find [PARTNER] with ids [300]");
        verify(lmsClient).getPartner(300L);
    }

    @Test
    @DisplayName("Магазин не найден")
    void noShop() {
        softly.assertThatCode(() -> setupStockSyncStrategyProcessor.processPayload(payload(300, 100)))
            .hasMessage("Failed to find [SHOP] with ids [100]");
    }

    @Test
    @DisplayName("Партнер неправильного типа")
    void invalidPartnerType() {
        softly.assertThatCode(() -> setupStockSyncStrategyProcessor.processPayload(payload(400, 1)))
            .hasMessage("Partner 400 has invalid type XDOC");
        verify(lmsClient).getPartner(400L);
    }

    @Nonnull
    private ShopIdPartnerIdPayload payload(long partnerId, long shopId) {
        return new ShopIdPartnerIdPayload("1", partnerId, shopId);
    }
}
