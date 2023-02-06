package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmCampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxReachCustomPeriodDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmStrategy;

@ParametersAreNonnullByDefault
public class CpmCampaignWithCustomStrategyUpdateOperationSupportTest {
    private static long campaignId;
    private static CpmCampaignWithCustomStrategyUpdateOperationSupport testingSupport;
    private static RestrictedCampaignsUpdateOperationContainer updateContainer;
    private static LocalDateTime now;

    @BeforeClass
    public static void beforeClass() {
        updateContainer = RestrictedCampaignsUpdateOperationContainer.create(RandomNumberUtils.nextPositiveInteger(),
                RandomNumberUtils.nextPositiveLong(), ClientId.fromLong(RandomNumberUtils.nextPositiveLong()),
                RandomNumberUtils.nextPositiveLong(), RandomNumberUtils.nextPositiveLong());
        campaignId = RandomNumberUtils.nextPositiveLong();
        now = LocalDateTime.now();

        testingSupport = new CpmCampaignWithCustomStrategyUpdateOperationSupport();
    }

    @Test
    public void updateStrategyWithCustomPeriodToSameStrategyWithAnotherFinishDate_IncreaseCounter() {
        DbStrategy oldStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now.minusDays(1));
        long dailyChangeCount = 3L;
        oldStrategy.getStrategyData()
                .withLastUpdateTime(now)
                .withDailyChangeCount(dailyChangeCount);

        CpmCampaignWithCustomStrategy oldCampaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withStrategy(oldStrategy);

