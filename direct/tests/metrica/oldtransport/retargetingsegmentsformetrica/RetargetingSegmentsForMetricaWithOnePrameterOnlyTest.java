package ru.yandex.autotests.directintapi.tests.metrica.oldtransport.retargetingsegmentsformetrica;

import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.RetargetingForMetricaRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

/**
 * Created by semkagtn on 11/27/14.
 */
@Aqua.Test(title = "RetargetingSegmentsForMetrica: запрос только с одним параметром")
@Features(FeatureNames.METRICA_RETARGETING_SEGMENTS_FOR_METRICA)
public class RetargetingSegmentsForMetricaWithOnePrameterOnlyTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Test
    public void requestWithouChuckCountTest() {
        darkSideSteps.getMetricaOldTransportSteps()
                .retargetingSegmentsForMetricaExpectError(
                        new RetargetingForMetricaRequest()
                                .withChunkNum(1),
                        500);
    }

    @Test
    public void requestWithouChuckNumTest() {
        darkSideSteps.getMetricaOldTransportSteps()
                .retargetingSegmentsForMetricaExpectError(
                        new RetargetingForMetricaRequest()
                                .withChunkCount(1),
                        500);
    }
}
