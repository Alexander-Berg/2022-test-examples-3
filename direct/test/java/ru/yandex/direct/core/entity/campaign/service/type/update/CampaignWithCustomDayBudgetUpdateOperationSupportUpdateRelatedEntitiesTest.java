package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.adgroup.model.StatusAutobudgetShow;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomDayBudget;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class CampaignWithCustomDayBudgetUpdateOperationSupportUpdateRelatedEntitiesTest {

    @Mock
    private DslContextProvider dslContextProvider;
    @Mock
    private AdGroupRepository adGroupRepository;
    @InjectMocks
    private CampaignWithCustomDayBudgetUpdateOperationSupport campaignWithDayBudgetUpdateOperationSupport;

    private static ClientId clientId;
    private static Long uid;
    private static Long campaignId;
    private static CampaignWithCustomDayBudget campaign;
    private static Collection<Long> campaignIds;
    private static int shard;

    public void before(CampaignType campaignType, BigDecimal dayBudget, Boolean needUpdateStatusAutoBudgetShow) {
        initMocks(this);
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
        shard = 1;
        campaignId = RandomNumberUtils.nextPositiveLong();
        campaign = createCampaign(campaignType)
                .withDayBudget(dayBudget);
        campaignIds = needUpdateStatusAutoBudgetShow ? List.of(campaignId) : Collections.emptyList();
    }

    @Test
    @TestCaseName("старый дневной бюджет {0}, новый дневной бюджет {1},"
            + " новый режим показа {2}, должен ли быть запрос в базу")
    @Parameters(method = "parametersForUpdateAdditionTablesTest")
    public void updateAdditionTablesTest_textCampaign(BigDecimal oldDayBudget,
                                                      BigDecimal newDayBudget,
                                                      DayBudgetShowMode dayBudgetShowMode,
                                                      Boolean needUpdateStatusAutoBudgetShow) {
        before(CampaignType.TEXT, oldDayBudget, needUpdateStatusAutoBudgetShow);
        updateAdditionTablesTest(newDayBudget, dayBudgetShowMode);
    }

    @Test
    @TestCaseName("старый дневной бюджет {0}, новый дневной бюджет {1},"
            + " новый режим показа {2}, должен ли быть запрос в базу")
    @Parameters(method = "parametersForUpdateAdditionTablesTest")
    public void updateAdditionTablesTest_dynamicCampaign(BigDecimal oldDayBudget,
                                                         BigDecimal newDayBudget,
                                                         DayBudgetShowMode dayBudgetShowMode,
                                                         Boolean needUpdateStatusAutoBudgetShow) {
        before(CampaignType.DYNAMIC, oldDayBudget, needUpdateStatusAutoBudgetShow);
        updateAdditionTablesTest(newDayBudget, dayBudgetShowMode);
    }

    @Test
    @TestCaseName("старый дневной бюджет {0}, новый дневной бюджет {1},"
            + " новый режим показа {2}, должен ли быть запрос в базу")
    @Parameters(method = "parametersForUpdateAdditionTablesTest")
    public void updateAdditionTablesTest_mcBannerCampaign(BigDecimal oldDayBudget,
                                                          BigDecimal newDayBudget,
                                                          DayBudgetShowMode dayBudgetShowMode,
                                                          Boolean needUpdateStatusAutoBudgetShow) {
        before(CampaignType.MCBANNER, oldDayBudget, needUpdateStatusAutoBudgetShow);
        updateAdditionTablesTest(newDayBudget, dayBudgetShowMode);
    }

    @Test
    @TestCaseName("старый дневной бюджет {0}, новый дневной бюджет {1},"
            + " новый режим показа {2}, должен ли быть запрос в базу")
    @Parameters(method = "parametersForUpdateAdditionTablesTest")
    public void updateAdditionTablesTest_mobileContentCampaign(BigDecimal oldDayBudget,
                                                               BigDecimal newDayBudget,
                                                               DayBudgetShowMode dayBudgetShowMode,
                                                               Boolean needUpdateStatusAutoBudgetShow) {
        before(CampaignType.MOBILE_CONTENT, oldDayBudget, needUpdateStatusAutoBudgetShow);
        updateAdditionTablesTest(newDayBudget, dayBudgetShowMode);
    }

    private void updateAdditionTablesTest(BigDecimal newDayBudget, DayBudgetShowMode dayBudgetShowMode) {
        ModelChanges<CampaignWithCustomDayBudget> modelChanges =
                ModelChanges.build(campaign, TextCampaign.DAY_BUDGET, newDayBudget);
        modelChanges.process(dayBudgetShowMode, TextCampaign.DAY_BUDGET_SHOW_MODE);

        RestrictedCampaignsUpdateOperationContainer updateParameters = RestrictedCampaignsUpdateOperationContainer.create(
                shard,
                uid,
                clientId,
                uid,
                uid);
        campaignWithDayBudgetUpdateOperationSupport.updateRelatedEntitiesInTransaction(
                dslContextProvider.ppc(shard),
                updateParameters,
                List.of(modelChanges.applyTo(campaign)));

        verify(adGroupRepository)
                .updateStatusAutoBudgetShowForCampaign(any(), eq(campaignIds), eq(StatusAutobudgetShow.YES));
    }

    public static Object[][] parametersForUpdateAdditionTablesTest() {
        return new Object[][]{
                {BigDecimal.ZERO, BigDecimal.ONE, DayBudgetShowMode.DEFAULT_, true},
                {BigDecimal.ONE, BigDecimal.TEN, DayBudgetShowMode.STRETCHED, true},
                {BigDecimal.ONE, BigDecimal.TEN, DayBudgetShowMode.DEFAULT_, false},
                {BigDecimal.ONE, BigDecimal.ZERO, DayBudgetShowMode.STRETCHED, false}
        };
    }

    private static CampaignWithCustomDayBudget createCampaign(CampaignType campaignType) {
        CommonCampaign campaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withClientId(clientId.asLong())
                .withName("campaign")
                .withType(ru.yandex.direct.core.entity.campaign.model.CampaignType.valueOf(campaignType.name()))
                .withId(campaignId)
                .withUid(uid);
        return ((CampaignWithCustomDayBudget) campaign)
                .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_);
    }
}
