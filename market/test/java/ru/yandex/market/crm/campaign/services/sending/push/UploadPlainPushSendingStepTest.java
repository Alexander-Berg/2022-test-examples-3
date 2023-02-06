package ru.yandex.market.crm.campaign.services.sending.push;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.PushSendingFactInfo;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactStatus;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactTransportState;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactType;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingVariantConf;
import ru.yandex.market.crm.campaign.services.appmetrica.PushMessagesUploader;
import ru.yandex.market.crm.campaign.services.appmetrica.UploadingResult;
import ru.yandex.market.crm.campaign.services.external.tsum.EventsTimelineService;
import ru.yandex.market.crm.campaign.services.properties.PropertiesService;
import ru.yandex.market.crm.campaign.services.sending.SendingNfService;
import ru.yandex.market.crm.campaign.services.sending.facts.PushSendingFactInfoDAO;
import ru.yandex.market.crm.campaign.services.sending.facts.PushSendingFactInfoRowMapper;
import ru.yandex.market.crm.campaign.services.sending.tasks.AbstractUploadPushSendingTask;
import ru.yandex.market.crm.campaign.services.sending.tasks.UploadPushSendingData;
import ru.yandex.market.crm.campaign.services.sending.tasks.UploadPushSendingSettings;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.services.appmetrica.AppMetricaService;
import ru.yandex.market.crm.core.services.logging.SentPushesLogWriter;
import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author vtarasoff
 * @since 13.02.2021
 */
@RunWith(MockitoJUnitRunner.class)
public class UploadPlainPushSendingStepTest {
    private static class PushSendingFactInfoTestDAO extends PushSendingFactInfoDAO {
        private PushSendingFactInfo fact;

        public PushSendingFactInfoTestDAO() {
            super(null, null, new PushSendingFactInfoRowMapper());
        }

        @Override
        public void save(PushSendingFactInfo fact) {
            this.fact = fact;
        }

        @Override
        public void updateState(PushSendingFactInfo fact) {
            this.fact = fact;
        }

        @Override
        public PushSendingFactInfo getById(String id) {
            return fact;
        }

        @Override
        public Optional<PushSendingFactInfo> tryGetById(String id) {
            return Optional.ofNullable(fact);
        }
    }

    private static final String AUTHOR = "author";
    private static final String ERROR_MESSAGE = "error";
    private static final int BATCH_SIZE = 4000;

    private UploadPlainPushSendingStep step;

    private Control<UploadPushSendingData> control;

    private UploadPushSendingData data;

    private PushSendingFactInfoTestDAO factDao;

    @Mock
    private YtClient ytClient;

    @Mock
    private PushMessagesUploader pushMessagesUploader;

    @Mock
    private EventsTimelineService eventsTimelineService;

    @Mock
    private SentPushesLogWriter sentPushesLogWriter;

    @Mock
    private SendingNfService sendingNfService;

    @Mock
    private PushSendingYtPaths pushSendingYtPaths;

    @Mock
    private PropertiesService propertiesService;

    @Mock
    private AppMetricaService appMetricaService;