        ModelChanges<CpmCampaignWithCustomStrategy> cpmCampaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, CpmCampaignWithCustomStrategy.class);
        DbStrategy newStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now.minusDays(1));
        newStrategy.getStrategyData()
                .withFinish(oldStrategy.getStrategyData().getFinish().plusDays(1));
        cpmCampaignWithCustomStrategyModelChanges.process(
                newStrategy,
                CpmCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<CpmCampaignWithCustomStrategy> appliedChanges =
                cpmCampaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
        testingSupport.onChangesApplied(updateContainer,
                List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getDailyChangeCount())
                .isEqualTo(dailyChangeCount + 1);
        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getLastUpdateTime())
                .isNotEqualTo(now);
    }

    @Test
    public void updateStrategyWithCustomPeriodToSameStrategyWithAnotherBudget_IncreaseCounter() {
        DbStrategy oldStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now.minusDays(1));
        long dailyChangeCount = 3L;
        oldStrategy.getStrategyData()
                .withLastUpdateTime(now)
                .withDailyChangeCount(dailyChangeCount);

        CpmCampaignWithCustomStrategy oldCampaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withStrategy(oldStrategy);

        ModelChanges<CpmCampaignWithCustomStrategy> cpmCampaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, CpmCampaignWithCustomStrategy.class);
        DbStrategy newStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now.minusDays(1));
        newStrategy.getStrategyData()
                .withBudget(oldStrategy.getStrategyData().getBudget().add(BigDecimal.TEN));
        cpmCampaignWithCustomStrategyModelChanges.process(
                newStrategy,
                CpmCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<CpmCampaignWithCustomStrategy> appliedChanges =
                cpmCampaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
        testingSupport.onChangesApplied(updateContainer,
                List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getDailyChangeCount())
                .isEqualTo(dailyChangeCount + 1);
        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getLastUpdateTime())
                .isNotEqualTo(now);
    }

    @Test
    public void updateStrategyWithCustomPeriodToManualStrategy_RemoveDailyChangeCountAndLastUpdate() {
        DbStrategy oldStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now);
        long dailyChangeCount = 3L;
        oldStrategy.getStrategyData()
                .withLastUpdateTime(now)
                .withDailyChangeCount(dailyChangeCount);

        CpmCampaignWithCustomStrategy oldCampaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withStrategy(oldStrategy);

        ModelChanges<CpmCampaignWithCustomStrategy> cpmCampaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, CpmCampaignWithCustomStrategy.class);
        cpmCampaignWithCustomStrategyModelChanges.process(
                defaultCpmStrategy(),
                CpmCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<CpmCampaignWithCustomStrategy> appliedChanges =
                cpmCampaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
        testingSupport.onChangesApplied(updateContainer, List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getDailyChangeCount())
                .isNull();
        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getLastUpdateTime())
                .isNull();
    }

    @Test
    public void updateStrategyWithCustomPeriodToSameStrategyWithAnotherBudget_StrategyWasNotStarted_CounterNotChanged() {
        DbStrategy oldStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now.plusDays(2));
        long dailyChangeCount = 3L;
        oldStrategy.getStrategyData()
                .withLastUpdateTime(now)
                .withDailyChangeCount(dailyChangeCount);

        CpmCampaignWithCustomStrategy oldCampaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withStrategy(oldStrategy);

        ModelChanges<CpmCampaignWithCustomStrategy> cpmCampaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, CpmCampaignWithCustomStrategy.class);
        DbStrategy newStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now.plusDays(2));
        newStrategy.getStrategyData()
                .withBudget(oldStrategy.getStrategyData().getBudget().add(BigDecimal.TEN));
        cpmCampaignWithCustomStrategyModelChanges.process(
                newStrategy,
                CpmCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<CpmCampaignWithCustomStrategy> appliedChanges =
                cpmCampaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
        testingSupport.onChangesApplied(updateContainer,
                List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getDailyChangeCount())
                .isEqualTo(dailyChangeCount);
        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getLastUpdateTime())
                .isEqualTo(now);
    }

    @Test
    public void updateStrategyWithCustomPeriodToSameStrategy_CounterNotChanged() {
        DbStrategy oldStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now);
        long dailyChangeCount = 3L;
        oldStrategy.getStrategyData()
                .withLastUpdateTime(now)
                .withDailyChangeCount(dailyChangeCount);

        CpmCampaignWithCustomStrategy oldCampaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withStrategy(oldStrategy);

        ModelChanges<CpmCampaignWithCustomStrategy> cpmCampaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, CpmCampaignWithCustomStrategy.class);
        cpmCampaignWithCustomStrategyModelChanges.process(
                defaultAutobudgetMaxReachCustomPeriodDbStrategy(LocalDateTime.now()),
                CpmCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<CpmCampaignWithCustomStrategy> appliedChanges =
                cpmCampaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
        testingSupport.onChangesApplied(updateContainer, List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getDailyChangeCount())
                .isEqualTo(dailyChangeCount);
        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getLastUpdateTime())
                .isEqualTo(now);
    }

    @Test
    public void updateStrategyWithCustomPeriodToStrategyWithAnotherAutoProlongation_CounterNotChanged() {
        DbStrategy oldStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now);
        long dailyChangeCount = 3L;
        oldStrategy.getStrategyData()
                .withLastUpdateTime(now)
                .withDailyChangeCount(dailyChangeCount);

        CpmCampaignWithCustomStrategy oldCampaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withStrategy(oldStrategy);

        ModelChanges<CpmCampaignWithCustomStrategy> cpmCampaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, CpmCampaignWithCustomStrategy.class);
        DbStrategy newStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(LocalDateTime.now());
        newStrategy.getStrategyData().setAutoProlongation(0L);
        newStrategy.getStrategyData().setBudget(oldStrategy.getStrategyData().getBudget().setScale(3));
        cpmCampaignWithCustomStrategyModelChanges.process(
                newStrategy,
                CpmCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<CpmCampaignWithCustomStrategy> appliedChanges =
                cpmCampaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
        testingSupport.onChangesApplied(updateContainer,
                List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getDailyChangeCount())
                .isEqualTo(dailyChangeCount);
        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getLastUpdateTime())
                .isEqualTo(now);
    }

    @Test
    public void updateStrategyWithCustomPeriodToSameStrategy_LastUpdateTimeWasYesterday_CounterNoChanged() {
        DbStrategy oldStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now);
        long dailyChangeCount = 3L;
        oldStrategy.getStrategyData()
                .withLastUpdateTime(now.minusDays(1))
                .withDailyChangeCount(dailyChangeCount);

        CpmCampaignWithCustomStrategy oldCampaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withStrategy(oldStrategy);

        ModelChanges<CpmCampaignWithCustomStrategy> cpmCampaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, CpmCampaignWithCustomStrategy.class);
        cpmCampaignWithCustomStrategyModelChanges.process(
                defaultAutobudgetMaxReachCustomPeriodDbStrategy(LocalDateTime.now()),
                CpmCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<CpmCampaignWithCustomStrategy> appliedChanges =
                cpmCampaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
        testingSupport.onChangesApplied(updateContainer,
                List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getDailyChangeCount())
                .isEqualTo(3L);

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getLastUpdateTime())
                .isEqualTo(now.minusDays(1));
    }

    @Test
    public void updateStrategyWithCustomPeriodToStrategyWithAnotherStartDate_ResetCounter() {
        DbStrategy oldStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now);
        long dailyChangeCount = 3L;
        oldStrategy.getStrategyData()
                .withLastUpdateTime(now)
                .withDailyChangeCount(dailyChangeCount);

        CpmCampaignWithCustomStrategy oldCampaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withStrategy(oldStrategy);

        ModelChanges<CpmCampaignWithCustomStrategy> cpmCampaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, CpmCampaignWithCustomStrategy.class);
        DbStrategy newStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(LocalDateTime.now());
        newStrategy.getStrategyData().setAutoProlongation(0L);
        newStrategy.getStrategyData().setStart(now.plusDays(1).toLocalDate());
        cpmCampaignWithCustomStrategyModelChanges.process(
                newStrategy,
                CpmCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<CpmCampaignWithCustomStrategy> appliedChanges =
                cpmCampaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
        testingSupport.onChangesApplied(updateContainer,
                List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getDailyChangeCount())
                .isEqualTo(1L);
        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getLastUpdateTime())
                .isNotEqualTo(now);
    }

    @Test
    public void updateStrategyWithCustomPeriodToStrategyWithAnotherAvgCpm_ResetCounter() {
        DbStrategy oldStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now);
        long dailyChangeCount = 3L;
        oldStrategy.getStrategyData()
                .withLastUpdateTime(now)
                .withDailyChangeCount(dailyChangeCount);

        CpmCampaignWithCustomStrategy oldCampaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withStrategy(oldStrategy);

        ModelChanges<CpmCampaignWithCustomStrategy> cpmCampaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, CpmCampaignWithCustomStrategy.class);
        DbStrategy newStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(LocalDateTime.now());
        newStrategy.getStrategyData().setAvgCpm(BigDecimal.TEN);
        cpmCampaignWithCustomStrategyModelChanges.process(
                newStrategy,
                CpmCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<CpmCampaignWithCustomStrategy> appliedChanges =
                cpmCampaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
        testingSupport.onChangesApplied(updateContainer,
                List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getDailyChangeCount())
                .isEqualTo(dailyChangeCount + 1);
        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getLastUpdateTime())
                .isNotEqualTo(now);
    }

    @Test
    public void updateStrategyWithCustomPeriodToAnotherStrategyWithCustomPeriod_ResetCounter() {
        DbStrategy oldStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now);
        long dailyChangeCount = 3L;
        oldStrategy.getStrategyData()
                .withLastUpdateTime(now)
                .withDailyChangeCount(dailyChangeCount);

        CpmCampaignWithCustomStrategy oldCampaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withStrategy(oldStrategy);

        ModelChanges<CpmCampaignWithCustomStrategy> cpmCampaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, CpmCampaignWithCustomStrategy.class);
        DbStrategy newStrategy = defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.now());
        newStrategy.getStrategyData().setDailyChangeCount(null);
        cpmCampaignWithCustomStrategyModelChanges.process(
                newStrategy,
                CpmCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<CpmCampaignWithCustomStrategy> appliedChanges =
                cpmCampaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
        testingSupport.onChangesApplied(updateContainer,
                List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getDailyChangeCount())
                .isEqualTo(1L);
        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getLastUpdateTime())
                .isNotEqualTo(now);
    }

    @Test
    public void updateDefaultStrategyToStrategyWithCustomPeriod_ResetCounter() {
        DbStrategy oldStrategy = defaultCpmStrategy();

        CpmCampaignWithCustomStrategy oldCampaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withStrategy(oldStrategy);

        ModelChanges<CpmCampaignWithCustomStrategy> cpmCampaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, CpmCampaignWithCustomStrategy.class);
        cpmCampaignWithCustomStrategyModelChanges.process(
                defaultAutobudgetMaxReachCustomPeriodDbStrategy(LocalDateTime.now()),
                CpmCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<CpmCampaignWithCustomStrategy> appliedChanges =
                cpmCampaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
        testingSupport.onChangesApplied(updateContainer,
                List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getDailyChangeCount())
                .isEqualTo(1);
        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getLastUpdateTime())
                .isNotEqualTo(now);
    }

    @Test
    public void updateStrategyWithCustomPeriodToSameStrategyWithAnotherBudget_LastUpdateTimeWasYesterday_ResetCounter() {
        DbStrategy oldStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now.minusDays(1));
        long dailyChangeCount = 3L;
        oldStrategy.getStrategyData()
                .withLastUpdateTime(now.minusDays(1))
                .withDailyChangeCount(dailyChangeCount);

        CpmCampaignWithCustomStrategy oldCampaign = new CpmBannerCampaign()
                .withId(campaignId)
                .withStrategy(oldStrategy);

        ModelChanges<CpmCampaignWithCustomStrategy> cpmCampaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, CpmCampaignWithCustomStrategy.class);
        DbStrategy newStrategy = defaultAutobudgetMaxReachCustomPeriodDbStrategy(now.minusDays(1));
        newStrategy.getStrategyData().setBudget(BigDecimal.TEN);
        cpmCampaignWithCustomStrategyModelChanges.process(
                newStrategy,
                CpmCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<CpmCampaignWithCustomStrategy> appliedChanges =
                cpmCampaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
        testingSupport.onChangesApplied(updateContainer,
                List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getDailyChangeCount())
                .isEqualTo(1L);
        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getLastUpdateTime())
                .isNotEqualTo(now);
    }
}
