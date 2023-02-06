package ru.yandex.autotests.direct.cmd.creatives.search.perfomance;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.creatives.Creative;
import ru.yandex.autotests.direct.cmd.data.creatives.SearchCreativesFilterEnum;
import ru.yandex.autotests.direct.cmd.data.creatives.StatusModerateFilterEnum;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PerfCreativesStatusmoderate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

@Aqua.Test
@Description("Поиск креативов по статусу модерации через searchCreative (страница креативов)")
@Stories(TestFeatures.Creatives.FILTERS)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class SearchCreativesStatusModerateTest {

    private static final String CLIENT = "at-direct-search-cr6";

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.defaultClassRule();

    private static HashMap<PerfCreativesStatusmoderate, Long> creativesStatusModerateIdMap = new HashMap<>();

    @Parameterized.Parameter(0)
    public StatusModerateFilterEnum statusModerateFilter;

    @Parameterized.Parameter(1)
    public List<PerfCreativesStatusmoderate> expectedStatusModerate;

    @Parameterized.Parameters(name = "searchCreatives по статусу модерации {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {StatusModerateFilterEnum.NEW, Arrays.asList(PerfCreativesStatusmoderate.New,
                        PerfCreativesStatusmoderate.Error)
                },
                {StatusModerateFilterEnum.WAIT, Arrays.asList(PerfCreativesStatusmoderate.Ready,
                        PerfCreativesStatusmoderate.Sending, PerfCreativesStatusmoderate.Sent)
                },
                {StatusModerateFilterEnum.YES, singletonList(PerfCreativesStatusmoderate.Yes)},
                {StatusModerateFilterEnum.NO, singletonList(PerfCreativesStatusmoderate.No)}
        });
    }

    @BeforeClass
    public static void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .deletePerfCreativesByClientId(Long.valueOf(User.get(CLIENT).getClientID()));
        createCreatives(); //создаем креативы со всеми возможными статусами модерации
    }

    @AfterClass
    public static void after() {
        if (!creativesStatusModerateIdMap.isEmpty()) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                    .deletePerfCreatives(creativesStatusModerateIdMap.values().stream()
                            .collect(Collectors.toList()));
        }
    }

    @Test
    @Description("Поиск креативов по статусу модерации")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10843")
    public void testSearchCreativesFilterBusiness() {
        List<Long> creativesIds = cmdRule.cmdSteps().creativesSteps()
                .searchCreatives(SearchCreativesFilterEnum.STATUS_MODERATE, statusModerateFilter.getValue()).stream()
                .map(Creative::getCreativeId)
                .collect(Collectors.toList());
        assertThat("найденный креатив в ответе ручки searchCreatives соответствует ожидаемому",
                creativesIds, contains(getExpectedIds()));
    }

    private Long[] getExpectedIds() {
        List<Long> expectedIds = new ArrayList<>();
        expectedStatusModerate.forEach(st -> expectedIds.add(creativesStatusModerateIdMap.get(st)));
        return expectedIds.toArray(new Long[expectedIds.size()]);
    }

    private static void createCreatives() {
        for (PerfCreativesStatusmoderate statusModerate : PerfCreativesStatusmoderate.values()) {
            Long creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                    .perfCreativesSteps().saveDefaultPerfCreative(User.get(CLIENT).getClientID());
            TestEnvironment.newDbSteps().perfCreativesSteps().setStatusModerate(creativeId, statusModerate);
            creativesStatusModerateIdMap.put(statusModerate, creativeId);
        }
    }
}
