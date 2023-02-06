package ru.yandex.autotests.directintapi.tests.metrica.oldtransport.retargetingsegmentsformetrica;

import ch.lambdaj.function.convert.Converter;
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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static ch.lambdaj.Lambda.convert;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by semkagtn on 11/27/14.
 */
@RunWith(Parameterized.class)
@Aqua.Test(title = "RetargetingSegmentsForMetrica: проверка критерия выбора segment_id")
@Description("Если заданы параметры в запросе, то segment_id выбираются только такие, которые удовлетворяют условию" +
        " segment_id % chunk_count == chunk_num")
@Features(FeatureNames.METRICA_RETARGETING_SEGMENTS_FOR_METRICA)
public class RetargetingSegmentsForMetricaSegmentIdsSelectionTest {

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
        RetargetingSegmentsForMetricaResponse response = darkSideSteps.getMetricaOldTransportSteps()
                .retargetingSegmentsForMetricaNoErrors(new RetargetingForMetricaRequest()
                        .withChunkCount(chunkCount)
                        .withChunkNum(chunkNum));
        List<Long> segmentIdsModChunkNum = convert(response.getSegments(),
                new Converter<RetargetingSegmentsForMetricaResponse.Segment, Long>() {
                    @Override
                    public Long convert(RetargetingSegmentsForMetricaResponse.Segment from) {
                        return from.getSegmentId() % chunkCount;
                    }
                });
        assertThat("для любого segment_id выполняется: segment_id % chunk_count == chunk_num",
                segmentIdsModChunkNum, everyItem(equalTo(Long.valueOf(chunkNum))));
    }
}
