package ru.yandex.autotests.direct.cmd.creatives.search.canvas;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.creatives.Creative;
import ru.yandex.autotests.direct.cmd.data.creatives.SearchCreativesResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

// таск: https://st.yandex-team.ru/TESTIRT-10210
@Aqua.Test
@Description("Поиск canvas креативов по id позитивные тесты")
@Stories(TestFeatures.Creatives.SEARCH_CREATIVES)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CANVAS_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class SearchCanvasCreativesPositiveTest {
    protected static final String CLIENT = "at-direct-search-creatives5";
    protected static final String CLIENT_AGENCY = Logins.AGENCY;
    protected static final String CLIENT_REPRESENTATIVE = "at-direct-search-cr-sub2";
    protected static final int CREATIVES_NUMBER = 3;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static long clientId;
    protected List<Long> creativeIds;

    @Before
    public void before() {
        clientId =
                Long.valueOf(defaultClassRule.darkSideSteps().getClientFakeSteps().getClientData(CLIENT).getClientID());
        createCreatives();
        defaultClassRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
    }


    protected void createCreatives() {
        creativeIds = new ArrayList<>(CREATIVES_NUMBER);
        for (int i = 0; i < CREATIVES_NUMBER; i++) {
            Long creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                    .perfCreativesSteps().saveDefaultCanvasCreativesForClient(clientId);
            creativeIds.add(creativeId);
        }
    }

    @Test
    @Description("поиск одного креатива")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9668")
    public void searchOneCreative() {
        List<Creative> actualCreatives =
                defaultClassRule.cmdSteps().creativesSteps().searchCanvasCreatives(creativeIds.get(0));
        assumeThat("найден один креатив", actualCreatives, hasSize(1));
        assertThat("полученные креативы соответствуют ожиданиям", actualCreatives.get(0).getCreativeId(),
                equalTo(creativeIds.get(0)));
    }

    @Test
    @Description("поиск нескольких креативов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9669")
    public void searchALotOfCreative() {
        List<Creative> actualCreatives =
                defaultClassRule.cmdSteps().creativesSteps().searchCanvasCreatives(creativeIds);
        List<Long> actualCreativeIds = actualCreatives.stream()
                .map(Creative::getCreativeId)
                .collect(Collectors.toList());
        assertThat("полученные креативы соответствуют ожиданиям",
                actualCreativeIds,
                hasItems(creativeIds.toArray(new Long[creativeIds.size()])));
    }

    @Test
    @Description("Поиск креатива не главным представителем клиента")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9670")
    public void searchCreativeBySecondRepresentative() {
        defaultClassRule.cmdSteps().authSteps().authenticate(User.get(CLIENT_REPRESENTATIVE));
        SearchCreativesResponse actualResponse = defaultClassRule.cmdSteps().creativesSteps()
                .rawSearchCanvasCreatives(creativeIds.get(0));
        assumeThat("полученный результат не null", actualResponse.getResult(), notNullValue());
        assertThat("полученные креативы соответствуют ожиданиям",
                actualResponse.getResult().getCreatives(),
                Matchers.hasSize(1));
    }

    @Test
    @Description("Поиск креатива агентством")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9671")
    public void searchCreativeByAgency() {
        defaultClassRule.cmdSteps().authSteps().authenticate(User.get(CLIENT_AGENCY));
        SearchCreativesResponse actualResponse = defaultClassRule.cmdSteps().creativesSteps()
                .rawSearchCanvasCreatives(CLIENT, creativeIds.get(0));
        assumeThat("полученный результат не null", actualResponse.getResult(), notNullValue());

        assertThat("полученные креативы соответствуют ожиданиям",
                actualResponse.getResult().getCreatives(),
                Matchers.hasSize(1));
    }

    @After
    public void after() {
        TestEnvironment.newDbSteps().perfCreativesSteps().deletePerfCreatives(creativeIds);
    }
}
