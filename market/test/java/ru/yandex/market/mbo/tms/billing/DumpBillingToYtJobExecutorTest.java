package ru.yandex.market.mbo.tms.billing;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.billing.BillingSessionManager;
import ru.yandex.market.mbo.billing.YtBillingDumpSessionManager;
import ru.yandex.market.mbo.yt.TestYt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DumpBillingToYtJobExecutorTest {
    private static final Logger log = LoggerFactory.getLogger(DumpBillingToYtJobExecutorTest.class);

    private static String mstatPathStr = "//home/mstat/billing/paid_operation_log";
    private static String dumpPathStr = "//home/mbo/billing/paid_operation_log_new";

    @Mock
    private JdbcTemplate scatJdbcTemplate;
    @Mock
    private JdbcTemplate yqlJdbcTemplate;
    @Mock
    private YtBillingDumpSessionManager ytBillingDumpSessionManager;
    @Mock
    private BillingSessionManager billingSessionManager;

    private DumpBillingToYtJobExecutor dumpBillingToYtJobExecutor;

    private TestYt yt = Mockito.spy(new TestYt());

    @Before
    public void setUp() {
        dumpBillingToYtJobExecutor = new DumpBillingToYtJobExecutor() {
            @Override
            protected void createRecentLink(Optional<GUID> transactionId, YPath path) {
                createLink(getRecentLink(), path);
            }

            @Override
            protected void copyTable(YPath sourceTablePath, YPath newTablePath) {
                //there is a bug in TestYt
                yt.cypress().create(newTablePath, CypressNodeType.TABLE);
            }
        };
        dumpBillingToYtJobExecutor.setYtDumpPath(YPath.simple(dumpPathStr));
        dumpBillingToYtJobExecutor.setYtMstatPath(YPath.simple(mstatPathStr));
        dumpBillingToYtJobExecutor.setYtBillingDumpSessionManager(ytBillingDumpSessionManager);
        dumpBillingToYtJobExecutor.setYt(yt);
        dumpBillingToYtJobExecutor.setBillingSessionManager(billingSessionManager);
        dumpBillingToYtJobExecutor.setYqlTemplate(yqlJdbcTemplate);
        dumpBillingToYtJobExecutor.setScatJdbcTemplate(scatJdbcTemplate);

        YPath tablePath = YPath.simple(mstatPathStr).child("20210202_1234");
        yt.cypress().create(tablePath, CypressNodeType.TABLE);
        //hack for links
        YPath linkPath = YPath.simple(mstatPathStr).child("latest");
        createLink(linkPath, tablePath);
    }

    private void createLink(YPath linkPath, YPath tablePath) {
        yt.cypress().remove(linkPath);
        yt.cypress().create(linkPath, CypressNodeType.MAP);
        yt.cypress().set(linkPath.attribute("key"), tablePath.name());
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void testInitialization() throws Exception {
        when(yqlJdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("2021-02-01");
        when(ytBillingDumpSessionManager.getDumpStatus()).thenReturn(null);
        Calendar start = Calendar.getInstance();
        start.set(2021, 2, 5);
        Calendar end = Calendar.getInstance();
        end.setTime(start.getTime());
        end.add(Calendar.DAY_OF_MONTH, 1);
        when(billingSessionManager.getBillingPeriod()).thenReturn(new Pair<>(start, end));
        Mockito.doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(2);
            ResultSet rs = Mockito.mock(ResultSet.class);
            when(rs.getDate(anyString())).thenReturn(new java.sql.Date(System.currentTimeMillis()));
            when(rs.getDouble(anyString())).thenReturn(1d);
            rch.processRow(rs);
            return null;
        }).when(scatJdbcTemplate).query(Mockito.anyString(),
                any(Object[].class),
                any(RowCallbackHandler.class));

        dumpBillingToYtJobExecutor.doRealJob(null);
        Mockito.verify(yqlJdbcTemplate).update(Mockito.anyString(), eq("2021-01-31"));
        LocalDate dt = LocalDateTime.ofInstant(start.toInstant(), start.getTimeZone().toZoneId())
                .toLocalDate();
        Mockito.verify(ytBillingDumpSessionManager).update(
                eq(new YtBillingDumpSessionManager.DumpStatus(dt, YtBillingDumpSessionManager.Status.SUCCESS))
        );
    }

    @Test
    public void testRotation() {
        YPath tablePath = YPath.simple(dumpPathStr).child("1");
        yt.cypress().create(tablePath, CypressNodeType.TABLE);
        createLink(YPath.simple(dumpPathStr).child(DumpBillingToYtJobExecutor.RECENT), tablePath);
        YPath tablePath2 = YPath.simple(dumpPathStr).child("0");
        yt.cypress().create(tablePath2, CypressNodeType.TABLE);
        YPath tablePath3 = YPath.simple(dumpPathStr).child("3");
        yt.cypress().create(tablePath3, CypressNodeType.TABLE);
        dumpBillingToYtJobExecutor.rotateOldTables();
        List<String> lst = yt.cypress().list(YPath.simple(dumpPathStr)).stream()
                .map(v -> v.getValue())
                .collect(Collectors.toList());
        Assertions.assertThat(lst).containsExactlyInAnyOrder(DumpBillingToYtJobExecutor.RECENT, "1");
    }
}
