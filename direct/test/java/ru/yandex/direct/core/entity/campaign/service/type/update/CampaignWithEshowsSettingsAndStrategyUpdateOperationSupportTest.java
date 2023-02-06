package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.context.junit4.rules.SpringClassRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithEshowsSettingsAndStrategy;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.EshowsSettings;
import ru.yandex.direct.core.entity.campaign.model.EshowsVideoType;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAvgCpvCustomPeriodStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAvgCpvStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignType;

@RunWith(Parameterized.class)
public class CampaignWithEshowsSettingsAndStrategyUpdateOperationSupportTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @InjectMocks
    private CampaignWithEshowsSettingsAndStrategyUpdateOperationSupport operationSupport;

    @Mock
    private RequestBasedMetrikaClientAdapter metrikaClientAdapter;

    private static final Long CID = RandomUtils.nextLong();
    private static ClientId clientId;
    private static Long uid;
    private static int shard;
    private static RestrictedCampaignsUpdateOperationContainer updateParameters;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.CPM_BANNER}
        });
    }

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
        shard = 1;

        updateParameters = new RestrictedCampaignsUpdateOperationContainerImpl(
                shard,
                uid,
                clientId,
                uid,
                uid,
                metrikaClientAdapter,
                new CampaignOptions(),
                null,
                emptyMap()
        );
    }

    @Test
    public void successChangeVideoType() {
        changeVideoType(
                EshowsVideoType.LONG_CLICKS,
                EshowsVideoType.COMPLETES,
                EshowsVideoType.COMPLETES,
                defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.now()),
                defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.now())
        );
    }

    @Test
    public void successChangeVideoTypeWithChangeStrategyToAvgCpv() {
        changeVideoType(
                EshowsVideoType.LONG_CLICKS,
                null,
                null,
                defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.now()),
                defaultAvgCpvStrategy(LocalDateTime.now())
        );
    }

    @Test
    public void successChangeVideoTypeWithChangeStrategyToAvgCpvWithCustomPeriod() {
        changeVideoType(
                EshowsVideoType.LONG_CLICKS,
                null,
                null,
                defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.now()),
                defaultAvgCpvCustomPeriodStrategy(LocalDateTime.now())
        );
    }

    @Test
    public void successChangeVideoTypeFromNull() {
        changeVideoType(
                null,
                EshowsVideoType.LONG_CLICKS,
                EshowsVideoType.LONG_CLICKS,
                defaultAvgCpvCustomPeriodStrategy(LocalDateTime.now()),
                defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.now())
        );
    }

    @Test
    public void dropVideoTypeChangeIfStrategyChangeToAvgCpv() {
        changeVideoType(
                EshowsVideoType.LONG_CLICKS,
                EshowsVideoType.COMPLETES,
                null,
                defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.now()),
                defaultAvgCpvStrategy(LocalDateTime.now())
        );
    }

    @Test
    public void dropVideoTypeChangeIfStrategyChangeToAvgCpvWithCustomPeriod() {
        changeVideoType(
                EshowsVideoType.LONG_CLICKS,
                EshowsVideoType.COMPLETES,
                null,
                defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.now()),
                defaultAvgCpvCustomPeriodStrategy(LocalDateTime.now())
        );
    }

    @Test
    public void dropVideoTypeChangeIfAvgCpvStrategy() {
        changeVideoType(
                null,
                EshowsVideoType.COMPLETES,
                null,
                defaultAvgCpvStrategy(LocalDateTime.now()),
                defaultAvgCpvStrategy(LocalDateTime.now())
        );
    }

    @Test
    public void dropVideoTypeChangeIfAvgCpvWithCustomPeriodStrategy() {
        changeVideoType(
                null,
                EshowsVideoType.COMPLETES,
                null,
                defaultAvgCpvCustomPeriodStrategy(LocalDateTime.now()),
                defaultAvgCpvCustomPeriodStrategy(LocalDateTime.now())
        );
    }

    @Test
    public void dropNullVideoTypeChangeIfNotAvgCpvStrategy() {
        changeVideoType(
                EshowsVideoType.LONG_CLICKS,
                null,
                EshowsVideoType.LONG_CLICKS,
                defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.now()),
                defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.now())
        );
    }

    @Test
    public void setDefaultVideoTypeValueIfOldStrategyIsAvgCpv() {
        changeVideoType(
                null,
                null,
                EshowsVideoType.COMPLETES,
                defaultAvgCpvStrategy(LocalDateTime.now()),
                defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.now())
        );
    }

    public void changeVideoType(
        EshowsVideoType oldVideoType,
        EshowsVideoType newVideoType,
        EshowsVideoType expected,
        DbStrategy oldStrategy,
        DbStrategy newStrategy
    ) {
        CampaignWithEshowsSettingsAndStrategy campaign =
                ((CampaignWithEshowsSettingsAndStrategy) newCampaignByCampaignType(campaignType))
                        .withId(CID)
                        .withStrategy(oldStrategy)
                        .withEshowsSettings((new EshowsSettings()).withVideoType(oldVideoType));

        ModelChanges<CampaignWithEshowsSettingsAndStrategy> campaignModelChanges = new ModelChanges<>(CID,
                CampaignWithEshowsSettingsAndStrategy.class);

        campaignModelChanges.process(
                (new EshowsSettings()).withVideoType(newVideoType),
                CampaignWithEshowsSettingsAndStrategy.ESHOWS_SETTINGS
        );
        campaignModelChanges.process(
                newStrategy,
                CampaignWithEshowsSettingsAndStrategy.STRATEGY
        );
        AppliedChanges<CampaignWithEshowsSettingsAndStrategy> campaignAppliedChanges = campaignModelChanges.applyTo(campaign);
        var changes = List.of(campaignAppliedChanges);
        operationSupport.beforeExecution(updateParameters, changes);

        assertThat(
                changes.get(0).getNewValue(CampaignWithEshowsSettingsAndStrategy.ESHOWS_SETTINGS).getVideoType()
        ).isEqualTo(expected);
    }
}
