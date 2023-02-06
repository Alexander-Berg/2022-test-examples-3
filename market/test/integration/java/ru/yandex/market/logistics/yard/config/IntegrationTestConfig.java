package ru.yandex.market.logistics.yard.config;

import java.io.StringReader;
import java.time.Clock;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.yandex.market.ff.client.dto.RequestStatusChangesDto;
import ru.yandex.market.logbroker.consumer.util.LbParser;
import ru.yandex.market.logbroker.producer.SimpleAsyncProducer;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;
import ru.yandex.market.logistics.les.client.component.sqs.SqsRequestTraceTskvLogger;
import ru.yandex.market.logistics.les.client.configuration.properties.TraceProperties;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.LiquibaseTestConfiguration;
import ru.yandex.market.logistics.yard.client.dto.event.read.YardClientEventPayload;
import ru.yandex.market.logistics.yard.config.logbroker.FfwfEventConsumerConfiguration;
import ru.yandex.market.logistics.yard.config.logbroker.LogbrokerClientEventConsumerConfiguration;
import ru.yandex.market.logistics.yard.config.logbroker.RequestStatusHandlerConsumer;
import ru.yandex.market.logistics.yard.service.env.SystemPropertyService;
import ru.yandex.market.logistics.yard.service.event.DtoLbParser;
import ru.yandex.market.logistics.yard_v2.config.HystrixConfiguration;
import ru.yandex.market.logistics.yard_v2.config.LibraryConfig;
import ru.yandex.market.logistics.yard_v2.config.MyBatisConfig;
import ru.yandex.market.logistics.yard_v2.config.QueueShardConfiguration;
import ru.yandex.market.logistics.yard_v2.config.les.SqsSCEventConsumer;
import ru.yandex.market.logistics.yard_v2.config.logbroker.LogbrokerObjectMapperConfig;
import ru.yandex.market.logistics.yard_v2.config.logbroker.LogbrokerProducerProvider;
import ru.yandex.market.logistics.yard_v2.config.logbroker.TopicSpecificLogbrokerProducerProvider;
import ru.yandex.market.logistics.yard_v2.dbqueue.unparsed_logbroker_events.UnparsedLogbrokerEventsProducer;
import ru.yandex.market.logistics.yard_v2.domain.service.pass.PassService;
import ru.yandex.market.logistics.yard_v2.facade.CourierFacade;
import ru.yandex.market.logistics.yard_v2.facade.RouteIdToPartnerIdFacade;
import ru.yandex.market.logistics.yard_v2.facade.ServiceFacadeInterface;
import ru.yandex.market.logistics.yard_v2.facade.ShopRequestInfoFacade;
import ru.yandex.market.logistics.yard_v2.facade.TripPointInfoFacade;
import ru.yandex.market.logistics.yard_v2.logbroker.producer.LogbrokerPublishingService;
import ru.yandex.market.logistics.yard_v2.repository.mapper.ServiceParamMapper;
import ru.yandex.market.logistics.yard_v2.repository.mapper.YardClientInfoMapper;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@Import({
        TestDataSourceConfiguration.class,
        LiquibaseTestConfiguration.class,
        DbUnitTestConfiguration.class,
        TestClockConfig.class,
        JpaConfiguration.class,
        DatabaseConnectionConfig.class,
        LibraryConfig.class,
        MyBatisConfig.class,
        HystrixConfiguration.class,
        QueueShardConfiguration.class,
        LogbrokerObjectMapperConfig.class,
        MbiApiClientConfig.class
})
@EnableWebMvc
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan({
        "ru.yandex.market.logistics.yard.service",
        "ru.yandex.market.logistics.yard.repository",
        "ru.yandex.market.logistics.yard.controller",
        "ru.yandex.market.logistics.yard_v2.controller",
        "ru.yandex.market.logistics.yard_v2.configurator",
        "ru.yandex.market.logistics.yard_v2.converter",
        "ru.yandex.market.logistics.yard_v2.dbqueue.*",
        "ru.yandex.market.logistics.yard_v2.dbqueue.property",
        "ru.yandex.market.logistics.yard_v2.dbqueue.task",
        "ru.yandex.market.logistics.yard_v2.domain",
        "ru.yandex.market.logistics.yard_v2.facade",
        "ru.yandex.market.logistics.yard_v2.repository",
        "ru.yandex.market.logistics.yard_v2.time",
        "ru.yandex.market.logistics.yard_v2.validator",
        "ru.yandex.market.logistics.yard_v2.health",
        "ru.yandex.market.logistics.yard_v2.logbroker",
        "ru.yandex.market.logistics.yard_v2.external.service",
        "ru.yandex.market.logistics.yard_v2.config.sms",
        "ru.yandex.market.logistics.yard_v2.security",
        "ru.yandex.market.logistics.yard_v2.external.pass_connector.dummy",
        "ru.yandex.market.logistics.yard_v2.lms",
        "ru.yandex.market.logistics.yard_v2.market_tpl",
})

