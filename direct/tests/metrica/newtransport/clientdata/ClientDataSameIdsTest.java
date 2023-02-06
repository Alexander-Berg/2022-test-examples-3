package ru.yandex.autotests.directintapi.tests.metrica.newtransport.clientdata;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.clientdata.ClientDataResponseItem;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by semkagtn on 16.06.15.
 * https://st.yandex-team.ru/TESTIRT-5923
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.CLIENT_DATA)
@Description("ClientData - одинаковые идентификаторы в запросе")
@Issue("https://st.yandex-team.ru/DIRECT-40916")
public class ClientDataSameIdsTest {

    private static final String LOGIN = Logins.CLIENT_DATA_SINGLE;

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static DarkSideSteps darkSideSteps = api.userSteps.getDarkSideSteps();

    private Long clientId;

    @Before
    @Step("Подготовка данных для теста")
    public void initData() {
        clientId = Long.parseLong(darkSideSteps.getClientFakeSteps().getClientData(LOGIN).getClientID());
    }

    @Test
    public void clientDataWithoutMarkChiefRepsRequest() {
        Map<Long, ClientDataResponseItem> actualResponse =
                darkSideSteps.getClientDataSteps().getByClientID(Arrays.asList(clientId, clientId), true);

        assertThat("в ответе вернулась информация об одном клиенте", actualResponse.entrySet(), hasSize(1));
    }
}
