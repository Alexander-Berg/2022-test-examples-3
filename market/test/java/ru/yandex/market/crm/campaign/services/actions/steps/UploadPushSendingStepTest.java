package ru.yandex.market.crm.campaign.services.actions.steps;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.domain.actions.ActionConfig;
import ru.yandex.market.crm.campaign.domain.actions.ActionVariant;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.status.SendPushesStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.SendPushesStepStatus.UploadProgress;
import ru.yandex.market.crm.campaign.domain.actions.steps.SendPushesStep;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.loggers.SentPromoPushesLogger;
import ru.yandex.market.crm.campaign.services.actions.ActionYtPaths;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;
import ru.yandex.market.crm.campaign.services.actions.contexts.SendPushesStepContext;
import ru.yandex.market.crm.campaign.services.actions.tasks.ActionExecutionContext;
import ru.yandex.market.crm.campaign.services.actions.tasks.PlainStepStatusUpdaterImpl;
import ru.yandex.market.crm.campaign.services.appmetrica.PushMessagesUploader;
import ru.yandex.market.crm.campaign.services.appmetrica.UploadingResult;
import ru.yandex.market.crm.campaign.services.external.tsum.EventsTimelineService;
import ru.yandex.market.crm.campaign.services.gen.GlobalSplittingDescription;
import ru.yandex.market.crm.campaign.services.properties.PropertiesService;
import ru.yandex.market.crm.campaign.services.sending.FrequencyToggleService;
import ru.yandex.market.crm.campaign.services.sending.tasks.UploadPushSendingData;
import ru.yandex.market.crm.campaign.test.StepStatusDAOMock;
import ru.yandex.market.crm.campaign.test.StepStatusUpdaterMock;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.PushMessageConf;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.services.appmetrica.AppMetricaService;
import ru.yandex.market.crm.core.services.logging.SentPushesLogWriter;
import ru.yandex.market.crm.core.services.sending.UtmLinks;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.MobilePlatform;
import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.util.Exceptions.TrashConsumer;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author apershukov
 */
@RunWith(MockitoJUnitRunner.class)
public class UploadPushSendingStepTest {

    @Mock
    private YtClient ytClient;

    @Mock
    private SentPushesLogWriter logWriter;

    @Mock
    private EventsTimelineService timelineService;

    @Mock
    private PropertiesService propertiesService;

    @Mock
    private AppMetricaService appMetricaService;

    private StepsStatusDAO stepsStatusDAO;
    private PushMessagesUploaderStub pushMessagesUploader;
    private UploadPushSendingStep uploadStep;

    private PlainAction action;
    private SendPushesStep step;

    @Before
    public void setUp() {
        stepsStatusDAO = new StepStatusDAOMock();
        var frequencyToggleService = mock(FrequencyToggleService.class);
        pushMessagesUploader = new PushMessagesUploaderStub(frequencyToggleService, ytClient);

        uploadStep = new UploadPushSendingStep(
                ytClient,
                pushMessagesUploader,
                timelineService,
                propertiesService,
                appMetricaService,
                logWriter
        );

        step = new SendPushesStep();
        step.setId("action_step");

        ActionVariant variant = new ActionVariant();
        variant.setId("a");
        variant.setPercent(100);
        variant.setSteps(List.of(step));

        ActionConfig config = new ActionConfig();
        config.setTarget(new TargetAudience(LinkingMode.DIRECT_ONLY, "segment_id"));
        config.setVariants(List.of(variant));

        action = new PlainAction();
        action.setId("action");
        action.setName("name");
        action.setConfig(config);
    }

    /**
     * После выгрузки всех 20-ти сообщений, статистика выгрузки устанавливается как 20/20
     */
    @Test
    public void testStatusAfterSuccessfulUpload() throws Exception {
        setTotalRowCount(20);

        ExecutionResult result = execute();
        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETING, result.getNextStatus());

        SendPushesStepStatus status = getStepStatus();

