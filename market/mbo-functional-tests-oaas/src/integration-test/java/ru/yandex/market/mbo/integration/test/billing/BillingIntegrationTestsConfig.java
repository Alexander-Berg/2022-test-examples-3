package ru.yandex.market.mbo.integration.test.billing;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Resource;

import com.google.common.collect.ImmutableSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.db.IdGenerator;
import ru.yandex.common.util.db.OracleSequenceIdGenerator;
import ru.yandex.common.util.db.PgSequenceIdGenerator;
import ru.yandex.market.mbo.billing.BillingCounterRegistry;
import ru.yandex.market.mbo.billing.counter.BillingOperations;
import ru.yandex.market.mbo.billing.counter.BillingOperationsImpl;
import ru.yandex.market.mbo.billing.counter.PaidOperationLoader;
import ru.yandex.market.mbo.billing.counter.SuspendedOperationsCounter;
import ru.yandex.market.mbo.billing.counter.tt.AbstractBillingCounterConfig;
import ru.yandex.market.mbo.billing.counter.tt.CheckClusterizerLinkCounter;
import ru.yandex.market.mbo.billing.counter.tt.CheckGroupLogCounter;
import ru.yandex.market.mbo.billing.counter.tt.LogBasketCounter;
import ru.yandex.market.mbo.billing.counter.tt.TTOperationCounterConfig;
import ru.yandex.market.mbo.billing.tarif.TarifProvider;
import ru.yandex.market.mbo.core.conf.DatabasesConfig;
import ru.yandex.market.mbo.core.conf.databases.IdGeneratorConfig;
import ru.yandex.market.mbo.core.conf.databases.MboChytConfig;
import ru.yandex.market.mbo.core.conf.databases.MboOracleDBConfig;
import ru.yandex.market.mbo.core.kdepot.api.KnowledgeDepotService;
import ru.yandex.market.mbo.core.kdepot.impl.OracleKnowledgeDepotService;
import ru.yandex.market.mbo.db.MboDbSelector;
import ru.yandex.market.mbo.db.OfferService;
import ru.yandex.market.mbo.http.OfferStorageServiceStub;
import ru.yandex.market.mbo.http.OffersStorage;
import ru.yandex.market.mbo.tms.billing.MboBillingInfrastructureConfig;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.TaskTrackerImpl;
import ru.yandex.market.mbo.tt.events.AuditEventAction;
import ru.yandex.market.mbo.tt.events.EventManager;
import ru.yandex.market.mbo.tt.events.EventManagerImpl;
import ru.yandex.market.mbo.tt.legacy.BadClusterizerLinkChainsHelper;
import ru.yandex.market.mbo.tt.legacy.CheckGroupingLogManager;
import ru.yandex.market.mbo.tt.legacy.ClusterizerLinkService;
import ru.yandex.market.mbo.tt.legacy.ClusterizerLinkTaskManager;
import ru.yandex.market.mbo.tt.legacy.LogTaskManager;
import ru.yandex.market.mbo.tt.legacy.TaskTrackerBeans;
import ru.yandex.market.mbo.tt.owner.OwnerManager;
import ru.yandex.market.mbo.tt.owner.OwnerManagerImpl;
import ru.yandex.market.mbo.tt.status.StatusManager;
import ru.yandex.market.mbo.tt.status.StatusManagerImpl;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.utils.db.TransactionChainCall;

@Configuration
@Import({
    MboBillingInfrastructureConfig.class,
    IdGeneratorConfig.class,
    DatabasesConfig.class,
    MboChytConfig.class
})
@ComponentScan(value = {
    "ru.yandex.market.mbo.category"
})
public class BillingIntegrationTestsConfig {

    @Resource
    private IdGeneratorConfig idGeneratorConfig;
    @Resource
    private MboOracleDBConfig mboOracleDBConfig;

    @Resource(name = "ttIdGenerator")
    private PgSequenceIdGenerator ttIdGenerator;

