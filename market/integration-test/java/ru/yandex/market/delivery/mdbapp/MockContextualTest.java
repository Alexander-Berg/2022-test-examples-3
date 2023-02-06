package ru.yandex.market.delivery.mdbapp;

import org.junit.ClassRule;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnApi;
import ru.yandex.market.delivery.mdbapp.components.email.sender.MailSender;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbapp.components.service.marketid.LegalInfoReceiver;
import ru.yandex.market.delivery.mdbapp.components.service.yt.YtLogisticsPointService;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.configuration.ZookeeperConfig;
import ru.yandex.market.delivery.mdbapp.configuration.integration.persistence.JdbcMetadataStore;
import ru.yandex.market.delivery.mdbapp.integration.service.ReturnRequestService;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;
import ru.yandex.market.sc.internal.client.ScIntClient;
import ru.yandex.market.tpl.internal.client.TplInternalClient;

@MockBean(classes = {
    DeliveryClient.class,
    FulfillmentClient.class,
    HealthManager.class,
    MailSender.class,
    LMSClient.class,
    PechkinHttpClient.class,
    LomClient.class,
    TarifficatorClient.class,
    TplInternalClient.class,
    MbiApiClient.class,
    LegalInfoReceiver.class,
    ScIntClient.class,
    PvzLogisticsClient.class,
    CheckouterReturnApi.class,
    MqmClient.class,
})
@SpyBean(classes = {
    CheckouterAPI.class,
    OrderEventFailoverableService.class,
    FeatureProperties.class,
    ReturnRequestService.class,
    YtLogisticsPointService.class,
    CheckouterServiceClient.class,
})
@TestExecutionListeners(
    value = ZookeeperConfig.ZkCleanListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
public abstract class MockContextualTest extends AbstractContextualTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @MockBean
    @Qualifier("orderEventsJdbcMetadataStore")
    public JdbcMetadataStore jdbcMetadataStore;
}
