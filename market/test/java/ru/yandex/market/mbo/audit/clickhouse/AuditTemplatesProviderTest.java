package ru.yandex.market.mbo.audit.clickhouse;

import java.sql.Timestamp;
import java.util.Arrays;

import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbo.core.activitylock.JdbcTemplateCallback;
import ru.yandex.market.mbo.core.activitylock.LockHelper;
import ru.yandex.market.mbo.core.activitylock.LockedActivity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:magicnumber")
public class AuditTemplatesProviderTest {

    private ZookeeperStateChecker zookeeperStateChecker;
    private AuditTemplatesProvider auditTemplatesProvider;

    private NamedJdbcTemplateMock masterMock;
    private NamedJdbcTemplateMock fallback1;
    private NamedJdbcTemplateMock fallback2;

    private String thisHost;
    private String otherHost;

    private final LockHelper lockHelper = Mockito.mock(LockHelper.class);
    private final ArgumentCaptor<JdbcTemplateCallback> lockHelperCallback
        = ArgumentCaptor.forClass(JdbcTemplateCallback.class);

    @Before
    public void setUp() throws Exception {
        zookeeperStateChecker = new ZookeeperStateCheckerMock("hosts", "path");
        masterMock = Mockito.spy(new NamedJdbcTemplateMock("master", Mockito.mock(DataSource.class)));
        fallback1 = Mockito.spy(new NamedJdbcTemplateMock("fallback1", Mockito.mock(DataSource.class)));
        fallback2 = Mockito.spy(new NamedJdbcTemplateMock("fallback2", Mockito.mock(DataSource.class)));

        thisHost = "thisHost";
        otherHost = "otherHost";
        auditTemplatesProvider = new AuditTemplatesProvider(masterMock, Arrays.asList(fallback1, fallback2),
            zookeeperStateChecker, 2, thisHost, true,
            "action", 1, 60, "audit", lockHelper);
        // let's say by default everything is stable
        auditTemplatesProvider.afterPropertiesSet();
        zookeeperStateChecker.setState(thisHost, ClickhouseConnectionState.MASTER);
        zookeeperStateChecker.setState(otherHost, ClickhouseConnectionState.MASTER);
        when(lockHelper.getLastModificationTimestamp(LockedActivity.AUDIT_SYNC_LOCK)).thenReturn(new Timestamp(15));
    }

    @Test
    public void shouldFailAllOperationsIfTemplateNotInitialized() {
        AuditTemplatesProvider audit = new AuditTemplatesProvider(masterMock, Arrays.asList(fallback1, fallback2),
            zookeeperStateChecker, 2, thisHost, false,
            "action", 1, 1, "audit", lockHelper);
        Assertions.assertThatThrownBy(() -> audit.getTemplateForRead(true))
            .hasMessage("Template not initialized yet");
        Assertions.assertThatThrownBy(() -> audit.getTemplateForRead(false))
            .hasMessage("Template not initialized yet");
        Assertions.assertThatThrownBy(() -> audit.getTemplateForWrite())
            .hasMessage("Template not initialized yet");

    }

