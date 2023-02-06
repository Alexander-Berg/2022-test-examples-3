package ru.yandex.market.crm.campaign.test;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.Set;

import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.crm.campaign.yql.AsyncYqlService;
import ru.yandex.market.crm.campaign.yql.YqlOperationStrategy;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.templates.TemplateService;
import ru.yandex.market.crm.yql.Pragmas;
import ru.yandex.market.crm.yql.SyncYqlService;
import ru.yandex.market.crm.yql.YqlAclConfig;
import ru.yandex.market.crm.yql.YqlTemplateService;
import ru.yandex.market.crm.yql.YqlTokenSupplier;
import ru.yandex.market.crm.yql.client.YqlClient;
import ru.yandex.market.crm.yt.operations.OperationHandler;
import ru.yandex.market.mcrm.http.HttpClientFactory;
import ru.yandex.market.mcrm.http.internal.HttpClientFactoryImpl;
import ru.yandex.market.mcrm.http.tvm.TvmService;
import ru.yandex.market.mcrm.queue.retry.RetryTaskService;

import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
@Configuration
public class LocalYqlConfig {

    @Bean
    public YqlTemplateService yqlTemplateService(TemplateService templateService,
                                                 @Value("${yt.pool}") String ytPool,
                                                 @Value("${yt.cluster}") String cluster) {
        return new YqlTemplateService(
                cluster,
                templateService,
                Collections.singleton(Pragmas.ytPool(ytPool))
        );
    }

    @Bean
    public SyncYqlService yqlService(YqlClient yqlClient, YqlTemplateService yqlTemplateService) {
        return new SyncYqlService(yqlClient, yqlTemplateService);
    }

    @Bean
    public AsyncYqlService asyncYqlService(@Named("yqlOperationHandler") OperationHandler<String, ResultSet> operationHandler,
                                           YqlClient yqlClient) {
        return new AsyncYqlService(operationHandler, yqlClient);
    }

    @Bean
    public YqlClient yqlClient(JsonSerializer jsonSerializer,
                               JsonDeserializer jsonDeserializer,
                               YqlTokenSupplier yqlTokenSupplier,
                               ConfigurableBeanFactory beanFactory,
                               RetryTaskService retryTaskService,
                               TvmService tvmService,
                               @Value("${yt.cluster}") String cluster) {
        HttpClientFactory clientFactory = new HttpClientFactoryImpl(beanFactory, retryTaskService, tvmService);
        YqlAclConfig yqlAclConfig = new YqlAclConfig(Set.of());
        return new YqlClient(clientFactory, jsonSerializer, jsonDeserializer, yqlTokenSupplier, yqlAclConfig, cluster);
    }

    @Bean
    public OperationHandler<String, ResultSet> yqlOperationHandler() {
        return mock(OperationHandler.class);
    }

    @Bean
    public YqlTokenSupplier yqlTokenSupplier() {
        return mock(YqlTokenSupplier.class);
    }

    @Bean
    public YqlOperationStrategy yqlOperationStrategy() {
        return mock(YqlOperationStrategy.class);
    }
}
