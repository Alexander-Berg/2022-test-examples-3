package ru.yandex.autotests.direct.cmd.creatives.search.canvas;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.creatives.SearchCreativesResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
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
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.*;

// таск: https://st.yandex-team.ru/TESTIRT-10210
@Aqua.Test
@Description("Поиск canvas креативов по id негативные тесты")
@Stories(TestFeatures.Creatives.SEARCH_CREATIVES)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CANVAS_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class SearchCanvasCreativesNegativeTest {
    protected static final String CLIENT_FIRST = "at-direct-search-creatives1";
    protected static final String CLIENT_SECOND = "at-direct-backend-c";
    private static final String ERROR_TEXT_ILLIGAL_CREATIVE_ID = "Ошибка: указан некорректный или несуществующий номер креатива";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private PerformanceBannersRule bannersRule = (ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT_FIRST);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private Long firstClientCreativeId;
    private Long secondClientCreativeId;
    private Long firstClientId;
    private Long secondClientId;


    @Before
    public void before() {
        firstClientId = Long.valueOf(cmdRule.darkSideSteps().getClientFakeSteps().getClientData(CLIENT_FIRST).getClientID());
        secondClientId = Long.valueOf(cmdRule.darkSideSteps().getClientFakeSteps().getClientData(CLIENT_SECOND).getClientID());
        firstClientCreativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT_FIRST)
                .perfCreativesSteps().saveDefaultCanvasCreativesForClient(firstClientId);
        secondClientCreativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT_SECOND)
                .perfCreativesSteps().saveDefaultCanvasCreativesForClient(secondClientId);

    }

    @Test
    @Description("Поиск чужого креатива")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9665")
    public void searchSomeoneElseCreative() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT_SECOND));

        SearchCreativesResponse actualResponse = cmdRule.cmdSteps().creativesSteps().rawSearchCanvasCreatives(firstClientCreativeId);

        assertThat("полученные креативы соответствуют ожиданиям",
                actualResponse.getResult().getCreatives(),
                hasSize(0));

    }

    @Test
    @Description("Поиск и чужого креатива и своего")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9666")
    public void searchClientsAndSomeOneElseCreatives() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT_SECOND));

        SearchCreativesResponse actualResponse = cmdRule.cmdSteps().creativesSteps()
                .rawSearchCanvasCreatives(firstClientCreativeId, secondClientCreativeId);


        assumeThat("число полученных креативов соответствуют ожиданиям",
                actualResponse.getResult().getCreatives(),
                hasSize(1));

        assertThat("число полученных креативов соответствуют ожиданиям",
                actualResponse.getResult().getCreatives().get(0).getCreativeId(),
                equalTo(secondClientCreativeId));


    }

    @Test
    @Description("Поиск своего удаленного креатива")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9667")
    public void searchClientsDeletedCreative() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT_SECOND));
        TestEnvironment.newDbSteps().perfCreativesSteps().deletePerfCreatives(secondClientCreativeId);

        SearchCreativesResponse actualResponse = cmdRule.cmdSteps().creativesSteps()
                .rawSearchCanvasCreatives(secondClientCreativeId);


        assertThat("полученные креативы соответствуют ожиданиям",
                actualResponse.getResult().getCreatives(),
                hasSize(0));

    }

    @After
    public void after() {
        TestEnvironment.newDbSteps().perfCreativesSteps().deletePerfCreatives(firstClientCreativeId);
        TestEnvironment.newDbSteps().perfCreativesSteps().deletePerfCreatives(secondClientCreativeId);
    }

}
