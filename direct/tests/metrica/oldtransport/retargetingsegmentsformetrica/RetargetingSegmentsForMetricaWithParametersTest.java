package ru.yandex.autotests.directintapi.tests.metrica.oldtransport.retargetingsegmentsformetrica;

import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.RetargetingForMetricaRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.tsv.RetargetingSegmentsForMetricaResponse;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by semkagtn on 11/27/14.
 */
@RunWith(Parameterized.class)
@Aqua.Test(title = "RetargetingSegmentsForMetrica: проверка корректности ответа")
@Features(FeatureNames.METRICA_RETARGETING_SEGMENTS_FOR_METRICA)
public class RetargetingSegmentsForMetricaWithParametersTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Parameterized.Parameter(0)
    public int chunkCount;

    @Parameterized.Parameter(1)
    public int chunkNum;

    @Parameterized.Parameters(name = "chuck_count = {0}, chuck_num = {1}")
    public static Collection data() {
        return Arrays.asList(new Object[][]{
                {100, 0},
                {100, 50},
                {100, 99},
        });
    }

    @Test
    public void requestWithParametersTest() {
        List<RetargetingSegmentsForMetricaResponse.Segment> segments = darkSideSteps.getMetricaOldTransportSteps()
                .retargetingSegmentsForMetricaNoErrors(new RetargetingForMetricaRequest()
                        .withChunkCount(chunkCount)
                        .withChunkNum(chunkNum)).getSegments();
        assertThat("возвращаемые значения лежат в допустимом диапазоне", segments, everyItem((Matcher) allOf(
                having(on(RetargetingSegmentsForMetricaResponse.Segment.class).getSegmentId(), greaterThan(0l)),
                having(on(RetargetingSegmentsForMetricaResponse.Segment.class).getInterval(),
                        allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(30)))
        )));
    }

}
