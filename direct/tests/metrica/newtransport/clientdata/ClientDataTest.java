package ru.yandex.autotests.directintapi.tests.metrica.newtransport.clientdata;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.clientdata.ClientDataResponseItem;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.clientdata.ClientDataUidItem;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.ClientFakeInfo;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created by semkagtn on 16.06.15.
 * https://st.yandex-team.ru/TESTIRT-5923
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.CLIENT_DATA)
@Description("Проверка ручки ClientData")
@Issue("https://st.yandex-team.ru/DIRECT-40916")
public class ClientDataTest {

    private static final String LOGIN_SINGLE = Logins.CLIENT_DATA_SINGLE;

    private static final String LOGIN_MAIN = Logins.CLIENT_DATA_MAIN;
    private static final String LOGIN_REP1 = Logins.CLIENT_DATA_REP1;
    private static final String LOGIN_REP2 = Logins.CLIENT_DATA_REP2;

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static DarkSideSteps darkSideSteps = api.userSteps.getDarkSideSteps();

    private static ClientFakeInfo singleFakeInfo;
    private static ClientFakeInfo mainFakeInfo;
    private static ClientFakeInfo rep1FakeInfo;
    private static ClientFakeInfo rep2FakeInfo;

    @BeforeClass
    public static void initData() {
        singleFakeInfo = darkSideSteps.getClientFakeSteps().getClientData(
                LOGIN_SINGLE, new String[]{"fio", "name"});
        mainFakeInfo = darkSideSteps.getClientFakeSteps().getClientData(
                LOGIN_MAIN, new String[]{"fio", "name"});
        rep1FakeInfo = darkSideSteps.getClientFakeSteps().getClientDataByLogin(LOGIN_REP1, "fio");
        rep2FakeInfo = darkSideSteps.getClientFakeSteps().getClientDataByLogin(LOGIN_REP2, "fio");
    }

    @Test
    @Description("Клиент, имеющий единственного (главного) представителя")
    public void singleClient() {
        Long clientId = Long.parseLong(singleFakeInfo.getClientID());
        Map<Long, ClientDataResponseItem> actualResponse = darkSideSteps.getClientDataSteps()
                .getByClientID(Arrays.asList(clientId), true);

        ClientDataUidItem clientDataUidItem = new ClientDataUidItem();
        clientDataUidItem.setChief(true);
        clientDataUidItem.setFio(singleFakeInfo.getFio());

        Map<Long, ClientDataUidItem> clientDataUidItemMap = new HashMap<>();
        clientDataUidItemMap.put(Long.parseLong(singleFakeInfo.getPassportID()), clientDataUidItem);

        ClientDataResponseItem clientDataResponseItem = new ClientDataResponseItem();
        clientDataResponseItem.setName(singleFakeInfo.getName());
        clientDataResponseItem.setUids(clientDataUidItemMap);
        Map<Long, ClientDataResponseItem> expectedResponse = new HashMap<>();
        expectedResponse.put(clientId, clientDataResponseItem);

        assertThat("вернулась верная информация об одном пользователе", actualResponse, beanDiffer(expectedResponse));
    }

