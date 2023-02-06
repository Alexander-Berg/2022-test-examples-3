package ru.yandex.market.delivery.mdbapp;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.delivery.mdbapp.components.email.sender.MailSender;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.components.geo.GeoInfo;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.queue.order.changetoondemand.confirm.ConfirmOrderChangeToOnDemandRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.CheckouterSetOrderStatusEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.service.crm.client.OrderCommands;
import ru.yandex.market.delivery.mdbapp.components.service.lms.LmsLogisticsPointClient;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PartnerExternalParamsRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PossibleOrderChangeRepository;
import ru.yandex.market.delivery.mdbapp.integration.service.CancelParcelEnqueueService;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;
import ru.yandex.market.sc.internal.client.ScIntClient;
import ru.yandex.market.tpl.internal.client.TplInternalClient;

@MockBean(classes = {
    CancelParcelEnqueueService.class,
    DeliveryClient.class,
    FulfillmentClient.class,
    GeoInfo.class,
    HealthManager.class,
    LmsLogisticsPointClient.class,
    MailSender.class,
    OrderCommands.class,
    PartnerExternalParamsRepository.class,
    LMSClient.class,
    PechkinHttpClient.class,
    LomClient.class,
    TarifficatorClient.class,
    MbiApiClient.class,
    PossibleOrderChangeRepository.class,
    PvzLogisticsClient.class,
    TplInternalClient.class,
    ConfirmOrderChangeToOnDemandRequestEnqueueService.class,
    ScIntClient.class,
    CheckouterSetOrderStatusEnqueueService.class,
})
@SpyBean(classes = {
    CheckouterAPI.class,
    OrderEventFailoverableService.class,
})
public abstract class AllMockContextualTest extends MockContextualTest {
}
