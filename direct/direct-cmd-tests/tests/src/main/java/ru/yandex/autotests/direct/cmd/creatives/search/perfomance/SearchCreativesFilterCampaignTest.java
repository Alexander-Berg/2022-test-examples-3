package ru.yandex.autotests.direct.cmd.creatives.search.perfomance;


import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.creatives.Creative;
import ru.yandex.autotests.direct.cmd.data.creatives.SearchCreativesFilterEnum;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersPerformanceRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Поиск креативов по связки с кампанией через searchCreative (страница креативов)")
@Stories(TestFeatures.Creatives.FILTERS)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
public class SearchCreativesFilterCampaignTest {

    private static final String CLIENT = "at-direct-search-creatives1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private PerformanceBannersRule bannersRule1 = new PerformanceBannersRule().withUlogin(CLIENT);
    private PerformanceBannersRule bannersRule2 = new PerformanceBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule1, bannersRule2);


    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
    }

    @Test
    @Description("Поиск креатива, связанного с одной кампанией, по ид кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10829")
    public void testSearchCreativesFilterOneCampaign() {
        List<Long> creativesIds = cmdRule.cmdSteps().creativesSteps()
                .searchCreatives(SearchCreativesFilterEnum.CAMPAIGNS, bannersRule1.getCampaignId().toString()).stream()
                .map(Creative::getCreativeId)
                .collect(Collectors.toList());
        assertThat("найденный креатив в ответе ручки searchCreatives соответствует ожидаемому",
                creativesIds, contains(bannersRule1.getCreativeId()));
    }

    @Test
    @Description("Поиск креатива, связанного с несколькими кампаниями, по ид кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10828")
    public void testSearchCreativesFilterTwoCampaign() {
        connectBannerWithCreative(bannersRule2.getBannerId(), bannersRule1.getCreativeId());
        List<Creative> creatives = cmdRule.cmdSteps().creativesSteps()
                .searchCreatives(SearchCreativesFilterEnum.CAMPAIGNS, bannersRule2.getCampaignId().toString());
        assertThat("найденный креатив в ответе ручки searchCreatives соответствует ожидаемому",
                creatives.stream()
                        .map(Creative::getCreativeId)
                        .collect(Collectors.toList()), contains(bannersRule1.getCreativeId()));
    }

    private void connectBannerWithCreative(long bid, long creativeId) {
        BannersPerformanceRecord banner = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannersPerformanceSteps().findBannersPerformance(bid).get(0);
        banner.setCreativeId(creativeId);
        TestEnvironment.newDbSteps().bannersPerformanceSteps().updateBannersPerformanceById(banner);
    }
}
