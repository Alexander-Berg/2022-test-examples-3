package ru.yandex.autotests.directintapi.tests.metrica.oldtransport.campaignsformetrica;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;


/**
 * User: omaz
 * Date: 27.09.13
 * https://jira.yandex-team.ru/browse/TESTIRT-1044
 */
@Aqua.Test(title = "CampaignsForMetrica для некорректных UID")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.METRICA_CAMPAIGNS_FOR_METRICA)
public class CampaignsForMetricaInvalidUIDTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    DarkSideSteps darkSideSteps = new DarkSideSteps();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Test
    public void campaignsForMetricaNonexistUIDTest() {
        darkSideSteps.getMetricaOldTransportSteps().campaignsForMetricaExpectError("2", 404);

    }

    @Test
    public void campaignsForMetricaZeroUIDTest() {
        darkSideSteps.getMetricaOldTransportSteps().campaignsForMetricaExpectError("0", 404);

    }

    @Test
    public void campaignsForMetricaNegativeUIDTest() {
        int uid = Integer.valueOf(api.userSteps.clientFakeSteps()
                .getClientData(Logins.LOGIN_MAIN).getPassportID());
        darkSideSteps.getMetricaOldTransportSteps()
                .campaignsForMetricaExpectError("-" + uid, 404);

    }

}
