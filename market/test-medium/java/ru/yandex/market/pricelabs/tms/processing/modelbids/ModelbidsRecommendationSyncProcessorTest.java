package ru.yandex.market.pricelabs.tms.processing.modelbids;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeDeepCopier;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.ModelbidsRecommendation;
import ru.yandex.market.pricelabs.model.Shop;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.services.database.model.Task;
import ru.yandex.market.pricelabs.tms.ConfigurationForTests.MockWebServerControls;
import ru.yandex.market.pricelabs.tms.jobs.PostProcessingModelbidsRecommendation;
import ru.yandex.market.pricelabs.tms.processing.AbstractProcessorSpringConfiguration;
import ru.yandex.market.pricelabs.tms.processing.ProcessingRouter;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.update;

public class ModelbidsRecommendationSyncProcessorTest
        extends AbstractProcessorSpringConfiguration<ModelbidsRecommendation> {

    private static final int SHOP_ID = 2289;

    @Autowired
    private ModelbidsRecommendationSyncProcessor processor;

    @Autowired
    private PostProcessingModelbidsRecommendation job;

    @Autowired
    private ProcessingRouter router;

    private YtScenarioExecutor<ModelbidsRecommendation> executor;


    @Autowired
    @Qualifier("mockWebServerMarketReport")
    private MockWebServerControls mockWebServerMarketReport;

    private String marketReportResponse;

    @BeforeEach
    void init() {
        Shop shop = shop(SHOP_ID);
        shop.setRegion_id(1);

        this.executor = executors.modelBids();
        this.marketReportResponse = Utils.readResource("tms/processing/modelbids/modelbids_recommendation.json");

        testControls.executeInParallel(
                () -> testControls.saveShop(shop),
                () -> mockWebServerMarketReport.cleanup(),
                () -> testControls.cleanupTasksService());
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/modelbids/modelbids_recommendation_target.csv";
    }

    @Override
    protected Class<ModelbidsRecommendation> getTargetClass() {
        return ModelbidsRecommendation.class;
    }

    @Test
    void testNoData() {
        this.test(List.of(), List.of());
    }

    @Test
    void testJobScheduleSyncWithReport() {
        var sourceList = sourceList();

        executor.insert(sourceList); // Нужен, чтобы собрать список задач для процессинга
        mockWebServerMarketReport.enqueue(new MockResponse().setBody(marketReportResponse));

        job.scheduleSyncWithReport(null);

        var tasks = testControls.startScheduledTasks(200, JobType.MODELBIDS_RECOMMENDATION);
        testControls.checkNoScheduledTasks();

        testControls.executeTask(tasks.get(0));

        var sum = sourceList.stream()
                .map(ModelbidsRecommendation::getModel_id)
                .collect(Collectors.summarizingLong(Integer::intValue));

        @Nullable ModelsRangeArg last = null;
        for (Task task : tasks) {
            ModelsRangeArg args = router.fromJsonArg(task);
            if (last == null) {
                assertEquals(sum.getMin(), args.getFromModelId());
            } else {
                assertEquals(args.getFromModelId(), last.getToModelId());
            }
            last = args;
        }
        if (last != null) {
            assertTrue(last.getToModelId() >= sum.getMax());
        }
    }

    @Test
    void testModelbidsUpdate() {
        mockWebServerMarketReport.enqueue(new MockResponse().setBody(marketReportResponse));

        List<ModelbidsRecommendation> expect = new ArrayList<>();

        var target = readTargetList();
        assertEquals(2, target.size()); // считаем, что до добавления новой строки была одна запись

        var sourceList = sourceList();
        sourceList.stream()
                .filter(m -> (!(m.getRegion_id() == 213 && m.getModel_id() == 12631379) && m.getRegion_id() != 225))
                .forEach(expect::add);

        var model = YTreeDeepCopier.deepCopyOf(sourceList.get(3));
        model.setVbid(0);
        model.setMinVbid(12);
        model.setRegion_id(213);
        model.setPositions(List.of(
                TmsTestUtils.modelbidsPosition(0, 15, 95),
                TmsTestUtils.modelbidsPosition(0, 12, 96)
        ));
        model.setModel_id(12631379);
        model.setEstimatePosition(0);
        model.setCategory_id(0);
        model.setBrand_id(0);
        model.setModel_updated_at(timeSource().getInstant()); // Только это поле с timestamp-ом будет обновлено
        expect.add(model);
        sourceList.stream()
                .filter(m -> (m.getRegion_id() != 213))
                .forEach(expect::add);

        this.test(sourceList, expect);

        checkRequest(mockWebServerMarketReport.getMessage(), 2, 19, 23, 12631379);

        mockWebServerMarketReport.checkNoMessages();
    }


    @Test
    void testWithoutPositions() {
        marketReportResponse = Utils.readResource("tms/processing/modelbids" +
                "/modelbids_recommendation_without_positions.json");
        mockWebServerMarketReport.enqueue(new MockResponse().setBody(marketReportResponse));

        test(sourceList(), update(sourceList(), model -> {
            if (model.getModel_id() == 12631379 && model.getRegion_id() == 213) {
                model.setModel_updated_at(getInstant());
            }
        }));

        checkRequest(mockWebServerMarketReport.getMessage(), 2, 19, 23, 12631379);

        mockWebServerMarketReport.checkNoMessages();
    }

    private List<ModelbidsRecommendation> sourceList() {
        return readTargetList("tms/processing/modelbids/modelbids_recommendation_source.csv");
    }

    private void test(List<ModelbidsRecommendation> existingRows, List<ModelbidsRecommendation> expectRows) {
        var arg = new ModelsRangeArg(2, 12631379, 1, Long.MAX_VALUE);
        executor.test(() -> processor.syncModels(arg),
                existingRows, expectRows);
    }

    private void checkRequest(RecordedRequest request, Integer... models) {
        assertNotNull(request);

        var url = request.getRequestUrl();
        assertEquals("model_bids_recommender", url.queryParameter("place"));
        assertEquals("213", url.queryParameter("rids"));
        assertEquals("2", url.queryParameter("bsformat"));
        assertEquals("7", url.queryParameter("pp"));

        String expectModels = Objects.requireNonNull(url.queryParameter("hyperid"));
        var actual = Stream.of(expectModels.split(",")).map(Integer::parseInt).collect(Collectors.toSet());
        assertEquals(Set.of(models), actual);

        assertEquals("/yandsearch", url.encodedPath());
    }

}
