package ru.yandex.autotests.direct.cmd.creatives.search.perfomance;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

@Aqua.Test
@Description("Поиск креативов по вхождению подстроки в название (searchCreatives, страница креативов)")
@Stories(TestFeatures.Creatives.SEARCH_CREATIVES)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class SearchCreativesTest {


    private static final String CLIENT = "at-direct-search-creatives1";

    private static PerformanceBannersRule bannersRule = (PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT);
    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.defaultClassRule().useAuth(true).withRules(bannersRule);
    private static List<String> creativeNames;
    private static Map<String, Long> nameIdMap = new HashMap<>();
    private static long clientId;
    private static int shard;
    @Parameterized.Parameter
    public String searchText;

    @Parameterized.Parameters
    public static Collection<Object[]> searchTexts() {
        String text1 = RandomStringUtils.randomAlphabetic(4);
        String text2 = RandomStringUtils.randomAlphabetic(4);
        String text3 = RandomStringUtils.randomAlphabetic(4);
        String text4 = RandomStringUtils.randomAlphabetic(4) + " " + RandomStringUtils.randomAlphabetic(4);
        String text5 = RandomStringUtils.randomAlphabetic(4);
        String text6 = RandomStringUtils.randomAlphabetic(4);

        creativeNames = new ArrayList<>(Arrays.asList(
                text1 + " // 1 case",
                "start case: // 3 case " + text3,
                "start case: " + text4 + " // 4 case",
                "start case: erj" + text5 + "5case",
                text6 + " // 6 case",
                "start case: erj" + text6 + "5case"));

        return new ArrayList<>(Arrays.asList(
                new Object[]{text1},
                new Object[]{text2},
                new Object[]{text3},
                new Object[]{text4},
                new Object[]{text5},
                new Object[]{text6}));
    }

    @BeforeClass
    public static void beforeClass() {
        clientId = Long.valueOf(cmdRule.darkSideSteps().getClientFakeSteps().getClientData(CLIENT).getClientID());
        shard = TestEnvironment.newDbSteps().shardingSteps().getShardByClientID(clientId);

        createCreatives();
    }

    @AfterClass
    public static void afterClass() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .deletePerfCreatives(nameIdMap.values().stream().collect(Collectors.toList()));
    }

    private static void createCreatives() {
        creativeNames.stream().forEach(name -> {
            long creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                    .saveDefaultPerfCreativesForClient(
                            name, CreativeTemplate.MANY_OFFERS_MANY_DESCRIPTIONS_AND_CAROUSEL_240x400, clientId);
            nameIdMap.put(name, creativeId);
        });
    }

    @Test
    @Description("Поиск креативов по вхождению подстроки в название (searchCreatives, страница креативов)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9678")
    public void testSearchCreativesBySubstring() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));

        List<Long> expectedCreativeIds = getExpectedFoundCreativeIds(searchText);
        List<Long> actualCreativeIds = getActualFoundCreativeIds(searchText);

        assertThat("найдены искомые креативы по подстроке " + searchText,
                actualCreativeIds, containsInAnyOrder(expectedCreativeIds.toArray(new Long[expectedCreativeIds.size()])));
        assertThat("лишних креативов не найдено по подстроке " + searchText,
                actualCreativeIds, hasSize(actualCreativeIds.size()));
    }

    private List<Long> getExpectedFoundCreativeIds(String searchText) {
        List<Long> expectedFoundIds = new ArrayList<>();
        nameIdMap.forEach((String name, Long creativeId) -> {
            if (name.contains(searchText)) {
                expectedFoundIds.add(creativeId);
            }
        });
        return expectedFoundIds;
    }

    private List<Long> getActualFoundCreativeIds(String searchText) {
        List<Creative> foundCreatives = cmdRule.cmdSteps().creativesSteps().searchCreatives(searchText);
        return foundCreatives.stream().map(Creative::getCreativeId).collect(Collectors.toList());
    }
}
