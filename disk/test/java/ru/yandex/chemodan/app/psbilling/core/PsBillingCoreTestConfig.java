package ru.yandex.chemodan.app.psbilling.core;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.app.psbilling.core.balance.BalanceService;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.GroupServiceTransactionsCalculationService;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.payment.AutoPaymentUtils;
import ru.yandex.chemodan.app.psbilling.core.config.PsBillingCoreConfiguration;
import ru.yandex.chemodan.app.psbilling.core.config.PsBillingCoreMocksConfig;
import ru.yandex.chemodan.app.psbilling.core.config.tasks.TaskBeansConfiguration;
import ru.yandex.chemodan.app.psbilling.core.dao.features.GroupServiceFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.features.UserServiceFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.BucketContentDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceMemberDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServicePriceOverrideDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.EmailTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.FeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductOwnerDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductTemplateFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductBucketDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPeriodDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPricesDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promocodes.PromoCodeDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.UserPromoDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.group.GroupPromoDao;
import ru.yandex.chemodan.app.psbilling.core.dao.texts.TankerKeyDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.OrderDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.UserServiceDao;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupServicesManager;
import ru.yandex.chemodan.app.psbilling.core.groups.TrialService;
import ru.yandex.chemodan.app.psbilling.core.mail.dataproviders.LocalizedEmailSenderDataProvider;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.products.BucketContentManager;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProductManager;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductManager;
import ru.yandex.chemodan.app.psbilling.core.synchronization.groupmember.GroupServiceMemberActualizationService;
import ru.yandex.chemodan.app.psbilling.core.synchronization.groupservice.GroupServicesActualizationService;
import ru.yandex.chemodan.app.psbilling.core.synchronization.userservice.UserServiceActualizationService;
import ru.yandex.chemodan.app.psbilling.core.synchronization.userservicefeature.UserFeaturesActualizationService;
import ru.yandex.chemodan.app.psbilling.core.texts.TextsManager;
import ru.yandex.chemodan.app.psbilling.core.utils.EmailHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.PromoHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.factories.FactoryConfig;
import ru.yandex.chemodan.app.psbilling.core.utils.factories.PsBillingPromoFactory;
import ru.yandex.chemodan.boot.ChemodanTestBaseContextConfiguration;
import ru.yandex.chemodan.boot.value.OverridableValuePrefix;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.util.ZkUtils;
import ru.yandex.chemodan.util.jdbc.DataSourceProperties;
import ru.yandex.chemodan.util.test.EmbeddedDBDataSourceProperties;
import ru.yandex.chemodan.zk.configuration.ImportZkEmbeddedConfiguration;
import ru.yandex.commune.zk2.ZkPath;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.spring.ServicesStarter;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

@Configuration
@Import({
        PsBillingCoreConfiguration.class,
        PsBillingCoreMocksConfig.class,
        TaskBeansConfiguration.class,
        FactoryConfig.class
})
@ImportEmbeddedPg
@ImportZkEmbeddedConfiguration
public class PsBillingCoreTestConfig extends ChemodanTestBaseContextConfiguration {
    @Bean
    @OverridableValuePrefix("ps-billing")
    public DataSourceProperties dataSourceProperties(EmbeddedPostgres embeddedPostgres) {
        return new EmbeddedDBDataSourceProperties(
                PreparedDbProvider.forPreparer("ps_billing_db", embeddedPostgres)
                        .createDatabase()
        );
    }

    @Bean
    public PsBillingGroupsFactory psBillingGroupsFactory(GroupDao groupDao, GroupServicesManager groupServicesManager,
                                                         GroupServiceDao groupServiceDao,
                                                         GroupServiceMemberDao groupServiceMemberDao,
                                                         GroupProductManager groupProductManager,
                                                         UserProductManager userProductManager,
                                                         PsBillingProductsFactory productsFactory,
                                                         PsBillingTextsFactory textsFactory,
                                                         GroupProductDao groupProductDao,
                                                         GroupServiceFeatureDao groupServiceFeatureDao,
                                                         ProductTemplateFeatureDao productTemplateFeatureDao,
                                                         GroupServicePriceOverrideDao groupServicePriceOverrideDao,
                                                         TankerKeyDao tankerKeyDao) {
        return new PsBillingGroupsFactory(
                groupDao, groupServicesManager, groupServiceMemberDao, groupServiceDao, groupProductManager,
                userProductManager, productsFactory, textsFactory, groupProductDao, groupServiceFeatureDao,
                productTemplateFeatureDao, groupServicePriceOverrideDao, tankerKeyDao);
    }

    @Bean
    public PsBillingUsersFactory psBillingUsersFactory(UserServiceFeatureDao userServiceFeatureDao,
                                                       UserServiceDao userServiceDao,
                                                       PsBillingProductsFactory psBillingProductsFactory,
                                                       ProductTemplateFeatureDao productTemplateFeatureDao) {
        return new PsBillingUsersFactory();
    }