    @Test
    @Description("Клиент имеющий несколько представителей")
    public void clientSeveralReps() {
        Long clientId = Long.parseLong(mainFakeInfo.getClientID());
        Map<Long, ClientDataResponseItem> actualResponse = darkSideSteps.getClientDataSteps()
                .getByClientID(Arrays.asList(clientId), true);

        ClientDataUidItem mainClientDataUidItem = new ClientDataUidItem();
        mainClientDataUidItem.setChief(true);
        mainClientDataUidItem.setFio(mainFakeInfo.getFio());

        ClientDataUidItem rep1ClientDataUidItem = new ClientDataUidItem();
        rep1ClientDataUidItem.setChief(null);
        rep1ClientDataUidItem.setFio(rep1FakeInfo.getFio());

        ClientDataUidItem rep2ClientDataUidItem = new ClientDataUidItem();
        rep2ClientDataUidItem.setChief(null);
        rep2ClientDataUidItem.setFio(rep2FakeInfo.getFio());

        Map<Long, ClientDataUidItem> clientDataUidItemMap = new HashMap<>();
        clientDataUidItemMap.put(Long.parseLong(mainFakeInfo.getPassportID()), mainClientDataUidItem);
        clientDataUidItemMap.put(Long.parseLong(rep1FakeInfo.getPassportID()), rep1ClientDataUidItem);
        clientDataUidItemMap.put(Long.parseLong(rep2FakeInfo.getPassportID()), rep2ClientDataUidItem);

        ClientDataResponseItem clientDataResponseItem = new ClientDataResponseItem();
        clientDataResponseItem.setName(mainFakeInfo.getName());
        clientDataResponseItem.setUids(clientDataUidItemMap);
        Map<Long, ClientDataResponseItem> expectedResponse = new HashMap<>();
        expectedResponse.put(clientId, clientDataResponseItem);

        assertThat("вернулась верня информация о трёх пользователях", actualResponse, beanDiffer(expectedResponse));
    }

    @Test
    @Description("Два клиента в одном запросе")
    public void twoClientsInRequest() {
        Long mainClientId = Long.parseLong(mainFakeInfo.getClientID());
        Long singleClientId = Long.parseLong(singleFakeInfo.getClientID());

        Map<Long, ClientDataResponseItem> actualResponse = darkSideSteps.getClientDataSteps()
                .getByClientID(Arrays.asList(mainClientId, singleClientId), true);

        ClientDataUidItem mainClientDataUidItem = new ClientDataUidItem();
        mainClientDataUidItem.setChief(true);
        mainClientDataUidItem.setFio(mainFakeInfo.getFio());

        ClientDataUidItem clientRep1UidItem = new ClientDataUidItem();
        clientRep1UidItem.setChief(null);
        clientRep1UidItem.setFio(rep1FakeInfo.getFio());

        ClientDataUidItem clientRep2UidItem = new ClientDataUidItem();
        clientRep2UidItem.setChief(null);
        clientRep2UidItem.setFio(rep2FakeInfo.getFio());

        Map<Long, ClientDataUidItem> mainClientDataUidItemMap = new HashMap<>();
        mainClientDataUidItemMap.put(Long.parseLong(mainFakeInfo.getPassportID()), mainClientDataUidItem);
        mainClientDataUidItemMap.put(Long.parseLong(rep1FakeInfo.getPassportID()), clientRep1UidItem);
        mainClientDataUidItemMap.put(Long.parseLong(rep2FakeInfo.getPassportID()), clientRep2UidItem);

        ClientDataResponseItem mainClientDataResponseItem = new ClientDataResponseItem();
        mainClientDataResponseItem.setName(mainFakeInfo.getName());
        mainClientDataResponseItem.setUids(mainClientDataUidItemMap);


        ClientDataUidItem singleClientDataUidItem = new ClientDataUidItem();
        singleClientDataUidItem.setChief(true);
        singleClientDataUidItem.setFio(singleFakeInfo.getFio());

        Map<Long, ClientDataUidItem> singleClientDataUidItemMap = new HashMap<>();
        singleClientDataUidItemMap.put(Long.parseLong(singleFakeInfo.getPassportID()), singleClientDataUidItem);

        ClientDataResponseItem singleClientDataResponseItem = new ClientDataResponseItem();
        singleClientDataResponseItem.setName(singleFakeInfo.getName());
        singleClientDataResponseItem.setUids(singleClientDataUidItemMap);

        Map<Long, ClientDataResponseItem> expectedResponse = new HashMap<>();
        expectedResponse.put(mainClientId, mainClientDataResponseItem);
        expectedResponse.put(singleClientId, singleClientDataResponseItem);

        assertThat("вернулась верня информация о четырёх пользователях", actualResponse, beanDiffer(expectedResponse));
    }
}
