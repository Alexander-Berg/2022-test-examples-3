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
@Aqua.Test(title = "AgencyCheckForMetrica: полноправный представитель")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.METRICA_AGENCY_CHECK_FOR_METRICA)
@RunWith(Parameterized.class)
public class AgencyCheckForMetricaFullAgentTest {
    DarkSideSteps darkSideSteps = new DarkSideSteps();

    @Parameterized.Parameter
    public String clientUid;

    @Parameterized.Parameters(name = "клиент: {0}")
    public static java.util.Collection<Object[]> data() {
        Object[][] data = new Object[][]{{MetricaOldTransportSteps.AGENCY_CLIENT_UID},
                {MetricaOldTransportSteps.LIMITED_AGENT_CLIENT_UID},};
        return Arrays.asList(data);
    }

    @Test
    public void agencyCheckForMetricaLimitedAgentTest() {
        AgencyCheckForMetrikaResponse response =
                darkSideSteps.getMetricaOldTransportSteps().checkAgencyRightsForMetrikaNoErrors(
                        MetricaOldTransportSteps.FULL_AGENT_UID,
                        MetricaOldTransportSteps.AGENCY_UID,
                        clientUid
                );
        AgencyCheckForMetrikaResponse.Data expected =
                new AgencyCheckForMetrikaResponse.Data();
        expected.setResult(1);
        expected.setIsLimited(0);
        expected.setMessage("All ok - " + MetricaOldTransportSteps.AGENCY_UID + " is chief of " +
                MetricaOldTransportSteps.FULL_AGENT_UID + " and " +
                clientUid +
                " is subclient of " + MetricaOldTransportSteps.FULL_AGENT_UID
        );
        assertThat("Данные в ответе не совпадают с ожидаемыми", response.data,
                beanEquals(expected));
    }

}
