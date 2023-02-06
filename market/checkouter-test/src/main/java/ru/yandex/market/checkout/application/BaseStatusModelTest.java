package ru.yandex.market.checkout.application;

import java.io.Serializable;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.configuration.MockitoConfiguration;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.backbone.validation.order.status.StatusUpdateValidator;
import ru.yandex.market.checkout.backbone.validation.order.status.graph.OrderStatusGraph;
import ru.yandex.market.checkout.backbone.validation.order.status.rule.CancellationByShopRule;
import ru.yandex.market.checkout.checkouter.delivery.DeferredCourierDeliveryNewFlowEnabledCondition;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusExpiry;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestProcessor;
import ru.yandex.market.checkout.checkouter.order.status.OrderStatusRouters;
import ru.yandex.market.checkout.checkouter.order.status.PrescriptionDeliveryRule;
import ru.yandex.market.checkout.checkouter.order.validation.PendingStatusExpiry;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.service.communicationproxy.CommunicationProxyService;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.queuedcalls.QueuedCallService;

/**
 * Базовый класс для проверки переходов по статусам.
 * Содержит сильно урезанную конфигурацию.
 */
@ActiveProfiles("test")
@ContextConfiguration(classes = BaseStatusModelTest.TestBaseConfig.class)
@ExtendWith(SpringExtension.class)
public class BaseStatusModelTest {

    private static final Answer<Object> THROW_EXCEPTION = new ThrowException();

    @MockBean
    protected CheckouterFeatureReader checkouterFeatureReader;

    @MockBean(name = "checkouterProperties")
    protected CheckouterProperties checkouterProperties;

    @MockBean
    protected PrescriptionDeliveryRule prescriptionDeliveryRule;

    @MockBean
    protected QueuedCallService queuedCallService;

    @MockBean
    protected PaymentService paymentService;

    @MockBean
    protected OrderStatusRouters orderStatusRouters;

    @MockBean
    protected PendingStatusExpiry defaultPendingStatusExpiry;

    @MockBean(name = "orderStatusExpiryMinutes")
    protected Map<OrderStatus, OrderStatusExpiry> orderStatusExpiryMinutes;

    @MockBean(name = "checkpointStatusToOrderStatusUpdateAction")
    protected Map<DeliveryCheckpointStatus, Object> checkpointStatusToOrderStatusUpdateAction;

    @MockBean(name = "cancelFromProcessingMap")
    protected Map<DeliveryType, Object> cancelFromProcessingMap;

    @MockBean
    protected OrderService orderService;

    @MockBean
    protected OrderUpdateService orderUpdateService;

    @MockBean
    protected CommunicationProxyService communicationProxyService;

    @MockBean
    protected ShopService shopService;

    @Autowired
    protected CancellationByShopRule cancellationByShopRule;

    @Autowired
    protected StatusUpdateValidator statusUpdateValidator;

    @Autowired
    protected CancellationRequestProcessor cancellationRequestProcessor;

    @ImportResource({
            "classpath:context/statuses.xml"
    })
    public static class TestBaseConfig {

        @Bean
        public CancellationByShopRule cancellationByShopRule() {
            return new CancellationByShopRule();
        }

        @Bean
        public StatusUpdateValidator statusUpdateValidator(
                @Qualifier("orderStatusGraph") OrderStatusGraph statusGraph
        ) {
            return new StatusUpdateValidator(statusGraph);
        }

        @Bean
        public CancellationRequestProcessor cancellationRequestProcessor(
                OrderService orderService, OrderUpdateService orderUpdateService,
                StatusUpdateValidator statusUpdateValidator,
                CheckouterFeatureReader checkouterFeatureReader) {
            return new CancellationRequestProcessor(orderService, orderUpdateService, statusUpdateValidator,
                    checkouterFeatureReader);
        }

        @Bean
        public DeferredCourierDeliveryNewFlowEnabledCondition deferredCourierDeliveryNewFlowEnabledCondition(
                CheckouterFeatureReader checkouterFeatureReader) {
            return new DeferredCourierDeliveryNewFlowEnabledCondition(checkouterFeatureReader);
        }

        @Bean
        public TestableClock clock() {
            return new TestableClock();
        }
    }

    @BeforeEach
    public void beforeEach() {
        // Данная вещь проверяет, что на пути выполнения теста не встречается ни одного не обработанного
        // вызова кода, связанного с моками
        MockitoConfiguration.setDefaultAnswer(THROW_EXCEPTION);
    }

    @AfterEach
    public void afterEach() {
        Mockito.reset(checkouterFeatureReader);
        MockitoConfiguration.setDefaultAnswer(null);
    }


    public static class ThrowException implements Answer<Object>, Serializable {

        private static final long serialVersionUID = 7618312406617949441L;

        @Override
        public Object answer(final InvocationOnMock invocation) {
            throw new RuntimeException("Answer was not mocked");
        }
    }
}
