package ru.yandex.autotests.direct.cmd.campaigns.showcamp;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.interest.InterestCategory;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterestsFactory;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.steps.retargeting.RetargetingHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Просмотр категорий интересов с родительскими категориями в РМП кампаниях")
@Stories(TestFeatures.Campaigns.SHOW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.MOBILE)
public class ShowCampTargetInterestsWithParentTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private MobileBannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;
    private Long parentCategoryId = RetargetingHelper.getParentCategoryId();
    private Long categoryId = RetargetingHelper.getChildCategoryId();

    public ShowCampTargetInterestsWithParentTest() {
        bannersRule = new MobileBannersRule()
                .overrideGroupTemplate(new Group().withTargetInterests(
                        Collections.singletonList(
                                TargetInterestsFactory.defaultTargetInterest(parentCategoryId))
                        )
                )
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }


    @Before
    public void before() {
        RetargetingHelper.setParentCategoryId(categoryId, parentCategoryId);
    }

    @After
    public void afrer() {
        RetargetingHelper.setParentCategoryId(categoryId, null);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10644")
    public void showCamp() {
        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT,
                bannersRule.getCampaignId().toString());

        List<InterestCategory> actualInterests = showCamp.getInterestCategories().stream()
                .filter(x -> x.getTargetCategoryId().equals(parentCategoryId)).collect(Collectors.toList());

        assumeThat("Категория не вернулась в корне", actualInterests
                        .stream().map(InterestCategory::getTargetCategoryId).collect(Collectors.toList()),
                not(hasItem(categoryId)));

        assertThat("Категория вернулась в детях", actualInterests.get(0).getChilds()
                        .stream().map(InterestCategory::getTargetCategoryId).collect(Collectors.toList()),
                hasItem(categoryId));

    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10645")
    public void showCampCheckAvailable() {
        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT,
                bannersRule.getCampaignId().toString());

        assertThat("Категория доступна",
                showCamp.getInterestCategories().stream()
                        .filter(x -> x.getTargetCategoryId().equals(parentCategoryId))
                        .findFirst()
                        .orElseThrow(() -> new DirectCmdStepsException("не найден интерес"))
                        .getAvailable(),
                equalTo(0L)
        );
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10646")
    public void showCampMultiEdit() {
        ShowCampMultiEditResponse showCampMultiEditResponse = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(CLIENT,
                bannersRule.getCampaignId());

        List<InterestCategory> actualInterests = showCampMultiEditResponse.getInterestCategories().stream()
                .filter(x -> x.getTargetCategoryId().equals(parentCategoryId)).collect(Collectors.toList());

        assumeThat("Категория не вернулась в корне", actualInterests
                        .stream().map(InterestCategory::getTargetCategoryId).collect(Collectors.toList()),
                not(hasItem(categoryId)));

        assertThat("Категория вернулась в детях", actualInterests.get(0).getChilds()
                        .stream().map(InterestCategory::getTargetCategoryId).collect(Collectors.toList()),
                hasItem(categoryId));
    }
}
