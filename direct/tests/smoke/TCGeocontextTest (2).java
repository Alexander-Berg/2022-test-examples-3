package ru.yandex.autotests.directintapi.tests.smoke;

import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.GetManagerInfoResponse;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by ginger on 03.07.15.
 * https://st.yandex-team.ru/TESTIRT-6105
 */
@Aqua.Test()
@Features(FeatureNames.GEOCONTEXT_API_MONITORING)
@Description("Проверка работоспособности API Геоконтекста")
public class TCGeocontextTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());

    private static String managerUid = "112645149";

    @ClassRule
    public static ApiSteps api = new ApiSteps().url(DirectTestRunProperties.getInstance().getGeoContextHost());

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Test
    public void getManagerInfoTest() {
        log.info("Проверим API Геоконтекста");
        GetManagerInfoResponse getManagerInfoResponse =
                api.userSteps.getDarkSideSteps().getGetManagerInfoSteps().getManagerInfoNoErrors(managerUid);
        assertThat("вызов успешен", getManagerInfoResponse.getHasAgencies(), notNullValue());
    }

}
