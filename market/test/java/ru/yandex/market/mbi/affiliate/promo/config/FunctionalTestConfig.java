package ru.yandex.market.mbi.affiliate.promo.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.mbi.affiliate.promo.dao.CatalogCategoriesDao;
import ru.yandex.market.mbi.affiliate.promo.dao.CatalogPromoDao;
import ru.yandex.market.mbi.affiliate.promo.dao.GeneratedPromocodesDao;
import ru.yandex.market.mbi.affiliate.promo.dao.PartnerDao;
import ru.yandex.market.mbi.affiliate.promo.dao.PartnerGroupDao;
import ru.yandex.market.mbi.affiliate.promo.dao.PromoDao;
import ru.yandex.market.mbi.affiliate.promo.dao.VarsDao;
import ru.yandex.market.mbi.affiliate.promo.distribution.DistributionPlaceClient;
import ru.yandex.market.mbi.affiliate.promo.distribution.DistributionReportClient;
import ru.yandex.market.mbi.affiliate.promo.random.RandomStringGenerator;
import ru.yandex.market.mbi.affiliate.promo.service.LoyaltyClientWrapper;
import ru.yandex.market.mbi.affiliate.promo.service.PartnerService;
import ru.yandex.market.mbi.affiliate.promo.service.PromocodeGenerationService;
import ru.yandex.market.mbi.affiliate.promo.stroller.DataCampStrollerClient;
import ru.yandex.market.mbi.affiliate.promo.stroller.StrollerJsonApi;
import ru.yandex.market.mbi.affiliate.promo.stroller.StrollerProtoApi;

import static org.mockito.Mockito.mock;

@Configuration
@Import(DatabaseConfig.class)
public class FunctionalTestConfig {
    public final LocalDateTime clockDateTime = LocalDateTime.of(2021, 8, 10, 14, 43, 11);

    @Bean
    public StrollerProtoApi strollerProtoApi() {
        return mock(StrollerProtoApi.class, Mockito.RETURNS_DEEP_STUBS);
    }

    @Bean
    public StrollerJsonApi strollerJsonApi() {
        return mock(StrollerJsonApi.class, Mockito.RETURNS_DEEP_STUBS);
    }

    @Bean
    public DataCampStrollerClient dataCampStrollerClient(StrollerProtoApi protoApi, StrollerJsonApi jsonApi) {
        return new DataCampStrollerClient(protoApi, jsonApi, 1, 1000);
    }

    @Bean
    public DistributionReportClient distributionReportClient() {
        return mock(DistributionReportClient.class);
    }

    @Bean
    public DistributionPlaceClient distributionPlaceClient() {
        return mock(DistributionPlaceClient.class);
    }

    @Bean
    public MarketLoyaltyClient marketLoyaltyClient() {
        return mock(MarketLoyaltyClient.class);
    }

    @Bean
    public PromoDao promoDao(NamedParameterJdbcTemplate postgresJdbcTemplate, Clock clock) {
        return new PromoDao(postgresJdbcTemplate, clock);
    }

    @Bean
    public PartnerDao partnerDao(NamedParameterJdbcTemplate postgresJdbcTemplate) {
        return new PartnerDao(postgresJdbcTemplate);
    }

    @Bean
    public PartnerGroupDao partnerGroupDao(NamedParameterJdbcTemplate postgresJdbcTemplate,
                                 TransactionTemplate transactionTemplate) {
        return new PartnerGroupDao(postgresJdbcTemplate, transactionTemplate);
    }

    @Bean
    public CatalogPromoDao catalogPromoDao(
            NamedParameterJdbcTemplate postgresJdbcTemplate, TransactionTemplate transactionTemplate) {
        return new CatalogPromoDao(postgresJdbcTemplate, transactionTemplate);
    }

    @Bean
    public CatalogCategoriesDao catalogCategoriesDao(
            NamedParameterJdbcTemplate postgresJdbcTemplate, TransactionTemplate transactionTemplate) {
        return new CatalogCategoriesDao(postgresJdbcTemplate, transactionTemplate);
    }

    @Bean
    public VarsDao varsDao(NamedParameterJdbcTemplate postgresJdbcTemplate) {
        return new VarsDao(postgresJdbcTemplate);
    }

    @Bean
    public GeneratedPromocodesDao generatedPromocodesDao(NamedParameterJdbcTemplate postgresJdbcTemplate) {
        return new GeneratedPromocodesDao(postgresJdbcTemplate);
    }

    @Bean
    public PartnerService partnerService(
            PartnerDao partnerDao,
            PartnerGroupDao partnerGroupDao,
            DistributionPlaceClient distributionPlaceClient,
            DistributionReportClient distributionReportClient) {
        return new PartnerService(partnerDao, partnerGroupDao, distributionPlaceClient, distributionReportClient);
    }

    @Bean
    public RandomStringGenerator mockRandomStringGenerator() {
        return mock(RandomStringGenerator.class);
    }

    @Bean
    public PromocodeGenerationService promocodeGenerationService(
            PromoDao promoDao,
            PartnerDao partnerDao,
            GeneratedPromocodesDao generatedPromocodesDao,
            RandomStringGenerator mockRandomStringGenerator,
            MarketLoyaltyClient marketLoyaltyClient,
            DataCampStrollerClient dataCampStrollerClient,
            TransactionTemplate transactionTemplate,
            Clock clock

    ) {
        return new PromocodeGenerationService(
                promoDao, partnerDao, generatedPromocodesDao,
                mockRandomStringGenerator,
                new LoyaltyClientWrapper(marketLoyaltyClient),
                dataCampStrollerClient,
                transactionTemplate,
                clock
        );
    }

    @Bean
    public Clock clock() {
        ZoneId zone = ZoneId.of("Europe/Moscow");
        return Clock.fixed(clockDateTime.atZone(zone).toInstant(), zone);
    }
}
