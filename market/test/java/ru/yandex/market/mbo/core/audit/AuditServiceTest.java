package ru.yandex.market.mbo.core.audit;

import java.util.stream.Stream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditFilter;
import ru.yandex.market.mbo.http.MboAudit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.mbo.core.audit.AuditService.FETCH_BATCH_SIZE;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class AuditServiceTest {
    private static final long RANDOM_SEED = 11042018;

    private AuditService auditService;
    private MboAuditServiceMock auditServiceMock;
    private EnhancedRandom random;

    @Before
    public void setUp() throws Exception {
        auditServiceMock = Mockito.spy(MboAuditServiceMock.class);

        auditService = new AuditService(
            auditServiceMock, auditServiceMock, "no matter what environment");

        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(RANDOM_SEED)
            .stringLengthRange(3, 10)
            .collectionSizeRange(1, 5)
            .build();
    }

    @Test
    public void loadActionsWillLoadWithBatchesExactFetchSize() {
        loadActionsWillLoadWithBatches(0);
    }

    @Test
    public void loadActionsWillLoadWithBatchesExtraData() {
        loadActionsWillLoadWithBatches(42);
    }

    private void loadActionsWillLoadWithBatches(int extra) {
        // create & place audit actions to service
        MboAudit.WriteActionsRequest.Builder writeRequest = MboAudit.WriteActionsRequest.newBuilder();
        Stream.generate(this::createRandomAction).limit(FETCH_BATCH_SIZE * 11 + extra)
            .forEach(writeRequest::addActions);
        auditServiceMock.writeActions(writeRequest.build());

        // infinite request
        Mockito.clearInvocations(auditServiceMock);
        auditService.loadAudit(0, Integer.MAX_VALUE, new AuditFilter());
        verify(auditServiceMock, times(12))
            .findActions(any());

        Mockito.clearInvocations(auditServiceMock);
        auditService.loadAudit(70, FETCH_BATCH_SIZE * 10, new AuditFilter());
        verify(auditServiceMock, times(10)).findActions(any());

        Mockito.clearInvocations(auditServiceMock);
        auditService.loadAudit(0, FETCH_BATCH_SIZE * 11, new AuditFilter());
        verify(auditServiceMock, times(11)).findActions(any());

        Mockito.clearInvocations(auditServiceMock);
        auditService.loadAudit(FETCH_BATCH_SIZE, FETCH_BATCH_SIZE * 5 - 100, new AuditFilter());
        verify(auditServiceMock, times(5)).findActions(any());

        Mockito.clearInvocations(auditServiceMock);
        auditService.loadAudit(FETCH_BATCH_SIZE / 2, 0, new AuditFilter());
        verify(auditServiceMock, times(0)).findActions(any());

        Mockito.clearInvocations(auditServiceMock);
        auditService.loadAudit(100, 10, new AuditFilter());
        verify(auditServiceMock, times(1)).findActions(any());

        Mockito.clearInvocations(auditServiceMock);
        auditService.loadAudit(0, FETCH_BATCH_SIZE, new AuditFilter());
        verify(auditServiceMock, times(1)).findActions(any());

        Mockito.clearInvocations(auditServiceMock);
        auditService.loadAudit(5, FETCH_BATCH_SIZE + 100, new AuditFilter());
        verify(auditServiceMock, times(2)).findActions(any());
    }

    @Test
    public void loadActionsWithCorrectPaging() {
        // create & place audit actions to service
        MboAudit.WriteActionsRequest.Builder writeRequest = MboAudit.WriteActionsRequest.newBuilder();
        Stream.generate(this::createRandomAction).limit(FETCH_BATCH_SIZE * 10).forEach(writeRequest::addActions);
        auditServiceMock.writeActions(writeRequest.build());

        // first check
        Mockito.clearInvocations(auditServiceMock);
        auditService.loadAudit(0, FETCH_BATCH_SIZE * 2 + 100, new AuditFilter());

        MboAudit.FindActionsRequest firstRequest = buildRequest(0, FETCH_BATCH_SIZE);
        MboAudit.FindActionsRequest secondRequest = buildRequest(FETCH_BATCH_SIZE, FETCH_BATCH_SIZE);
        MboAudit.FindActionsRequest thirdRequest = buildRequest(2 * FETCH_BATCH_SIZE, 100);

        verify(auditServiceMock).findActions(eq(firstRequest));
        verify(auditServiceMock).findActions(eq(secondRequest));
        verify(auditServiceMock).findActions(eq(thirdRequest));
        verifyNoMoreInteractions(auditServiceMock);

        // second check
        Mockito.clearInvocations(auditServiceMock);
        auditService.loadAudit(0, FETCH_BATCH_SIZE, new AuditFilter());

        MboAudit.FindActionsRequest singleRequest = buildRequest(0, FETCH_BATCH_SIZE);

        verify(auditServiceMock, times(1)).findActions(eq(singleRequest));
        verifyNoMoreInteractions(auditServiceMock);

        // third check
        Mockito.clearInvocations(auditServiceMock);
        auditService.loadAudit(0, FETCH_BATCH_SIZE, new AuditFilter());

        singleRequest = buildRequest(0, FETCH_BATCH_SIZE);

        verify(auditServiceMock, times(1)).findActions(eq(singleRequest));
        verifyNoMoreInteractions(auditServiceMock);

        // fource check with offset
        Mockito.clearInvocations(auditServiceMock);
        auditService.loadAudit(87, FETCH_BATCH_SIZE * 2 + 100, new AuditFilter());

        firstRequest = buildRequest(87, FETCH_BATCH_SIZE);
        secondRequest = buildRequest(87 + FETCH_BATCH_SIZE, FETCH_BATCH_SIZE);
        thirdRequest = buildRequest(87 + 2 * FETCH_BATCH_SIZE, 100);

        verify(auditServiceMock).findActions(eq(firstRequest));
        verify(auditServiceMock).findActions(eq(secondRequest));
        verify(auditServiceMock).findActions(eq(thirdRequest));
        verifyNoMoreInteractions(auditServiceMock);
    }

    private MboAudit.FindActionsRequest buildRequest(int offset, int length) {
        return MboAudit.FindActionsRequest.newBuilder()
            .setOffset(offset)
            .setLength(length)
            .setRequestType(MboAudit.RequestType.PLAIN)
            .setCriticalRead(true)
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadActionsWillFailWithNegativeOffset() {
        auditService.loadAudit(-10, 100, new AuditFilter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadActionsWillFailWithNegativeLength() {
        auditService.loadAudit(1, -10, new AuditFilter());
    }

    private MboAudit.MboAction createRandomAction() {
        AuditAction action = random.nextObject(AuditAction.class);
        return AuditActionConverter.convert(action);
    }
}
