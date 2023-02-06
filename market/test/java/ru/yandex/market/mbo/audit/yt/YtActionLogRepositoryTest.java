package ru.yandex.market.mbo.audit.yt;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbo.audit.conf.AuditProperties;
import ru.yandex.market.mbo.audit.yt.lb.AuditLogbrokerEvent;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.yt.util.table.YtTableRpcApi;

/**
 * @author apluhin
 * @created 7/27/21
 */
@SuppressWarnings("checkstyle:magicNumber")
public class YtActionLogRepositoryTest {

    private static final List<MboAudit.MboAction> TEST_ACTIONS =
        Collections.singletonList(MboAudit.MboAction.newBuilder()
            .setActionId(100L)
            .build());

    private YtActionLogRepository actionLogRepository;
    private LogbrokerEventPublisher<AuditLogbrokerEvent> logbrokerEventPublisher;

    private YtTableRpcApi ytTableRpcApi;


    @Before
    public void setUp() throws Exception {
        logbrokerEventPublisher = Mockito.mock(LogbrokerEventPublisher.class);
        actionLogRepository = new YtActionLogRepository(
            null,
            null,
            null,
            null,
            null,
            new AuditProperties(10, 10), logbrokerEventPublisher
        );
        ytTableRpcApi = Mockito.mock(YtTableRpcApi.class);
        ReflectionTestUtils.setField(actionLogRepository, "rpcApi", ytTableRpcApi);
    }

    @Test
    public void testSaveEventToYtFromHttpRequest() {
        actionLogRepository.saveAuditActions(TEST_ACTIONS);
        Mockito.verify(ytTableRpcApi, Mockito.times(1))
            .doInTransaction(Mockito.any(), Mockito.any());
        Mockito.verify(logbrokerEventPublisher, Mockito.times(0)).publishEvent(Mockito.any());
    }

    @Test
    public void testFailedSaveEventToYtStoreToLb() {
        Mockito.doThrow(RuntimeException.class).when(ytTableRpcApi).doInTransaction(Mockito.any(), Mockito.any());
        actionLogRepository.saveAuditActions(TEST_ACTIONS);

        Mockito.verify(logbrokerEventPublisher, Mockito.times(1)).publishEvent(Mockito.any());
    }

    @Test
    public void testSaveFromLb() {
        actionLogRepository.saveAuditActionsFromLb(TEST_ACTIONS);

        Mockito.verify(ytTableRpcApi, Mockito.times(1))
            .doInTransaction(Mockito.any(), Mockito.any());
        Mockito.verify(logbrokerEventPublisher, Mockito.times(0)).
            publishEvent(Mockito.any());
    }

}
