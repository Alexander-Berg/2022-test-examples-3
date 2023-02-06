package ru.yandex.autotests.direct.cmd.creatives.search.perfomance;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

@Aqua.Test
@Description("Поиск креативов постранично через searchCreative (страница креативов)")
@Stories(TestFeatures.Creatives.FILTERS)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(TrunkTag.YES)
public class SearchCreativesPaginationTest {

    private static final String CLIENT = "at-direct-search-cr7";
    private static final int CREATIVES_NUMBER = 3;

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.defaultClassRule();

    private List<Long> creativeIds;

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .perfCreativesSteps().deletePerfCreativesByClientId(Long.valueOf(User.get(CLIENT).getClientID()));
        createCreatives();
    }

    @After
    public void after() {
        if (creativeIds != null) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                    .deletePerfCreatives(creativeIds);
        }
    }

    @Test
    @Description("Провека получения креативов не попавших на страницу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10840")
    public void testSearchCreativesOtherId() {
        List<Long> otherIds = cmdRule.cmdSteps().creativesSteps()
                .postSearchCreatives(new SearchCreativesRequest().withPerPage(CREATIVES_NUMBER - 1))
                .getResult().getOtherIds().stream()
                .map(ShortCreative::getId)
                .collect(Collectors.toList());
        assertThat("otherId содержит последний креатив",
                otherIds, contains(creativeIds.get(CREATIVES_NUMBER - 1)));
    }

    private void createCreatives() {
        creativeIds = new ArrayList<>(CREATIVES_NUMBER);
        for (int i = 0; i < CREATIVES_NUMBER; i++) {
            long creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps().
                    saveDefaultPerfCreativesForClient(
                            RandomStringUtils.randomAlphabetic(5),
                            CreativeTemplate.MANY_OFFERS_MANY_DESCRIPTIONS_AND_CAROUSEL_240x400,
                            Long.valueOf(User.get(CLIENT).getClientID()));
            creativeIds.add(creativeId);
        }
    }
}
