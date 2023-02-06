package ru.yandex.market.checkout.checkouter.pay.bnpl;

import java.io.IOException;
import java.math.BigDecimal;

import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplPlanCheckRequestBody;
import ru.yandex.market.checkout.checkouter.viewmodel.CreatePaymentResponse;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.loyalty.api.model.CashbackType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.checkout.multiware.CommonPaymentServiceTest.CASHBACK_AMOUNT;
import static ru.yandex.market.checkout.checkouter.pay.BnplPaymentTest.createOrderItem;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParametersWithItems;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;

public class BnplOrderItemsTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;

    @Resource(name = "checkouterAnnotationObjectMapper")
    private ObjectMapper checkouterAnnotationObjectMapper;

    @BeforeEach
    public void mockBnpl() {
        checkouterProperties.setEnableServicesPrepay(true);
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();
    }

    @Test
    @DisplayName("Проверяем общую стоимость позиции в bnpl-заказе с кэшбэком.")
    public void testCreateBnplPaymentWithCashback() throws IOException {
        trustMockConfigurer.mockListWalletBalanceResponse();

        Parameters parameters = defaultBnplParameters(
                createOrderItem(0, "bnpl-1", 50.00, 3, SupplierType.FIRST_PARTY, 90864),
                createOrderItem(0, "bnpl-2", 250.00, 1, SupplierType.FIRST_PARTY, 90864)

        );
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.getBuiltMultiCart().setCashback(new Cashback(null, CashbackOptions.allowed(CASHBACK_AMOUNT, "1")));

        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);

        var bnplOrderCreateRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_ORDER_CREATE);
        var orderCreateRequest = checkouterAnnotationObjectMapper.readValue(bnplOrderCreateRequestBody,
                BnplPlanCheckRequestBody.class);

        var firstOrderCreateItem = orderCreateRequest.getServices().iterator().next()
                .getItems().stream()
                .filter(item -> item.getTitle().equals("bnpl-1"))
                .findFirst()
                .orElse(null);

        assertEquals(3, firstOrderCreateItem.getCount());
        //spendLimit=10 per item. Price = 50; count = 3
        assertEquals(new BigDecimal("40.0000"), firstOrderCreateItem.getPrice());
        assertEquals(new BigDecimal("120.0000"), firstOrderCreateItem.getTotal());

        var secondOrderCreateItem = orderCreateRequest.getServices().iterator().next()
                .getItems().stream()
                .filter(item -> item.getTitle().equals("bnpl-2"))
                .findFirst()
                .orElse(null);

        assertEquals(1, secondOrderCreateItem.getCount());
        //spendLimit=30 per item. Price = 250; count = 1;
        assertEquals(new BigDecimal("220.0000"), secondOrderCreateItem.getPrice());
        assertEquals(new BigDecimal("220.0000"), secondOrderCreateItem.getTotal());

        var bnplPlanCheckRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_PLAN_CHECK);
        var planCheckRequest = checkouterAnnotationObjectMapper.readValue(bnplPlanCheckRequestBody,
                BnplPlanCheckRequestBody.class);

        var firstPlanCheckItem = planCheckRequest.getServices().iterator().next()
                .getItems().stream()
                .filter(item -> item.getTitle().equals("bnpl-1"))
                .findFirst()
                .orElse(null);

        assertEquals(3, firstPlanCheckItem.getCount());
        assertEquals(new BigDecimal("40.0000"), firstPlanCheckItem.getPrice());
        assertEquals(new BigDecimal("120.0000"), firstPlanCheckItem.getTotal());

        var secondPlanCheckItem = planCheckRequest.getServices().iterator().next()
                .getItems().stream()
                .filter(item -> item.getTitle().equals("bnpl-2"))
                .findFirst()
                .orElse(null);

        assertEquals(1, secondPlanCheckItem.getCount());
        assertEquals(new BigDecimal("220.0000"), secondPlanCheckItem.getPrice());
        assertEquals(new BigDecimal("220.0000"), secondPlanCheckItem.getTotal());
    }

    @Test
    @DisplayName("Проверяем общую стоимость позиции в bnpl-заказе без кэшбэка.")
    public void testCreateBnplPaymentWithoutCashback() throws IOException {
        Parameters parameters = defaultBlueOrderParametersWithItems(createOrderItem(1, "bnpl", 250.00, 2,
                SupplierType.FIRST_PARTY, 90864));
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payWithRealResponse(order);

        var bnplOrderCreateRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_ORDER_CREATE);
        var orderCreateRequest = checkouterAnnotationObjectMapper.readValue(bnplOrderCreateRequestBody,
                BnplPlanCheckRequestBody.class);

        var orderCreateItems = orderCreateRequest.getServices().iterator().next()
                .getItems().stream()
                .filter(item -> item.getTitle().equals("bnpl"))
                .findFirst()
                .orElse(null);


        assertEquals(2, orderCreateItems.getCount());
        assertEquals(new BigDecimal("250.0000"), orderCreateItems.getPrice());
        assertEquals(new BigDecimal("500.0000"), orderCreateItems.getTotal());

        var bnplPlanCheckRequestBody = bnplMockConfigurer.getBnplRequestBody(BnplMockConfigurer.POST_PLAN_CHECK);
        var planCheckRequest = checkouterAnnotationObjectMapper.readValue(bnplPlanCheckRequestBody,
                BnplPlanCheckRequestBody.class);

        var planCheckItems = planCheckRequest.getServices().iterator().next()
                .getItems().stream()
                .filter(item -> item.getTitle().equals("bnpl"))
                .findFirst()
                .orElse(null);

        assertEquals(2, planCheckItems.getCount());
        assertEquals(new BigDecimal("250.0000"), planCheckItems.getPrice());
        assertEquals(new BigDecimal("500.0000"), planCheckItems.getTotal());
    }
}
