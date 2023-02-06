package ru.yandex.autotests.directintapi.tests.metrica.oldtransport.retargetingsegmentsformetrica;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.RetargetingForMetricaRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by semkagtn on 11/27/14.
 */
@Aqua.Test(title = "RetargetingSegmentsForMetrica: некорретные значения параметров")
@Features(FeatureNames.METRICA_RETARGETING_SEGMENTS_FOR_METRICA)
@RunWith(Parameterized.class)
public class RetargetingSegmentsForMetricaIncorrectParametersTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Parameterized.Parameter(0)
    public String chunkCount;

    @Parameterized.Parameter(1)
    public String chunkNum;

    @Parameterized.Parameters(name = "chunk_count = {0}, chunk_num = {1}")
    public static Collection data() {
        return Arrays.asList(new Object[][]{
                {"3.14", "1"}, // chunk_count == double
                {"1", "3.14"}, // chunk_num == double
                {"5", "-1"}, // chunk_num < 0
                {"5", "5"}, // chunk_num == chunk_count
        });
    }

    @Test
    public void parametersOfInvalidTypeInRequestTest() {
        darkSideSteps.getMetricaOldTransportSteps().retargetingSegmentsForMetricaExpectError(
                new RetargetingForMetricaRequest()
                        .withChunkCount(chunkCount)
                        .withChunkNum(chunkNum),
                500);
    }
}
