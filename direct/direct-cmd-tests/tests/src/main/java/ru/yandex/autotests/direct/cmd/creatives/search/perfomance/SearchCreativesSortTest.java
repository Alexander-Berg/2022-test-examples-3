package ru.yandex.autotests.direct.cmd.creatives.search.perfomance;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.creatives.Creative;
import ru.yandex.autotests.direct.cmd.data.creatives.SearchCreativesRequest;
import ru.yandex.autotests.direct.cmd.data.sort.SortBy;
import ru.yandex.autotests.direct.cmd.data.sort.SortOrder;
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

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

@Aqua.Test
@Description("Поиск креативов по фильтрам через searchCreative (страница креативов)")
@Stories(TestFeatures.Creatives.FILTERS)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(TrunkTag.YES)
public class SearchCreativesSortTest {

    private static final String CLIENT = "at-direct-search-cr4";
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
    @Description("Сортировка креативов по ид asc")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10841")
    public void testSearchCreativesAscOrder() {
        Collections.sort(creativeIds);
        check(SortOrder.ASCENDING.getName());
    }

    @Test
    @Description("Сортировка креативов по ид desc")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10842")
    public void testSearchCreativesDescOrder() {
        Collections.sort(creativeIds, Collections.reverseOrder());
        check(SortOrder.DESCENDING.getName());
    }

    private void check(String order) {
        List<Long> creativesIds = cmdRule.cmdSteps().creativesSteps()
                .postSearchCreatives(new SearchCreativesRequest()
                        .withCreative(creativeIds).withSort(SortBy.ID.getName()).withOrder(order))
                .getResult().getCreatives().stream()
                .map(Creative::getCreativeId)
                .collect(Collectors.toList());
        assertThat("список креативов в searchCreatives отсортирован",
                creativesIds, contains(creativeIds.toArray()));
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
