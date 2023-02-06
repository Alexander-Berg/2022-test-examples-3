package ru.yandex.autotests.direct.cmd.conditions.common;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

public abstract class AfterLastConditionTestBase {

    protected static final String PRICE_CONTEXT = "3";
    private static final String CLIENT = "at-direct-backend-c5";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected BannersRule bannersRule;
    protected String campaignId;
    protected Group expectedGroup;
    protected Banner expectedBanner;


    public AfterLastConditionTestBase(BannersRule bannersRule) {
        this.bannersRule = bannersRule.withUlogin(getClient());
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId().toString();
        BsSyncedHelper.makeCampSynced(cmdRule, bannersRule.getCampaignId());

        expectedGroup = bannersRule.getCurrentGroup();
        expectedBanner = expectedGroup.getBanners().get(0);
    }

    protected abstract void deleteCondition();

    protected abstract void suspendCondition();

    protected String getClient() {
        return CLIENT;
    }

    @Description("Проверка сброса статусов после удаления последнего условия показа")
    public void deleteConditionTest() {
        deleteCondition();
        check();
    }

    @Description("Проверка сброса статусов после остановки последнего условия показа")
    public void suspendConditionTest() {
        suspendCondition();
        check();
    }

    private void check() {
        Group actualGroup = bannersRule.getCurrentGroup();

        assertThat("статусы установились", actualGroup,
                beanDiffer(getExpectedGroupStatuses()).useCompareStrategy(onlyExpectedFields()));
    }

    protected Group getExpectedGroupStatuses() {
        return new Group().withStatusModerate(expectedGroup.getStatusModerate())
                .withStatusPostModerate(expectedGroup.getStatusPostModerate())
                .withStatusBsSynced(StatusBsSynced.NO.toString())
                .withBanners(singletonList(new Banner()
                        .withStatusModerate(expectedBanner.getStatusModerate())
                        .withPhoneFlag(expectedBanner.getPhoneFlag())
                        .withStatusBsSynced(expectedBanner.getStatusBsSynced())
                        .withStatusPostModerate(expectedBanner.getStatusPostModerate())));
    }
}
