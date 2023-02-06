package ru.yandex.market.clab.tms.executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.clab.tms.batch.JobExceptionCollector;

import javax.annotation.PostConstruct;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 25.01.2019
 */
@SpringBootTest(
    properties = {
        "spring.main.allow-bean-definition-overriding=true"
    },
    classes = {
        BatchAutoConfiguration.class,
        SpringBatchTriggerTest.Context.class
    }
)
@EnableBatchProcessing()
@RunWith(SpringRunner.class)
public class SpringBatchTriggerTest {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ThrowingProcessor<?> firstProcessor;

    @Autowired
    private ThrowingProcessor<?> middleProcessor;

    @Autowired
    private ThrowingProcessor<?> lastProcessor;

    @Autowired
    private ListItemWriter<String> listItemWriter;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private Job testJob;

    private SpringBatchTrigger trigger;

    @Before
    public void before() {
        listItemWriter.getWrittenItems().clear();
        firstProcessor.doThrow(null);
        middleProcessor.doThrow(null);
        lastProcessor.doThrow(null);
        trigger = new SpringBatchTrigger(
            new ComplexMonitoring(), jobOperator, jobExplorer, testJob);
    }

    @Test
    public void testSuccessful() throws Exception {
        trigger.doRealJob(null);

        assertThat(getWrittenItems()).containsExactly("value & value", "10 & 10", "2015 & 2015");
    }

    @Test
    public void testFailedInMiddle() {
        middleProcessor.doThrow("test exception");

        assertThatThrownBy(() -> trigger.doRealJob(null))
            .isInstanceOf(JobExecutionException.class)
            .hasMessageContaining("testJob")
            .hasMessageContaining("test exception")
            .hasMessageContaining("cause exception")
            .hasMessageContaining(
                "ru.yandex.market.clab.tms.executors.SpringBatchTriggerTest$ThrowingProcessor.process")
            .hasMessageContaining("ru.yandex.market.clab.tms.executors.SpringBatchTriggerTest.testFailedInMiddle");

        assertThat(getWrittenItems()).containsExactly("value & value");
    }

    @SuppressWarnings("unchecked")
    private List<String> getWrittenItems() {
        return (List<String>) listItemWriter.getWrittenItems();
    }

    @Configuration
    public static class Context {

        @Autowired
        private JobBuilderFactory jobs;

        @Autowired
        private StepBuilderFactory steps;

        @Autowired
        private JobRegistry jobRegistry;

        @PostConstruct
        public void registerJobs() throws DuplicateJobException {
            // in production code JobRegistryBeanPostProcessor does it for us
            jobRegistry.register(new ReferenceJobFactory(testJob()));
        }

        @Bean
        public Job testJob() {
            return jobs.get("testJob")
                .incrementer(new RunIdIncrementer())
                .start(firstStep())
                .next(middleStep())
                .next(lastStep())
                .listener(jobExceptionCollector())
                .build();
        }

        @Bean
        @JobScope
        public Step firstStep() {
            return steps.get("firstStep")
                .<String, String>chunk(1)
                .reader(new ListItemReader<>(Collections.singletonList("value")))
                .processor(firstProcessor())
                .writer(itemWriter())
                .listener(jobExceptionCollector())
                .build();
        }

        @Bean
        @JobScope
        public Step middleStep() {
            return steps.get("middleStep")
                .<BigInteger, String>chunk(1)
                .reader(new ListItemReader<>(Collections.singletonList(BigInteger.TEN)))
                .processor(middleProcessor())
                .writer(itemWriter())
                .listener(jobExceptionCollector())
                .build();
        }

        @Bean
        @JobScope
        public Step lastStep() {
            return steps.get("lastStep")
                .<Integer, String>chunk(1)
                .reader(new ListItemReader<>(Collections.singletonList(2015)))
                .processor(lastProcessor())
                .writer(itemWriter())
                .listener(jobExceptionCollector())
                .build();
        }

        @Bean
        @JobScope
        public JobExceptionCollector jobExceptionCollector() {
            return new JobExceptionCollector();
        }

        @Bean
        public ItemProcessor<String, String> firstProcessor() {
            return new ThrowingProcessor<>();
        }

        @Bean
        public ItemProcessor<BigInteger, String> middleProcessor() {
            return new ThrowingProcessor<>();
        }

        @Bean
        public ItemProcessor<Integer, String> lastProcessor() {
            return new ThrowingProcessor<>();
        }

        @Bean
        public ItemWriter<String> itemWriter() {
            return new ListItemWriter<>();
        }

        @Bean
        public BatchConfigurer batchConfigurer() {
            return new DatasourceLessBatchConfigurer();
        }
    }

    private static class ThrowingProcessor<T> implements ItemProcessor<T, String> {
        private String exceptionMessage;

        public void doThrow(String exceptionMessage) {
            this.exceptionMessage = exceptionMessage;
        }

        @Override
        public String process(T item) throws Exception {
            if (exceptionMessage != null) {
                try {
                    throw new RuntimeException("cause exception");
                } catch (RuntimeException re) {
                    throw new RuntimeException(exceptionMessage, re);
                }
            }
            return item + " & " + item;
        }
    }

    private static class DatasourceLessBatchConfigurer extends DefaultBatchConfigurer {

    }

}
