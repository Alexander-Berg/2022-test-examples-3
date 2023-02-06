package ru.yandex.autotests.direct.cmd.campaigns.showcamp;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterests;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterestsFactory;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.retargeting.RetargetingHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Просмотр интересов РМП кампаниях")
@Stories(TestFeatures.Campaigns.SHOW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.MOBILE)
public class ShowCampTargetInterestsTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private MobileBannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;
    private TargetInterests expectedInterests;
    private Long categoryId;

    public ShowCampTargetInterestsTest() {
        categoryId = RetargetingHelper.getRandomTargetCategoryId();
        bannersRule = new MobileBannersRule()
                .overrideGroupTemplate(new Group().withTargetInterests(
                        Collections.singletonList(
                                TargetInterestsFactory.defaultTargetInterest(categoryId))
                        )
                )
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        expectedInterests = new TargetInterests()
                .withTargetCategoryId(categoryId)
                .withPriceContext(0.78d);
    }

    @Test
    @Description("интересы в showCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10642")
    public void showCamp() {
        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT,
                bannersRule.getCampaignId().toString());

        List<TargetInterests> actualInterests = showCamp.getGroups().get(0).getTargetInterests();

        assumeThat("Сохранилось ождаемое число интересов", actualInterests, hasSize(1));

        assertThat("Таргетинг на интересы соответсвует ожиданием",
                actualInterests.get(0),
                beanDiffer(expectedInterests).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @Description("интересы в showCampMultiEdit")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10643")
    public void showCampMultiEdit() {
        ShowCampMultiEditResponse showCampMultiEditResponse = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(CLIENT,
                bannersRule.getCampaignId());

        List<TargetInterests> actualInterests = showCampMultiEditResponse.getCampaign().getGroups().get(0).getTargetInterests();

        assumeThat("Сохранилось ождаемое число интересов", actualInterests, hasSize(1));

        assertThat("Таргетинг на интересы соответсвует ожиданием",
                actualInterests.get(0),
                beanDiffer(expectedInterests).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }
}
