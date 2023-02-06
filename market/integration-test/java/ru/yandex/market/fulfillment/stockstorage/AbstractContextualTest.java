package ru.yandex.market.fulfillment.stockstorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Condition;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.fulfillment.stockstorage.config.ExecutionQueueRetryingServiceConfig;
import ru.yandex.market.fulfillment.stockstorage.config.ExecutorsConfiguration;
import ru.yandex.market.fulfillment.stockstorage.config.JpaConfig;
import ru.yandex.market.fulfillment.stockstorage.config.LmsClientConfiguration;
import ru.yandex.market.fulfillment.stockstorage.config.PusherToSolomonClientConfiguration;
import ru.yandex.market.fulfillment.stockstorage.config.ReplicaEntityManagerConfig;
import ru.yandex.market.fulfillment.stockstorage.config.RtyConfiguration;
import ru.yandex.market.fulfillment.stockstorage.configuration.DateTimeTestConfig;
import ru.yandex.market.fulfillment.stockstorage.configuration.ExceptionsParserConfiguration;
import ru.yandex.market.fulfillment.stockstorage.configuration.MockMvcConfiguration;
import ru.yandex.market.fulfillment.stockstorage.configuration.SecurityTestConfig;
import ru.yandex.market.fulfillment.stockstorage.configuration.ServicesTestConfiguration;
import ru.yandex.market.fulfillment.stockstorage.configuration.StockStorageDbUnitTestConfiguration;
import ru.yandex.market.fulfillment.stockstorage.configuration.StockStorageEmbeddedPostgresConfiguration;
import ru.yandex.market.fulfillment.stockstorage.repository.JdbcSkuRepository;
import ru.yandex.market.fulfillment.stockstorage.service.StocksAvailabilityCheckingService;
import ru.yandex.market.fulfillment.stockstorage.service.audit.FreezeEventAuditService;
import ru.yandex.market.fulfillment.stockstorage.service.audit.JpaStockCreateEventHandler;
import ru.yandex.market.fulfillment.stockstorage.service.audit.JpaStockEventsHandler;
import ru.yandex.market.fulfillment.stockstorage.service.audit.SkuEventAuditService;
import ru.yandex.market.fulfillment.stockstorage.service.lms.FulfillmentLmsClient;
import ru.yandex.market.fulfillment.stockstorage.service.lms.FulfillmentPartnersList;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.WarehousePropertiesService;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.WarehouseSyncService;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.backorder.BackorderService;
import ru.yandex.market.fulfillment.stockstorage.util.hibernate.ResettableSequenceStyleGenerator;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.client.LMSClientFactory;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.ListWrapper;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.market.logistics.util.client.HttpTemplate;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = {
                StockStorageEmbeddedPostgresConfiguration.class,
                StockStorageDbUnitTestConfiguration.class,
                PusherToSolomonClientConfiguration.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@TestPropertySource("classpath:application-integration-test.properties")
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureDataJpa
@EnableJpaRepositories("ru.yandex.market.fulfillment.stockstorage.repository")
@EntityScan("ru.yandex.market.fulfillment.stockstorage.*")
@ComponentScan({
        "ru.yandex.market.fulfillment.stockstorage.controller",
        "ru.yandex.market.fulfillment.stockstorage.repository",
        "ru.yandex.market.fulfillment.stockstorage.util",
        "ru.yandex.market.fulfillment.stockstorage.service",
        "ru.yandex.market.fulfillment.stockstorage.domain",
        "ru.yandex.market.fulfillment.stockstorage.facade"
})
@Import({
        ExecutionQueueRetryingServiceConfig.class,
        LmsClientConfiguration.class,
        ServicesTestConfiguration.class,
        ExceptionsParserConfiguration.class,
        DateTimeTestConfig.class,
        JpaConfig.class,
        ReplicaEntityManagerConfig.class,
        ExecutorsConfiguration.class,
        RtyConfiguration.class,
        SecurityTestConfig.class,
        MockMvcConfiguration.class
})
@MockBean({
        LMSClient.class,
        FulfillmentClient.class,
        LogbrokerService.class
})
@SpyBean(value = {
        SkuEventAuditService.class,
        FreezeEventAuditService.class,
        JpaStockEventsHandler.class,
        JpaStockCreateEventHandler.class,
        JdbcSkuRepository.class,
        BackorderService.class,
        StocksAvailabilityCheckingService.class
}, reset = MockReset.BEFORE)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        ResetDatabaseTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        MockitoTestExecutionListener.class,
        ResetMocksTestExecutionListener.class
})
@CleanDatabase
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection",
        "replicaDbUnitDatabaseConnection", "qrtzDbUnitDatabaseConnection", "archiveDbUnitDatabaseConnection"})