    @Before
    public void setUp() {
        factDao = new PushSendingFactInfoTestDAO();

        step = new UploadPlainPushSendingStep(
                ytClient,
                pushMessagesUploader,
                eventsTimelineService,
                propertiesService,
                sentPushesLogWriter,
                factDao,
                appMetricaService,
                sendingNfService
        );

        data = null;
        control = data -> UploadPlainPushSendingStepTest.this.data = data;

        when(eventsTimelineService.addTimelineEvent(any())).thenReturn(randomString());

        when(propertiesService.getObject(AbstractUploadPushSendingTask.UPLOAD_PUSH_SENDING_SETTINGS_CODE,
                UploadPushSendingSettings.class)).thenReturn(Optional.of(prepareUploadPushSendingSettings()));

        doAnswer(inv -> new UploadingResult(randomInt(), BATCH_SIZE, 0, 0))
                .when(pushMessagesUploader).uploadInterval(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testBaseLifecycle() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.run(context, data, control);

        verify(eventsTimelineService, times(1)).addTimelineEvent(any());
        verify(sendingNfService, times(1)).onUploadStarted(any(), any());

        PushSendingFactInfo fact = factDao.fact;

        assertEquals(context.getSending().getId(), fact.getId());
        assertEquals(context.getSending().getId(), fact.getSendingId());
        assertSame(SendingFactType.FINAL, fact.getType());
        assertSame(SendingFactStatus.UPLOADING_IN_PROGRESS, fact.getStatus());
        assertNotNull(fact.getStartUploadTime());
        assertNull(fact.getUploadTime());
        assertEquals(AUTHOR, fact.getAuthor());
        assertEquals(BATCH_SIZE, fact.getUploadedCount());
        assertSame(SendingFactTransportState.IN_PROGRESS, fact.getSendingState());
        assertNotNull(fact.getStartSendingTime());
        assertNull(fact.getSendingTime());
        assertNull(fact.getErrorMessage());
        assertEquals(1, fact.getTransferIds().size());

        PushSendingFactInfo prevFact = copyFact(fact);

        step.run(context, data, control);

        assertSame(prevFact.getStatus(), fact.getStatus());
        assertEquals(prevFact.getStartUploadTime(), fact.getStartUploadTime());
        assertEquals(prevFact.getUploadTime(), fact.getUploadTime());
        assertEquals(prevFact.getUploadedCount() + BATCH_SIZE, fact.getUploadedCount());
        assertSame(prevFact.getSendingState(), fact.getSendingState());
        assertEquals(prevFact.getStartSendingTime(), fact.getStartSendingTime());
        assertEquals(prevFact.getSendingTime(), fact.getSendingTime());
        assertEquals(prevFact.getErrorMessage(), fact.getErrorMessage());
        assertEquals(prevFact.getTransferIds().size() + 1, fact.getTransferIds().size());

        prevFact = copyFact(fact);

        step.onPause(context, data, control);

        assertSame(SendingFactStatus.PAUSED, fact.getStatus());
        assertEquals(prevFact.getStartUploadTime(), fact.getStartUploadTime());
        assertEquals(prevFact.getUploadTime(), fact.getUploadTime());
        assertEquals(prevFact.getUploadedCount(), fact.getUploadedCount());
        assertSame(prevFact.getSendingState(), fact.getSendingState());
        assertEquals(prevFact.getStartSendingTime(), fact.getStartSendingTime());
        assertEquals(prevFact.getSendingTime(), fact.getSendingTime());
        assertEquals(prevFact.getErrorMessage(), fact.getErrorMessage());
        assertEquals(prevFact.getTransferIds().size(), fact.getTransferIds().size());

        verify(sendingNfService, times(1)).onUploadSuspended(any());

        prevFact = copyFact(fact);

        step.run(context, data, control);

        assertSame(SendingFactStatus.UPLOADING_IN_PROGRESS, fact.getStatus());
        assertEquals(prevFact.getStartUploadTime(), fact.getStartUploadTime());
        assertEquals(prevFact.getUploadTime(), fact.getUploadTime());
        assertEquals(prevFact.getUploadedCount() + BATCH_SIZE, fact.getUploadedCount());
        assertSame(prevFact.getSendingState(), fact.getSendingState());
        assertEquals(prevFact.getStartSendingTime(), fact.getStartSendingTime());
        assertEquals(prevFact.getSendingTime(), fact.getSendingTime());
        assertEquals(prevFact.getErrorMessage(), fact.getErrorMessage());
        assertEquals(prevFact.getTransferIds().size() + 1, fact.getTransferIds().size());

        prevFact = copyFact(fact);

        step.onSuccess(context, data, control);

        assertSame(SendingFactStatus.SENDING_IN_PROGRESS, fact.getStatus());
        assertEquals(prevFact.getStartUploadTime(), fact.getStartUploadTime());
        assertNotNull(fact.getUploadTime());
        assertEquals(prevFact.getUploadedCount(), fact.getUploadedCount());
        assertSame(prevFact.getSendingState(), fact.getSendingState());
        assertEquals(prevFact.getStartSendingTime(), fact.getStartSendingTime());
        assertEquals(prevFact.getSendingTime(), fact.getSendingTime());
        assertEquals(prevFact.getErrorMessage(), fact.getErrorMessage());
        assertEquals(prevFact.getTransferIds().size(), fact.getTransferIds().size());

        verify(eventsTimelineService, times(1)).addTimelineEvent(any());
        verify(eventsTimelineService, never()).safeHandleCompletion(any());
        verify(eventsTimelineService, never()).safeHandleError(any(), any());

        verify(sendingNfService, times(1)).onUploadStarted(any(), any());
        verify(sendingNfService, times(1)).onUploadSuspended(any());
        verify(sendingNfService, times(1)).onUploadCompleted(any(), any());
        verify(sendingNfService, never()).onUploadFailed(any(), any());
        verify(sendingNfService, never()).onUploadCancelled(any(), any());
    }

    @Test
    public void testCancelBeforeRun() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.onCancel(context, data, control);

        PushSendingFactInfo fact = factDao.fact;

        assertSame(SendingFactStatus.CANCELLED, fact.getStatus());
        assertNotNull(fact.getStartUploadTime());
        assertNotNull(fact.getUploadTime());
        assertEquals(0, fact.getUploadedCount());
        assertNull(fact.getSendingState());
        assertNull(fact.getStartSendingTime());
        assertNull(fact.getSendingTime());
        assertNull(fact.getErrorMessage());
        assertTrue(fact.getTransferIds().isEmpty());

        verify(eventsTimelineService, never()).addTimelineEvent(any());
        verify(eventsTimelineService, never()).safeHandleCompletion(any());
        verify(eventsTimelineService, never()).safeHandleError(any(), any());

        verify(sendingNfService, never()).onUploadStarted(any(), any());
        verify(sendingNfService, never()).onUploadSuspended(any());
        verify(sendingNfService, never()).onUploadCompleted(any(), any());
        verify(sendingNfService, never()).onUploadFailed(any(), any());
        verify(sendingNfService, times(1)).onUploadCancelled(any(), any());
    }

