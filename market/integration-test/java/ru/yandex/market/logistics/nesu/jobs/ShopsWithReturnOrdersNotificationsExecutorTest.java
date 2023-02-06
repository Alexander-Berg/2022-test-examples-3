package ru.yandex.market.logistics.nesu.jobs;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.configuration.properties.ReturnOrdersNotificationsProperties;
import ru.yandex.market.logistics.nesu.jobs.executor.ShopsWithReturnOrdersNotificationsExecutor;
import ru.yandex.market.logistics.nesu.jobs.producer.SendNotificationToShopProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SendReturnOrdersWaitingNotificationProducer;
import ru.yandex.market.logistics.nesu.service.shop.ShopService;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@DisplayName("Создание тасок для оповещения о наличии заказов, ожидающих возврата")
@ParametersAreNonnullByDefault
class ShopsWithReturnOrdersNotificationsExecutorTest extends AbstractContextualTest {
    @Autowired
    private LomClient lomClient;

    @Autowired
    private SendNotificationToShopProducer sendNotificationToShopProducer;

    @Autowired
    private SendReturnOrdersWaitingNotificationProducer sendReturnOrdersWaitingNotificationProducer;

    @Autowired
    private ShopService shopService;

    @Autowired
    private ReturnOrdersNotificationsProperties properties;

    private ShopsWithReturnOrdersNotificationsExecutor shopsWithReturnOrdersNotificationsExecutor;

    @BeforeEach
    void setup() {
        shopsWithReturnOrdersNotificationsExecutor = new ShopsWithReturnOrdersNotificationsExecutor(
            shopService,
            sendReturnOrdersWaitingNotificationProducer,
            properties
        );

        doNothing().when(sendReturnOrdersWaitingNotificationProducer)
            .produceTask(anyList());
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(sendNotificationToShopProducer, lomClient);
    }

    @Test
    @DisplayName("Разбиение всех магазинов на батчи и запуск задач")
    @DatabaseSetup("/jobs/executors/return_orders/before/prepare.xml")
    void splitShopsAndProduceJob() {
        shopsWithReturnOrdersNotificationsExecutor.doJob(null);
        verify(sendReturnOrdersWaitingNotificationProducer).produceTask(List.of(1L, 2L, 5L));
        verify(sendReturnOrdersWaitingNotificationProducer).produceTask(List.of(6L));
    }

    @Test
    @DisplayName("Разбиение всех магазинов на батчи и запуск задач - магазинов нет")
    void splitNoShopsAndProduceJob() {
        shopsWithReturnOrdersNotificationsExecutor.doJob(null);
        verifyZeroInteractions(sendReturnOrdersWaitingNotificationProducer);
    }
}
