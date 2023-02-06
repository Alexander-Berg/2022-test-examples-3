package ru.yandex.direct.jobs.statistics.activeorders;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.log.service.LogActiveOrdersService;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.WhenMoneyOnCampWasRepository;
import ru.yandex.direct.core.entity.campoperationqueue.CampOperationQueueRepository;
import ru.yandex.direct.core.entity.campoperationqueue.model.CampQueueOperation;
import ru.yandex.direct.core.entity.statistics.container.ProceededActiveOrder;
import ru.yandex.direct.core.entity.statistics.repository.ActiveOrdersRepository;
import ru.yandex.direct.dbschema.ppc.tables.CampOperationsQueue;
import ru.yandex.direct.dbschema.ppc.tables.WhenMoneyOnCampWas;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.INTERNAL_DISTRIB;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.INTERNAL_FREE;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT;
import static ru.yandex.direct.core.entity.campoperationqueue.model.CampQueueOperationName.UNARC;

class ActiveOrdersImportServiceUpdateOrdersTest {
    private ActiveOrdersImportService activeOrdersImportService;
    private ActiveOrdersMetrics activeOrdersMetrics;
    private CampaignRepository campaignRepository;
    private PpcPropertiesSupport ppcPropertiesSupport;
    private WhenMoneyOnCampWasRepository whenMoneyOnCampWasRepository;
    private CampOperationQueueRepository campOperationQueueRepository;
    private ActiveOrdersRepository activeOrdersRepository;
    private WalletMoneyCalculator walletMoneyCalculator;

    @BeforeEach
    void before() {
        var logActiveOrdersService = mock(LogActiveOrdersService.class);
        campaignRepository = mock(CampaignRepository.class);
        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        activeOrdersRepository = mock(ActiveOrdersRepository.class);
        whenMoneyOnCampWasRepository = mock(WhenMoneyOnCampWasRepository.class);
        campOperationQueueRepository = mock(CampOperationQueueRepository.class);
        walletMoneyCalculator = mock(WalletMoneyCalculator.class);
        activeOrdersImportService = new ActiveOrdersImportService(campaignRepository, ppcPropertiesSupport,
                whenMoneyOnCampWasRepository, campOperationQueueRepository, walletMoneyCalculator,
                activeOrdersRepository, logActiveOrdersService);
        activeOrdersMetrics = new ActiveOrdersMetrics(1);
    }

    /**
     * Тест проверяет, что если на кампании закончились деньги, то в таблицу {@link WhenMoneyOnCampWas} добавится
     * информация о закрытом интервале
     */
    @Test
    void updateOrdersMoneyEndTest() {
        int shard = 5;
        var processActiveOrder = proceededMoneyOrder(TEXT, 10, 4_000_000);
        processActiveOrder.setMoneyEnd(true);
        var singleProcessedOrder = List.of(processActiveOrder);

        activeOrdersImportService.updateOrders(shard, singleProcessedOrder, activeOrdersMetrics);
        verify(whenMoneyOnCampWasRepository).closeInterval(eq(shard), eq(List.of(1L)));
        verify(campOperationQueueRepository, never()).addCampaignQueueOperations(anyInt(), anyCollection());
        verify(activeOrdersRepository).updateCampaigns(eq(shard), eq(singleProcessedOrder));
    }

    /**
     * Тест проверяет, что если кампания разорхивирована, то она добавится в очередь на разархивацию в таблицу
     * {@link CampOperationsQueue}
     */
    @Test
    void updateOrdersUnarchCampaign() {
        int shard = 5;
        var processActiveOrder = proceededMoneyOrder(TEXT, 3_000_000, 4_000_000);
        processActiveOrder.setUnarchived(true);
        var singleProcessedOrder = List.of(processActiveOrder);

        activeOrdersImportService.updateOrders(shard, singleProcessedOrder, activeOrdersMetrics);
        verify(whenMoneyOnCampWasRepository, never()).closeInterval(anyInt(), anyCollection());

        var campQueueOperation = new CampQueueOperation().withCid(1L).withCampQueueOperationName(UNARC);
        verify(campOperationQueueRepository).addCampaignQueueOperations(eq(shard), eq(List.of(campQueueOperation)));
        verify(activeOrdersRepository).updateCampaigns(eq(shard), eq(singleProcessedOrder));
    }

    /**
     * Тест проверяет, что если внутренняя бесплатная кампания разархивирована, то она добавится в очередь
     * на разархиваицию в таблицу {@link CampOperationsQueue}
     */
    @Test
    void updateOrdersUnarchInternalFreeCampaign() {
        int shard = 5;
        var processActiveOrder = proceededOrder(INTERNAL_FREE, 0, 0, 10, 8);
        processActiveOrder.setUnarchived(true);
        var singleProcessedOrder = List.of(processActiveOrder);

        activeOrdersImportService.updateOrders(shard, singleProcessedOrder, activeOrdersMetrics);
        verify(whenMoneyOnCampWasRepository, never()).closeInterval(anyInt(), anyCollection());

        var campQueueOperation = new CampQueueOperation().withCid(1L).withCampQueueOperationName(UNARC);
        verify(campOperationQueueRepository).addCampaignQueueOperations(eq(shard), eq(List.of(campQueueOperation)));
        verify(activeOrdersRepository).updateCampaigns(eq(shard), eq(singleProcessedOrder));
    }

    @Test
    void updateOrdersInternalFreeCampaignDoesntAffectWhenMoneyOnCampWas() {
        int shard = 5;
        var processActiveOrder = proceededOrder(INTERNAL_FREE, 0, 0, 10, 11);
        processActiveOrder.setFinished(true);
        var singleProcessedOrder = List.of(processActiveOrder);

        activeOrdersImportService.updateOrders(shard, singleProcessedOrder, activeOrdersMetrics);

        verify(whenMoneyOnCampWasRepository, never()).closeInterval(anyInt(), anyCollection());
        verify(activeOrdersRepository).updateCampaigns(eq(shard), eq(singleProcessedOrder));
    }

    @Test
    void updateOrdersInternalDistribCampaignDoesntAffectWhenMoneyOnCampWas() {
        int shard = 5;
        var processActiveOrder = proceededMoneyOrder(INTERNAL_DISTRIB, 1_000_000, 2_000_000);
        processActiveOrder.setFinished(true);
        var singleProcessedOrder = List.of(processActiveOrder);

        activeOrdersImportService.updateOrders(shard, singleProcessedOrder, activeOrdersMetrics);

        verify(whenMoneyOnCampWasRepository, never()).closeInterval(anyInt(), anyCollection());
        verify(activeOrdersRepository).updateCampaigns(eq(shard), eq(singleProcessedOrder));
    }

    private static ProceededActiveOrder proceededMoneyOrder(CampaignType type, long sum, long sumSpent) {
        return proceededOrder(type, sum, sumSpent, 0, 0);
    }

    private static ProceededActiveOrder proceededOrder(
            CampaignType type, long sum, long sumSpent, long units, long unitsSpent) {
        return new ProceededActiveOrder(1, type, 2, 3,
                BigDecimal.valueOf(sumSpent), units, unitsSpent);
    }
}
