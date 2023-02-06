package ru.yandex.market.crm.operatorwindow.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.operatorwindow.checkout.JmfOrderUpdater;
import ru.yandex.market.crm.operatorwindow.domain.order.CancelOrderService;
import ru.yandex.market.crm.operatorwindow.domain.order.ConfirmOrderService;
import ru.yandex.market.crm.operatorwindow.domain.order.OrderConfirmCancellationService;
import ru.yandex.market.crm.operatorwindow.domain.order.OrderRules;
import ru.yandex.market.crm.operatorwindow.services.loyalty.OrderCouponsService;
import ru.yandex.market.crm.operatorwindow.services.order.cancellation.AvailableOrderCancelReasonService;
import ru.yandex.market.crm.operatorwindow.services.order.promo.CancelOrderPromoIdSelector;
import ru.yandex.market.ocrm.module.checkouter.CheckouterService;
import ru.yandex.market.ocrm.module.checkouter.InteractiveCheckouterService;
import ru.yandex.market.ocrm.module.yadelivery.LomService;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
        CheckouterTestConfiguration.class,
        MockOrderRulesConfiguration.class,
})
public class OrderOperationTestConfiguration {
    @Bean
    ConfirmOrderService confirmOrderService(OrderRules orderRules,
                                            InteractiveCheckouterService interactiveCheckouterService,
                                            JmfOrderUpdater jmfOrderUpdater) {
        return new ConfirmOrderService(orderRules,
                interactiveCheckouterService,
                jmfOrderUpdater
        );
    }


    @Bean
    public JmfOrderUpdater jmfOrderUpdater() {
        return mock(JmfOrderUpdater.class);
    }

    @Bean
    public LomService lomService() {
        return mock(LomService.class);
    }

    @Bean
    public OrderCouponsService orderCouponsService() {
        return mock(OrderCouponsService.class);
    }

    @Bean
    public CancelOrderPromoIdSelector cancelOrderPromoIdSelector() {
        return mock(CancelOrderPromoIdSelector.class);
    }

    @Bean
    OrderConfirmCancellationService orderConfirmCancellationService() {
        return mock(OrderConfirmCancellationService.class);
    }

    @Bean
    CancelOrderService cancelOrderService(OrderRules orderRules,
                                          CheckouterService checkouterService,
                                          OrderCouponsService orderCouponsService,
                                          CancelOrderPromoIdSelector cancelOrderPromoIdSelector,
                                          JmfOrderUpdater jmfOrderUpdater,
                                          LomService lomService) {
        return new CancelOrderService(orderRules,
                checkouterService,
                orderCouponsService,
                cancelOrderPromoIdSelector,
                jmfOrderUpdater,
                lomService);
    }

    @Bean
    public AvailableOrderCancelReasonService availableOrderCancelReasonService() {
        return mock(AvailableOrderCancelReasonService.class);
    }
}
