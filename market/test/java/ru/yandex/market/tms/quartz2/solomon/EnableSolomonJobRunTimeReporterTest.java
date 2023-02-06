package ru.yandex.market.tms.quartz2.solomon;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.inside.solomon.pusher.SolomonPusher;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;
import ru.yandex.misc.monica.solomon.sensors.PushSensorsData;
import ru.yandex.misc.monica.solomon.sensors.Sensor;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;

@ActiveProfiles("testing")
@RunWith(SpringJUnit4ClassRunner.class)
public class EnableSolomonJobRunTimeReporterTest {
    private static final TestExecutionStateHolder HOLDER = new TestExecutionStateHolder();

    private static final Integer JOB_DURATION_MILLIS = 100;

    @Autowired
    public SolomonPusher solomonPusher;
    @Autowired
    public SolomonJobRunTimeReporterProperties properties;

    @Ignore("Should be fixed in MBO-29675")
    @Test
    public void testSolomonJobRunTimeReporterForMethod() throws InterruptedException {
        HOLDER.getLatch().await(2, TimeUnit.SECONDS);

        Mockito.verify(solomonPusher, times(1))
            .push(argThat(new PushSensorsDataMather(new PushSensorsData(
                properties.getProject(),
                properties.getCluster(),
                properties.getService(),
                ImmutableMap.of(),
                ImmutableList.of(
                        new Sensor(ImmutableMap.of("sensor", properties.getSensor(),
                            SolomonJobRunTimeReporter.JOB_NAME_LABEL_NAME, "solomonReporterTestExecutorMethod"),
                            JOB_DURATION_MILLIS))
            ))));
    }

    @Configuration
    @Import({
        BaseTmsTestConfig.class,
        SolomonBaseConfig.class
    })
    @PropertySource("classpath:/ru/yandex/market/tms/quartz2/solomon/EnableSolomonReporter.properties")
    static class SolomonTestConfig {

        @Bean
        public SolomonPusher solomonPusher() {
            return Mockito.mock(SolomonPusher.class);
        }

        @Bean
        public SolomonJobRunTimeReporterProperties solomonJobRunTimeReporterProperties() {
            return new SolomonJobRunTimeReporterProperties();
        }

        @Bean
        public SolomonJobRunTimeReporter solomonReporter(
                SolomonJobRunTimeReporterProperties properties,
                SolomonPusher solomonPusher) {
            return new SolomonJobRunTimeReporter(properties, solomonPusher);
        }

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2);
            Stream.of(
                    "classpath:/sql/tms-core-quartz2_log_table.sql",
                    "classpath:/sql/tms-core-quartz2_schema.sql"
            ).forEach(builder::addScript);
            builder.setName("testDataBase" + System.currentTimeMillis());
            return new DelegatingDataSource(builder.build());
        }

        @Bean
        @CronTrigger(
                cronExpression = "* * * * * ?",
                description = "Executor method for solomon reporter testing"
        )
        public Executor solomonReporterTestExecutorMethod() {
            return context -> {
                try {
                    Thread.sleep(JOB_DURATION_MILLIS);
                    HOLDER.markSuccess();
                    HOLDER.getLatch().countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
        }
    }
}
