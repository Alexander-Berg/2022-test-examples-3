package ru.yandex.autotests.direct.cmd.phrases.minusphrases.rights;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
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
import java.util.List;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

//TESTIRT-10118
@Aqua.Test
@Description("Права на добавление минус-фраз")
@Stories(TestFeatures.Phrases.AJAX_TEST_PHRASES)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.AJAX_TEST_PHRASES)
@Tag(ObjectTag.PHRASE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class CreateMinusPhrasesForAccountTest {

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
                {Logins.SUPPORT, CLIENT},
                {Logins.MANAGER, CLIENT},
                {Logins.PLACER, CLIENT},
                {CLIENT, CLIENT},
                {Logins.AGENCY, Logins.AGENCY_CLIENT},
        });
    }

    public CreateMinusPhrasesForAccountTest(String login, String client){
        this.client = client;
        this.login = login;
        switch (login) {
            case Logins.AGENCY:
                bannersRule = new TextBannersRule()
                        .overrideCampTemplate(new SaveCampRequest().withFor_agency(login))
                        .withUlogin(client);
                break;
            default:
                bannersRule = new TextBannersRule().withUlogin(client);
                break;
        }
                cmdRule = DirectCmdRule.defaultRule().as(login).withRules(bannersRule);

    }

    @Rule
    public DirectCmdRule cmdRule;

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(login));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10723")
    public void addMinusPhraseToCampaign() {
        SaveCampRequest saveCampRequest = bannersRule.getSaveCampRequest()
                .withJsonCampaignMinusWords(singletonList(campMinusPhrase))
                .withCid(bannersRule.getCampaignId().toString())
                .withUlogin(client);
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);

        List actualCampMinusPhrases = cmdRule.cmdSteps().campaignSteps()
                .getCampaign(client, bannersRule.getCampaignId())
                .getMinusWords();
        assertThat("Сохраненные минус-фразы совпадают с ожидаемыми", actualCampMinusPhrases,
                equalTo(singletonList(campMinusPhrase)));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10724")
    public void addMinusPhraseToGroup() {
        Group group = bannersRule.getGroup().
                withAdGroupID(bannersRule.getGroupId().toString())
                .withTags(emptyMap())
                .withMinusWords(singletonList(groupMinusPhrase));
        group.getBanners().get(0).withBid(bannersRule.getBannerId());
        GroupsParameters groupsParameters = GroupsParameters
                .forExistingCamp(client, bannersRule.getCampaignId(), group);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);

        List actualGroupMinusPhrases = cmdRule.cmdSteps().groupsSteps()
                .getGroups(client, bannersRule.getCampaignId())
                .get(0).getMinusWords();
        assertThat("Сохраненные минус-фразы совпадают с ожидаемыми", actualGroupMinusPhrases,
                equalTo(singletonList(groupMinusPhrase)));
    }
}
