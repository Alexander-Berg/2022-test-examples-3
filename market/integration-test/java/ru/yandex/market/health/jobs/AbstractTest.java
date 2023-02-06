package ru.yandex.market.health.jobs;

import java.io.FileInputStream;
import java.util.Date;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.google.common.base.Strings;
import org.apache.commons.lang3.time.DateUtils;
import org.dbunit.IDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.health.jobs.configuration.TmsConfiguration;
import ru.yandex.market.health.jobs.configuration.TmsHttpEmbeddedPostgresConfiguration;
import ru.yandex.market.health.jobs.configuration.TmsPluginConfiguration;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.tms.quartz2.spring.config.DatabaseSchedulerFactoryConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@Import({
    TmsHttpEmbeddedPostgresConfiguration.class,
    LiquibaseAutoConfiguration.LiquibaseConfiguration.class,
    DatabaseSchedulerFactoryConfig.class,
    DbUnitTestConfiguration.class,
    TmsPluginConfiguration.class,
    TmsConfiguration.class
})
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DbUnitTestExecutionListener.class
})
@WebAppConfiguration
public abstract class AbstractTest {

    private static String HANGING_PREFIX = "/health/hangingJobs/";
    private static String FAILED_PREFIX = "/health/failedJobs/";


    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Autowired
    protected IDatabaseTester databaseTester;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Value("${tms.http.ok.cache.lifespan}")
    private long okCacheLifeSpan;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    String fireHangingJobs() {
        return fireRequestAndValidateHeaders(HANGING_PREFIX, null);
    }

    String fireHangingJob(String jobName) {
        return fireRequestAndValidateHeaders(HANGING_PREFIX, jobName);
    }

    String fireFailedJobs() {
        return fireRequestAndValidateHeaders(FAILED_PREFIX, null);
    }

    String fireFailedJob(String jobName) {
        return fireRequestAndValidateHeaders(FAILED_PREFIX, jobName);
    }

    private String fireRequestAndValidateHeaders(String prefix, String jobName) {
        try {
            return mockMvc.perform(get(String.format("%s%s", prefix, Strings.nullToEmpty(jobName))))
                .andExpect(status().isOk())
                .andExpect(header().exists("content-type"))
                .andExpect(header().string("content-type",
                    String.format("%s;%s", MediaType.TEXT_PLAIN_VALUE, "charset=UTF-8")))
                .andReturn().getResponse().getContentAsString();
        } catch (Exception e) {
            throw new RuntimeException("Error accessing web app", e);
        }
    }

    void prepareDataSet(String configLocation, int hoursToAdd) {
        try {
            IDataSet dataSet = new FlatXmlDataSetBuilder()
                .build(new FileInputStream(resourceLoader.getResource(configLocation).getFile()));
            ReplacementDataSet rDataSet = new ReplacementDataSet(dataSet);
            rDataSet.addReplacementObject("[fired_date]",
                DateUtils.addHours(new Date(), hoursToAdd).toInstant().toEpochMilli());
            databaseTester.setDataSet(rDataSet);
            databaseTester.onSetup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void waitCacheEviction() {
        try {
            Thread.sleep(okCacheLifeSpan);
        } catch (InterruptedException e) {
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
}