    @Resource(name = "ngActionIdGenerator")
    private OracleSequenceIdGenerator ngActionIdGenerator;

    @Resource(name = "ttOfferLinksIdGenerator")
    private IdGenerator ttOfferLinksIdGenerator;

    @Resource(name = "siteCatalogJdbcTemplate")
    private JdbcTemplate siteCatalogJdbcTemplate;

    @Resource(name = "namedScatJdbcTemplate")
    private NamedParameterJdbcTemplate namedSiteCatalogJdbcTemplate;

    @Resource
    private MboChytConfig mboChytConfig;

    @Resource(name = "chytNamedJdbcTemplate")
    private NamedParameterJdbcTemplate chytNamedJdbcTemplate;

    @Resource(name = "scatTransactionTemplate")
    private TransactionTemplate siteCatalogTransactionTemplate;

    @Resource(name = "siteCatalogPgJdbcTemplate")
    private JdbcTemplate siteCatalogPgJdbcTemplate;

    @Resource(name = "transactionChainCallOraPg")
    private TransactionChainCall transactionChainCallOraPg;

    @Resource(name = "logTaskManagerDbSelector")
    private MboDbSelector logTaskManagerDbSelector;

    @Resource
    private AutoUser autoUser;

    @Bean
    public TaskTrackerBeans taskTrackerBeans() {
        return new TaskTrackerBeans(
            taskTracker(),
            statusManager(),
            eventManager(),
            ownerManager(),
            null,
            autoUser,
            null
        );
    }

    @Bean
    public AbstractBillingCounterConfig abstractBillingCounterConfig() {
        return new AbstractBillingCounterConfig(
            siteCatalogJdbcTemplate,
                siteCatalogPgJdbcTemplate, null,
            null,
            billingOperations(),
            null,
            null,
            null,
            null
        );
    }

    @Bean
    public TTOperationCounterConfig ttOperationCounterConfig() {
        return new TTOperationCounterConfig(
            abstractBillingCounterConfig(),
            statusManager(),
            taskTracker()
        );
    }

    @Bean
    public CheckGroupLogCounter checkGroupLogCounter() {
        return new CheckGroupLogCounter(
            ttOperationCounterConfig(),
            checkGroupingLogManager(),
            siteCatalogPgJdbcTemplate);
    }

    @Bean
    public LogBasketCounter logBasketCounter() {
        return new LogBasketCounter(
            ttOperationCounterConfig(),
            logTaskManager());
    }

    @Bean
    public CheckClusterizerLinkCounter checkClusterizerLinkCounter() {
        return new CheckClusterizerLinkCounter(
            ttOperationCounterConfig(),
            clusterizerLinkTaskManager());
    }

    @Bean
    public CheckGroupingLogManager checkGroupingLogManager() {
        return new CheckGroupingLogManager(taskTrackerBeans(), null, null,
            null, logTaskManager(),  null);
    }

    @Bean
    public LogTaskManager logTaskManager() {
        return new LogTaskManager(taskTrackerBeans(), siteCatalogJdbcTemplate, namedSiteCatalogJdbcTemplate,
            chytNamedJdbcTemplate, null, null,
            siteCatalogTransactionTemplate, null, null, null,
            null, null, null, offerService(), null,
            logTaskManagerDbSelector);
    }

    @Bean
    public TaskTracker taskTracker() {
        return new TaskTrackerImpl(
            siteCatalogJdbcTemplate,
            siteCatalogPgJdbcTemplate,
            transactionChainCallOraPg,
            eventManager(),
            ttIdGenerator,
            statusManager(),
            ownerManager(),
            autoUser);
    }

    @Bean
    public StatusManager statusManager() {
        StatusManagerImpl statusManager = new StatusManagerImpl(
                siteCatalogJdbcTemplate, siteCatalogTransactionTemplate,
                siteCatalogPgJdbcTemplate, transactionChainCallOraPg,
                ngActionIdGenerator, eventManager());
        statusManager.setStatusValidators(Collections.emptyList());
        return statusManager;
    }

