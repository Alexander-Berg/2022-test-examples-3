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
 * Created by semkagtn on 17.06.15.
 * https://st.yandex-team.ru/TESTIRT-5923
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.CLIENT_DATA)
@Description("ClientData - запрос для агенства")
@Issue("https://st.yandex-team.ru/DIRECT-40916")
public class ClientDataAgencyTest {

    private static final String AGENCY = Logins.CLIENT_DATA_AGENCY;
    private static final String AGENCY_REP = Logins.CLIENT_DATA_AGENCY_REP;
    private static final String AGENCY_REP_CLIENTS = Logins.CLIENT_DATA_AGENCY_REP_CLIENTS;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static DarkSideSteps darkSideSteps = api.userSteps.getDarkSideSteps();

    private static ClientFakeInfo agencyInfo;
    private static ClientFakeInfo agencyRepInfo;
    private static ClientFakeInfo agencyRepClientInfo;

    @BeforeClass
    public static void initData() {
        agencyInfo = darkSideSteps.getClientFakeSteps().getClientData(
                AGENCY, new String[]{"fio", "name"});
        agencyRepInfo = darkSideSteps.getClientFakeSteps().getClientDataByLogin(AGENCY_REP, "fio");
        agencyRepClientInfo = darkSideSteps.getClientFakeSteps().getClientDataByLogin(AGENCY_REP_CLIENTS, "fio");
    }

    @Test
    public void requestForAgency() {
        Long clientId = Long.parseLong(agencyInfo.getClientID());
        Map<Long, ClientDataResponseItem> actualResponse = darkSideSteps.getClientDataSteps()
                .getByClientID(Arrays.asList(clientId), true);

        ClientDataUidItem mainClientDataUidItem = new ClientDataUidItem();
        mainClientDataUidItem.setChief(true);
        mainClientDataUidItem.setFio(agencyInfo.getFio());

        ClientDataUidItem rep1ClientDataUidItem = new ClientDataUidItem();
        rep1ClientDataUidItem.setChief(null);
        rep1ClientDataUidItem.setFio(agencyRepInfo.getFio());

        ClientDataUidItem rep2ClientDataUidItem = new ClientDataUidItem();
        rep2ClientDataUidItem.setChief(null);
        rep2ClientDataUidItem.setFio(agencyRepClientInfo.getFio());

        Map<Long, ClientDataUidItem> clientDataUidItemMap = new HashMap<>();
        clientDataUidItemMap.put(Long.parseLong(agencyInfo.getPassportID()), mainClientDataUidItem);
        clientDataUidItemMap.put(Long.parseLong(agencyRepInfo.getPassportID()), rep1ClientDataUidItem);
        clientDataUidItemMap.put(Long.parseLong(agencyRepClientInfo.getPassportID()), rep2ClientDataUidItem);

        ClientDataResponseItem clientDataResponseItem = new ClientDataResponseItem();
        clientDataResponseItem.setName(agencyInfo.getName());
        clientDataResponseItem.setUids(clientDataUidItemMap);
        Map<Long, ClientDataResponseItem> expectedResponse = new HashMap<>();
        expectedResponse.put(clientId, clientDataResponseItem);

        assertThat("вернулась верня информация о трёх пользователях", actualResponse, beanDiffer(expectedResponse));
    }
}
