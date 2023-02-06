package ru.yandex.autotests.direct.cmd.campaigns.showcamp;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.interest.InterestCategory;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterests;
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
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Просмотр категорий интересов в РМП кампаниях")
@Stories(TestFeatures.Campaigns.SHOW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.MOBILE)
public class ShowCampInterestCategoriesTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private MobileBannersRule bannersRule = new MobileBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private TargetInterests expectedInterests;
    private Long categoryId;

    @Before()
    public void before() {
        categoryId = RetargetingHelper.getRandomTargetCategoryId();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10639")
    public void showCamp() {
        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT,
                bannersRule.getCampaignId().toString());

        List<Long> actualInterests = showCamp.getInterestCategories().stream()
                .map(InterestCategory::getTargetCategoryId).collect(Collectors.toList());

        assertThat("Категории интересом содежрат ожидаемое значение", actualInterests, hasItem(categoryId));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10640")
    public void showCampCheckAvailable() {
        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT,
                bannersRule.getCampaignId().toString());

        assertThat("Категория доступна",
                showCamp.getInterestCategories().stream().filter(x -> x.getTargetCategoryId().equals(categoryId))
                        .findFirst().orElseThrow(() -> new DirectCmdStepsException("не найден интерес")).getAvailable(),
                equalTo(1l)
        );
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10641")
    public void showCampMultiEdit() {
        ShowCampMultiEditResponse showCampMultiEditResponse = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(CLIENT,
                bannersRule.getCampaignId());

        List<Long> actualInterests = showCampMultiEditResponse.getInterestCategories().stream()
                .map(InterestCategory::getTargetCategoryId).collect(Collectors.toList());

        assertThat("Категории интересов содежрат ожидаемое значение", actualInterests, hasItem(categoryId));
    }
}