    @Bean
    public OwnerManager ownerManager() {
        return new OwnerManagerImpl(siteCatalogJdbcTemplate, siteCatalogTransactionTemplate,
            siteCatalogPgJdbcTemplate, transactionChainCallOraPg,
            ngActionIdGenerator, eventManager());
    }

    @Bean
    public ClusterizerLinkService clusterizerLinkService() {
        return new ClusterizerLinkService(
            new BadClusterizerLinkChainsHelper(),
            namedSiteCatalogJdbcTemplate,
            siteCatalogTransactionTemplate,
            offerService(),
            ttOfferLinksIdGenerator,
            null);
    }

    @Bean
    public TestOperationCounter testOperationCounter() {
        TestOperationCounter counter = new TestOperationCounter();
        counter.setBillingOperations(billingOperations());
        return counter;
    }

    @Bean
    public PaidOperationLoader suspendedOperationsCounter() {
        SuspendedOperationsCounter counter = new SuspendedOperationsCounter();
        counter.setBillingOperations(billingOperations());
        counter.setSiteCatalogJdbcTemplate(siteCatalogJdbcTemplate);
        return counter;
    }

    @Bean
    public BillingCounterRegistry billingCounterRegistry() {
        return new BillingCounterRegistry(ImmutableSet.of(
            suspendedOperationsCounter(),
            testOperationCounter()
        ));
    }

    @Bean
    public BillingOperations billingOperations() {
        return new BillingOperationsImpl(siteCatalogJdbcTemplate);
    }

    @Bean
    public EventManager eventManager() {
        return new EventManagerImpl(null) {
            @Override
            public void writeAuditActions(Collection<AuditEventAction> oldAuditActions) {

            }
        };
    }

    @Bean
    public ClusterizerLinkTaskManager clusterizerLinkTaskManager() {
        return new ClusterizerLinkTaskManager(taskTrackerBeans(), null, siteCatalogTransactionTemplate,
            clusterizerLinkService(), namedSiteCatalogJdbcTemplate, offerService());
    }

    @Bean
    public OfferService offerService() {
        return new OfferService(namedSiteCatalogJdbcTemplate, new OfferStorageServiceStub() {
            @Override
            public OffersStorage.GetOffersResponse getOffersByIds(OffersStorage.GetOffersRequest getOffersRequest) {
                return OffersStorage.GetOffersResponse.newBuilder().build();
            }
        }, chytNamedJdbcTemplate).setYtMediumLogTable(mboChytConfig.getYtMediumLogTable());
    }

    @Bean
    public TarifProvider tarifProvider() {
        return new TarifProvider() {
            @Override
            public boolean containsTarif(int operationId, long guruCategoryId) {
                return true;
            }

            @Override
            public boolean containsOperationTarif(int operationId) {
                return true;
            }

            @Override
            public boolean containsCategoryOperationTarif(int operationId, long guruCategoryId) {
                return true;
            }

            @Override
            public BigDecimal getTarif(int operationId, long guruCategoryId, Calendar time) {
                return BigDecimal.valueOf(1D);
            }

            @Override
            public BigDecimal getCategoryOperationTarif(int operationId, long guruCategoryId, Calendar time) {
                return BigDecimal.valueOf(1D);
            }

            @Override
            public BigDecimal getOperationTarif(int operationId, Calendar time) {
                return BigDecimal.valueOf(1D);
            }
        };
    }

    @Bean(name = "marketKnowledgeDepotService")
    public KnowledgeDepotService marketKnowledgeDepotService() {
        OracleKnowledgeDepotService knowledgeDepotService = new OracleKnowledgeDepotService();
        knowledgeDepotService.setIdGenerator(idGeneratorConfig.idGenerator());
        knowledgeDepotService.setJdbcTemplate(mboOracleDBConfig.marketDepotJdbcTemplate());
        knowledgeDepotService.setTransactionTemplate(mboOracleDBConfig.marketDepotTransactionTemplate());
        return knowledgeDepotService;
    }
}