    @Bean
    public PsBillingProductsFactory psBillingProductsFactory(GroupProductDao groupProductDao, FeatureDao featureDao,
                                                             UserProductDao userProductDao,
                                                             ProductFeatureDao productFeatureDao,
                                                             ProductOwnerDao productOwnerDao,
                                                             UserProductPricesDao userProductPricesDao,
                                                             TrialService trialService, ProductLineDao productLineDao,
                                                             ProductSetDao productSetDao,
                                                             UserProductPeriodDao userProductPeriodDao,
                                                             GroupProductManager groupProductManager,
                                                             PsBillingTextsFactory psBillingTextsFactory,
                                                             UserProductBucketDao userProductBucketDao,
                                                             ProductTemplateFeatureDao productTemplateFeatureDao,
                                                             ProductTemplateDao productTemplateDao,
                                                             BucketContentDao bucketContentDao,
                                                             JdbcTemplate3 jdbcTemplate3,
                                                             BucketContentManager bucketContentManager) {
        return new PsBillingProductsFactory(groupProductDao, userProductDao, productFeatureDao, featureDao,
                productOwnerDao, userProductPricesDao, userProductPeriodDao, trialService, productLineDao,
                productSetDao, groupProductManager, psBillingTextsFactory, userProductBucketDao,
                productTemplateFeatureDao, productTemplateDao, bucketContentDao, bucketContentManager, jdbcTemplate3);
    }

    @Bean
    public PsBillingPromoFactory psBillingPromoFactory(PromoTemplateDao promoTemplateDao, UserPromoDao userPromoDao,
                                                       PromoCodeDao promoCodeDAO, JdbcTemplate3 jdbcTemplate) {
        return new PsBillingPromoFactory(promoTemplateDao, userPromoDao, promoCodeDAO, jdbcTemplate);
    }

    @Bean
    public PsBillingOrdersFactory psBillingOrdersFactory(OrderDao orderDao) {
        return new PsBillingOrdersFactory();
    }

    @Bean
    public PsBillingTextsFactory psBillingTextsFactory(TankerKeyDao tankerKeyDao, TextsManager textsManager) {
        return new PsBillingTextsFactory(tankerKeyDao, textsManager);
    }

    @Bean
    public ZkPath zkRoot() {
        return ZkUtils.rootPath("ps-billing-core", EnvironmentType.TESTS);
    }

    @Override
    protected AppName appName() {
        return new SimpleAppName("disk", "ps-billing-core");
    }

    @Bean
    public PromoHelper promoHelper(PromoTemplateDao promoTemplateDao, UserPromoDao userPromoDao, GroupPromoDao groupPromoDao) {
        return new PromoHelper(promoTemplateDao, userPromoDao, groupPromoDao);
    }

    @Bean
    public EmailHelper emailHelper(EmailTemplateDao emailTemplateDao, BazingaTaskManagerMock bazingaTaskManagerMock,
                                   LocalizedEmailSenderDataProvider senderDataProvider) {
        return new EmailHelper(emailTemplateDao, bazingaTaskManagerMock, senderDataProvider);
    }

    @Bean
    public Starter starter(ServicesStarter servicesStarter) {
        return new Starter(servicesStarter);
    }

    @Bean
    public PsBillingBalanceFactory psBillingBalanceFactory(BalanceService balanceService,
                                                           BalanceClientStub balanceClientStub,
                                                           ClientBalanceDao clientBalanceDao) {
        return new PsBillingBalanceFactory(balanceService, balanceClientStub, clientBalanceDao);
    }


    @Bean
    public PsBillingTransactionsFactory psBillingTransactionsFactory(
            PsBillingGroupsFactory psBillingGroupsFactory,
            PsBillingProductsFactory psBillingProductsFactory,
            DirectoryClient directoryClient,
            GroupServicesActualizationService groupServicesActualizationService,
            GroupServiceMemberActualizationService groupServiceMemberActualizationService,
            UserServiceActualizationService userServiceActualizationService,
            GroupServiceMemberDao groupServiceMemberDao,
            UserServiceDao userServiceDao,
            GroupServiceDao groupServiceDao,
            GroupProductDao groupProductDao,
            GroupProductManager groupProductManager,
            UserServiceFeatureDao userServiceFeatureDao,
            FeatureDao featureDao,
            UserFeaturesActualizationService userFeaturesActualizationService,
            GroupServicesManager groupServicesManager,
            GroupServiceTransactionsCalculationService groupServiceTransactionsCalculationService,
            GroupServiceTransactionsDao groupServiceTransactionsDao, GroupDao groupDao) {
        return new PsBillingTransactionsFactory(
                psBillingGroupsFactory,
                psBillingProductsFactory,
                directoryClient,
                groupServicesActualizationService,
                groupServiceMemberActualizationService,
                userServiceActualizationService,
                groupServiceMemberDao,
                userServiceDao,
                groupServiceDao,
                groupProductDao,
                groupProductManager,
                userServiceFeatureDao,
                featureDao,
                userFeaturesActualizationService,
                groupServicesManager,
                groupServiceTransactionsCalculationService,
                groupServiceTransactionsDao, groupDao);
    }

    @Bean
    public AutoPaymentUtils getAutoPaymentUtils() {
        return new AutoPaymentUtils();
    }

    @RequiredArgsConstructor
    public static class Starter {
        private final ServicesStarter servicesStarter;

        @PostConstruct
        public void setup() {
            servicesStarter.start();
        }

        @PreDestroy
        public void teardown() {
            servicesStarter.stop();
        }
    }
}

