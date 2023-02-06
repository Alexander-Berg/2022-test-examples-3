package ru.yandex.market.pers.address.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.IdentityHashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.TextFormat;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import yandex.maps.proto.arrival.Arrival;
import yandex.maps.proto.common2.response.ResponseOuterClass;
import yandex.maps.proto.search.geocoder.Geocoder;
import yandex.maps.proto.search.geocoder_internal.GeocoderInternal;
import yandex.maps.proto.search.search.Search;
import yandex.maps.proto.search.search_internal.SearchInternal;
import yandex.maps.proto.uri.Uri;
import ru.yandex.common.util.collections.Maybe;
import ru.yandex.common.util.geocoder.GeoClient;
import ru.yandex.common.util.geocoder.GeoSearchApiClient;
import ru.yandex.common.util.geocoder.TvmTicketProvider;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTreePlainTextBuilder;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.loyalty.lightweight.ExceptionUtils;
import ru.yandex.market.loyalty.monitoring.JugglerApplicationConfig;
import ru.yandex.market.loyalty.trace.Tracer;
import ru.yandex.market.pers.address.services.GeocoderService;
import ru.yandex.market.pers.address.services.PassportDataSyncClient;
import ru.yandex.market.pers.address.services.blackbox.BlackboxClient;
import ru.yandex.market.pers.address.services.monitor.Monitor;
import ru.yandex.market.pers.address.tvm.TvmClient;
import ru.yandex.market.pers.address.tvm.TvmClientsRegistry;
import ru.yandex.market.pers.address.util.MarketDataSyncClientTestImpl;
import ru.yandex.market.sdk.userinfo.service.UserInfoService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@Configuration
public class MockConfigurer {
    private static final Logger logger = LogManager.getLogger(MockConfigurer.class);

    @Mocks
    @Bean
    public Map<Object, Runnable> mocks() {
        return new IdentityHashMap<>();
    }

    @Configuration
    public static class UserInfoServiceConfig {

        @Blackbox
        @Bean
        public UserInfoService userInfoService(@Mocks Map<Object, Runnable> mocks) {
            UserInfoService mock = Mockito.mock(UserInfoService.class);
            mocks.put(mock, () -> {
            });
            return mock;
        }
    }

    @Configuration
    public static class BlackboxConfig {
        @Blackbox
        @Bean
        protected RestTemplate blackboxRestTemplate(@Mocks Map<Object, Runnable> mocks) {
            RestTemplate mock = Mockito.mock(RestTemplate.class);
            mocks.put(mock, () -> {
            });
            return mock;
        }

        @Bean
        public BlackboxClient blackboxClient(
                @Value("${market.pers.address.blackbox.url}") String blackBoxUrl,
                @Blackbox RestTemplate restTemplate,
                @Blackbox UserInfoService userInfoService
        ) {
            return new BlackboxClient(blackBoxUrl, restTemplate, userInfoService);
        }
    }

    @Configuration
    public static class DataSyncConfig {
        @Bean
        public MarketDataSyncClientTestImpl dataSyncClient() {
            return new MarketDataSyncClientTestImpl();
        }

        @PassportDataSync
        @Bean
        public RestTemplate passportRestTemplateMock(@Mocks Map<Object, Runnable> mocks) {
            RestTemplate mock = Mockito.mock(RestTemplate.class);
            mocks.put(mock, () -> {
            });
            return mock;
        }


        @Bean
        public PassportDataSyncClient passportDataSyncClient(
                TvmClient tvmClient,
                @Value("${passport.datasync.url}") String datasyncUrl,
                @PassportDataSync RestTemplate restTemplate
        ) {
            return new PassportDataSyncClient(tvmClient, datasyncUrl, restTemplate);
        }
    }

    @Configuration
    public static class GeoExportConfig {
        @Bean
        public RegionService regionService(
                @InternalGeoExport RegionTreePlainTextBuilder internalRegionTreePlainTextBuilder
        ) {
            RegionService regionService = new RegionService();
            regionService.setRegionTreeBuilder(internalRegionTreePlainTextBuilder);
            return regionService;
        }
    }

    @Configuration
    public static class TvmConfig {
        @Bean
        public TvmClient tvmClient(@Mocks Map<Object, Runnable> mocks) {
            final TvmClient mock = Mockito.mock(TvmClient.class);
            mocks.put(mock, () -> {
            });
            return mock;
        }

        @Bean
        public TvmClientsRegistry tvmClientsRegistry(@Mocks Map<Object, Runnable> mocks) {
            final TvmClientsRegistry mock = Mockito.mock(TvmClientsRegistry.class);
            mocks.put(mock, () -> {
            });
            return mock;
        }
    }


    @Configuration
    public static class GeoCoderConfig {
        private static final ExtensionRegistry REGISTRY = ExtensionRegistry.newInstance();

        static {
            REGISTRY.add(Uri.gEOOBJECTMETADATA);
            REGISTRY.add(Search.gEOOBJECTMETADATA);
            REGISTRY.add(Arrival.gEOOBJECTMETADATA);
            REGISTRY.add(Search.rESPONSEMETADATA);
            REGISTRY.add(Geocoder.gEOOBJECTMETADATA);
            REGISTRY.add(Geocoder.rESPONSEMETADATA);
            REGISTRY.add(GeocoderInternal.rESPONSEINFO);
            REGISTRY.add(SearchInternal.rESPONSEINFO);
            REGISTRY.add(GeocoderInternal.tOPONYMINFO);
        }

