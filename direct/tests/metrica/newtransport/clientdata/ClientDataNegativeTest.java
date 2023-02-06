package ru.yandex.autotests.directintapi.tests.metrica.newtransport.clientdata;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.exceptions.DarkSideException;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by semkagtn on 16.06.15.
 * https://st.yandex-team.ru/TESTIRT-5923
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.CLIENT_DATA)
@Description("ClientData - неверные запросы")
@Issue("https://st.yandex-team.ru/DIRECT-40916")
public class ClientDataNegativeTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    private static DarkSideSteps darkSideSteps = api.userSteps.getDarkSideSteps();

    @Test
    @Description("Отсутствие идентификаторов клиента в запросе")
    public void requestWithoutClientId() {
        try {
            darkSideSteps.getClientDataSteps().getByClientID(Arrays.asList(), true);
        } catch (DarkSideException e) {
            assertThat("вернулся правильный тип исключения",
                    e.getCause().getClass(), equalTo(JsonRpcClientException.class));
        }
    }

    @Test
    @Description("Невалидный идентификатор клиента в запросе")
    public void invalidClientIdInRequest() {
        try {
            darkSideSteps.getClientDataSteps().getByClientID(Arrays.asList(-1l), true);
        } catch (DarkSideException e) {
            assertThat("вернулся правильный тип исключения",
                    e.getCause().getClass(), equalTo(JsonRpcClientException.class));
        }
    }

    @Test
    @Description("Неизвестное поле в fields")
    public void requestWithInvalidValueInFields() {
        try {
            darkSideSteps.getClientDataSteps().getByClientID(Arrays.asList(123l), true, Arrays.asList("invalid"));
        } catch (DarkSideException e) {
            assertThat("вернулся правильный тип исключения",
                    e.getCause().getClass(), equalTo(JsonRpcClientException.class));
        }
    }
}
