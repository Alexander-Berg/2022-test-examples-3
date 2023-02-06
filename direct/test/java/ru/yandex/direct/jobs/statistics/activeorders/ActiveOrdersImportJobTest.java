package ru.yandex.direct.jobs.statistics.activeorders;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.statistics.model.ActiveOrderChanges;
import ru.yandex.direct.core.entity.statistics.repository.OrderStatClusterChooseRepository;
import ru.yandex.direct.core.entity.statistics.repository.OrderStatRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.common.db.PpcPropertyNames.ACTIVE_ORDERS_BATCHES_COUNT;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.ytwrapper.model.YtCluster.ZENO;

@JobsTest
@ExtendWith(SpringExtension.class)
class ActiveOrdersImportJobTest {
    private static final int BATCH_SIZE = 10;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private Steps steps;

    private ActiveOrdersImportService activeOrdersImportService;
    private OrderStatRepository orderStatRepository;
    private ActiveOrdersImportJob activeOrdersImportJob;

    @BeforeEach
    public void before() throws NoSuchFieldException {
        PpcProperty<Long> batchesCountProperty = mock(PpcProperty.class);
        doReturn(2L).when(batchesCountProperty).getOrDefault(any());
        PpcPropertiesSupport ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        doReturn(batchesCountProperty)
                .when(ppcPropertiesSupport).get(ACTIVE_ORDERS_BATCHES_COUNT);
        ActiveOrdersImportParametersSource parametersSource = new ActiveOrdersImportParametersSource(4);

        orderStatRepository = spy(new OrderStatRepository(mock(YtProvider.class),
                ppcPropertiesSupport, mock(DirectYtDynamicConfig.class)));
        doReturn(63L).when(orderStatRepository).getCampaignsYtHashMaxValue(any());

        activeOrdersImportService = mock(ActiveOrdersImportService.class);
        activeOrdersImportJob = new ActiveOrdersImportJob(activeOrdersImportService,
                ppcPropertiesSupport, orderStatRepository, mock(OrderStatClusterChooseRepository.class),
                shardHelper,
                parametersSource,
                BATCH_SIZE);

        Map<Integer, ActiveOrdersMetrics> metricsMap = mock(Map.class);
        ReflectionTestUtils.setField(activeOrdersImportJob,
                "activeOrdersMetrics",
                metricsMap);
    }

    /**
     * Тест проверяет, что если запрос в YT возвращает строки, количество которых равно максимально допустимому
     * значению, то
     * 1) обработается первая пачка данных
     * 2) для второй итерации в запрос будет передан параметр cid'а равный максимальному cid'у из предыдущей пачки
     * 3) новая пачка данных обработается
     */
    @Test
    @SuppressWarnings("unchecked")
    void importActiveOrdersBatchesTest() {
        int shard = 1;
        CampaignInfo campaign1 = steps.campaignSteps().createCampaign(activeTextCampaign(null, null));
        CampaignInfo campaign2 = steps.campaignSteps().createCampaign(activeTextCampaign(null, null));
        CampaignInfo campaign3 = steps.campaignSteps().createCampaign(activeTextCampaign(null, null));
        var clusters = List.of(ZENO);
        var activeOrdersChanges1 = List.of(
                new ActiveOrderChanges.Builder()
                        .withCid(campaign1.getCampaignId())
                        .withShard(campaign1.getShard())
                        .build(),
                new ActiveOrderChanges.Builder()
                        .withCid(campaign2.getCampaignId())
                        .withShard(campaign2.getShard())
                        .build());

        var activeOrdersChanges2 = List.of(
                new ActiveOrderChanges.Builder()
                        .withCid(campaign3.getCampaignId())
                        .withShard(campaign3.getShard())
                        .build());

        var activeObjectsChangesCapture = ArgumentCaptor.forClass(List.class);

        /* Первый раз возвращается пачка, размер которой равен максимально допустмому размеру, воторой раз небольшая
        пачка */
        doReturn(activeOrdersChanges1).doReturn(activeOrdersChanges2).when(orderStatRepository).getChangedActiveOrders(
                any(), eq(clusters));

        activeOrdersImportJob.importActiveOrdersBatches(shard, clusters);

        /* С какими пачками запускалась обработка данных */
        verify(activeOrdersImportService, times(2)).importActiveOrders(eq(shard), activeObjectsChangesCapture.capture(),
                isNull());

        var capturedActiveOrderChanges = activeObjectsChangesCapture.getAllValues();
        assertSoftly(softly -> {
            softly.assertThat(capturedActiveOrderChanges).hasSize(2);
            softly.assertThat(capturedActiveOrderChanges.get(0)).isEqualTo(activeOrdersChanges1);
            softly.assertThat(capturedActiveOrderChanges.get(1)).isEqualTo(activeOrdersChanges2);
        });
    }

    /**
     * Если результат запроса в yt пустой - то код завершится без ошибкок, и методы сервиса не будут вызваны
     */
    @Test
    void importActiveOrdersEmptyResultTest() {
        int workerNum = 0;
        var clusters = List.of(ZENO);
        var activeOrdersChanges = List.of();

        doReturn(activeOrdersChanges).when(orderStatRepository).getChangedActiveOrders(any(), eq(clusters));

        assertThatCode(() -> activeOrdersImportJob.importActiveOrdersBatches(workerNum, clusters)).doesNotThrowAnyException();

        verify(orderStatRepository, times(2)).getChangedActiveOrders(any(), eq(clusters));

        verify(activeOrdersImportService, times(0)).importActiveOrders(anyInt(), anyList(),
                any(ActiveOrdersMetrics.class));
    }
}
