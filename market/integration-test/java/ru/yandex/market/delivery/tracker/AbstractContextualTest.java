package ru.yandex.market.delivery.tracker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Condition;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.json.JSONException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.delivery.tracker.configuration.BatchSuppliersConfiguration;
import ru.yandex.market.delivery.tracker.configuration.BusinessServicesConfiguration;
import ru.yandex.market.delivery.tracker.configuration.ClockTestConfiguration;
import ru.yandex.market.delivery.tracker.configuration.DbUnitTestConfiguration;
import ru.yandex.market.delivery.tracker.configuration.EmbeddedPostgresConfiguration;
import ru.yandex.market.delivery.tracker.configuration.HealthServicesConfiguration;
import ru.yandex.market.delivery.tracker.configuration.ResetAllOnDbUnitTestExecutionListener;
import ru.yandex.market.delivery.tracker.configuration.TestConsumersConfiguration;
import ru.yandex.market.delivery.tracker.configuration.TestQueueConfiguration;
import ru.yandex.market.delivery.tracker.configuration.TestTvmConfiguration;
import ru.yandex.market.delivery.tracker.configuration.WebConfig;
import ru.yandex.market.delivery.tracker.configuration.dbqueue.QueueConfiguration;
import ru.yandex.market.delivery.tracker.configuration.properties.AssignedTrackingBatchesProperties;
import ru.yandex.market.delivery.tracker.configuration.properties.BatchProcessingProperties;
import ru.yandex.market.delivery.tracker.configuration.properties.DeliveryServiceReqLimitProperties;
import ru.yandex.market.delivery.tracker.configuration.properties.DeliveryServiceSyncProperties;
import ru.yandex.market.delivery.tracker.configuration.properties.OtherFeaturesProperties;
import ru.yandex.market.delivery.tracker.configuration.properties.PriorityProperties;
import ru.yandex.market.delivery.tracker.configuration.properties.RequestIntervalProperties;
import ru.yandex.market.delivery.tracker.configuration.sqs.LesSqsConfiguration;
import ru.yandex.market.delivery.tracker.service.logger.BatchConsumerDelayTskvLogger;
import ru.yandex.market.delivery.tracker.service.logger.BusinessDataLogger;
import ru.yandex.market.delivery.tracker.service.logger.PushOrderStatusLogger;
import ru.yandex.market.delivery.tracker.service.pushing.PushCheckpointLesQueueProducer;
import ru.yandex.market.delivery.tracker.service.pushing.PushTrackQueueProducer;
import ru.yandex.market.delivery.tracker.service.pushing.PushTrackService;
import ru.yandex.market.delivery.tracker.service.tracking.CheckpointsProcessingService;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.util.client.tvm.TvmSecurityConfiguration;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = {
        LiquibaseAutoConfiguration.LiquibaseConfiguration.class,
        EmbeddedPostgresConfiguration.class,
        DbUnitTestConfiguration.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@TestPropertySource("classpath:application-integration-test.properties")
@AutoConfigureMockMvc
@AutoConfigureDataJpa
@ComponentScan({
    "ru.yandex.market.delivery.tracker.dao",
    "ru.yandex.market.delivery.tracker.client.tracking.lgw",
    "ru.yandex.market.delivery.tracker.controller",
    "ru.yandex.market.delivery.tracker.service",
    "ru.yandex.market.delivery.tracker.domain",
    "ru.yandex.market.logistics.les.client",
})
@Import({
    TestTvmConfiguration.class,
    WebConfig.class,
    HealthServicesConfiguration.class,
    BusinessServicesConfiguration.class,
    QueueConfiguration.class,
    TestQueueConfiguration.class,
    ClockTestConfiguration.class,
    TestConsumersConfiguration.class,
    TvmSecurityConfiguration.class,
    BatchSuppliersConfiguration.class,
    PriorityProperties.class,
    AssignedTrackingBatchesProperties.class,
    DeliveryServiceReqLimitProperties.class,
    BatchProcessingProperties.class,
    OtherFeaturesProperties.class,
    RequestIntervalProperties.class,
    DeliveryServiceSyncProperties.class,
    LesSqsConfiguration.class
})
@MockBean({
    DeliveryClient.class,
    FulfillmentClient.class,
    BusinessDataLogger.class,
    BatchConsumerDelayTskvLogger.class,
    LMSClient.class,
    PushOrderStatusLogger.class,
    MdsS3Client.class,
    ResourceLocationFactory.class,
})
@SpyBean({
    PushTrackService.class,
    PushTrackQueueProducer.class,
    PushCheckpointLesQueueProducer.class,
    CheckpointsProcessingService.class,
})
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    ResetAllOnDbUnitTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
})
@DatabaseSetup
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
public abstract class AbstractContextualTest {
    protected static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    @Autowired
    protected BusinessDataLogger businessDataLogger;
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @RegisterExtension
    JUnitJupiterSoftAssertions assertions = new JUnitJupiterSoftAssertions();

    protected JUnitJupiterSoftAssertions assertions() {
        return assertions;
    }

    @SuppressWarnings("unchecked")
    protected void verifyLogging(ArgumentCaptor<?> captor) {
        verifyLogging(captor, 1);
    }

    @SuppressWarnings("unchecked")
    protected void verifyLogging(ArgumentCaptor<?> captor, int times) {
        Mockito.verify(businessDataLogger, Mockito.times(times)).log(captor.capture());
    }

    protected final String extractFileContent(String relativePath) {
        try {
            return IOUtils.toString(getSystemResourceAsStream(relativePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String httpOperationWithResult(MockHttpServletRequestBuilder httpOperation,
                                             ResultMatcher... matchers) throws Exception {
        ResultActions resultActions = mockMvc.perform(httpOperation);
        for (ResultMatcher matcher : matchers) {
            resultActions.andExpect(matcher);
        }

        return resultActions
            .andReturn().getResponse()
            .getContentAsString();
    }

    protected Condition<? super String> jsonStrictMatching(String expectedJson) {
        return getCondition(expectedJson, JSONCompareMode.STRICT);
    }

    protected Condition<? super String> jsonNonStrictMatching(String expectedJson) {
        return getCondition(expectedJson, JSONCompareMode.LENIENT);
    }

    private Condition<? super String> getCondition(String expectedJson, JSONCompareMode compareMode) {
        return new Condition<String>() {
            @Override
            public boolean matches(String actualJson) {
                try {
                    JSONAssert.assertEquals(expectedJson, actualJson, compareMode);
                    return true;
                } catch (JSONException e) {
                    return false;
                }
            }
        };
    }

    protected void executeSql(String sqlStatement) {
        jdbcTemplate.execute(sqlStatement);
    }

    protected LocalDateTime extractLocalDateFromDb(String sqlStatement) {
        List<LocalDateTime> dateTimes = jdbcTemplate.queryForList(sqlStatement, LocalDateTime.class);
        if (dateTimes.size() > 1) {
            assertions.fail("sql statement " + sqlStatement + " should extract at most one line");
        }
        return dateTimes.isEmpty() ? null : dateTimes.get(0);
    }

}
