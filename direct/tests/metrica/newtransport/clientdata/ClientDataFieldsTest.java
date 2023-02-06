package ru.yandex.autotests.directintapi.tests.metrica.newtransport.clientdata;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.clientdata.ClientDataResponseItem;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.clientdata.ClientDataUidItem;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.clientdata.Fields;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.ClientFakeInfo;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.*;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 20.11.15.
 * https://st.yandex-team.ru/TESTIRT-7800
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.CLIENT_DATA)
@Description("Проверка поля fields в ручке ClientData")
@Issue("https://st.yandex-team.ru/DIRECT-48343")
@RunWith(Parameterized.class)
public class ClientDataFieldsTest {

    private static final String LOGIN_SINGLE = Logins.CLIENT_DATA_SINGLE;

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static DarkSideSteps darkSideSteps = api.userSteps.getDarkSideSteps();

    private static ClientFakeInfo singleFakeInfo;

    @Parameterized.Parameter(value = 0)
    public List<String> fields;

    @Parameterized.Parameter(value = 1)
    public ClientDataUidItem clientDataUidItem;

    @Parameterized.Parameters(name = "{0}")
    public static Collection strategies() {
        singleFakeInfo = darkSideSteps.getClientFakeSteps().getClientData(
                LOGIN_SINGLE, new String[]{"fio", "name"});
        Object[][] data = new Object[][]{
                {null, new ClientDataUidItem().withFio(singleFakeInfo.getFio())},
                {Arrays.asList(Fields.FIO.toString()), new ClientDataUidItem().withFio(singleFakeInfo.getFio())},
                {Arrays.asList(Fields.LOGIN.toString()), new ClientDataUidItem().withLogin(LOGIN_SINGLE)},
                {Arrays.asList(Fields.FIO.toString(), Fields.LOGIN.toString()),
                        new ClientDataUidItem().withFio(singleFakeInfo.getFio()).withLogin(LOGIN_SINGLE)},
        };
        return Arrays.asList(data);
    }

    @Test
    @Description("Клиент, имеющий единственного (главного) представителя")
    public void singleClient() {
        Long clientId = Long.parseLong(singleFakeInfo.getClientID());
        Map<Long, ClientDataResponseItem> actualResponse = darkSideSteps.getClientDataSteps()
                .getByClientID(Arrays.asList(clientId), true, fields);

        Map<Long, ClientDataUidItem> clientDataUidItemMap = new HashMap<>();
        clientDataUidItemMap.put(Long.parseLong(singleFakeInfo.getPassportID()), clientDataUidItem.withChief(true));

        ClientDataResponseItem clientDataResponseItem = new ClientDataResponseItem();
        clientDataResponseItem.setName(singleFakeInfo.getName());
        clientDataResponseItem.setUids(clientDataUidItemMap);
        Map<Long, ClientDataResponseItem> expectedResponse = new HashMap<>();
        expectedResponse.put(clientId, clientDataResponseItem);

        assertThat("вернулась верная информация об одном пользователе", actualResponse, beanDiffer(expectedResponse));
    }
}