    @Test
    public void testCancelAtRun() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.run(context, data, control);

        PushSendingFactInfo prevFact = copyFact(factDao.fact);

        step.onCancel(context, data, control);

        assertCancel(prevFact, factDao.fact);
    }

    @Test
    public void testCancelAtPause() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.run(context, data, control);
        step.onPause(context, data, control);

        PushSendingFactInfo prevFact = copyFact(factDao.fact);

        step.onCancel(context, data, control);

        assertCancel(prevFact, factDao.fact);
    }

    @Test
    public void testCancelAtFail() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.run(context, data, control);
        step.onFail(ERROR_MESSAGE, context, data, control);

        PushSendingFactInfo prevFact = copyFact(factDao.fact);

        step.onCancel(context, data, control);

        assertCancel(prevFact, factDao.fact);
    }

    private void assertCancel(PushSendingFactInfo prevFact, PushSendingFactInfo fact) {
        assertSame(SendingFactStatus.CANCELLED, fact.getStatus());
        assertEquals(prevFact.getStartUploadTime(), fact.getStartUploadTime());
        assertNotNull(fact.getUploadTime());
        assertEquals(prevFact.getUploadedCount(), fact.getUploadedCount());
        assertSame(prevFact.getSendingState(), fact.getSendingState());
        assertEquals(prevFact.getStartSendingTime(), fact.getStartSendingTime());
        assertEquals(prevFact.getSendingTime(), fact.getSendingTime());
        assertEquals(prevFact.getErrorMessage(), fact.getErrorMessage());
        assertEquals(prevFact.getTransferIds().size(), fact.getTransferIds().size());
    }

    @Test
    public void testFailBeforeRun() {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.onFail(ERROR_MESSAGE, context, data, control);

        PushSendingFactInfo fact = factDao.fact;

        assertSame(SendingFactStatus.ERROR, fact.getStatus());
        assertNotNull(fact.getStartUploadTime());
        assertNull(fact.getUploadTime());
        assertEquals(0, fact.getUploadedCount());
        assertNull(fact.getSendingState());
        assertNull(fact.getStartSendingTime());
        assertNull(fact.getSendingTime());
        assertEquals(ERROR_MESSAGE, fact.getErrorMessage());
        assertTrue(fact.getTransferIds().isEmpty());

        verify(eventsTimelineService, never()).addTimelineEvent(any());
        verify(eventsTimelineService, never()).safeHandleCompletion(any());
        verify(eventsTimelineService, never()).safeHandleError(any(), any());

        verify(sendingNfService, never()).onUploadStarted(any(), any());
        verify(sendingNfService, never()).onUploadSuspended(any());
        verify(sendingNfService, never()).onUploadCompleted(any(), any());
        verify(sendingNfService, times(1)).onUploadFailed(any(), any());
        verify(sendingNfService, never()).onUploadCancelled(any(), any());
    }

    @Test
    public void testFailAtRun() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.run(context, data, control);

        PushSendingFactInfo prevFact = copyFact(factDao.fact);

        step.onFail(ERROR_MESSAGE, context, data, control);

        assertFail(prevFact, factDao.fact);
    }

    @Test
    public void testFailAtPause() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.run(context, data, control);
        step.onPause(context, data, control);

        PushSendingFactInfo prevFact = copyFact(factDao.fact);

        step.onFail(ERROR_MESSAGE, context, data, control);

        assertFail(prevFact, factDao.fact);
    }

    @Test
    public void testFailAtCancel() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.run(context, data, control);
        step.onCancel(context, data, control);

        PushSendingFactInfo prevFact = copyFact(factDao.fact);

        step.onFail(ERROR_MESSAGE, context, data, control);

        assertFail(prevFact, factDao.fact);
    }

    @Test
    public void testFailAtSuccess() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.run(context, data, control);
        step.onSuccess(context, data, control);

        PushSendingFactInfo prevFact = copyFact(factDao.fact);

        step.onFail(ERROR_MESSAGE, context, data, control);

        assertFail(prevFact, factDao.fact);
    }

    @Test
    public void testFailAtFail() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.run(context, data, control);
        step.onFail("before " + ERROR_MESSAGE, context, data, control);

        PushSendingFactInfo prevFact = copyFact(factDao.fact);

        step.onFail(ERROR_MESSAGE, context, data, control);

        assertFail(prevFact, factDao.fact);
    }

    private void assertFail(PushSendingFactInfo prevFact, PushSendingFactInfo fact) {
        assertSame(SendingFactStatus.ERROR, fact.getStatus());
        assertEquals(prevFact.getStartUploadTime(), fact.getStartUploadTime());
        assertEquals(prevFact.getUploadTime(), fact.getUploadTime());
        assertEquals(prevFact.getUploadedCount(), fact.getUploadedCount());
        assertSame(prevFact.getSendingState(), fact.getSendingState());
        assertEquals(prevFact.getStartSendingTime(), fact.getStartSendingTime());
        assertEquals(prevFact.getSendingTime(), fact.getSendingTime());
        assertEquals(ERROR_MESSAGE, fact.getErrorMessage());
        assertEquals(prevFact.getTransferIds().size(), fact.getTransferIds().size());
    }

    @Test
    public void testRunAtFail() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.run(context, data, control);
        step.onFail(ERROR_MESSAGE, context, data, control);

        PushSendingFactInfo prevFact = copyFact(factDao.fact);

        step.run(context, data, control);

        PushSendingFactInfo fact = factDao.fact;

        assertSame(SendingFactStatus.UPLOADING_IN_PROGRESS, fact.getStatus());
        assertEquals(prevFact.getStartUploadTime(), fact.getStartUploadTime());
        assertEquals(prevFact.getUploadTime(), fact.getUploadTime());
        assertEquals(prevFact.getUploadedCount() + BATCH_SIZE, fact.getUploadedCount());
        assertSame(prevFact.getSendingState(), fact.getSendingState());
        assertEquals(prevFact.getStartSendingTime(), fact.getStartSendingTime());
        assertEquals(prevFact.getSendingTime(), fact.getSendingTime());
        assertNull(fact.getErrorMessage());
        assertEquals(prevFact.getTransferIds().size() + 1, fact.getTransferIds().size());
    }

    @Test
    public void testShortLifecycle() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending(0);

        step.run(context, data, control);

        verify(eventsTimelineService, times(1)).addTimelineEvent(any());
        verify(sendingNfService, never()).onUploadStarted(any(), any());

        PushSendingFactInfo fact = factDao.fact;

        assertEquals(context.getSending().getId(), fact.getId());
        assertEquals(context.getSending().getId(), fact.getSendingId());
        assertSame(SendingFactType.FINAL, fact.getType());
        assertSame(SendingFactStatus.UPLOADING_IN_PROGRESS, fact.getStatus());
        assertNotNull(fact.getStartUploadTime());
        assertEquals(AUTHOR, fact.getAuthor());
        assertEquals(0, fact.getUploadedCount());
        assertNull(fact.getSendingState());
        assertNull(fact.getStartSendingTime());
        assertNull(fact.getSendingTime());
        assertNull(fact.getErrorMessage());
        assertTrue(fact.getTransferIds().isEmpty());
        assertThat(fact.getTransferIds(), empty());

        PushSendingFactInfo prevFact = copyFact(fact);

        step.onSuccess(context, data, control);

        fact = factDao.fact;

        assertSame(SendingFactStatus.FINISHED, fact.getStatus());
        assertEquals(prevFact.getStartUploadTime(), fact.getStartUploadTime());
        assertNotNull(fact.getUploadTime());
        assertEquals(prevFact.getUploadedCount(), fact.getUploadedCount());
        assertSame(SendingFactTransportState.FINISHED, fact.getSendingState());
        assertNotNull(fact.getStartSendingTime());
        assertNotNull(fact.getSendingTime());
        assertNull(fact.getErrorMessage());
        assertThat(fact.getTransferIds(), empty());

        verify(eventsTimelineService, times(1)).addTimelineEvent(any());
        verify(eventsTimelineService, times(1)).safeHandleCompletion(any());
        verify(eventsTimelineService, never()).safeHandleError(any(), any());

        verify(sendingNfService, never()).onUploadStarted(any(), any());
        verify(sendingNfService, never()).onUploadSuspended(any());
        verify(sendingNfService, never()).onUploadCompleted(any(), any());
        verify(sendingNfService, never()).onUploadFailed(any(), any());
        verify(sendingNfService, never()).onUploadCancelled(any(), any());
    }

    @Test
    public void testFailAtShortLifecycle() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending(0);

        step.run(context, data, control);

        PushSendingFactInfo prevFact = copyFact(factDao.fact);

        step.onFail(ERROR_MESSAGE, context, data, control);

        assertFail(prevFact, factDao.fact);

        verify(eventsTimelineService, times(1)).addTimelineEvent(any());
        verify(eventsTimelineService, never()).safeHandleCompletion(any());
        verify(eventsTimelineService, times(1)).safeHandleError(any(), any());

        verify(sendingNfService, never()).onUploadFailed(any(), any());
    }

    @Test
    public void testFailAfterShortLifecycle() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending(0);

        step.run(context, data, control);
        step.onSuccess(context, data, control);

        PushSendingFactInfo prevFact = copyFact(factDao.fact);

        step.onFail(ERROR_MESSAGE, context, data, control);

        assertFail(prevFact, factDao.fact);

        verify(eventsTimelineService, times(1)).addTimelineEvent(any());
        verify(eventsTimelineService, times(1)).safeHandleCompletion(any());
        verify(eventsTimelineService, times(1)).safeHandleError(any(), any());

        verify(sendingNfService, never()).onUploadFailed(any(), any());
    }

    @Test
    public void testResumeSendingState() throws Exception {
        UploadPlainPushSendingTaskContext context = prepareSending();

        step.run(context, data, control);
        step.onPause(context, data, control);
        externalFinishSendingState(factDao.fact);

        step.run(context, data, control);

        PushSendingFactInfo fact = factDao.fact;

        assertSame(SendingFactTransportState.IN_PROGRESS, fact.getSendingState());
        verify(eventsTimelineService, times(2)).addTimelineEvent(any());

        step.onPause(context, data, control);
        externalErrorSendingState(factDao.fact);
        externalErrorMessage(factDao.fact);

        step.run(context, data, control);

        assertSame(SendingFactTransportState.IN_PROGRESS, fact.getSendingState());
        assertNull(fact.getErrorMessage());
        verify(eventsTimelineService, times(3)).addTimelineEvent(any());

        step.onFail(ERROR_MESSAGE, context, data, control);
        externalFinishSendingState(factDao.fact);

        step.run(context, data, control);

        assertSame(SendingFactTransportState.IN_PROGRESS, fact.getSendingState());
        verify(eventsTimelineService, times(4)).addTimelineEvent(any());

        step.onFail(ERROR_MESSAGE, context, data, control);
        externalErrorSendingState(factDao.fact);

        step.run(context, data, control);

        assertSame(SendingFactTransportState.IN_PROGRESS, fact.getSendingState());
        assertNull(fact.getErrorMessage());
        verify(eventsTimelineService, times(5)).addTimelineEvent(any());
        verify(eventsTimelineService, never()).safeHandleCompletion(any());
        verify(eventsTimelineService, never()).safeHandleError(any(), any());
    }

    private void externalFinishSendingState(PushSendingFactInfo fact) {
        fact.setSendingState(SendingFactTransportState.FINISHED);
    }

    private void externalErrorSendingState(PushSendingFactInfo fact) {
        fact.setSendingState(SendingFactTransportState.ERROR);
    }

    private void externalErrorMessage(PushSendingFactInfo fact) {
        fact.setErrorMessage(ERROR_MESSAGE);
    }

    private UploadPlainPushSendingTaskContext prepareSending() {
        return prepareSending(BATCH_SIZE * 10L);
    }

    private UploadPlainPushSendingTaskContext prepareSending(long count) {
        when(ytClient.getRowCount(any())).thenReturn(count);

        PushSendingConf config = new PushSendingConf();

        PushSendingVariantConf variant = new PushSendingVariantConf();
        AndroidPushConf pushConf = new AndroidPushConf();
        variant.setPushConfigs(Map.of(pushConf.getPlatform(), pushConf));
        config.setVariants(List.of(variant));
        config.setTarget(new TargetAudience(LinkingMode.NONE, randomString()));

        PushPlainSending sending = new PushPlainSending();
        sending.setId(randomString());
        sending.setName(randomString());
        sending.setConfig(config);

        var application = new MobileApplication();
        application.setId(MobileApplication.MARKET_APP);
        application.setMetricaAppId(111);

        return new UploadPlainPushSendingTaskContext(
                sending,
                pushSendingYtPaths,
                AUTHOR,
                null,
                sending.getId(),
                false,
                application
        );
    }

    private PushSendingFactInfo copyFact(PushSendingFactInfo fact) {
        PushSendingFactInfo copy = new PushSendingFactInfo();
        copy.setStatus(fact.getStatus());
        copy.setStartSendingTime(fact.getStartSendingTime());
        copy.setUploadedCount(fact.getUploadedCount());
        copy.setStartUploadTime(fact.getStartUploadTime());
        copy.setUploadTime(fact.getUploadTime());
        copy.setSendingState(fact.getSendingState());
        copy.setSendingTime(fact.getSendingTime());
        copy.setErrorMessage(fact.getErrorMessage());
        copy.setTransferIds(List.copyOf(fact.getTransferIds()));

        return copy;
    }

    private UploadPushSendingSettings prepareUploadPushSendingSettings() {
        UploadPushSendingSettings settings = new UploadPushSendingSettings();
        settings.setDefaultWaitTime(90);
        settings.setDefaultBatchSize(BATCH_SIZE);
        return settings;
    }

    private String randomString() {
        return RandomStringUtils.random(10);
    }

    private int randomInt() {
        return RandomUtils.nextInt();
    }
}
