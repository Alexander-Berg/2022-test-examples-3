package ru.yandex.autotests.direct.intapi.java.tests.creatives;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.data.Logins;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.models.CreativeCampaignResult;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка работы DisplayCanvas get_creatives_campaigns")
@Stories(TestFeatures.DisplayCanvas.GET_CREATIVES_CAMPAIGNS)
@Features(TestFeatures.DISPLAY_CANVAS)
@Tag(Tags.DISPLAY_CANVAS)
@Tag(TagDictionary.TRUNK)
@Tag("DIRECT-64379")
@Issue("DIRECT-64379")
public class DisplayCanvasCampaignTest {
    private static Long clientId;
    private static Long operatorUid;
    private static String CLIENT = "at-direct-bssync-banners1";
    private static String SUPER = Logins.SUPER;
    private List<Long> creativeIds;

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private PerformanceBannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);

    private DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(bannersRule);
    @Rule
    public DirectRule directRule = DirectRule.defaultRule()
            .withRules(cmdRule);

    @Before
    public void before() {
        clientId = directClassRule.dbSteps().shardingSteps().getClientIdByLogin(CLIENT);
        operatorUid = directClassRule.dbSteps().shardingSteps().getUidByLogin(CLIENT);
        directClassRule.dbSteps().useShardForLogin(CLIENT);
        creativeIds = directClassRule.dbSteps().bannersPerformanceSteps().findCreativeIds(bannersRule.getBannerId());
    }

    @Test
    public void getCampIdsByCreativeId() {
        Map<Long, List<CreativeCampaignResult>> response = directClassRule.intapiSteps().displayCanvasSteps()
                .getCreativesCampaigns(operatorUid, clientId, creativeIds);
        List<Long> capmIds = getCapmIdsList(response);
        assertThat("Получили нужный id кампании", capmIds,
                beanDiffer(Collections.singletonList(bannersRule.getCampaignId())));
    }

    @Test
    public void сreativeWithoutCamp() {
        Long creativeId = directClassRule.dbSteps().perfCreativesSteps().saveDefaultCanvasCreativesForClient(clientId);
        Map<Long, List<CreativeCampaignResult>> response = directClassRule.intapiSteps().displayCanvasSteps()
                .getCreativesCampaigns(operatorUid, clientId, Collections.singletonList(creativeId));
        List<Long> capmIds = getCapmIdsList(response);
        assertThat("Креатив не используется ни в одной кампании", capmIds, hasSize(0));
    }

    @Test
    public void getCampIdsBySuper() {
        Map<Long, List<CreativeCampaignResult>> response = directClassRule.intapiSteps().displayCanvasSteps()
                .getCreativesCampaigns(directClassRule.dbSteps().shardingSteps().getUidByLogin(SUPER),
                        clientId, creativeIds);
        List<Long> capmIds = getCapmIdsList(response);
        assertThat("Получили нужный id кампании", capmIds,
                beanDiffer(Collections.singletonList(bannersRule.getCampaignId())));
    }

    private List<Long> getCapmIdsList(Map<Long, List<CreativeCampaignResult>> response) {
        return response.entrySet()
                .stream().map(Map.Entry::getValue)
                .collect(Collectors.toList())
                .stream().flatMap(List::stream)
                .map(CreativeCampaignResult::getCampaignId)
                .collect(Collectors.toList());
    }
}
