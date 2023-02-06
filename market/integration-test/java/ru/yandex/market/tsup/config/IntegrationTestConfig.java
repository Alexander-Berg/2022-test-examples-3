package ru.yandex.market.tsup.config;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.delivery.gruzin.client.GruzinClient;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.javaframework.clients.config.ClientsAutoConfiguration;
import ru.yandex.market.javaframework.security.config.MjWebSecurityAutoConfiguration;
import ru.yandex.market.javaframework.tvm.config.TraceApacheHttpClientAutoConfiguration;
import ru.yandex.market.javaframework.tvm.config.TvmModuleConfigurersAutoConfiguration;
import ru.yandex.market.javaframework.tvm.config.TvmSecurityAutoConfiguration;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.starter.properties.tvm.TvmProperties;
import ru.yandex.market.starter.tvm.config.TvmPropertiesAutoConfiguration;
import ru.yandex.market.tpl.common.db.queue.base.BaseQueueProducer;
import ru.yandex.market.tsup.audit.AuditController;
import ru.yandex.market.tsup.config.external.calendaring_service.CalendaringServiceConfig;
import ru.yandex.market.tsup.config.external.gruzin.GruzinConfig;
import ru.yandex.market.tsup.config.external.lms.LmsConfig;
import ru.yandex.market.tsup.config.external.tm.TmConfig;
import ru.yandex.market.tsup.config.internal.DataStorageProperties;
import ru.yandex.market.tsup.config.internal.JacksonConfig;
import ru.yandex.market.tsup.config.internal.LiquibaseConfig;
import ru.yandex.market.tsup.config.internal.MyBatisConfig;
import ru.yandex.market.tsup.config.internal.ServletConfig;
import ru.yandex.market.tsup.config.internal.TsupTraceConfig;
import ru.yandex.market.tsup.config.internal.TsupWebMvcConfiguration;
import ru.yandex.market.tsup.config.internal.dbqueue.DbQueueConfig;
import ru.yandex.market.tsup.config.internal.dbqueue.DbQueueProperties;
import ru.yandex.market.tsup.core.pipeline.PipelineConfigProvider;
import ru.yandex.market.tsup.service.provider.PipelineCubeProvider;
import ru.yandex.market.tsup.service.provider.PipelineProvider;
import ru.yandex.market.tsup.service.user_process.EntityAuthorService;
import ru.yandex.mj.generated.client.carrier.api.CompanyApiClient;
import ru.yandex.mj.generated.client.carrier.api.DeliveryServiceApiClient;
import ru.yandex.mj.generated.client.carrier.api.DutyApiClient;
import ru.yandex.mj.generated.client.carrier.api.DutyScheduleApiClient;
import ru.yandex.mj.generated.client.carrier.api.LocationApiClient;
import ru.yandex.mj.generated.client.carrier.api.RatingApiClient;
import ru.yandex.mj.generated.client.carrier.api.RoutePointApiClient;
import ru.yandex.mj.generated.client.carrier.api.RoutingTransportScheduleRuleApiClient;
import ru.yandex.mj.generated.client.carrier.api.RoutingTransportTypeApiClient;
import ru.yandex.mj.generated.client.carrier.api.RunApiClient;
import ru.yandex.mj.generated.client.carrier.api.TransportApiClient;
import ru.yandex.mj.generated.client.carrier.api.UserApiClient;
import ru.yandex.mj.generated.client.carrier.model.DeliveryServiceSuggest;
import ru.yandex.mj.generated.client.carrier.model.PageOfDeliveryServiceSuggest;
import ru.yandex.mj.generated.client.routing.api.RoutingApiClient;
import ru.yandex.passport.tvmauth.TvmClient;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Import({
    DbUnitTestConfiguration.class,
    IntegrationTestClockConfig.class,
    MyBatisConfig.class,
    LiquibaseConfig.class,
    DbQueueConfig.class,
    DbQueueProperties.class,
    LmsConfig.class,
    TmConfig.class,
    GruzinConfig.class,
    CalendaringServiceConfig.class,
    DatasourceConfiguration.class,
    DataStorageProperties.class,
    RedisConfiguration.class,
    ServletConfig.class,
    PipelineConfiguration.class,
    IntegrationTestCacheInvalidationStrategyConfig.class,
    JacksonConfig.class,
    TsupTraceConfig.class,
    EmbeddedPostgresConfiguration.class,
    TsupWebMvcConfiguration.class,
})
@SpyBean({
    BaseQueueProducer.class,
    PipelineProvider.class,
    PipelineCubeProvider.class,
    PipelineConfigProvider.class,
    EntityAuthorService.class
})
@MockBean({
    LMSClient.class,
    TransportManagerClient.class,
    GruzinClient.class,
    LocationApiClient.class,
    RunApiClient.class,
    DutyApiClient.class,
    DutyScheduleApiClient.class,
    RoutePointApiClient.class,
    CompanyApiClient.class,
    TransportApiClient.class,
    UserApiClient.class,
    TvmClient.class,
    RoutingApiClient.class,
    CalendaringServiceClientApi.class,
    RoutingTransportTypeApiClient.class,
    RoutingTransportScheduleRuleApiClient.class,
    RatingApiClient.class
})
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    ClientsAutoConfiguration.class,
    MjWebSecurityAutoConfiguration.class,
    TvmPropertiesAutoConfiguration.class,
    TvmModuleConfigurersAutoConfiguration.class,
    TvmSecurityAutoConfiguration.class,
    TraceApacheHttpClientAutoConfiguration.class
})
public class IntegrationTestConfig {

