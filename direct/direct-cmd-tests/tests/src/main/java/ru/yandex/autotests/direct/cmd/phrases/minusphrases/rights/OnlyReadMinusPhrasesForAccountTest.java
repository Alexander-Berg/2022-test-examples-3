package ru.yandex.autotests.direct.cmd.phrases.minusphrases.rights;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

//TESTIRT-10118
@Aqua.Test
@Description("Недоступность добавления минус-фраз")
@Stories(TestFeatures.Phrases.AJAX_TEST_PHRASES)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.AJAX_TEST_PHRASES)
@Tag(ObjectTag.PHRASE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class OnlyReadMinusPhrasesForAccountTest {
    private final String campMinusPhrase = "конь !в яблоках";
    private final String groupMinusPhrase = "белый медведь";
    private final static String CLIENT = "at-backend-minuswords";
    public String login;
    public String client;
    public TextBannersRule bannersRule;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Parameterized.Parameters(name = "Создание кампании с минус фразой под {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Logins.SUPER_READER, CLIENT},
                {Logins.AGENCY_CLIENT, Logins.AGENCY_CLIENT},
        });
    }

    public OnlyReadMinusPhrasesForAccountTest(String login, String client){
        this.client = client;
        this.login = login;
        switch (login) {
            case Logins.AGENCY_CLIENT:
                bannersRule = new TextBannersRule()
                        .overrideCampTemplate(new SaveCampRequest().withFor_agency(Logins.AGENCY))
                        .withUlogin(client);
                cmdRule = DirectCmdRule.defaultRule().as(Logins.AGENCY).withRules(bannersRule);
                break;
            default:
                bannersRule = new TextBannersRule().withUlogin(client);
                cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
                break;
        }
    }

    @Rule
    public DirectCmdRule cmdRule;

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(login));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10725")
    public void addMinusPhraseToCampaign() {
        SaveCampRequest saveCampRequest = bannersRule.getSaveCampRequest()
                .withJsonCampaignMinusWords(singletonList(campMinusPhrase))
                .withCid(bannersRule.getCampaignId().toString())
                .withUlogin(client);
        CampaignErrorResponse errorResponse = cmdRule.cmdSteps().campaignSteps().postSaveCampInvalidData(saveCampRequest);

        assertThat("Нет прав для выполнения данной операции!", errorResponse.getError(),
                equalTo("Нет прав для выполнения данной операции!"));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10726")
    public void addMinusPhraseToGroup() {
        Group group = bannersRule.getGroup().
                withAdGroupID(bannersRule.getGroupId().toString())
                .withTags(emptyMap())
                .withMinusWords(singletonList(groupMinusPhrase));
        group.getBanners().get(0).withBid(bannersRule.getBannerId());
        GroupsParameters groupsParameters = GroupsParameters
                .forExistingCamp(client, bannersRule.getCampaignId(), group);
        GroupErrorsResponse errorResponse = cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroupsInvalidData(groupsParameters);

        assertThat("Нет прав для выполнения данной операции!", errorResponse.getError(),
                equalTo("Нет прав для выполнения данной операции!"));
    }
}
