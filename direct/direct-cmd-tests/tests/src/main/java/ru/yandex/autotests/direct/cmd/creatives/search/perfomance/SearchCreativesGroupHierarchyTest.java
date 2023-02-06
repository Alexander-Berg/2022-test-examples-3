package ru.yandex.autotests.direct.cmd.creatives.search.perfomance;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.creatives.Creative;
import ru.yandex.autotests.direct.cmd.data.creatives.CreativeGroup;
import ru.yandex.autotests.direct.cmd.data.creatives.ShortCreative;
import ru.yandex.autotests.direct.cmd.data.creatives.SearchCreativesRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.beans.creatives.CreativeTemplate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Получение креативов в виде списка/иерархии через searchCreative (страница креативов)")
@Stories(TestFeatures.Creatives.FILTERS)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(TrunkTag.YES)
public class SearchCreativesGroupHierarchyTest {

    private static final String CLIENT = "at-direct-search-cr8";
    private static final int CREATIVES_NUMBER = 3;
    private static final Long CREATIVE_GROUP_ID = 3L;

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.defaultClassRule();

    private List<Long> expectedCreativeIds;

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .perfCreativesSteps().deletePerfCreativesByClientId(Long.valueOf(User.get(CLIENT).getClientID()));
        createCreatives();
    }

    @After
    public void after() {
        if (expectedCreativeIds != null) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                    .deletePerfCreatives(expectedCreativeIds);
        }
    }

    @Test
    @Description("Провека получения креативов в виде списка")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10837")
    public void testSearchCreativesGroupList() {
        List<Long> creativeIds = cmdRule.cmdSteps().creativesSteps()
                .postSearchCreatives(new SearchCreativesRequest().withGroup(0))
                .getResult().getCreatives().stream()
                .map(Creative::getCreativeId)
                .collect(Collectors.toList());
        assertThat("креативы вернулись списком",
                creativeIds, containsInAnyOrder(expectedCreativeIds.toArray(new Long[expectedCreativeIds.size()])));
    }

    @Test
    @Description("Провека получения креативов в виде иерархии")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10836")
    public void testSearchCreativesGroupHierarchy() {
        List<CreativeGroup> creativeGroups = cmdRule.cmdSteps().creativesSteps()
                .postSearchCreatives(new SearchCreativesRequest().withGroup(1))
                .getResult().getGroups();
        assumeThat("группы креативов получены", creativeGroups, hasSize(1));

        List<Long> creativeIds = creativeGroups.get(0).getCreativesData().stream()
                .map(ShortCreative::getId)
                .collect(Collectors.toList());
        assertThat("креативы вернулись иерархией",
                creativeIds, containsInAnyOrder(expectedCreativeIds.toArray(new Long[expectedCreativeIds.size()])));
    }


    private void createCreatives() {
        expectedCreativeIds = new ArrayList<>(CREATIVES_NUMBER);
        for (int i = 0; i < CREATIVES_NUMBER; i++) {
            long creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps().
                    saveDefaultPerfCreativesForClient(
                            RandomStringUtils.randomAlphabetic(5),
                            CreativeTemplate.MANY_OFFERS_MANY_DESCRIPTIONS_AND_CAROUSEL_240x400,
                            Long.valueOf(User.get(CLIENT).getClientID()));
            TestEnvironment.newDbSteps().perfCreativesSteps().setCreativeGroupId(creativeId, CREATIVE_GROUP_ID);
            expectedCreativeIds.add(creativeId);
        }
    }
}
