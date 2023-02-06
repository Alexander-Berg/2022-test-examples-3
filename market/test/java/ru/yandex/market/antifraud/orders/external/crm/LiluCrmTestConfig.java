package ru.yandex.market.antifraud.orders.external.crm;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.service.BlacklistService;
import ru.yandex.market.antifraud.orders.service.BuyerDataService;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.service.GluesService;
import ru.yandex.market.antifraud.orders.service.RoleService;
import ru.yandex.market.antifraud.orders.service.UserMarkerResolver;
import ru.yandex.market.antifraud.orders.storage.dao.AntifraudDao;
import ru.yandex.market.antifraud.orders.storage.dao.MarketUserIdDao;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestRetryHandler;
import ru.yandex.market.request.httpclient.trace.TraceHttpResponseInterceptor;
import ru.yandex.market.request.trace.Module;

import static org.mockito.Mockito.mock;

/**
 * @author dzvyagin
 */
@Configuration
public class LiluCrmTestConfig {

    private String serviceUrl = "http://test.ya.ru";

    @Bean
    public RestTemplate crmRestTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setSSLSocketFactory(csf)
                .addInterceptorFirst(new TraceHttpRequestInterceptor(Module.MARKET_PLATFORM_API))
                .setRetryHandler(new TraceHttpRequestRetryHandler(DefaultHttpRequestRetryHandler.INSTANCE))
                .addInterceptorFirst(new TraceHttpResponseInterceptor())
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory
                = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(5_000); // TODO
        requestFactory.setReadTimeout(20_000);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(requestFactory);
        restTemplate.setMessageConverters(List.of(
                new ProtobufHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter(AntifraudJsonUtil.OBJECT_MAPPER))
        );
        return restTemplate;
    }

    @Bean
    public HttpLiluCrmClient liluCrmHttpClient(RestTemplate crmRestTemplate) {
        return new HttpLiluCrmClient(crmRestTemplate, serviceUrl);
    }

    @Bean
    public BuyerDataService buyerDataService(HttpLiluCrmClient httpLiluCrmClient) {
        return new BuyerDataService(
                httpLiluCrmClient,
                mock(RoleService.class),
                mock(AntifraudDao.class),
                mock(MarketUserIdDao.class),
                List.of(),
                mock(GluesService.class),
                new UserMarkerResolver(mock(ConfigurationService.class)),
                mock(BlacklistService.class));
    }
}
