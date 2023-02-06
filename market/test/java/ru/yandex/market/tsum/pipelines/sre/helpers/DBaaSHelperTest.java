package ru.yandex.market.tsum.pipelines.sre.helpers;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.clients.dbaas.clickhouse.ClickHouseClusterInfo;
import ru.yandex.market.tsum.clients.dbaas.pg.PgClusterInfo;
import ru.yandex.market.tsum.clients.iam.YcFolderId;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipelines.sre.resources.CreatePostgresPipelineConfig;

import static org.junit.Assert.assertEquals;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 16/10/2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class DBaaSHelperTest {

    private static final CreatePostgresPipelineConfig CONFIG = new CreatePostgresPipelineConfig.Builder()
        .setEnvironment("production")
        .setFolderId(YcFolderId.MARKET_DB_OPS.toString())
        .setDatabaseName("test-database")
        .setStartrekTicketKey("TEST-01")
        .setInstanceType("s2.nano")
        .setVolumeSizeGb(10)
        .setConnLimit(100)
        .setPasswordPropertyName("app-name.database-type.password")
        .build();

    private static final PgClusterInfo.Builder PG_CLUSTER_INFO_BUILDER = PgClusterInfo.newBuilder()
        .withDatabaseName("test-database")
        .withConnLimit(10)
        .withClusterName("test-cluster")
        .withHostsGeo(Arrays.asList("test-host01h.market.yandex.net", "test-host01v.market.yandex.net"))
        .withUserName("test-user")
        .withPassword("test-password")
        .withMonitoringUrl("https://test-dashboard.market.yandex.net");
    private static final ClickHouseClusterInfo.Builder CH_CLUSTER_INFO_BUILDER = ClickHouseClusterInfo.newBuilder()
        .withDatabaseName("test-database")
        .withClusterName("test-cluster")
        .withHostsGeo(Arrays.asList("test-host01h.market.yandex.net", "test-host01v.market.yandex.net"))
        .withUserName("test-user")
        .withPassword("test-password")
        .withMonitoringUrl("https://test-dashboard.market.yandex.net");
    private static final JobContext CONTEXT = Mockito.mock(JobContext.class, Mockito.RETURNS_DEEP_STUBS);

    @Test
    public void getIssueDescription() {
        String description = DBaaSHelper.getIssueDescription(
            "testLaunchId", CONFIG, "https://somehost/somepath/applicationId"
        );
        String expected = "Please review a service specification ((https://somehost/somepath/applicationId " +
            "test-database)) that has been created via ((testLaunchId TSUM Pipeline)).\n" +
            "Check the db specification ((https://somehost/somepath/applicationId here)).\n" +
            "At least one of the requested resources exceeds automatically approved limits.\n" +
            "The automatically approved limits:\n" +
            "- Flavour <= s2.nano\n" +
            "- VolumeSize <= 20\n";
        assertEquals(expected, description);
    }

    @Test
    public void getIssueSummary() {
        String summary = DBaaSHelper.getIssueSummary(CONFIG);
        String expected = "Review the specification of a new database test-database";
        assertEquals(expected, summary);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkConfigWithoutAbc() {
        final CreatePostgresPipelineConfig pipelineConfig = new CreatePostgresPipelineConfig.Builder()
            .setDatabaseName("testDb")
            .setEnvironment("production")
            .setStartrekTicketKey("TEST-01")
            .setInstanceType("s2.nano")
            .build();
        DBaaSHelper.checkConfig(pipelineConfig);
    }
//    тест пока сломан, но он нужен. Починю после правки остальных потребителей DBaasClient
//    @Test
//    public void getStartrekCommentNotification() {
//        when(CONTEXT.getPipeLaunch().getTriggeredBy()).thenReturn("test-user");
//
//        StartrekCommentNotification commentNotification = DBaaSHelper.getStartrekCommentNotification(
//            CONTEXT,
//            PG_CLUSTER_INFO_BUILDER.build()
//        );
//        String expected = "Создана база test-database\n" +
//            "Графики https://test-dashboard.market.yandex.net\n" +
//            "Пароль от базы и примеры строк подключения отправлены на ваш ((test-user@yandex-team.ru email)).\n";
//        assertEquals(expected, commentNotification.getStartrekComment());
//    }

//    @Test
//    public void getEmailNotificationPgCluster() {
//        when(CONTEXT.getPipeLaunch().getTriggeredBy()).thenReturn("test-user");
//        when(CONTEXT.getPipeLaunchUrl()).thenReturn("https://some-tsum/pipe-launch-url");
//        when(CONTEXT.getJobLaunchDetailsUrl()).thenReturn("https://some-tsum/job-launch-url");
//        when(CONTEXT.getPipeLaunch().getPipeId()).thenReturn("test-id");
//
//        PgClusterInfo pgClusterInfo = PG_CLUSTER_INFO_BUILDER.build();
//        pgClusterInfo.getInfrastructureOptions().setHosts(
//            Arrays.asList(
//                new DbaasInfrastructureOptions.DbaasHost("test-host01h.market.yandex.net", null),
//                new DbaasInfrastructureOptions.DbaasHost("test-host01e.market.yandex.net", null),
//                new DbaasInfrastructureOptions.DbaasHost("test-host01v.market.yandex.net", null)
//            )
//        );
//
//        EmailNotification commentNotification = DBaaSHelper.getEmailNotification(
//            CONTEXT,
//            pgClusterInfo,
//            "test-password"
//        );
//        String expectedSubject = "Cоздана база test-cluster";
//        String expectedMessage = "Пайплайн test-id создал базу test-database в production (возможно операция " +
//            "создания ещё в процессе выполнения, пожалуйста, дождитесь успешного завершения запуска пайплайна):\n" +
//            "\n" +
//            "Примеры строк подключения:\n" +
//            "%%\n" +
//            "PSYCOPG_RW_DSN = 'postgresql://test-user:test-password@test-host01h.market.yandex.net," +
//            "test-host01e.market.yandex.net,test-host01v.market.yandex.net:6432/test-database?sslmode=require&" +
//            "target_session_attrs=read-write&connect_timeout=1'\n" +
//            "PSYCOPG_RO_DSN = 'postgresql://test-user:test-password@test-host01h.market.yandex.net," +
//            "test-host01e.market.yandex.net,test-host01v.market.yandex.net:6432/test-database?sslmode=require&" +
//            "target_session_attrs=any&connect_timeout=1'\n" +
//            "\n" +
//            "JDBC_RW_DSN = 'jdbc:postgresql://test-user:test-password@test-host01h.market.yandex.net:6432," +
//            "test-host01e.market.yandex.net:6432,test-host01v.market.yandex.net:6432/test-database?sslmode=require&" +
//            "sslmode=require&targetServerType=master&prepareThreshold=0&connectTimeout=1'\n" +
//            "JDBC_RO_DSN = 'jdbc:postgresql://test-user:test-password@test-host01h.market.yandex.net:6432," +
//            "test-host01e.market.yandex.net:6432,test-host01v.market.yandex.net:6432/test-database?sslmode=require&" +
//            "sslmode=require&targetServerType=preferSlave&loadBalanceHosts=true&prepareThreshold=0&" +
//            "connectTimeout=1'\n" +
//            "\n" +
//            "PSQL_RW_DSN = 'postgresql://test-user:test-password@test-host01h.market.yandex.net:6432," +
//            "test-host01e.market.yandex.net:6432,test-host01v.market.yandex.net:6432/test-database?sslmode=require&" +
//            "target_session_attrs=read-write&connect_timeout=1'\n" +
//            "PSQL_RO_DSN = 'postgresql://test-user:test-password@test-host01h.market.yandex.net:6432," +
//            "test-host01e.market.yandex.net:6432,test-host01v.market.yandex.net:6432/test-database?sslmode=require&" +
//            "target_session_attrs=any&connect_timeout=1'\n" +
//            "%%\n" +
//            "Графики: https://test-dashboard.market.yandex.net\n" +
//            "\n" +
//            "Перейти к пайплайну: https://some-tsum/pipe-launch-url\n" +
//            "Перейти к задаче: https://some-tsum/job-launch-url\n";
//        assertEquals(expectedSubject, commentNotification.getEmailSubject());
//        assertEquals(expectedMessage, commentNotification.getEmailMessage());
//    }
//
//    @Test
//    public void getEmailNotificationClickHouseCluster() {
//        when(CONTEXT.getPipeLaunch().getTriggeredBy()).thenReturn("test-user");
//        when(CONTEXT.getPipeLaunchUrl()).thenReturn("https://some-tsum/pipe-launch-url");
//        when(CONTEXT.getJobLaunchDetailsUrl()).thenReturn("https://some-tsum/job-launch-url");
//        when(CONTEXT.getPipeLaunch().getPipeId()).thenReturn("test-id");
//
//        ClickHouseClusterInfo clickHouseClusterInfo = CH_CLUSTER_INFO_BUILDER.build();
//        DbaasInfrastructureOptions.DbaasHost.DbaasHostOptions options = new DbaasInfrastructureOptions.DbaasHost
//            .DbaasHostOptions("sas", DbaasHostType.CLICKHOUSE);
//        clickHouseClusterInfo.getInfrastructureOptions().setHosts(
//            Arrays.asList(
//                new DbaasInfrastructureOptions.DbaasHost("test-host01h.market.yandex.net", options),
//                new DbaasInfrastructureOptions.DbaasHost("test-host01e.market.yandex.net", options),
//                new DbaasInfrastructureOptions.DbaasHost("test-host01v.market.yandex.net", options)
//            )
//        );
//
//        EmailNotification commentNotification = DBaaSHelper.getEmailNotification(
//            CONTEXT,
//            clickHouseClusterInfo,
//            "test-password"
//        );
//        String expectedSubject = "Cоздана база test-cluster";
//        String expectedMessage = "Пайплайн test-id создал базу test-database в production " +
//            "(возможно операция создания ещё в процессе выполнения, пожалуйста, дождитесь успешного " +
//            "завершения запуска пайплайна):\n" +
//            "\n" +
//            "Примеры строк подключения:\n" +
//            "%%\n" +
//            "clickhouse-client -s --host <test-host01h.market.yandex.net or test-host01e.market.yandex.net or " +
//            "test-host01v.market.yandex.net> --user test-user --password test-password " +
//            "--port 9440 -d test-database\n" +
//            "\n" +
//            "JDBC_DSN = 'jdbc:clickhouse://test-user:test-password@test-host01h.market.yandex.net:8443," +
//            "test-host01e.market.yandex.net:8443,test-host01v.market.yandex.net:8443/test-database\n" +
//            "%%\n" +
//            "Графики: https://test-dashboard.market.yandex.net\n" +
//            "\n" +
//            "Перейти к пайплайну: https://some-tsum/pipe-launch-url\n" +
//            "Перейти к задаче: https://some-tsum/job-launch-url\n";
//        assertEquals(expectedSubject, commentNotification.getEmailSubject());
//        assertEquals(expectedMessage, commentNotification.getEmailMessage());
//    }
}