public abstract class AbstractContextualTest extends BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected FreezeEventAuditService freezeEventAuditService;

    @Autowired
    protected SkuEventAuditService skuEventAuditService;

    @Autowired
    protected WarehouseSyncService warehouseSyncService;

    @Autowired
    protected WarehousePropertiesService warehousePropertiesService;

    /**
     * Явно указываем, что необходимо рестартануть до теста,
     * т.к. клиент вызывается в рамках PostConstruct'а WarehouseSyncService'а.
     */
    @SpyBean(reset = MockReset.BEFORE)
    public FulfillmentLmsClient fulfillmentLmsClient;
    @MockBean(reset = MockReset.BEFORE)
    protected LMSClient lmsClient;

    @MockBean(name = "lmsHttpTemplate")
    protected HttpTemplate lmsTemplate;

    protected JpaStockEventsHandler stockEventsHandler;
    @Autowired
    private JpaStockEventsHandler stockEventsHandlerProxied;
    private final ThreadLocal<Integer> syncCounter = new ThreadLocal<>();

    private static final ObjectMapper MAPPER = LMSClientFactory.createLmsJsonConverter().getObjectMapper();

    @AfterEach
    public void resetMocks() {
        Mockito.reset(freezeEventAuditService, skuEventAuditService, stockEventsHandler, lmsTemplate);
        warehousePropertiesService.clearCache();
    }

    @BeforeEach
    public void setUp() {
        //extract real mock
        ResettableSequenceStyleGenerator.resetAllInstances();
        stockEventsHandler = AopTestUtils.getTargetObject(stockEventsHandlerProxied);
        syncCounter.set(0);
    }

    protected final String extractFileContent(String relativePath) {
        try {
            return IOUtils.toString(getSystemResourceAsStream(relativePath),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Condition<? super String> jsonMatchingWithoutOrder(String expectedJson) {
        return jsonMatchingInternal(expectedJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    protected Condition<? super String> jsonMatching(String expectedJson) {
        return jsonMatchingInternal(expectedJson, JSONCompareMode.STRICT);
    }

    private Condition<? super String> jsonMatchingInternal(String expectedJson, JSONCompareMode mode) {
        return new Condition<>() {
            @Override
            public boolean matches(String actualJson) {
                try {
                    JSONAssert.assertEquals(expectedJson, actualJson, mode);
                    return true;
                } catch (JSONException e) {
                    return false;
                }
            }
        };
    }

    protected final void setActiveWarehouses(Integer... warehouseIds) {
        List<PartnerResponse> partnerResponses = Stream.of(warehouseIds)
                .map(id ->
                        PartnerResponse.newBuilder()
                                .id(id)
                                .partnerType(PartnerType.FULFILLMENT)
                                .name("Warehouse Name")
                                .status(PartnerStatus.ACTIVE)
                                .stockSyncEnabled(true)
                                .build()
                )
                .collect(Collectors.toList());

        mockSearchPartners(partnerResponses);

        warehouseSyncService.recomputeCache();

        verify(fulfillmentLmsClient, atLeast(syncCounter.get() + 1))
                .searchPartners(any(SearchPartnerFilter.class));
    }

    protected void mockSearchPartners(List<PartnerResponse> partners) {
        FulfillmentPartnersList partnersFF = null;
        try {
            String partnersLms = MAPPER.writeValueAsString(new ListWrapper<PartnerResponse>(partners));
            partnersFF = MAPPER.readValue(partnersLms, FulfillmentPartnersList.class);
        } catch (JsonProcessingException e) {
            // Ответ приходит от LMS в формате ListWrapper<PartnerResponse>.
            // Проверим, что умеем распарсить этот ответ в FulfillmentPartnersList.
            Assertions.fail("Cannot transform LMS DTO to stock-storage DTO");
            e.printStackTrace();
        }

        when(lmsTemplate.executePut(any(), eq(FulfillmentPartnersList.class),
                eq("externalApi"), eq("partners"), eq("search")))
                .thenReturn(partnersFF);
    }
}
