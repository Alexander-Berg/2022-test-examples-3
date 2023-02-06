package ru.yandex.autotests.direct.cmd.feeds.delete;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxDeleteFeedsRequest;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxDeleteFeedsResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedsErrors;
import ru.yandex.autotests.direct.cmd.data.groups.DynamicGroupSource;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка ошибок удаления фида с привязанными кампаниями контроллером ajaxDeleteFeeds")
@Stories(TestFeatures.Feeds.AJAX_DELETE_FEEDS)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.AJAX_DELETE_FEEDS)
@Tag(ObjectTag.FEED)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(CampTypeTag.DYNAMIC)
@RunWith(Parameterized.class)
public class FeedsNegativeWithExistCampAtAjaxDeleteFeedsTest {
    private static final String CLIENT = "at-direct-backend-feeds-c";
    private static final String DELETE_FEEDS_ERROR = FeedsErrors.FEEDS_DELETE_USED_ERROR.toString();

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected BannersRule bannersRule;
    private Long feedId;
    private AjaxDeleteFeedsRequest deleteFeedsRequest;

    public FeedsNegativeWithExistCampAtAjaxDeleteFeedsTest(CampaignTypeEnum campaignTypeEnum, BannersRule bannersRule) {
        this.bannersRule = bannersRule;
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Невозможность удаления привязанного фида. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.DMO, new PerformanceBannersRule().withUlogin(CLIENT)},
                {CampaignTypeEnum.DTO, new DynamicBannersRule().withSource(DynamicGroupSource.FEED).withUlogin(CLIENT)}
        });
    }

    @Before
    public void before() {
        feedId = Long.valueOf(bannersRule.getCurrentGroup().getFeedId());
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .updateFeedsStatus(FeedsUpdateStatus.Done, User.get(CLIENT).getClientID(), feedId);
        deleteFeedsRequest = new AjaxDeleteFeedsRequest();
        deleteFeedsRequest.setFeedsIds(String.valueOf(feedId));
        deleteFeedsRequest.setUlogin(CLIENT);
    }

    @Test
    @Description("Проверяем невозможность удаления фида с кампанией")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9749")
    public void checkInvalidDeleteFeedWithCampaign() {
        AjaxDeleteFeedsResponse actualBean = cmdRule.cmdSteps().ajaxDeleteFeedsSteps().deleteFeed(deleteFeedsRequest);

        assertThat("ошибка соответстует ожиемой", actualBean.getMessage().get(feedId),
                equalTo(getExpectedResponse().getMessage().get(feedId)));
    }

    private AjaxDeleteFeedsResponse getExpectedResponse() {
        HashMap<Long, String> expectedHashMap = new HashMap<>();
        expectedHashMap.put(feedId, DELETE_FEEDS_ERROR);
        return new AjaxDeleteFeedsResponse().withMessage(expectedHashMap);
    }
}
