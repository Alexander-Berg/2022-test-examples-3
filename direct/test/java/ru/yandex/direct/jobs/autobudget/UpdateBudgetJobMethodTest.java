package ru.yandex.direct.jobs.autobudget;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy;
import ru.yandex.direct.core.entity.strategy.model.StrategyWithDayBudget;
import ru.yandex.direct.core.entity.strategy.repository.StrategyModifyRepository;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


/**
 * Тесты на внутренние методы джобы UpdateBudgetJob
 */
@JobsTest
@ExtendWith(SpringExtension.class)
class UpdateBudgetJobMethodTest {

    private static final int SHARD = 1;

    private UpdateDayBudgetJob job;
    private LocalDateTime now;
    private PpcPropertyName<LocalDate> propertyName;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private StrategyModifyRepository strategyModifyRepository;

    @Mock
    private StrategyTypedRepository strategyTypedRepository;

    @Captor
    private ArgumentCaptor<Collection<Long>> captor;

    @BeforeEach
    void before() {
        initMocks(this);
        job = new UpdateDayBudgetJob(SHARD, campaignRepository, strategyTypedRepository, strategyModifyRepository, ppcPropertiesSupport);
        propertyName = job.getLastRunDatePropertyName();
        now = LocalDateTime.now();
    }

    @SuppressWarnings("unchecked")
    private static <T> PpcProperty<T> mockProperty() {
        return mock(PpcProperty.class);
    }

    @Test
    void canRunPositiveNoProperty() {
        PpcProperty<LocalDate> property = mockProperty();
        when(property.get()).thenReturn(null);
        doReturn(property).when(ppcPropertiesSupport).get(propertyName);
        assertTrue(job.canRun(), "при отсутствующей property canRun == true");
    }

    @Test
    void canRunPositiveOldProperty() {
        LocalDate propValue = LocalDate.now()
                .minusDays(1);
        PpcProperty<LocalDate> property = mockProperty();
        when(property.get()).thenReturn(propValue);
        doReturn(property).when(ppcPropertiesSupport).get(propertyName);
        assertTrue(job.canRun(), "при property <= now() canRun == true");
    }

    @Test
    void canRunNegativeTodayProperty() {
        LocalDate propValue = LocalDate.now();
        PpcProperty<LocalDate> property = mockProperty();
        when(property.get()).thenReturn(propValue);
        doReturn(property).when(ppcPropertiesSupport).get(propertyName);
        assertFalse(job.canRun(), "при property == now() canRun == false");
    }

    @Test
    void checkUpdateLastRunProperty() {
        PpcProperty<LocalDate> property = mockProperty();
        when(ppcPropertiesSupport.get(eq(propertyName))).thenReturn(property);

        LocalDateTime lastRunDate = now;
        job.updateLastRunProperty(now.toLocalDate());
        verify(property).set(lastRunDate.toLocalDate());
    }

    @Test
    void checkResetDayBudgetStopTime() {
        Set<Long> cidsToResetStopTime = ImmutableSet.of(123L, 1234L);
        doReturn(cidsToResetStopTime).when(campaignRepository)
                .getCampaignIdsWhichDayBudgetStopTimeLessThan(job.getShard(), now);

        job.resetDayBudgetStopTime(now);
        verify(campaignRepository)
                .resetDayBudgetStopTimeAndNotificationStatus(eq(job.getShard()), captor.capture(), eq(now));
        assertEquals(new ArrayList<>(cidsToResetStopTime), captor.getValue());
    }

    @Test
    void checkResetDayBudgetStopTimeForCampaignsCountGreaterThanUpdatePacketSize() {
        long lastCid = UpdateDayBudgetJob.UPDATE_PACKET_SIZE + 1;
        Set<Long> cidsToResetStopTime = LongStream.rangeClosed(1, lastCid)
                .boxed()
                .collect(toSet());
        doReturn(cidsToResetStopTime).when(campaignRepository)
                .getCampaignIdsWhichDayBudgetStopTimeLessThan(job.getShard(), now);

        job.resetDayBudgetStopTime(now);
        verify(campaignRepository, times(2))
                .resetDayBudgetStopTimeAndNotificationStatus(eq(job.getShard()), captor.capture(), eq(now));
        assertThat(captor.getAllValues(), hasSize(2));
        assertEquals(singletonList(lastCid), captor.getAllValues().get(1),
                "во вторую пачку должна попасть только последняя кампания");
    }

    @Test
    void checkResetDayBudgetChangeCount() {
        Set<Long> cidsToResetChangeCount = ImmutableSet.of(123L, 1234L);
        doReturn(cidsToResetChangeCount).when(campaignRepository)
                .getCampaignIdsWithDayBudgetDailyChanges(job.getShard());

        job.resetDayBudgetChangeCount();
        verify(campaignRepository).resetDayBudgetDailyChangeCount(eq(job.getShard()), captor.capture());
        assertEquals(new ArrayList<>(cidsToResetChangeCount), captor.getValue());
    }

    @Test
    void checkResetDayBudgetChangeCountForCampaignsCountGreaterThanUpdatePacketSize() {
        long lastCid = UpdateDayBudgetJob.UPDATE_PACKET_SIZE + 1;
        Set<Long> cidsToResetChangeCount = LongStream.rangeClosed(1, lastCid)
                .boxed()
                .collect(toSet());
        doReturn(cidsToResetChangeCount).when(campaignRepository)
                .getCampaignIdsWithDayBudgetDailyChanges(job.getShard());

        job.resetDayBudgetChangeCount();
        verify(campaignRepository, times(2)).resetDayBudgetDailyChangeCount(eq(job.getShard()), captor.capture());
        assertThat(captor.getAllValues(), hasSize(2));
        assertEquals(singletonList(lastCid), captor.getAllValues().get(1),
                "во вторую пачку должна попасть только последняя кампания");
    }

    @Test
    void checkResetDayBudgetChangeCountForCallingToUpdateStrategies() {
        final long cid = RandomNumberUtils.nextPositiveLong();
        final long strategyId = RandomNumberUtils.nextPositiveLong();

        Set<Long> cidsToResetChangeCount = Set.of(cid);

        doReturn(Map.of(cid, strategyId))
                .when(campaignRepository)
                .getStrategyIdsByCampaignIds(eq(job.getShard()), any());

        var strategy = new DefaultManualStrategy()
                .withDayBudgetDailyChangeCount(1)
                .withId(strategyId);

        doReturn(Map.of(strategyId, strategy))
                .when(strategyTypedRepository)
                .getIdToModelTyped(eq(job.getShard()), any());

        doReturn(cidsToResetChangeCount).when(campaignRepository)
                .getCampaignIdsWithDayBudgetDailyChanges(eq(job.getShard()));

        doNothing().when(strategyModifyRepository)
                        .updateStrategiesTable(eq(job.getShard()), any());

        job.resetDayBudgetChangeCount();

        verify(strategyModifyRepository).updateStrategiesTable(
                eq(job.getShard()),
                argThat(appliedChanges -> {
                    if (appliedChanges.isEmpty()) return false;
                    AppliedChanges change = new ArrayList<>(appliedChanges).get(0);
                    return change.changed(StrategyWithDayBudget.DAY_BUDGET_DAILY_CHANGE_COUNT)
                            && change.getModel() instanceof StrategyWithDayBudget
                            && ((StrategyWithDayBudget) change.getModel()).getId() == strategyId;
                }));

        verify(campaignRepository).resetDayBudgetDailyChangeCount(eq(job.getShard()), captor.capture());
        assertEquals(new ArrayList<>(cidsToResetChangeCount), captor.getValue());
    }
}
