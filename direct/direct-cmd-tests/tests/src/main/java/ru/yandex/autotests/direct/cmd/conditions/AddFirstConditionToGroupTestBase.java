package ru.yandex.autotests.direct.cmd.conditions;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@RunWith(Parameterized.class)
public abstract class AddFirstConditionToGroupTestBase {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    protected BannersRule bannersRule = getBannersRule().withUlogin(getClient());

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    protected Group expectedGroup;

    @Parameterized.Parameter
    public String isSuspended;

    @Parameterized.Parameters(name = "Добавление первого условия с is_suspended = {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"0"},
                {"1"},
        });
    }

    public abstract BannersRule getBannersRule();

    @Description("Добавление первого условия в группу-черновик")
    public void addFistConditionToDraftGroupTest() {
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(getClient(), bannersRule.getCampaignId(), expectedGroup));
        Group actualGroup = bannersRule.getCurrentGroup();
        assertThat("группа соответствует ожиданию", actualGroup,
                beanDiffer(getExpectedGroupDraft()).useCompareStrategy(onlyExpectedFields()));
    }

    @Description("Добавление первого условия в активную группу")
    public void addFistConditionToActiveGroupTest() {
        BsSyncedHelper.makeCampSynced(cmdRule, bannersRule.getCampaignId());
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(getClient(), bannersRule.getCampaignId(), expectedGroup));
        Group actualGroup = bannersRule.getCurrentGroup();
        assertThat("группа соответствует ожиданию", actualGroup,
                beanDiffer(getExpectedGroupActive()).useCompareStrategy(onlyExpectedFields()));
    }

    protected String getClient() {
        return Logins.DEFAULT_CLIENT;
    }

    protected Group getExpectedGroupDraft() {
        return new Group()
                .withStatusModerate(StatusModerate.NEW.toString())
                .withStatusBsSynced(StatusBsSynced.NO.toString())
                .withBanners(singletonList(new Banner()
                        .withStatusModerate(StatusModerate.NEW.toString())
                        .withStatusBsSynced(StatusBsSynced.NO.toString()))
                );
    }

    protected Group getExpectedGroupActive() {
        return new Group()
                .withStatusModerate(StatusModerate.YES.toString())
                .withStatusBsSynced(StatusBsSynced.NO.toString())
                .withBanners(singletonList(new Banner()
                        .withStatusModerate(StatusModerate.YES.toString())
                        .withStatusBsSynced(StatusBsSynced.YES.toString()))
                );
    }
}
