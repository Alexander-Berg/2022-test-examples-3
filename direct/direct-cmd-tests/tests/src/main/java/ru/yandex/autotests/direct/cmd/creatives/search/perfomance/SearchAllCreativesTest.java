package ru.yandex.autotests.direct.cmd.creatives.search.perfomance;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.creatives.Creative;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;

@Aqua.Test
@Description("Поиск всех креативов по пустой строке (searchCreatives, страница креативов)")
@Stories(TestFeatures.Creatives.SEARCH_CREATIVES)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
public class SearchAllCreativesTest {

    private static final String CLIENT = "at-direct-search-creatives1";
    private static final int CREATIVES_NUMBER = 3;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private PerformanceBannersRule bannersRule = (PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private long clientId;
    private List<Long> creativeIds;

    @Before
    public void before() {
        clientId = Long.valueOf(User.get(CLIENT).getClientID());

        createCreatives();
    }

    @After
    public void after() {
        deleteCreatives();
    }

    private void createCreatives() {
        creativeIds = new ArrayList<>(CREATIVES_NUMBER);
        for (int i = 0; i < CREATIVES_NUMBER; i++) {
            long creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps().
                    saveDefaultPerfCreativesForClient(
                            RandomStringUtils.randomAlphabetic(5),
                            CreativeTemplate.MANY_OFFERS_MANY_DESCRIPTIONS_AND_CAROUSEL_240x400,
                            clientId);
            creativeIds.add(creativeId);
        }
    }

    private void deleteCreatives() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps().deletePerfCreatives(creativeIds);
    }

    @Test
    @Description("Поиск всех креативов по пустой строке (searchCreatives, страница креативов)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9673")
    public void testSearchAllCreatives() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));

        List<Creative> actualCreatives = cmdRule.cmdSteps().creativesSteps().searchCreatives("");

        assertThat("найдено не менее " + CREATIVES_NUMBER + " креативов по пустой строке",
                actualCreatives.size(), greaterThanOrEqualTo(CREATIVES_NUMBER));
    }
}
