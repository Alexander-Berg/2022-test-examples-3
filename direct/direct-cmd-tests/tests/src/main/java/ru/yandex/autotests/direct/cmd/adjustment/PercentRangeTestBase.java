package ru.yandex.autotests.direct.cmd.adjustment;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.DemographyMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.GroupMultiplierStats;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.MobileMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.RetargetingMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.model.User;

public abstract class PercentRangeTestBase {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    protected TextBannersRule bannersRule = new TextBannersRule().withUlogin(getLogin());

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    protected String campaignId;
    protected Integer retargetingId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId().toString();
        TestEnvironment.newDbSteps().useShardForLogin(getLogin()).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(getLogin()).getClientID()));
        cmdRule.apiSteps().retargetingSteps().addConditionsForUser(getLogin(), 1);
        retargetingId = cmdRule.apiSteps().retargetingSteps().getRetargetingConditions(getLogin())[0];
    }

    protected abstract String getLogin();

    protected HierarchicalMultipliers getHierarchicalMultipliers(String mobileMultiplierPct,
                                                                 String demographyMultiplierPct,
                                                                 String retargetingMultiplierPct) {
        HierarchicalMultipliers hierarchicalMultipliers = new HierarchicalMultipliers();
        if (mobileMultiplierPct != null) {
            hierarchicalMultipliers.withMobileMultiplier(MobileMultiplier.
                    getDefaultMobileMultiplier(mobileMultiplierPct));
        }
        if (demographyMultiplierPct != null) {
            hierarchicalMultipliers.withDemographyMultiplier(DemographyMultiplier.
                    getDefaultDemographyMultiplier(demographyMultiplierPct));
        }
        if (retargetingMultiplierPct != null) {
            hierarchicalMultipliers.withRetargetingMultiplier(RetargetingMultiplier.
                    getDefaultRetargetingMultiplier(retargetingId.toString(), retargetingMultiplierPct));
        }
        return hierarchicalMultipliers;
    }

    protected Group getGroup(String mobileMultiplierPct,
                             String demographyMultiplierPct,
                             String retargetingMultiplierPct) {
        Group group = bannersRule.getGroup()
                .withAdGroupID(bannersRule.getGroupId().toString())
                .withHierarchicalMultipliers(
                        getHierarchicalMultipliers(mobileMultiplierPct,
                                demographyMultiplierPct,
                                retargetingMultiplierPct));
        group.getBanners().get(0).withBid(bannersRule.getBannerId());
        return group;
    }

    protected GroupMultiplierStats getExpectedGroupMultiplierStats(String expectedLowerBound, String expectedUpperBound) {
        return new GroupMultiplierStats()
                .withAdjustmentsLowerBound(expectedLowerBound)
                .withAdjustmentsUpperBound(expectedUpperBound);
    }
}
