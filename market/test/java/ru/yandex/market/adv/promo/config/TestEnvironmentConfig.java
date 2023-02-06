package ru.yandex.market.adv.promo.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.common.util.terminal.CommandExecutor;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.adv.promo.logbroker.model.DatacampMessageLogbrokerEvent;
import ru.yandex.market.adv.promo.service.environment.CachedEnvironmentService;
import ru.yandex.market.adv.promo.service.environment.EnvironmentService;
import ru.yandex.market.adv.promo.service.environment.dao.EnvironmentSettingDao;
import ru.yandex.market.adv.promo.service.loyalty.client.LoyaltyClient;
import ru.yandex.market.adv.promo.service.promo.statistic.CachedPromoStatisticService;
import ru.yandex.market.adv.promo.service.promo.statistic.PromoStatisticService;
import ru.yandex.market.adv.promo.service.promo.statistic.dao.PromoStatisticDao;
import ru.yandex.market.adv.promo.tms.command.dao.PromoYTDao;
import ru.yandex.market.adv.promo.tms.job.notification.promo_error.dao.PromoErrorYTDao;
import ru.yandex.market.adv.promo.tms.job.notification.recent_promos.dao.AvailablePromosYTDao;
import ru.yandex.market.adv.promo.tms.job.promos.import_promos.dao.PartnerParticipatedPromosYTDao;
import ru.yandex.market.adv.promo.tms.job.promos.import_promos.service.PartnerParticipatedPromosYTHelperService;
import ru.yandex.market.adv.promo.tms.job.promos.statistics.dao.PartnerPromosYTDao;
import ru.yandex.market.adv.promo.tms.yt.YtCluster;
import ru.yandex.market.adv.promo.tms.yt.YtTemplate;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerEventPublisherImpl;
import ru.yandex.market.logbroker.model.LogbrokerCluster;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.RestMbiApiClient;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.misc.thread.executor.SyncExecutor;

import static org.mockito.Mockito.mock;

@Configuration
public class TestEnvironmentConfig {

    @Autowired
    private EnvironmentSettingDao environmentSettingDao;

    @Autowired
    private PromoStatisticDao promoStatisticDao;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurerForTests() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocations(new ClassPathResource("functional-test.properties"));
        return configurer;
    }

    /**
     * Моки сущностей, взаимодействующих с YT.
     */

    @Bean
    public YtTemplate promoYtTemplate(
            @Value("#{'${yt.adv.promo.hosts}'.split(',')}") List<String> promoYtHosts
    ) {
        return new YtTemplate(new YtCluster[]{
                new YtCluster(promoYtHosts.get(0), mock(Yt.class)),
                new YtCluster(promoYtHosts.get(1), mock(Yt.class))
        });
    }

    @Bean
    public NamedParameterJdbcTemplate yqlNamedParameterJdbcTemplate() {
        return mock(NamedParameterJdbcTemplate.class);
    }

    @Bean
    public PromoErrorYTDao promoErrorYTDao() {
        return mock(PromoErrorYTDao.class);
    }

    @Bean
    public PromoYTDao promoYTDao() {
        return mock(PromoYTDao.class);
    }

    @Bean
    public AvailablePromosYTDao availableRecentPromosYTDao() {
        return mock(AvailablePromosYTDao.class);
    }

    @Bean
    public PartnerParticipatedPromosYTDao partnerParticipatedPromosYTDao() {
        return mock(PartnerParticipatedPromosYTDao.class);
    }

    @Bean
    public PartnerPromosYTDao partnerCashbackPromosYTDao() {
        return mock(PartnerPromosYTDao.class);
    }

    @Bean
    public PartnerParticipatedPromosYTHelperService ytHelperService() {
        return mock(PartnerParticipatedPromosYTHelperService.class);
    }

    @Bean("idxOffersYtTemplate")
    public YtTemplate idxOffersYtTemplate(
            @Value("#{${idx.offers.table.by.yt.host}}") Map<String, String> idxOffersTablePathByYtHost
    ) {
        return new YtTemplate(new YtCluster[]{
                new YtCluster(idxOffersTablePathByYtHost.keySet().iterator().next(), mock(Yt.class))
        });
    }

    /**
     * Моки для внешних сервисов и клиентов.
     */

    @Bean
    @Primary
    public MbiApiClient mbiApiClient() {
        return mock(RestMbiApiClient.class);
    }

    @Bean
    public DataCampClient dataCampClient() {
        return mock(DataCampClient.class);
    }

    @Bean
    public LoyaltyClient loyaltyClient() {
        return mock(LoyaltyClient.class);
    }

    @Bean
    @Primary
    public LogbrokerEventPublisher<DatacampMessageLogbrokerEvent> logbrokerPublisher() {
        return mock(LogbrokerEventPublisherImpl.class);
    }

    @Bean
    public LogbrokerCluster logbrokerCluster() {
        return mock(LogbrokerCluster.class);
    }

    @Bean
    public SaasService saasService() {
        return mock(SaasService.class);
    }

    @Bean
    public EnvironmentService environmentService() {
        return new EnvironmentService(environmentSettingDao);
    }

    @Bean
    public CachedEnvironmentService cachedEnvironmentService() {
        return new CachedEnvironmentService(environmentSettingDao);
    }

    @Bean
    public PromoStatisticService promoStatisticService() {
        return new PromoStatisticService(promoStatisticDao);
    }

    @Bean
    public CachedPromoStatisticService cachedPromoStatisticService() {
        return new CachedPromoStatisticService(promoStatisticDao);
    }

    @Bean
    public AsyncMarketReportService marketReportService() {
        return mock(AsyncMarketReportService.class);
    }

    /**
     * Моки для executor'ов.
     */

    @Bean
    public ExecutorService currentAndFuturePromosExecutor() {
        return new SyncExecutor();
    }

    @Bean
    public ExecutorService offerPriceInfoExecutorService() {
        return new SyncExecutor();
    }

    @Bean
    public ExecutorService assortmentValidationExecutor() {
        return new SyncExecutor();
    }

    @Bean
    public ScheduledExecutorService validationRestartExecutor() {
        return mock(ScheduledExecutorService.class);
    }

    /**
     * Моки для прочих сущностей, которых не хватает в тестовом конфиге.
     */

    @Bean
    public RetryTemplate retryTemplate() {
        return new RetryTemplate();
    }

    @Bean
    public CommandExecutor commandExecutor() {
        return mock(CommandExecutor.class);
    }

    @Bean
    public Terminal terminal() {
        return mock(Terminal.class);
    }
}
