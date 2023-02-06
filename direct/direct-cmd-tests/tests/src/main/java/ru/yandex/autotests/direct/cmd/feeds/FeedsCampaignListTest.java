package ru.yandex.autotests.direct.cmd.feeds;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxFeedsResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.Feed;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedCampaign;
import ru.yandex.autotests.direct.cmd.data.groups.DynamicGroupSource;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка наличие кампании в списке кампаний фида")
@Stories(TestFeatures.Feeds.AJAX_GET_FEEDS)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.AJAX_GET_FEEDS)
@Tag(ObjectTag.FEED)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class FeedsCampaignListTest {

    private static final String CLIENT = "at-direct-backend-feeds-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private BannersRule bannersRule;
    private String feedId;

    public FeedsCampaignListTest(CampaignTypeEnum campaignTypeEnum, BannersRule bannersRule) {
        this.bannersRule = bannersRule;
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Наличие привязанной кампании в списки кампаний фида. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.DMO, new PerformanceBannersRule().withUlogin(CLIENT)},
                {CampaignTypeEnum.DTO, new DynamicBannersRule().withSource(DynamicGroupSource.FEED).withUlogin(CLIENT)}
        });
    }

    @Before
    public void before() {
        feedId = bannersRule.getCurrentGroup().getFeedId();
    }

    @After
    public void after() {
        FeedHelper.deleteAllFeeds(cmdRule, CLIENT);
    }

    @Test
    @Description("Проверяем наличие кампании в списке кампаний фида")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9744")
    public void checkCampaignList() {
        AjaxFeedsResponse response = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(CLIENT);
        Feed feedResponse = response.getFeedsResult().getFeeds().stream()
                .filter(t -> t.getFeedId().equals(feedId)).findFirst().get();
        assertThat("количество кампаний соотетстует ожидаемому", feedResponse.getCampaigns(), hasSize(1));
        assertThat("кампания связана с фидом", feedResponse.getCampaigns().stream()
                .map(FeedCampaign::getId).collect(Collectors.toList()), hasItem(bannersRule.getCampaignId()));
    }
}