        @Value("${market.pers.maps.geocoder.url}")
        private String geoCoderBaseUrl;

        @Autowired
        private ResourceLoader resourceLoader;

        @Bean
        public GeoCoderMock geoCoderMock(@Mocks Map<Object, Runnable> mocks) {
            GeoCoderMock mock = Mockito.mock(GeoCoderMock.class);
            mocks.put(mock, () -> {
            });
            return mock;
        }

        @Bean
        public GeoClient geoClient(GeoCoderMock geoCoderMock, TvmTicketProvider tvmTicketProvider) throws IOException {
            Map<GeoCoderMock.Response, String> urlToResponseMap = ImmutableMap.<GeoCoderMock.Response, String>builder()
                    .put(GeoCoderMock.Response.TVERSKAI_6, getResponseByResource("geo.tverskaya-6.txt"))
                    .put(GeoCoderMock.Response.TOLSTOGO_16, getResponseByResource("geo.lva-tolstogo-16.txt"))
                    .put(GeoCoderMock.Response.GRUZINSKAYA_12, getResponseByResource("geo.b-gruzinskaya-12.txt"))
                    .put(GeoCoderMock.Response.BOLSHAYA_CHEREMUSHKINSKAYA_11_W1, getResponseByResource("geo" +
                            ".b-cheremushkinskaya-11wing1.txt"))
                    .put(GeoCoderMock.Response.BOLSHAYA_CHEREMUSHKINSKAYA_11_W2, getResponseByResource("geo" +
                            ".b-cheremushkinskaya-11wing2.txt"))
                    .put(GeoCoderMock.Response.PROFSOYUZNAYA_146, getResponseByResource("geo.profsoyuznaya-146.txt"))
                    .put(GeoCoderMock.Response.ZARECHNAYA_12, getResponseByResource("geo.zarechnaya-12.txt"))
                    .put(GeoCoderMock.Response.TWO_GEO_OBJECTS, getResponseByResource("two_geo_objects.txt"))
                    .put(GeoCoderMock.Response.BAD_PRECISION, getResponseByResource("bad_precision.txt"))
                    .put(GeoCoderMock.Response.NEAR_PRECISION, getResponseByResource("near_precision.txt"))
                    .put(GeoCoderMock.Response.PRECISION_NEAR_BAD_ACCURACY, getResponseByResource(
                            "precision_near_bad_accuracy.txt"))
                    .build();


            GeoSearchApiClient.Config config = new GeoSearchApiClient.Config(tvmTicketProvider);
            config.setApiBaseUrl(geoCoderBaseUrl);
            config.setCacheDisabled(true);
            return new GeoSearchApiClient(config) {
                @Override
                protected Maybe<byte[]> requestGeocoderSafe(String url) {
                    logger.info("Requested url: {}", url);
                    String request = ExceptionUtils.makeExceptionsUnchecked(() -> URLEncodedUtils
                            .parse(new URI(url), "UTF-8")
                            .stream()
                            .filter(param -> param.getName().equals("text"))
                            .findFirst()
                            .orElseThrow(AssertionError::new)
                            .getValue()
                    );

                    GeoCoderMock.Response geoCoderResponse = geoCoderMock.find(request);
                    logger.info("mock returns {} for {}", geoCoderResponse, request);
                    assertNotNull(geoCoderResponse, "mock not configured for request " + request);
                    if (geoCoderResponse == GeoCoderMock.Response.FAIL_OR_NOTHING) {
                        logger.info("geocoder returns nothing on fail");
                        return Maybe.nothing();
                    }
                    ResponseOuterClass.Response.Builder response = ResponseOuterClass.Response.newBuilder();
                    try {
                        TextFormat.merge(urlToResponseMap.get(geoCoderResponse), REGISTRY, response);
                    } catch (TextFormat.ParseException e) {
                        throw new IllegalStateException("Error reading mock geo response from file", e);
                    }
                    return Maybe.just(response.build().toByteArray());
                }
            };
        }

        private String getResponseByResource(String location) throws IOException {
            try (InputStream is = resourceLoader.getResource("classpath:" + location).getInputStream()) {
                return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
            }
        }
    }

    @Configuration
    public static class CheckouterClientConfig {
        @Bean
        public CheckouterClient getCheckouterClientMock(@Mocks Map<Object, Runnable> mocks) {
            CheckouterClient result = Mockito.mock(CheckouterClient.class);
            mocks.put(result, () -> {
            });
            return result;
        }
    }

    @Bean
    public JugglerApplicationConfig applicationConfig(
    ) {
        return null;
    }

    @Configuration
    public static class GeobaseRestTemplateConfig {
        @Bean
        public RestTemplate geobaseRestTemplate(@Mocks Map<Object, Runnable> mocks) {
            RestTemplate result = Mockito.mock(RestTemplate.class);
            given(result.getForObject(anyString(), any())).willReturn(-1);
            mocks.put(result, () -> {
            });
            return result;
        }
    }
}
