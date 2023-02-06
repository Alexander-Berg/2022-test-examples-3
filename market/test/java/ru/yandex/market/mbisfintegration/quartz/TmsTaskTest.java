package ru.yandex.market.mbisfintegration.quartz;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.javaframework.quartz.test.AbstractQuartzTest;
import ru.yandex.market.mbisfintegration.CommonUnitTestConfig;
import ru.yandex.market.mbisfintegration.quartz.task.TmsTasks;
import ru.yandex.market.mbisfintegration.salesforce.SalesForceTestConfiguration;
import ru.yandex.market.mbisfintegration.salesforce.SoapHolder;
import ru.yandex.market.tms.quartz2.model.Executor;

@ContextConfiguration(classes = {CommonUnitTestConfig.class, TmsTasks.class, SalesForceTestConfiguration.class})
public class TmsTaskTest extends AbstractQuartzTest {

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("tmsLogsCleanupExecutor")
    public Executor tmsLogsCleanupExecutor;

    @Autowired
    @Qualifier("soapRefreshSessionExecutor")
    public Executor soapRefreshSessionExecutor;

    @Autowired
    public SoapHolder soapHolder;

    @Test
    public void testTmsLogsCleanup() {
        tmsLogsCleanupExecutor.doJob(mockContext());
        jdbcTemplate.query("select * from qrtz_job_details", (rs) -> {
            System.out.println(rs.getString("JOB_NAME"));
        });
    }

    @Test
    public void testSoapRefreshSession() {
        Mockito.clearInvocations(soapHolder); //soapHolder.refreshSession() is PostConstruct method
        soapRefreshSessionExecutor.doJob(mockContext());
        Mockito.verify(soapHolder, Mockito.times(1)).refreshSession();
    }

}
