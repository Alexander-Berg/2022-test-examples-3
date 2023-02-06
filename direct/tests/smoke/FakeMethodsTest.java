package ru.yandex.autotests.directintapi.tests.smoke;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.model.ShardNumbers;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.common.api45.EventsLogItem;
import ru.yandex.autotests.directapi.common.api45.EventsLogItemAttributes;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.BannerPhraseFakeInfo;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.ClientFakeInfo;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.ConvertType;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.GroupFakeInfo;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.RetargetingConditionFakeInfo;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.hazelcast.SemaphoreRule;
import ru.yandex.terra.junit.rules.BottleMessageRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * User: xy6er
 * https://jira.yandex-team.ru/browse/TESTIRT-1406
 */
@Aqua.Test
@Features(FeatureNames.FAKE_INTAPI_MONITOR)
public class FakeMethodsTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    private static Long adgroupId;
    private static Long phraseId;

    private static String LOGIN = "fakeClient";
    private static int CLIENT_ID = 3874884;
    private static final String LOGIN_FOR_RESHARD = "fakeClientForReshard";
    private static final String LOGIN_FOR_CONVERT = "fakeClientForConvert";
    private static final String AGENCY_LOGIN = "at-agency-fakeadm";

    @Rule
    public BottleMessageRule bmr = new BottleMessageRule();

    @Rule
    public Trashman trasher = new Trashman(api);

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @BeforeClass
    public static void prepare() {
        final long campaignId = api.as(Logins.SUPER_LOGIN, LOGIN).userSteps.campaignSteps().addDefaultTextCampaign();
        adgroupId = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        phraseId = api.userSteps.keywordsSteps().addDefaultKeyword(adgroupId);
    }

    @Stories("FakeGetGroupParams")
    @Test
    public void getGroupParamsTest() {
        GroupFakeInfo groupFakeInfo = darkSideSteps.getGroupsFakeSteps().getGroupParams(adgroupId);
        assertThat("Неверный pid у группы", groupFakeInfo.getPid(), equalTo(adgroupId));
        assertNotNull(groupFakeInfo.getGroupName());
    }

    @Stories("FakeGroupParams")
    @Test
    public void updateGroupParamsTest() {
        String newName = "NewName " + RandomUtils.getNextInt();
        GroupFakeInfo groupFakeInfo = darkSideSteps.getGroupsFakeSteps().getGroupParams(adgroupId);
        groupFakeInfo.setGroupName(newName);
        darkSideSteps.getGroupsFakeSteps().updateGroupParams(groupFakeInfo);
        assertThat("Неверный groupName у группы",
                darkSideSteps.getGroupsFakeSteps().getGroupParams(adgroupId).getGroupName(), equalTo(newName));
    }

    @Stories("FakeGetPhrasesParams")
    @Test
    public void getPhrasesParamsTest() {
        BannerPhraseFakeInfo[] phraseFakeInfos =
                darkSideSteps.getBannerPhrasesFakeSteps().getPhrasesParamsById(phraseId);
        assertThat("Кол-во фраз должна быть равна одному", phraseFakeInfos.length, equalTo(1));
        assertThat("Неверный id фразы", phraseFakeInfos[0].getId(), equalTo(phraseId));
    }

    @Stories("FakePhrasesParams")
    @Test
    public void updatePhrasesParamsTest() {
        String phrase = "New phrase " + RandomUtils.getNextInt(1000);
        BannerPhraseFakeInfo[] phraseFakeInfos =
                darkSideSteps.getBannerPhrasesFakeSteps().getPhrasesParamsById(phraseId);
        phraseFakeInfos[0].setPhrase(phrase);
        darkSideSteps.getBannerPhrasesFakeSteps().updateBannerPhrasesParams(phraseFakeInfos);
        BannerPhraseFakeInfo[] newPhraseFakeInfos =
                darkSideSteps.getBannerPhrasesFakeSteps().getPhrasesParamsById(phraseId);
        assertThat("Неверный текст фразы", newPhraseFakeInfos[0].getPhrase(), equalTo(phrase));
    }

    @Stories("FakeGetClientParams")
    @Test
    public void getFakeClientParamsTest() {
        ClientFakeInfo clientFakeInfo = darkSideSteps.getClientFakeSteps().getClientData(LOGIN);
        assertThat("Неверный логин у клиента", clientFakeInfo.getLogin().toLowerCase(), equalTo(LOGIN.toLowerCase()));
        assertThat("Неверный clientID у клиента", clientFakeInfo.getClientID(), equalTo(String.valueOf(CLIENT_ID)));
    }

    @Stories("FakeClientParams")
    @Test
    public void updateFakeClientParamsTest() {
        darkSideSteps.getClientFakeSteps().disableGeo(LOGIN);
        ClientFakeInfo clientFakeInfo = darkSideSteps.getClientFakeSteps()
                .getClientDataByLogin(LOGIN, "api_geo_allowed");
        assertThat("Неверное значение api_geo_allowed у клиента",
                clientFakeInfo.getApiGeoAllowed(), equalTo(Status.NO));
    }

    @Stories("GetUserShard")
    @Test
    public void getUserShardTest() {
        int shard = darkSideSteps.getClientFakeSteps().getUserShard(LOGIN);
        assertThat("Шард не должен был измениться",
                darkSideSteps.getClientFakeSteps().getUserShard(CLIENT_ID), equalTo(shard));
    }

    @Stories("ReshardUser")
    @Test
    public void reshardUserTest() {
        int newShard = darkSideSteps.getClientFakeSteps().getUserShard(LOGIN_FOR_RESHARD) == ShardNumbers.DEFAULT_SHARD.getShardNumber()
                ? ShardNumbers.EXTRA_SHARD.getShardNumber() : ShardNumbers.DEFAULT_SHARD.getShardNumber();
        darkSideSteps.getClientFakeSteps().reshardUser(LOGIN_FOR_RESHARD, newShard);
        assertThat("Шард не изменился",
                darkSideSteps.getClientFakeSteps().getUserShard(LOGIN_FOR_RESHARD), equalTo(newShard));
    }

    @Stories("FakeBalanceNotificationNDS")
    @Test
    public void setVATRateTest() {
        boolean result = darkSideSteps.getClientFakeSteps().setVATRate(LOGIN, RandomUtils.getNextInt(100));
        assertTrue("Method FakeBalanceNotificationNDS returned false", result);
    }

    @Stories("FakeSetAllowBudgetAccountForAgency")
    @Test
    public void fakeSetAllowBudgetAccountForAgencyTest() {
        boolean result = darkSideSteps.fakeAdminSteps().setAllowBudgetAccountForAgency(AGENCY_LOGIN, Status.YES);
        assertTrue("Method FakeSetAllowBudgetAccountForAgency returned false", result);
    }

    @Stories("AddEvents")
    @Test
    public void addEventsTest() {
        EventsLogItem event = new EventsLogItem();
        event.setCampaignID(FakeCampaignMethodsTest.CAMPAIGN_ID);
        event.setEventType("MoneyIn");
        event.setEventName("");
        event.setTimestamp("");
        EventsLogItemAttributes attributes = new EventsLogItemAttributes();
        attributes.setPayed(15f);
        event.setAttributes(attributes);
        boolean result = darkSideSteps.getEventLogFakeSteps().addEvents(event);
        assertTrue("Method AddEvents returned false", result);
    }

    @Stories("FakeUpdateRetargetingGoals")
    @Test
    public void fakeUpdateRetargetingGoalsTest() {
        int conditionID = 123;
        RetargetingConditionFakeInfo retargetingConditionFakeInfo =
                darkSideSteps.retargetingFakeSteps().updateRetargetingGoals(conditionID);
        assertNotNull(retargetingConditionFakeInfo.getClientID());
    }

    @Stories("FakeConvertCurrencyClient")
    @Test
    public void fakeConvertCurrencyClientTest() {
        Currency currency = Currency.values()[RandomUtils.getNextInt(Currency.values().length - 1) + 1];
        boolean result = darkSideSteps.getClientFakeSteps().
                convertCurrencyWithDelay(LOGIN_FOR_CONVERT, currency.value(), ConvertType.MODIFY, 1);
        assertTrue("Method FakeConvertCurrencyClient returned false", result);
    }

}
