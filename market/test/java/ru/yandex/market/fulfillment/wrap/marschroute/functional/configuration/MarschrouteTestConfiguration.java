package ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration;

import java.util.Arrays;
import java.util.Set;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import ru.yandex.market.fulfillment.wrap.core.api.RequestType;
import ru.yandex.market.fulfillment.wrap.core.configuration.CommonControllersConfiguration;
import ru.yandex.market.fulfillment.wrap.core.configuration.RequestProcessingConfiguration;
import ru.yandex.market.fulfillment.wrap.core.configuration.SecurityConfiguration;
import ru.yandex.market.fulfillment.wrap.core.configuration.xml.XmlMappingConfiguration;
import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenContextHolder;
import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenValidator;
import ru.yandex.market.fulfillment.wrap.core.util.EndpointUrlBuilder;
import ru.yandex.market.fulfillment.wrap.core.util.FulfillmentApiKeyManager;
import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteFulfillmentAPI;
import ru.yandex.market.fulfillment.wrap.marschroute.api.ProductsClient;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.DeliveryOptionProviderConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.DeliveryServiceMetaProviderConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.GeoInformationProviderConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.MarschrouteWrapperConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.client.MarschrouteMonitoredClientConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.MarschrouteProductsResponse;
import ru.yandex.market.fulfillment.wrap.marschroute.notification.Notifier;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.EmailRepository;
import ru.yandex.market.fulfillment.wrap.marschroute.service.MarschrouteProductsService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.delivery.DeliveryServiceMetaCacheLoader;
import ru.yandex.market.fulfillment.wrap.marschroute.service.delivery.DeliveryServiceMetaProvider;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.fulfillment.wrap.marschroute.transformation.LifetimeChecker;
import ru.yandex.market.logistic.api.model.common.request.Token;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;

@Import(value = {ConversionConfiguration.class,
    XmlMappingConfiguration.class,
    CommonControllersConfiguration.class,
    SecurityConfiguration.class,
    RequestProcessingConfiguration.class,
    MarschrouteFulfillmentAPI.class,
    MarschrouteWrapperConfiguration.class,
    DeliveryOptionProviderConfiguration.class,
    GeoInformationProviderConfiguration.class,
    DeliveryServiceMetaProviderConfiguration.class,
    MarschrouteMonitoredClientConfiguration.class,
    LifetimeChecker.class,
})
@Configuration
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class})
@ComponentScan(value = {
    "ru.yandex.market.fulfillment.wrap.marschroute.service",
    "ru.yandex.market.fulfillment.wrap.marschroute.api",
    "ru.yandex.market.fulfillment.wrap.marschroute.repository",
})
@MockBeans({
    @MockBean(Notifier.class),
    @MockBean(GeoInformationProvider.class),
    @MockBean(EmailRepository.class),
    @MockBean(JavaMailSender.class)
})
public class MarschrouteTestConfiguration {

    @Value("${fulfillment.marschroute.api.url}")
    private String apiUrl;

    @Value("${fulfillment.marschroute.api.shooting.url}")
    private String apiShootingUrl;

    @Value("${fulfillment.marschroute.api.key}")
    private String apiKey;

    @Bean(name = "apiEndpointBuilder")
    @Primary
    public EndpointUrlBuilder apiEndpointBuilder() {
        return new EndpointUrlBuilder(apiUrl);
    }

    @Bean(name = "shootingEndpointBuilder")
    @Primary
    public EndpointUrlBuilder shootingEndpointBuilder() {
        return new EndpointUrlBuilder(apiShootingUrl);
    }

    @Bean
    @Primary
    public FulfillmentApiKeyManager apiKeyProvider() {
        return new FulfillmentApiKeyManager(apiKey);
    }

    @Bean
    @Primary
    public DeliveryServiceMetaProvider deliveryServiceMetaProvider(DeliveryServiceMetaCacheLoader cacheLoader) {
        return new DeliveryServiceMetaProvider(
            CacheBuilder.newBuilder()
                .maximumSize(0)
                .build(cacheLoader)
        );
    }

    @Bean
    @Primary
    public MarschrouteProductsService mockProductService(ProductsClient productsClient){
        class MockMarschrouteProductsService extends MarschrouteProductsService{

            public MockMarschrouteProductsService(ProductsClient productsClient) {
                super(productsClient);
            }

            @NotNull
            @Override
            public MarschrouteProductsResponse execute(@NotNull Set<UnitId> unitIds) {
                return new MarschrouteProductsResponse();
            }
        }

        return new MockMarschrouteProductsService(productsClient);
    }

    @Bean
    public TokenContextHolder tokenContextHolder() {
        return new TokenContextHolder();
    }

    @Bean
    public TokenValidator tokenValidator() {
        return new TokenValidator(this::getMap);
    }

    private Multimap<Token, RequestType> getMap() {
        Multimap<Token, RequestType> multimap = ArrayListMultimap.create();
        multimap.putAll(new Token("zawr8kexa3Re7ecrusagus3estesapav4Uph7yavu5achustum4brutep2thatrE"),
            Arrays.asList(RequestType.values()));
        multimap.putAll(new Token("xxxxxxxxxxxxxxxxxxxxxxmarschrouteTokenxxxxxxxxxxxxxxxxxxxxxxxxxx"),
            Arrays.asList(RequestType.values()));
        multimap.putAll(new Token("xxxxxxxxxxxxxxxxxxxxxxxxmarschrouteTokenxxxxxxxxxxxxxxxxxxxxxxxx"),
            Arrays.asList(RequestType.values()));
        multimap.put(new Token("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"),
            RequestType.GET_STOCKS);

        return multimap;
    }

}