@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
public class IntegrationTestConfig {

    private final Logger logger = LoggerFactory.getLogger(IntegrationTestConfig.class);
    @Bean
    @Qualifier("testMainObjectMapper")
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    @Bean(name = "lbObjectMapper")
    public ObjectMapper lbObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(NON_ABSENT);

        return objectMapper;
    }

    @Bean
    public LbParser<YardClientEventPayload> yardClientEventDtoLbParser(
            @Qualifier("lbObjectMapper") ObjectMapper objectMapper,
            UnparsedLogbrokerEventsProducer unparsedLogbrokerEventsProducer) {
        return new DtoLbParser<>(
                LogbrokerClientEventConsumerConfiguration.ENTITY_NAME,
                objectMapper,
                YardClientEventPayload.class,
                unparsedLogbrokerEventsProducer
        );
    }

    @Bean
    public LogbrokerProducerProvider logbrokerProducerProvider(SimpleAsyncProducer mockedSimpleAsyncProducer)
            throws Exception {
        var mock = Mockito.mock(LogbrokerProducerProvider.class);
        var providerProviderMock = mock(TopicSpecificLogbrokerProducerProvider.class);
        when(providerProviderMock.asyncProducer()).thenReturn(mockedSimpleAsyncProducer);
        when(mock.provide(any())).thenReturn(providerProviderMock);
        return mock;
    }

    @Bean
    public SimpleAsyncProducer mockedSympleAsyncProducer() {
        return Mockito.mock(SimpleAsyncProducer.class);
    }

    @Bean
    @Primary
    public LogbrokerPublishingService logbrokerPublishingService(LogbrokerPublishingService bean) {
        return Mockito.spy(bean);
    }

    @Bean
    @Primary
    @Qualifier("passportSmsClient")
    public HttpClient passportSmsClient() throws Exception {

        SAXReader reader = new SAXReader();
        Element rootElement = reader.read(new StringReader("<doc><message-sent id=\"123\"/></doc>")).getRootElement();
        HttpClient client = mock(HttpClient.class);
        Mockito.when(client.execute(any(HttpUriRequest.class), any(ResponseHandler.class))).thenReturn(rootElement);
        return client;
    }

    @Bean
    SqsSCEventConsumer sqsSCEventConsumer(
            RouteIdToPartnerIdFacade routeIdToPartnerIdFacade,
            TripPointInfoFacade tripPointInfoFacade,
            CourierFacade courierFacade,
            PassService passService,
            ServiceParamMapper serviceParamMapper,
            Clock clock,
            SystemPropertyService systemPropertyService,
            YardClientInfoMapper yardClientInfoMapper,
            ServiceFacadeInterface serviceFacade
            ) {

        return new SqsSCEventConsumer(new SqsRequestTraceTskvLogger(new TraceProperties(), clock, logger),
                routeIdToPartnerIdFacade,
                tripPointInfoFacade,
                courierFacade,
                passService,
                serviceParamMapper,
                systemPropertyService,
                yardClientInfoMapper,
                serviceFacade);
    }

    @Bean
    RequestStatusHandlerConsumer requestStatusHandlerConsumer(ShopRequestInfoFacade shopRequestInfoFacade) {
        return new RequestStatusHandlerConsumer(shopRequestInfoFacade);
    }

    @Bean
    LbParser<RequestStatusChangesDto> requestStatusDtoLbParser(
            @Qualifier("lbObjectMapper") ObjectMapper objectMapper,
            UnparsedLogbrokerEventsProducer unparsedLogbrokerEventsProducer) {
        return new DtoLbParser(
                FfwfEventConsumerConfiguration.ENTITY_NAME,
                objectMapper,
                RequestStatusChangesDto.class,
                unparsedLogbrokerEventsProducer
        );
    }

    @Bean
    @Primary
    @Qualifier("pechkinHttpClient")
    public PechkinHttpClient pechkinHttpClient() {
        PechkinHttpClient mock = Mockito.mock(PechkinHttpClient.class);

        Mockito.doNothing().when(mock).sendMessage(any(MessageDto.class));
        return mock;
    }
}
