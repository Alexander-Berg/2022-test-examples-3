package ru.yandex.autotests.direct.cmd.groups.performance;

import java.util.Collections;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.EditAdGroupsPerformanceResponse;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.emptyList;
import static ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper.mapEditGroupResponseToSaveRequest;

@Aqua.Test
@Description("Проверка редактирования фида в ДМО группе контроллером savePerformanceAdGroups")
@Stories(TestFeatures.Groups.FEEDS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SavePerformanceAdGroupsChangeFeedTest extends SavePerformanceAdgroupsTestBase {

    private Long anotherFeedId;

    @Before
    public void before() {
        super.before();
        anotherFeedId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .createFeed(defaultFeed, User.get(CLIENT).getClientID());
    }

    @After
    public void after() {
        super.after();
        if (anotherFeedId != null) {
            cmdRule.cmdSteps().ajaxDeleteFeedsSteps().deleteFeed(CLIENT, anotherFeedId);
        }
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("Проверка сохранения ДМО группы с другим фидом")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9838")
    public void editPerformanceBannerPositiveTest() {
        saveGroup();

        adgroupId = TestEnvironment.newDbSteps().bannersSteps()
                .getBannersByCid(campaignId).get(0).getPid().toString();
        bids = StringUtils.join(TestEnvironment.newDbSteps().bannersSteps()
                .getBannersByCid(campaignId).stream().filter(t -> t.getPid().equals(Long.valueOf(adgroupId)))
                .map(BannersRecord::getBid).collect(Collectors.toList()), ',');

        EditAdGroupsPerformanceResponse actualResponse = cmdRule.cmdSteps().groupsSteps()
                .getEditAdGroupsPerformance(CLIENT, String.valueOf(campaignId), adgroupId, bids);
        expectedGroup = mapEditGroupResponseToSaveRequest(actualResponse.getCampaign().getPerformanceGroups()).get(0);
        expectedGroup.setFeedId(String.valueOf(anotherFeedId));
        if (actualResponse.getCampaign().getPerformanceGroups().get(0).getMinusKeywords().isEmpty()) {
            expectedGroup.setMinusWords(Collections.emptyList());
        }
        expectedGroup.setHrefParams("");
        expectedGroup.setTags(emptyList());
        saveGroup();
    }
}