        UploadProgress uploadProgress = status.getUploadProgress();
        assertNotNull(uploadProgress);
        assertEquals(20, uploadProgress.getTotal());
        assertEquals(20, uploadProgress.getUploaded());
    }

    /**
     * Перед выгрузкой в статусе заполняется общее количество сообщений для выглузки.
     * При этом счетчик выгруженных сообщений устанавливается равным нулю
     */
    @Test
    public void testStatusBeforeAnyBatchIsUploaded() throws InterruptedException {
        setTotalRowCount(50);

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        onProcessInterval(i -> {
            latch1.countDown();
            latch2.await(5, TimeUnit.SECONDS);
        });

        new Thread(() -> {
            try {
                execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();

        latch1.await(5, TimeUnit.SECONDS);

        SendPushesStepStatus status = getStepStatus();

        UploadProgress uploadProgress = status.getUploadProgress();
        assertNotNull(uploadProgress);
        assertEquals(50, uploadProgress.getTotal());
        assertEquals(0, uploadProgress.getUploaded());

        latch2.countDown();
    }

    /**
     * Выгрузка из большой таблицы бьется на несколько частей
     */
    @Test
    public void testSplitBigUploadIntoBatches() throws Exception {
        setTotalRowCount(200_000);

        BlockingQueue<Pair<Integer, Integer>> intervals = new ArrayBlockingQueue<>(10);

        onProcessInterval(intervals::put);

        ExecutionResult result = execute();
        assertNotNull(result);
        assertEquals(TaskStatus.WAITING, result.getNextStatus());
        assertNotNull("No pause between batches", result.getNextRunTime());

        assertEquals(1, intervals.size());
        assertEquals(Pair.of(0, 40_000), intervals.take());

        SendPushesStepStatus status = getStepStatus();

        SendPushesStepStatus.UploadProgress uploadProgress = status.getUploadProgress();
        assertNotNull(uploadProgress);
        assertEquals(40_000, uploadProgress.getUploaded());
        assertEquals(200_000, uploadProgress.getTotal());
    }

    /**
     * В случае если в информации, связанной с таской уже сохранен прогресс, выгрузка продолжается с того места
     * на котором, согласно прогрессу, она завершилась в прошлый раз.
     */
    @Test
    public void testUploadStartsFromLastProcessedMessage() throws Exception {
        setTotalRowCount(200_000);

        BlockingQueue<Pair<Integer, Integer>> intervals = new ArrayBlockingQueue<>(10);

        onProcessInterval(intervals::put);

        SendPushesStepStatus status = new SendPushesStepStatus()
                .setUploadProgress(new UploadProgress(30_000, 200_000));

        UploadPushSendingData data = new UploadPushSendingData();
        data.setUploadedCount(30_000);
        data.setRowCount(200_000);

        execute(status, data);

        assertEquals(1, intervals.size());
        assertEquals(Pair.of(30_000, 70_000), intervals.take());

        status = getStepStatus();

        SendPushesStepStatus.UploadProgress uploadProgress = status.getUploadProgress();
        assertNotNull(uploadProgress);
        assertEquals(200_000, uploadProgress.getTotal());
        assertEquals(70_000, uploadProgress.getUploaded());
    }

    private SendPushesStepStatus getStepStatus() {
        return (SendPushesStepStatus) stepsStatusDAO.get(action.getId(), step.getId());
    }

    private ExecutionResult execute() throws Exception {
        return execute(new SendPushesStepStatus(), null);
    }

    private ExecutionResult execute(SendPushesStepStatus status, UploadPushSendingData data) throws Exception {
        stepsStatusDAO.upsert(action.getId(), status.setStepId(step.getId()));

        ActionExecutionContext parentContext = new ActionExecutionContext(
                action,
                step,
                status,
                new PlainStepStatusUpdaterImpl(action.getId(), step.getId(), new StepStatusUpdaterMock(stepsStatusDAO)),
                "",
                "",
                new ActionYtPaths(YPath.cypressRoot()),
                action.getOuterId() + "_" + step.getId()
        );

        PushMessageConf config = new PushMessageConf();
        config.setPushConfigs(Map.of(MobilePlatform.ANDROID, new AndroidPushConf()));

        var template = new MessageTemplate<PushMessageConf>();
        template.setConfig(config);

        var application = new MobileApplication();
        application.setId(MobileApplication.MARKET_APP);
        application.setMetricaAppId(111);

        SendPushesStepContext context = new SendPushesStepContext(
                parentContext,
                template,
                Collections.emptyList(),
                mock(GlobalSplittingDescription.class),
                application
        );

        return uploadStep.run(context, data, mock(Control.class));
    }

    private void setTotalRowCount(int count) {
        when(ytClient.getRowCount(any())).thenReturn((long) count);
    }

    private static class PushMessagesUploaderStub extends PushMessagesUploader {
        private final YtClient ytClient;
        private TrashConsumer<Pair<Integer, Integer>> callback;

        public PushMessagesUploaderStub(FrequencyToggleService frequencyToggleService, YtClient ytClient) {
            super(null, null, null, null, null, frequencyToggleService);
            this.ytClient = ytClient;
        }

        public void setCallback(TrashConsumer<Pair<Integer, Integer>> callback) {
            this.callback = callback;
        }

        @Nonnull
        @Override
        public UploadingResult uploadInterval(String sendingId,
                                              String sendingKey,
                                              MobileApplication application,
                                              UtmLinks utmLinks,
                                              YPath table,
                                              LocalTime finishLimit,
                                              SentPromoPushesLogger logger,
                                              Pair<Integer, Integer> interval) {
            if (callback != null) {
                callback.accept(interval);
            }

            var intervalSize = interval.getRight() - interval.getLeft();
            var rowsLeft = (int) ytClient.getRowCount(table) - interval.getLeft();
            var processed = Math.min(intervalSize, rowsLeft);
            return new UploadingResult(null, processed, 0, 0);
        }
    }

    private void onProcessInterval(TrashConsumer<Pair<Integer, Integer>> callback) {
        pushMessagesUploader.setCallback(callback);
    }
}