    @Test
    public void shouldAllowAllActionsIfAllHostsInMasterState() {
        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(true)).isEqualTo(masterMock);
        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(false)).isEqualTo(fallback1);
        Assertions.assertThat(auditTemplatesProvider.getTemplateForWrite()).isEqualTo(masterMock);
    }

    @Test
    public void shouldNotAllowCriticalReadsIfThisHostSwitchedToFallback() {
        masterMock.setTemplateEnabled(false);
        auditTemplatesProvider.updateCurrentTemplate();
        applyLambda();

        Assertions.assertThatThrownBy(() -> auditTemplatesProvider.getTemplateForRead(true))
            .hasMessage("Critical reads not allowed in fallback state");
        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(false)).isEqualTo(fallback2);
        Assertions.assertThat(auditTemplatesProvider.getTemplateForWrite()).isEqualTo(fallback1);
    }

    private void applyLambda() {
        verify(lockHelper).distributeSynchronize(any(LockedActivity.class), lockHelperCallback.capture());
        final JdbcTemplateCallback value = lockHelperCallback.getValue();
        value.doWithLock(new JdbcTemplate());
    }

    @Test
    public void shouldNotAllowCriticalReadsIfAnyOtherHostSwitchedToFallback() {
        zookeeperStateChecker.setState(otherHost, ClickhouseConnectionState.FALLBACK);

        Assertions.assertThatThrownBy(() -> auditTemplatesProvider.getTemplateForRead(true))
            .hasMessage("Not all hosts are in MASTER state");
        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(false)).isEqualTo(fallback1);
        Assertions.assertThat(auditTemplatesProvider.getTemplateForWrite()).isEqualTo(masterMock);
    }

    @Test
    public void shouldNotSwitchFromMasterWithoutAReason() {
        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(true)).isEqualTo(masterMock);
        Assertions.assertThat(zookeeperStateChecker.getHostStates().get(thisHost))
            .isEqualTo(ClickhouseConnectionState.MASTER);

        auditTemplatesProvider.updateCurrentTemplate();

        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(true)).isEqualTo(masterMock);
        Assertions.assertThat(zookeeperStateChecker.getHostStates().get(thisHost))
            .isEqualTo(ClickhouseConnectionState.MASTER);
    }

    @Test
    public void shouldSwitchBetweenFallbacksIfTwoDatacentersAreDown() {
        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(true)).isEqualTo(masterMock);
        Assertions.assertThat(zookeeperStateChecker.getHostStates().get(thisHost))
            .isEqualTo(ClickhouseConnectionState.MASTER);

        masterMock.setTemplateEnabled(false);
        auditTemplatesProvider.updateCurrentTemplate();
        applyLambda();

        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(false)).isEqualTo(fallback2);
        Assertions.assertThat(zookeeperStateChecker.getHostStates().get(thisHost))
            .isEqualTo(ClickhouseConnectionState.FALLBACK);

        fallback1.setTemplateEnabled(false);
        auditTemplatesProvider.updateCurrentTemplate();

        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(false)).isEqualTo(fallback2);
        Assertions.assertThat(zookeeperStateChecker.getHostStates().get(thisHost))
            .isEqualTo(ClickhouseConnectionState.FALLBACK);
    }


    @Test
    public void shouldSwitchBackToMasterIfMasterIsAvailableOnStartup() throws Exception {
        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(true)).isEqualTo(masterMock);
        Assertions.assertThat(zookeeperStateChecker.getHostStates().get(thisHost))
            .isEqualTo(ClickhouseConnectionState.MASTER);

        masterMock.setTemplateEnabled(false);
        auditTemplatesProvider.updateCurrentTemplate();
        applyLambda();

        Assertions.assertThatThrownBy(() -> auditTemplatesProvider.getTemplateForRead(true));
        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(false)).isEqualTo(fallback2);
        Assertions.assertThat(zookeeperStateChecker.getHostStates().get(thisHost))
            .isEqualTo(ClickhouseConnectionState.FALLBACK);

        // restart into MASTER
        masterMock.setTemplateEnabled(true);
        auditTemplatesProvider.afterPropertiesSet();

        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(true)).isEqualTo(masterMock);
        Assertions.assertThat(zookeeperStateChecker.getHostStates().get(thisHost))
            .isEqualTo(ClickhouseConnectionState.MASTER);
        JdbcOperations operationsMock = masterMock.getJdbcOperations();
        verify(operationsMock, Mockito.times(1))
            .execute(Mockito.eq("system sync replica audit.action"));
    }

    @Test
    public void loadBalancing() {
        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(false)).isEqualTo(fallback1);
        Assertions.assertThat(auditTemplatesProvider.getTemplateForRead(false)).isEqualTo(fallback2);
    }
}
