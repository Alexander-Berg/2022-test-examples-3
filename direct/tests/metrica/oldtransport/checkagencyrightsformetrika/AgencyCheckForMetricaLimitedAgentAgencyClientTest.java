package ru.yandex.autotests.directintapi.tests.metrica.oldtransport.checkagencyrightsformetrika;

import org.junit.Test;

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
@Aqua.Test(title = "AgencyCheckForMetrica: ограниченный представитель, клиент агентства")
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.METRICA_AGENCY_CHECK_FOR_METRICA)
public class AgencyCheckForMetricaLimitedAgentAgencyClientTest {
    DarkSideSteps darkSideSteps = new DarkSideSteps();

    @Test
    public void agencyCheckForMetricaLimitedAgentAgencyClientTest() {
        AgencyCheckForMetrikaResponse response =
                darkSideSteps.getMetricaOldTransportSteps().checkAgencyRightsForMetrikaNoErrors(
                        MetricaOldTransportSteps.LIMITED_AGENT_UID,
                        MetricaOldTransportSteps.AGENCY_UID,
                        MetricaOldTransportSteps.AGENCY_CLIENT_UID
                );
        AgencyCheckForMetrikaResponse.Data expected =
                new AgencyCheckForMetrikaResponse.Data();
        expected.setResult(0);
        expected.setMessage(MetricaOldTransportSteps.AGENCY_CLIENT_UID +
                " is not subclient of agency " +
                MetricaOldTransportSteps.LIMITED_AGENT_UID);
        assertThat("Данные в ответе не совпадают с ожидаемыми",
                response.data,
                beanEquals(expected));
    }
}
