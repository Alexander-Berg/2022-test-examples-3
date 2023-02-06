package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.DayBudgetChangeLogRecord;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategyAndCustomDayBudget;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.DayBudgetChangeLogRecord.LOG_DAY_BUDGET_CHANGE_CMD_NAME;
import static ru.yandex.direct.core.entity.campaign.service.type.update.CampaignWithCustomStrategyAndCustomDayBudgetUpdateOperationSupport.getDayBudgetLogRecord;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(Parameterized.class)
public class CampaignServiceLogDayBudgetChangeTest {

    private static final CurrencyCode CURRENCY_CODE = CurrencyCode.USD;
    private static final Long UID = RandomUtils.nextLong();
    private static final Long CID = RandomUtils.nextLong();
    private static final int SHARD = 1;

    @Autowired
    FeatureService featureService;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MCBANNER},
        });
    }

    @Test
    public void checkLoggingChangeDayBudget_hasChanges() {
        CampaignWithCustomStrategyAndCustomDayBudget campaign =
                ((CampaignWithCustomStrategyAndCustomDayBudget) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(CID)
                        .withCurrency(CURRENCY_CODE)
                        .withDayBudget(Currencies.getCurrency(CURRENCY_CODE).getMinDayBudget());

        ModelChanges<CampaignWithCustomStrategyAndCustomDayBudget> campaignModelChanges = new ModelChanges<>(CID,
                CampaignWithCustomStrategyAndCustomDayBudget.class);

        BigDecimal expectedNewDayBudget = new BigDecimal("123.45");
        campaignModelChanges.process(expectedNewDayBudget, CampaignWithCustomStrategyAndCustomDayBudget.DAY_BUDGET);
        AppliedChanges<CampaignWithCustomStrategyAndCustomDayBudget> campaignAppliedChanges =
                campaignModelChanges.applyTo(campaign);

        DayBudgetChangeLogRecord logRecord = getDayBudgetLogRecord(campaignAppliedChanges, UID);
        Map<String, Object> params = logRecord.getParam();

        Map<String, Object> expectedParams = ImmutableMap.<String, Object>builder()
                .put("cid", campaign.getId())
                .put("new_day_budget", expectedNewDayBudget.toPlainString())
                .put("old_day_budget", Currencies.getCurrency(CURRENCY_CODE).getMinDayBudget().toPlainString())
                .put("currency", CURRENCY_CODE.name())
                .build();

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(params)
                .is(matchedBy(beanDiffer(expectedParams)));
        softAssertions.assertThat(logRecord.getPath())
                .isEqualTo(LOG_DAY_BUDGET_CHANGE_CMD_NAME);
        softAssertions.assertThat(logRecord.getCids())
                .isEqualTo(List.of(CID));
        softAssertions.assertThat(logRecord.getOperatorUid())
                .isEqualTo(UID);

        softAssertions.assertAll();
    }

    @Test
    public void checkChangesFilteringForLogging_noChanges() {
        CampaignWithCustomStrategyAndCustomDayBudget campaign =
                ((CampaignWithCustomStrategyAndCustomDayBudget) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(CID)
                        .withCurrency(CURRENCY_CODE)
                        .withDayBudget(Currencies.getCurrency(CURRENCY_CODE).getMinDayBudget());

        ModelChanges<CampaignWithCustomStrategyAndCustomDayBudget> campaignModelChanges = new ModelChanges<>(CID,
                CampaignWithCustomStrategyAndCustomDayBudget.class);
        AppliedChanges<CampaignWithCustomStrategyAndCustomDayBudget> campaignAppliedChanges =
                campaignModelChanges.applyTo(campaign);

        CampaignWithCustomStrategyAndCustomDayBudgetUpdateOperationSupport operationSupport =
                spy(new CampaignWithCustomStrategyAndCustomDayBudgetUpdateOperationSupport(featureService));

        RestrictedCampaignsUpdateOperationContainer updateParameters = getUpdateCampaignParametersContainer();

        operationSupport.updateRelatedEntitiesOutOfTransaction(updateParameters, List.of(campaignAppliedChanges));

        verify(operationSupport).logDayBudgetChange(Collections.emptyList(), UID);
    }

    private RestrictedCampaignsUpdateOperationContainer getUpdateCampaignParametersContainer() {
        return RestrictedCampaignsUpdateOperationContainer.create(
                SHARD,
                UID,
                ClientId.fromLong(UID),
                UID,
                UID);
    }

    @Test
    public void checkChangesFilteringForLogging_hasDayBudgetChanges() {
        CampaignWithCustomStrategyAndCustomDayBudget campaign =
                ((CampaignWithCustomStrategyAndCustomDayBudget) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(CID)
                        .withCurrency(CURRENCY_CODE)
                        .withDayBudget(Currencies.getCurrency(CURRENCY_CODE).getMinDayBudget());

        ModelChanges<CampaignWithCustomStrategyAndCustomDayBudget> campaignModelChanges = new ModelChanges<>(CID,
                CampaignWithCustomStrategyAndCustomDayBudget.class);

        campaignModelChanges.process(BigDecimal.ZERO, CampaignWithCustomStrategyAndCustomDayBudget.DAY_BUDGET);
        AppliedChanges<CampaignWithCustomStrategyAndCustomDayBudget> campaignAppliedChanges =
                campaignModelChanges.applyTo(campaign);

        CampaignWithCustomStrategyAndCustomDayBudgetUpdateOperationSupport operationSupport =
                spy(new CampaignWithCustomStrategyAndCustomDayBudgetUpdateOperationSupport(featureService));

        RestrictedCampaignsUpdateOperationContainer updateParameters = getUpdateCampaignParametersContainer();

        operationSupport.updateRelatedEntitiesOutOfTransaction(updateParameters, List.of(campaignAppliedChanges));


        verify(operationSupport).logDayBudgetChange(List.of(campaignAppliedChanges), UID);
    }

    @Test
    public void checkChangesFilteringForLogging_hasNonDayBudgetChanges() {
        CampaignWithCustomStrategyAndCustomDayBudget campaign =
                ((CampaignWithCustomStrategyAndCustomDayBudget) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(CID)
                        .withCurrency(CURRENCY_CODE)
                        .withDayBudget(Currencies.getCurrency(CURRENCY_CODE).getMinDayBudget());

        ModelChanges<CampaignWithCustomStrategyAndCustomDayBudget> campaignModelChanges = new ModelChanges<>(CID,
                CampaignWithCustomStrategyAndCustomDayBudget.class);

        AppliedChanges<CampaignWithCustomStrategyAndCustomDayBudget> campaignAppliedChanges =
                campaignModelChanges.applyTo(campaign);

        CampaignWithCustomStrategyAndCustomDayBudgetUpdateOperationSupport operationSupport =
                spy(new CampaignWithCustomStrategyAndCustomDayBudgetUpdateOperationSupport(featureService));

        RestrictedCampaignsUpdateOperationContainer updateParameters = getUpdateCampaignParametersContainer();

        operationSupport.updateRelatedEntitiesOutOfTransaction(updateParameters, List.of(campaignAppliedChanges));


        verify(operationSupport).logDayBudgetChange(Collections.emptyList(), UID);
    }

}