    @Bean
    public Module sourceModule() {
        return Module.MARKET_TSUP;
    }

    @Bean
    @ConfigurationProperties("mj.tvm")
    public TvmProperties mjTvmProperties() {
        return new TvmProperties();
    }

    @Bean
    public DeliveryServiceApiClient deliveryServiceApiClient() {
        var client = Mockito.mock(DeliveryServiceApiClient.class);

        ExecuteCall<PageOfDeliveryServiceSuggest, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(
                CompletableFuture.completedFuture(page(List.of(
                        new DeliveryServiceSuggest().name("ФКК Груп").id(128371L),
                        new DeliveryServiceSuggest().name("ЭМСК").id(128737L),
                        new DeliveryServiceSuggest().name("Монополия").id(22153352L),
                        new DeliveryServiceSuggest().name("ВелесТорг").id(22154342L),
                        new DeliveryServiceSuggest().name("Авто-ПЭК").id(22158040L),
                        new DeliveryServiceSuggest().name("ИвТЭК").id(147949L),
                        new DeliveryServiceSuggest().name("ООО Туда-Сюда").id(15072021L),
                        new DeliveryServiceSuggest().name("Альтика М").id(147208L),
                        new DeliveryServiceSuggest().name("К2 Логистик").id(147939L),
                        new DeliveryServiceSuggest().name("ИТЕКО Россия").id(147946L),
                        new DeliveryServiceSuggest().name("Делко").id(147947L),
                        new DeliveryServiceSuggest().name("ТК Движение").id(116079L),
                        new DeliveryServiceSuggest().name("ГетКарго").id(147951L),
                        new DeliveryServiceSuggest().name("ИСТВАРД").id(138585L),
                        new DeliveryServiceSuggest().name("ГЛТ Москва").id(147943L),
                        new DeliveryServiceSuggest().name("ООО \"ВЕЗУ.РУ\"").id(148088L),
                        new DeliveryServiceSuggest().name("ООО \"ПВ Логистик\"").id(183962L),
                        new DeliveryServiceSuggest().name("ООО \"ЗЕФА-М\"").id(183974L),
                        new DeliveryServiceSuggest().name("ООО ТК \"Литон\"").id(184753L),
                        new DeliveryServiceSuggest().name("АТЛ РЕГИОНЫ").id(184773L)
                )))
        );

        Mockito.when(client.internalDeliveryServicesSuggestGet(
                Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any()
        )).thenReturn(call);

        return client;
    }

    @Bean
    public AuditController auditController() {
        return new AuditController();
    }

    private static PageOfDeliveryServiceSuggest page(List<DeliveryServiceSuggest> dtos) {
        return new PageOfDeliveryServiceSuggest()
                .content(dtos)
                .totalPages(0)
                .number(0)
                .totalElements((long) dtos.size())
                .size(dtos.size());
    }
}
