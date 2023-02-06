package ru.yandex.autotests.directintapi.tests.metrica.oldtransport.checkagencyrightsformetrika;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.AgencyCheckForMetrikaResponse;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.steps.MetricaOldTransportSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;

import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by omaz on 16.12.13.
 * https://jira.yandex-team.ru/browse/TESTIRT-1238
 */
@Aqua.Test(title = "AgencyCheckForMetrica: самоходный клиент")
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.METRICA_AGENCY_CHECK_FOR_METRICA)
@RunWith(Parameterized.class)
public class AgencyCheckForMetricaOutsideClientTest {
    DarkSideSteps darkSideSteps = new DarkSideSteps();

    @Parameterized.Parameter
    public String agentUid;

    @Parameterized.Parameters(name = "представитель: {0}")
    public static java.util.Collection<Object[]> data() {
        Object[][] data = new Object[][]{{MetricaOldTransportSteps.AGENCY_UID},
                {MetricaOldTransportSteps.FULL_AGENT_UID},
                {MetricaOldTransportSteps.LIMITED_AGENT_UID},};
        return Arrays.asList(data);
    }

    @Test
    public void agencyCheckForMetricaOutsideClientTest() {
        AgencyCheckForMetrikaResponse response =
                darkSideSteps.getMetricaOldTransportSteps().checkAgencyRightsForMetrikaNoErrors(
                        agentUid,
                        MetricaOldTransportSteps.AGENCY_UID,
                        MetricaOldTransportSteps.NON_AGENCY_CLIENT_UID
                );
        AgencyCheckForMetrikaResponse.Data expected =
                new AgencyCheckForMetrikaResponse.Data();
        expected.setResult(0);
        expected.setMessage(MetricaOldTransportSteps.NON_AGENCY_CLIENT_UID +
                " is not subclient of agency " +
                agentUid);
        assertThat("Данные в ответе не совпадают с ожидаемыми", response.data,
                beanEquals(expected));
    }
}
