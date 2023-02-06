package ru.yandex.market.health.ui;

import java.net.InetSocketAddress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import ma.glasnost.orika.MapperFacade;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;

import ru.yandex.market.clickhouse.ddl.ClickHouseDdlService;
import ru.yandex.market.health.configs.clickphite.ClickphiteConfigDao;
import ru.yandex.market.health.configs.clickphite.validation.query.MetricContextGroupValidator;
import ru.yandex.market.health.configs.logshatter.LogshatterConfigDao;
import ru.yandex.market.health.configs.logshatter.config_history.LogshatterVersionHistoryDao;
import ru.yandex.market.health.ui.config.internal.HealthUiInternalSpringConfig;
import ru.yandex.market.health.ui.features.auth.PermissionsValidator;
import ru.yandex.market.health.ui.features.clickphite_config.ClickphiteConfigController;
import ru.yandex.market.health.ui.features.clickphite_config.service.ClickphiteConfigJsonBuilderService;
import ru.yandex.market.health.ui.features.clickphite_config.service.ClickphiteConfigService;
import ru.yandex.market.health.ui.features.clickphite_config.service.ClickphiteConvertationService;
import ru.yandex.market.health.ui.features.logshatter_config.LogshatterConfigController;
import ru.yandex.market.health.ui.features.logshatter_config.NonAdminValidationHelper;
import ru.yandex.market.health.ui.features.logshatter_config.service.LogshatterConfigJsonBuilderService;
import ru.yandex.market.health.ui.features.logshatter_config.service.LogshatterConfigService;
import ru.yandex.market.tsum.clients.abc.AbcApiClient;
import ru.yandex.market.tsum.clients.blackbox.BlackBoxClient;

@Configuration
@Import({HealthUiInternalSpringConfig.class, ClickphiteConfigService.class, LogshatterConfigService.class})
@PropertySource("classpath:/test.properties")
public class TestConfig {

    @Bean(destroyMethod = "shutdown")
    public MongoServer mongoServer() {
        return new MongoServer(new MemoryBackend());
    }

    @Bean
    public MongoTemplate clickphiteConfigsMongoTemplate(
        MongoServer mongoServer,
        @Value("${mongo.port:#{null}}") Integer mongoPort
    ) {
        return createMongoTemplate(mongoServer, mongoPort);
    }

    @Bean
    public MongoTemplate logshatterConfigsMongoTemplate(
        MongoServer mongoServer,
        @Value("${mongo.port:#{null}}") Integer mongoPort
    ) {
        return createMongoTemplate(mongoServer, mongoPort);
    }

    private MongoTemplate createMongoTemplate(MongoServer mongoServer,
                                              Integer mongoPort) {
        final InetSocketAddress mongoSocketAddress;
        if (mongoPort == null) {
            mongoSocketAddress = mongoServer.bind();
        } else {
            mongoSocketAddress = new InetSocketAddress("localhost", mongoPort);
            mongoServer.bind(mongoSocketAddress);
        }
        return Mockito.spy(
            new MongoTemplate(
                new MongoClient(
                    new ServerAddress(mongoSocketAddress)
                ),
                "db"
            )
        );
    }

    @Bean
    public AbcApiClient abcApiClient() {
        return Mockito.mock(AbcApiClient.class);
    }

    @Bean
    public BlackBoxClient blackBoxClient() {
        return Mockito.mock(BlackBoxClient.class);
    }

    @Bean
    public ClickphiteConvertationService convertationService() {
        return new ClickphiteConvertationService(new ClassPathResource("activeMetrics.json"));
    }

    @Bean
    public ClickphiteConfigController clickphiteConfigController(
        ClickphiteConfigService clickphiteConfigService,
        MapperFacade mapperFacade,
        ClickphiteConfigDao clickphiteConfigDao,
        PermissionsValidator permissionsValidator,
        ClickphiteConvertationService convertationService,
        @Value("${yql.link:https://yql.yandex-team.ru/?query_type=CLICKHOUSE&" +
            "query=use%20marketclickhousetesting;%0A%0A}") String yqlLink
    ) {
        return new ClickphiteConfigController(clickphiteConfigService,
            clickphiteConfigDao,
            mapperFacade,
            permissionsValidator,
            convertationService,
            yqlLink);
    }

    @Bean
    public LogshatterVersionHistoryDao logshatterVersionHistoryDao() {
        return Mockito.mock(LogshatterVersionHistoryDao.class);
    }

    @Bean
    public NonAdminValidationHelper validationHelper(
        PermissionsValidator permissionsValidator,
        MapperFacade mapper,
        LogshatterConfigDao dao,
        @Value("${health.abc.admin.allow-restricted-fields:false}") boolean allowRestrictedFields
    ) {
        return new NonAdminValidationHelper(permissionsValidator, mapper, dao, allowRestrictedFields);
    }

    @Bean
    public LogshatterConfigController logshatterConfigController(
        LogshatterConfigService logshatterConfigService,
        ObjectMapper jacksonObjectMapper,
        LogshatterConfigDao logshatterConfigDao,
        MapperFacade mapperFacade,
        NonAdminValidationHelper validationHelper,
        ClickphiteConfigDao clickphiteConfigDao,
        @Value("${yql.link:https://yql.yandex-team.ru/?query_type=CLICKHOUSE&" +
            "query=use%20marketclickhousetesting;%0A%0A}") String yqlLink
    ) {
        return new LogshatterConfigController(logshatterConfigService,
            jacksonObjectMapper,
            logshatterConfigDao,
            mapperFacade,
            validationHelper,
            clickphiteConfigDao,
            yqlLink
        );
    }

    @Bean
    public ClickHouseDdlService clickHouseDdlService() {
        return Mockito.mock(ClickHouseDdlService.class);
    }

    @Bean
    public MetricContextGroupValidator metricContextGroupValidator() {
        return Mockito.mock(MetricContextGroupValidator.class);
    }

    @Bean
    public ClickphiteConfigJsonBuilderService clickphiteConfigJsonBuilderService(
        ClickphiteConfigDao dao,
        MapperFacade mapper
    ) {
        return new ClickphiteConfigJsonBuilderService(dao, mapper);
    }

    @Bean
    public LogshatterConfigJsonBuilderService logshatterConfigJsonBuilderService(
        LogshatterConfigDao dao,
        MapperFacade mapper
    ) {
        return new LogshatterConfigJsonBuilderService(dao, mapper);
    }
}
