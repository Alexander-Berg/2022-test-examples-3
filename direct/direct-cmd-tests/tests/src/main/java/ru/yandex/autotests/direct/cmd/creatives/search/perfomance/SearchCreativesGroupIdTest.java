package ru.yandex.autotests.direct.cmd.creatives.search.perfomance;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.creatives.Creative;
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

@Aqua.Test
@Description("Получение креативов по ид группы через searchCreative (страница креативов)")
@Stories(TestFeatures.Creatives.FILTERS)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(TrunkTag.YES)
public class SearchCreativesGroupIdTest {

    private static final String CLIENT = "at-direct-search-cr9";
    private static final int CREATIVES_WITH_GROUP_NUMBER = 2;
    private static final int CREATIVES_NUMBER = 2;
    private static final Long CREATIVE_GROUP_ID = 3L;

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.defaultClassRule();

    private List<Long> expectedCreativeIds;
    private List<Long> expectedCreativeWithGroupIds;

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .perfCreativesSteps().deletePerfCreativesByClientId(Long.valueOf(User.get(CLIENT).getClientID()));
        expectedCreativeIds = createCreatives(CREATIVES_NUMBER);
        expectedCreativeWithGroupIds = createCreatives(CREATIVES_WITH_GROUP_NUMBER);
        expectedCreativeWithGroupIds
                .forEach(id -> TestEnvironment.newDbSteps().perfCreativesSteps().setCreativeGroupId(id, CREATIVE_GROUP_ID));
    }

    @After
    public void after() {
        if (expectedCreativeIds != null) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                    .deletePerfCreatives(expectedCreativeIds);
        }
        if (expectedCreativeWithGroupIds != null) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                    .deletePerfCreatives(expectedCreativeWithGroupIds);
        }
    }

    @Test
    @Description("Провека поиска креативов по нулевому ид группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10838")
    public void testSearchCreativesZeroGroupId() {
        List<Long> creativeIds = sendRequest(0L);
        assertThat("вернулись креативы без групп",
                creativeIds, containsInAnyOrder(expectedCreativeIds.toArray(new Long[expectedCreativeIds.size()])));
    }

    @Test
    @Description("Провека поиска креативов по ид группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10839")
    public void testSearchCreativesGroupId() {
        List<Long> creativeIds = sendRequest(CREATIVE_GROUP_ID);
        assertThat("вернулись креативы с группой",
                creativeIds, containsInAnyOrder(expectedCreativeWithGroupIds
                        .toArray(new Long[expectedCreativeWithGroupIds.size()])));
    }

    private List<Long> sendRequest(Long groupId) {
        return cmdRule.cmdSteps().creativesSteps()
                .postSearchCreatives(new SearchCreativesRequest().withGroupId(groupId))
                .getResult().getCreatives().stream()
                .map(Creative::getCreativeId)
                .collect(Collectors.toList());
    }


    private List<Long> createCreatives(Integer numb) {
        List<Long> creativeIds = new ArrayList<>(numb);
        for (int i = 0; i < numb; i++) {
            long creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps().
                    saveDefaultPerfCreativesForClient(
                            RandomStringUtils.randomAlphabetic(5),
                            CreativeTemplate.MANY_OFFERS_MANY_DESCRIPTIONS_AND_CAROUSEL_240x400,
                            Long.valueOf(User.get(CLIENT).getClientID()));
            creativeIds.add(creativeId);
        }
        return creativeIds;
    }
}
