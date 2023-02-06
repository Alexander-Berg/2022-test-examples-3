package ru.yandex.autotests.directintapi.tests.metrica.oldtransport.checkagencyrightsformetrika;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.steps.MetricaOldTransportSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;

/**
 * Created by omaz on 16.12.13.
 * https://jira.yandex-team.ru/browse/TESTIRT-1238
 */
@Aqua.Test(title = "AgencyCheckForMetrica: некорректные параметры")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.METRICA_AGENCY_CHECK_FOR_METRICA)
@RunWith(Parameterized.class)
public class AgencyCheckForMetricaIncorrectParametersTest {
    DarkSideSteps darkSideSteps = new DarkSideSteps();

    @Parameterized.Parameter
    public String incorrectUID;

    @Parameterized.Parameters(name = "incorrect value: {0}")
    public static java.util.Collection<Object[]> data() {
        Object[][] data = new Object[][]{{"0"},
                {"-1"},
                {"abc"}};
        return Arrays.asList(data);
    }

    @Test
    public void agencyCheckForMetricaIncorrectUidTest() {
        darkSideSteps.getMetricaOldTransportSteps().checkAgencyRightsForMetrikaExpectError(
                incorrectUID,
                MetricaOldTransportSteps.AGENCY_UID,
                MetricaOldTransportSteps.LIMITED_AGENT_CLIENT_UID,
                500
        );
    }

    @Test
    public void agencyCheckForMetricaIncorrectAgencyUidTest() {
        darkSideSteps.getMetricaOldTransportSteps().checkAgencyRightsForMetrikaExpectError(
                MetricaOldTransportSteps.LIMITED_AGENT_UID,
                incorrectUID,
                MetricaOldTransportSteps.LIMITED_AGENT_CLIENT_UID,
                500
        );
    }

    @Test
    public void agencyCheckForMetricaIncorrectClientUidTest() {
        darkSideSteps.getMetricaOldTransportSteps().checkAgencyRightsForMetrikaExpectError(
                MetricaOldTransportSteps.LIMITED_AGENT_UID,
                MetricaOldTransportSteps.AGENCY_UID,
                incorrectUID,
                500
        );
    }
}
