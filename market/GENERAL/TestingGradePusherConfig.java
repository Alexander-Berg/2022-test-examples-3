package ru.yandex.market.pers.grade.core.config.external;

import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.pers.grade.core.saas.grade.pusher.ModelGradeSaasPusher;
import ru.yandex.market.pers.grade.core.saas.grade.pusher.PersStaticPusherClient;
import ru.yandex.market.pers.grade.core.saas.grade.pusher.ShopGradeSaasPusher;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.saas.indexer.SaasIndexerService;
import ru.yandex.market.util.HttpClientFactory;

/**
 * Config is used in testing by all modules.
 * Helps to push grades changes to saas directly.
 *
 * @author Ilya Kislitsyn / ilyakis@ / 26.11.2021
 */
@Configuration
@Profile({"testing", "junit"})
public class TestingGradePusherConfig {
    @Bean
    public HttpClient saasIndexerHttpClient() {
        return HttpClientFactory.createHttpClient(Module.YANDEX_SAAS);
    }

    @Bean
    public SaasIndexerService saasIndexerService(@Value("${saas.indexer.host}") String host,
                                                 @Value("${saas.indexer.service}") String service,
                                                 @Value("${saas.indexer.port}") int port,
                                                 @Value("${saas.indexer.debug}") boolean debug,
                                                 @Qualifier("saasIndexerHttpClient") HttpClient httpClient) {
        return new SaasIndexerService(host, service, port, debug, httpClient);
    }

    @Bean
    public PersStaticPusherClient persStaticClient(ModelGradeSaasPusher modelGradeSaasPusher,
                                                   ShopGradeSaasPusher shopGradeSaasPusher) {
        return new PersStaticPusherClient(modelGradeSaasPusher, shopGradeSaasPusher);
    }
}
