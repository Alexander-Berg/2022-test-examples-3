package ru.yandex.autotests.directintapi.tests.fakeintapi;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.common.api45mng.ClientInfo;
import ru.yandex.autotests.directapi.common.api45mng.EventsLogItem;
import ru.yandex.autotests.directapi.common.api45mng.EventsLogItemAttributes;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.BannerPhraseFakeInfo;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.ClientFakeInfo;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.GroupFakeInfo;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.RetargetingConditionFakeInfo;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.clients.ClientInfoMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;
import static ru.yandex.autotests.irt.testutils.matchers.NumberApproximatelyEqual.approxEqualTo;

/**
 * User: xy6er
 * https://jira.yandex-team.ru/browse/TESTIRT-1406
 */

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.FAKE_METHODS)
public class FakeMethodsTest {
    private static DarkSideSteps darkSideSteps;
    private static Long cid;
    private static Long adGroupID;
    private static long phraseID;

    private static final String LOGIN = "intapiFakeClient1";
    private static final String AGENCY_LOGIN = "at-agency-fakeadm";
    private static final int CLIENT_ID = 5651477;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN).wsdl(APIPort_PortType.class);

    @BeforeClass
    public static void createCampaign() {
        darkSideSteps = api.userSteps.getDarkSideSteps();
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        adGroupID = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        api.userSteps.adsSteps().addDefaultTextAd(adGroupID);
        phraseID = api.userSteps.keywordsSteps().addDefaultKeyword(adGroupID);
    }


    @Test
    public void getGroupParamsTest() {
        GroupFakeInfo groupFakeInfo = darkSideSteps.getGroupsFakeSteps().getGroupParams(adGroupID);
        assertThat("Неверный pid у группы", groupFakeInfo.getPid(), equalTo(adGroupID));
        assertNotNull(groupFakeInfo.getGroupName());
    }

    @Test
    public void updateGroupParamsTest() {
        String newName = "NewName " + RandomUtils.getNextInt();
        GroupFakeInfo groupFakeInfo = darkSideSteps.getGroupsFakeSteps().getGroupParams(adGroupID);
        groupFakeInfo.setGroupName(newName);
        darkSideSteps.getGroupsFakeSteps().updateGroupParams(groupFakeInfo);
        assertThat("Неверный groupName у группы",
                darkSideSteps.getGroupsFakeSteps().getGroupParams(adGroupID).getGroupName(), equalTo(newName));
    }

    @Test
    public void getPhrasesParamsTest() {
        BannerPhraseFakeInfo[] phraseFakeInfos =
                darkSideSteps.getBannerPhrasesFakeSteps().getPhrasesParamsById(phraseID);
        assertThat("Кол-во фраз должна быть равна одному", phraseFakeInfos.length, equalTo(1));
        assertThat("Неверный id фразы", phraseFakeInfos[0].getId(), equalTo(phraseID));
    }

    @Test
    public void updatePhrasesParamsTest() {
        String phrase = "New phrase " + RandomUtils.getNextInt(1000);
        BannerPhraseFakeInfo[] phraseFakeInfos =
                darkSideSteps.getBannerPhrasesFakeSteps().getPhrasesParamsById(phraseID);
        phraseFakeInfos[0].setPhrase(phrase);
        darkSideSteps.getBannerPhrasesFakeSteps().updateBannerPhrasesParams(phraseFakeInfos);
        BannerPhraseFakeInfo[] newPhraseFakeInfos =
                darkSideSteps.getBannerPhrasesFakeSteps().getPhrasesParamsById(phraseID);
        assertThat("Неверный текст фразы", newPhraseFakeInfos[0].getPhrase(), equalTo(phrase));
    }

    @Test
    public void getFakeClientParamsTest() {
        ClientFakeInfo clientFakeInfo = darkSideSteps.getClientFakeSteps().getClientData(LOGIN);
        assertThat("Неверный логин у клиента", clientFakeInfo.getLogin(), equalToIgnoringCase(LOGIN));
        assertThat("Неверный clientID у клиента", clientFakeInfo.getClientID(), equalTo(String.valueOf(CLIENT_ID)));
    }

    // https://st.yandex-team.ru/TESTIRT-2892
    @Test
    public void updateAPIUnitsForMainLoginTest() {
        int units = 31999;
        darkSideSteps.getClientFakeSteps().setAPIUnits(Logins.LOGIN_MAIN, units);
        ClientFakeInfo clientFakeInfo = darkSideSteps.getClientFakeSteps()
                .getClientDataByLogin(Logins.LOGIN_MAIN, "API_units");
        assertThat("Неверное кол-во баллов", Integer.parseInt(clientFakeInfo.getApiUnits()),
                approxEqualTo(units).withDifference(
                        2000)); //Во время выполнения этого теста параллельно другие тесты могут потратить баллы:)
    }

    @Test
    public void getUserShardTest() {
        int shard = darkSideSteps.getClientFakeSteps().getUserShard(LOGIN);
        assertThat("Шард не должен был измениться",
                darkSideSteps.getClientFakeSteps().getUserShard(CLIENT_ID), equalTo(shard));
    }

    @Test
    public void setVATRateTest() {
        boolean result = darkSideSteps.getClientFakeSteps().setVATRate(LOGIN, RandomUtils.getNextInt(100));
        assertTrue("Method FakeBalanceNotificationNDS returned false", result);
    }

    @Test
    public void fakeSetAllowBudgetAccountForAgencyTest() {
        boolean result = darkSideSteps.fakeAdminSteps().setAllowBudgetAccountForAgency(AGENCY_LOGIN, Status.YES);
        assertTrue("Method FakeSetAllowBudgetAccountForAgency returned false", result);
    }

    @Test
    public void addEventsTest() {
        EventsLogItem event = new EventsLogItem();
        event.setCampaignID(cid.intValue());
        event.setEventType("MoneyIn");
        event.setEventName("");
        event.setTimestamp("");
        EventsLogItemAttributes attributes = new EventsLogItemAttributes();
        attributes.setPayed(15f);
        event.setAttributes(attributes);
        boolean result = darkSideSteps.getEventLogFakeSteps().addEvents(event);
        assertTrue("Method AddEvents returned false", result);
    }

    @Test
    public void fakeUpdateRetargetingGoalsTest() {
        int conditionID = 123;
        RetargetingConditionFakeInfo retargetingConditionFakeInfo =
                darkSideSteps.retargetingFakeSteps().updateRetargetingGoals(conditionID);
        assertNotNull(retargetingConditionFakeInfo.getClientID());
    }

    @Test
    public void fakeCreateClient() {
        String login = RandomStringUtils.randomAlphabetic(30);
        String password = "super-duper-password1";
        Currency currency = Currency.YND_FIXED;
        Integer clientID = api.userSteps.clientFakeSteps().fakeCreateClient(login, password, currency);

        ClientInfo clientInfo = api.as(Logins.LOGIN_SUPER).userSteps.clientSteps().getClientInfo(login);
        assertThat("создался клиент с верными данными", clientInfo, beanEquivalent(new ClientInfoMap(api.type())
                .withClientID(clientID)
                .withLogin(login).getBean()));
    }
}
