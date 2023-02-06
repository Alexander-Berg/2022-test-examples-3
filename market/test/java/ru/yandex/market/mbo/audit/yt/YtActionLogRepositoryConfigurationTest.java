package ru.yandex.market.mbo.audit.yt;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbo.audit.conf.AuditProperties;
import ru.yandex.market.mbo.audit.yt.lb.AuditLogbrokerEvent;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.yt.utils.UnstableInit;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

/**
 * @author apluhin
 * @created 7/14/21
 */
@SuppressWarnings("checkstyle:magicNumber")
public class YtActionLogRepositoryConfigurationTest {

    private YtActionLogRepository actionLogRepository;
    private LogbrokerEventPublisher<AuditLogbrokerEvent> eventPublisher;
    private YtTableRpcApi rpcApi;

    @Before
    public void setUp() throws Exception {
        eventPublisher = Mockito.mock(LogbrokerEventPublisher.class);
        actionLogRepository = new YtActionLogRepository(
            Mockito.mock(UnstableInit.class),
            Mockito.mock(UnstableInit.class),
            Mockito.mock(YtTableModel.class),
            null,
            new CopyOnWriteArrayList<>(),
            new AuditProperties(10, 10), eventPublisher);
        rpcApi = Mockito.mock(YtTableRpcApi.class);
        ReflectionTestUtils.setField(actionLogRepository, "rpcApi", rpcApi);
    }

    @Test
    public void testConvertActionFromYt() {
        MboAudit.MboAction action = testAction();
        UnversionedRow unversionedRow = new UnversionedRow(
            Arrays.asList(
                //hash
                new UnversionedValue(0, ColumnValueType.INT64, false, 1L),
                //action_id
                new UnversionedValue(0, ColumnValueType.INT64, false, 1L),
                //timestamp
                new UnversionedValue(0, ColumnValueType.UINT64, false, 2L),
                //data
                new UnversionedValue(0, ColumnValueType.STRING, false, action.toByteArray())
            )
        );
        AuditRow auditRow = actionLogRepository.convertRow(unversionedRow);
        Assert.assertEquals(100L, auditRow.getKey().lookupKey().get(0));
        Assert.assertEquals(action, auditRow.getAction());
    }

    @Test
    public void testSendToLbToYtFailedSave() {
        MboAudit.MboAction mboAction = testAction();
        Mockito.doThrow(RuntimeException.class).when(rpcApi).doInTransaction(Mockito.any(), Mockito.any());
        actionLogRepository.saveAuditActions(Arrays.asList(mboAction));
        ArgumentCaptor<AuditLogbrokerEvent> captor =
            ArgumentCaptor.forClass(AuditLogbrokerEvent.class);
        Mockito.verify(eventPublisher, Mockito.times(1)).publishEvent(captor.capture());
        MboAudit.MboAction actions = captor.getValue().getPayload().getActions(0);
        Assert.assertEquals(mboAction, actions);
    }

    @Test
    public void saveByProperty() {
        actionLogRepository.saveAuditActions(Arrays.asList(testAction()));
        Mockito.verify(rpcApi, Mockito.times(1)).doInTransaction(
            Mockito.any(),
            Mockito.any()
        );
    }

    private MboAudit.MboAction testAction() {
        return MboAudit.MboAction.newBuilder()
            .setActionId(100)
            .build();
    }

}
