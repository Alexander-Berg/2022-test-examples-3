package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.math.BigDecimal;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomDayBudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithDayBudget;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.MCBANNER;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT;

@RunWith(JUnitParamsRunner.class)
public class CampaignWithCustomDayBudgetUpdateOperationSupportTest {
    private static final Long CID = RandomUtils.nextLong();

    @Test
    @Parameters(method = "testData")
    @TestCaseName("oldDayBudget = {0}, newDayBudget = {1}, processChanges = {2}")
    public void checkshouldResetDayBudgetNotificationStatusForTextCampaign(BigDecimal oldDayBudget,
                                                                           @Nullable BigDecimal newDayBudget,
                                                                           boolean processChanges, boolean expected) {
        checkshouldResetDayBudgetNotificationStatus(TEXT, oldDayBudget, newDayBudget, processChanges, expected);
    }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("oldDayBudget = {0}, newDayBudget = {1}, processChanges = {2}")
    public void checkshouldResetDayBudgetNotificationStatusForMcBannerCampaign(BigDecimal oldDayBudget,
                                                                               @Nullable BigDecimal newDayBudget,
                                                                               boolean processChanges,
                                                                               boolean expected) {
        checkshouldResetDayBudgetNotificationStatus(MCBANNER, oldDayBudget, newDayBudget, processChanges, expected);
    }

    public void checkshouldResetDayBudgetNotificationStatus(CampaignType campaignType,
                                                            BigDecimal oldDayBudget,
                                                            @Nullable BigDecimal newDayBudget,
                                                            boolean processChanges,
                                                            boolean expected) {
        CampaignWithCustomDayBudget campaign =
                ((CampaignWithCustomDayBudget) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(CID)
                        .withDayBudget(oldDayBudget);

        ModelChanges<CampaignWithCustomDayBudget> campaignModelChanges = new ModelChanges<>(CID,
                CampaignWithCustomDayBudget.class);

        if (processChanges) {
            campaignModelChanges.process(newDayBudget, CampaignWithDayBudget.DAY_BUDGET);
        }
        AppliedChanges<CampaignWithCustomDayBudget> campaignAppliedChanges = campaignModelChanges.applyTo(campaign);

        boolean actual = CampaignWithCustomDayBudgetUpdateOperationSupport
                .shouldResetDayBudgetNotificationStatus(campaignAppliedChanges);

        assertThat(actual).isEqualTo(expected);
    }

    Iterable<Object[]> testData() {
        return asList(new Object[][]{
                {BigDecimal.ZERO, null, false, false},
                {BigDecimal.ZERO, BigDecimal.TEN, false, false},
                {BigDecimal.ZERO, BigDecimal.ZERO, true, false},
                {BigDecimal.ZERO, BigDecimal.TEN, true, false},
                {BigDecimal.TEN, BigDecimal.ZERO, true, true},
                {BigDecimal.TEN, BigDecimal.ONE, true, false},
                {BigDecimal.TEN, BigDecimal.TEN, true, false},
                {BigDecimal.ONE, BigDecimal.TEN, true, true},
        });
    }

}
