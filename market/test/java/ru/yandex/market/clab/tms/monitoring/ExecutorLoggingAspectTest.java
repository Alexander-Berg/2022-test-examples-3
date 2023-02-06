package ru.yandex.market.clab.tms.monitoring;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobExecutionContext;
import org.quartz.impl.JobDetailImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.tms.quartz2.model.VerboseExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 28.12.2018
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ExecutorLoggingAspectTest {

    @Autowired
    private ExecutorLoggingAspect aspect;

    @Autowired
    private VerboseExecutorCounting verboseExecutor;

    @Autowired
    private VerboseExecutor verboseExecutorThrows;

    @Autowired
    private List<String> logHolder;

    private JobExecutionContext context;

    private static final String DATE_FORMAT = "20\\d{2}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{4}";

    @Before
    public void before() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setName("importantExecutor");
        jobDetail.setGroup("DEFAULT");
        logHolder.clear();
        context = mock(JobExecutionContext.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
    }

    @Test
    public void verboseExecutorHandling() throws Throwable {
        verboseExecutor.doRealJob(context);

        assertThat(verboseExecutor.getExecutionsCount()).isOne();
        String logRecord = "tskv" +
            "\tdate=" + DATE_FORMAT + "" +
            "\texecutor=importantExecutor" +
            "\tgroup=DEFAULT" +
            "\thost=[^\t]+" +
            "\tstarted=" + DATE_FORMAT +
            "\tfinished=" + DATE_FORMAT +
            "\tok=1";

        assertThat(logHolder).hasSize(1);
        assertThat(logHolder.get(0)).matches(Pattern.compile(logRecord));
    }

    @Test
    public void verboseExecutorErrorHandling() {
        assertThatThrownBy(() ->
            verboseExecutorThrows.doRealJob(context)
        ).hasMessage("division by zero").isInstanceOf(ArithmeticException.class);

        String logRecord = "tskv" +
            "\tdate=" + DATE_FORMAT + "" +
            "\texecutor=importantExecutor" +
            "\tgroup=DEFAULT" +
            "\thost=[^\t]+" +
            "\tstarted=" + DATE_FORMAT +
            "\tfinished=" + DATE_FORMAT +
            "\texception=ArithmeticException" +
            "\texception_message=division by zero" +
            "\tok=0";

        assertThat(logHolder).hasSize(1);
        assertThat(logHolder.get(0)).matches(Pattern.compile(logRecord));
    }

    static class VerboseExecutorCounting extends VerboseExecutor {
        private final AtomicInteger executions = new AtomicInteger();

        @Override
        public void doRealJob(JobExecutionContext context) throws Exception {
            executions.incrementAndGet();
        }

        public int getExecutionsCount() {
            return executions.get();
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    public static class Context {
        @Bean
        public List<String> logHolder() {
            return new ArrayList<>();
        }

        @Bean
        public VerboseExecutorCounting verboseExecutor() {
            return new VerboseExecutorCounting();
        }

        @Bean
        public VerboseExecutor verboseExecutorThrows() {
            return new VerboseExecutor() {
                @Override
                public void doRealJob(JobExecutionContext context) throws Exception {
                    throw new ArithmeticException("division by zero");
                }
            };
        }

        @Bean
        public ExecutorLoggingAspect executorLoggingAspect() {
            List<String> logHolder = logHolder();
            return new ExecutorLoggingAspect() {
                @Override
                protected void log(String line) {
                    logHolder.add(line);
                }
            };
        }
    }
}
