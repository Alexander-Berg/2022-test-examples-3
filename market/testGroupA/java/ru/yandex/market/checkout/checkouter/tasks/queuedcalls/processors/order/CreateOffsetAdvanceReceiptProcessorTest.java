package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.order;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.queuedcalls.ExecutionResult;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.shop.DeliveryReceiptNeedType.DONT_CREATE_DELIVERY_RECEIPT;
import static ru.yandex.market.checkout.checkouter.shop.DeliveryReceiptNeedType.DONT_CREATE_DELIVERY_RECEIPT_EXCLUDE_DELIVERY;


public class CreateOffsetAdvanceReceiptProcessorTest extends AbstractWebTestBase {

    private static final int SHOP_ID_WITH_FLAG_2 = 668;
    private static final long BUSINESS_ID = 668L;
    @Autowired
    protected OrderCreateHelper orderCreateHelper;
    @Autowired
    protected CreateOffsetAdvanceReceiptProcessor processor;

    @Test
    @DisplayName("Запрет создания повторного чека (чека доставки)")
    public void shouldDontCreateReceipt() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.configureMultiCart(
                multiCart -> multiCart.getCarts().forEach(o -> o.setShopId((long) SHOP_ID_WITH_FLAG_2))
        );
        ShopMetaDataBuilder shopBuilder = ShopMetaDataBuilder.createCopy(
                ShopSettingsHelper.createCustomNotFulfilmentMeta(SHOP_ID_WITH_FLAG_2, BUSINESS_ID));
        shopBuilder.withDeliveryReceiptNeedType(DONT_CREATE_DELIVERY_RECEIPT);
        parameters.addShopMetaData((long) SHOP_ID_WITH_FLAG_2, shopBuilder.build());
        Order order = orderCreateHelper.createOrder(parameters);

        var result = processor.process(
                new QueuedCallProcessor.QueuedCallExecution(
                        order.getId(),
                        null,
                        0, Instant.now(),
                        order.getId())
        );

        assertThat(result).isEqualTo(ExecutionResult.SUCCESS);
    }

    @Test
    @DisplayName("Отложенная задача создания повторного чека (чека доставки)")
    public void postponingReceiptCreation() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.configureMultiCart(
                multiCart -> multiCart.getCarts().forEach(o -> o.setShopId((long) SHOP_ID_WITH_FLAG_2))
        );
        ShopMetaDataBuilder shopBuilder = ShopMetaDataBuilder.createCopy(
                ShopSettingsHelper.createCustomNotFulfilmentMeta(SHOP_ID_WITH_FLAG_2, BUSINESS_ID));
        shopBuilder.withDeliveryReceiptNeedType(DONT_CREATE_DELIVERY_RECEIPT_EXCLUDE_DELIVERY);
        parameters.addShopMetaData((long) SHOP_ID_WITH_FLAG_2, shopBuilder.build());
        Order order = orderCreateHelper.createOrder(parameters);

        var result = processor.process(
                new QueuedCallProcessor.QueuedCallExecution(
                        order.getId(),
                        null,
                        0, Instant.now(),
                        order.getId())
        );
        // При ошибке внутри paymentService.createOffsetAdvanceReceipt(orderId)
        // должна быть создана отложенная задача
        assertTrue(result != null && result.isDelayExecution());
    }
}
