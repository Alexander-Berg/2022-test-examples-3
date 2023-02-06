package ru.yandex.market.admin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.admin.config.TestPropertiesConfig;
import ru.yandex.market.admin.mapping.WarehouseMappingController;
import ru.yandex.market.admin.mapping.WarehouseMappingMdsS3Service;
import ru.yandex.market.admin.service.remote.RemoteCheckouterUIService;
import ru.yandex.market.admin.ui.service.PassportUIService;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.common.rest.TvmTicketProvider;
import ru.yandex.market.core.asyncreport.AsyncReports;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.ExternalBalanceService;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.delivery.region_blacklist.dao.DeliveryRegionBlacklistYtDao;
import ru.yandex.market.core.mbo.PartnerChangeDao;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchClient;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.security.SecManager;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.mock;

/**
 * Spring java-config для функциональных тестов в модуле mbi-admin.
 *
 * @author fbokovikov
 */
@Configuration
@Import({
        ru.yandex.market.core.config.FunctionalTestConfig.class,
        TestPropertiesConfig.class,
        EmbeddedPostgresConfig.class
})
@ImportResource({
        "classpath:common/common-transactions.xml",
        "classpath:admin/admin-models.xml",
        "classpath:admin/admin-services.xml",
        "classpath:ru/yandex/market/admin/admin-test.xml",
})
public class FunctionalTestConfig {

    @Bean
    public PassportUIService remotePassportService() {
        return mock(PassportUIService.class);
    }

    @Bean
    public CheckouterClient checkouterClient() {
        return mock(CheckouterClient.class);
    }

    @Bean
    public RemoteCheckouterUIService checkouterUIService() {
        return mock(RemoteCheckouterUIService.class);
    }

    @Bean
    public BalanceService patientBalanceService() {
        return mock(ExternalBalanceService.class);
    }

    @Bean
    public SecManager secManager() {
        return mock(SecManager.class);
    }

    @Bean("ticketParserTvmClient")
    public TvmClient tvmClient() {
        return mock(TvmClient.class);
    }

    @Bean
    public LogbrokerService samovarLogbrokerService() {
        return mock(LogbrokerService.class);
    }

    @Bean
    public PartnerChangeDao partnerChangeDao(JdbcTemplate jdbcTemplate) {
        return Mockito.spy(new PartnerChangeDao(jdbcTemplate));
    }

    @Bean
    public WarehouseMappingController warehouseMappingController(AsyncReports<ReportsType> reportsService,
                                                                 TransactionTemplate transactionTemplate,
                                                                 WarehouseMappingMdsS3Service mdsS3Service) {
        return new WarehouseMappingController(reportsService, transactionTemplate, mdsS3Service);
    }

    @Bean(name = "checkPendingFilesExecutorService")
    public ScheduledExecutorService checkPendingFilesExecutorService() {
        return mock(ScheduledExecutorService.class);
    }

    @Bean(name = "outletProcessingExecutorService")
    public ExecutorService outletProcessingExecutorService() {
        return mock(ExecutorService.class);
    }

    @Bean
    public StockStorageSearchClient stockStorageSearchClient() {
        return Mockito.mock(StockStorageSearchClient.class);
    }

    @Bean
    public LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public TvmTicketProvider partnerApiDebugTvmTicketProvider() {
        return mock(TvmTicketProvider.class);
    }

    @Bean
    DeliveryRegionBlacklistYtDao deliveryRegionBlacklistYtDao() {
        return mock(DeliveryRegionBlacklistYtDao.class);
    }
}
