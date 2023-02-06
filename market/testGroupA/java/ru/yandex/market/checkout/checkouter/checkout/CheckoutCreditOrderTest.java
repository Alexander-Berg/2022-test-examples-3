package ru.yandex.market.checkout.checkouter.checkout;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.credit.CreditError;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.validation.PaymentMethodNotApplicableError;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.helpers.BlueCrossborderOrderHelper;
import ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;
import ru.yandex.market.common.report.model.json.credit.CreditDenial;
import ru.yandex.market.common.report.model.json.credit.CreditInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CREDIT;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.INSTALLMENT;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.bluePrepaidWithCustomPrice;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.DEFAULT_WARE_MD5;

/**
 * @author : poluektov
 * date: 22.03.2019.
 */
@Disabled
public class CheckoutCreditOrderTest extends AbstractWebTestBase {

    private static final BigDecimal CREDIT_ITEM_PRICE = new BigDecimal("3500");
    @Autowired
    protected ChangeOrderItemsHelper changeOrderItemsHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private BlueCrossborderOrderHelper blueCrossborderOrderHelper;

    @Test
    public void createFfOrderWithCredit() {
        Parameters parameters = bluePrepaidWithCustomPrice(CREDIT_ITEM_PRICE);
        parameters.setPaymentMethod(CREDIT);

        Order order = orderCreateHelper.createOrder(parameters);

        checkOrderInDb(order, CREDIT);
    }

    @Test
    public void createFfOrderWithCreditWitheList() {
        Parameters parameters = bluePrepaidWithCustomPrice(CREDIT_ITEM_PRICE);
        parameters.setPaymentMethod(CREDIT);

        Order order = orderCreateHelper.createOrder(parameters);

        checkOrderInDb(order, CREDIT);
    }

    @Test
    public void createFfOrderWithInstallment() {
        Parameters parameters = bluePrepaidWithCustomPrice(CREDIT_ITEM_PRICE);
        parameters.setPaymentMethod(PaymentMethod.INSTALLMENT);

        Order order = orderCreateHelper.createOrder(parameters);

        checkOrderInDb(order, PaymentMethod.INSTALLMENT);
    }

    @Test
    public void checkThresholdError() {
        Parameters parameters = bluePrepaidWithCustomPrice(new BigDecimal("2500"));
        parameters.setPaymentMethod(CREDIT);
        parameters.setCheckCartErrors(false);

        CreditInfo creditInfo = createCreditDenial();
        parameters.getReportParameters().setCreditInfo(creditInfo);

        MultiCart cart = orderCreateHelper.cart(parameters);
        List<ValidationResult> errors = cart.getCarts().get(0).getValidationErrors();
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getCode(), equalTo(PaymentMethodNotApplicableError.CODE));

        Set<PaymentMethod> deliveryPaymentOptions =
                cart.getCarts().get(0).getDeliveryOptions().get(0).getPaymentOptions();
        assertThat(deliveryPaymentOptions, not(hasItem(CREDIT)));
        assertThat(deliveryPaymentOptions, not(hasItem(INSTALLMENT)));

        assertThat(cart.getCreditInformation().getCreditErrors(), hasSize(1));
        assertThat(
                Iterables.getOnlyElement(cart.getCreditInformation().getCreditErrors()).getErrorCode(),
                is("TOO_CHEAP")
        );
    }

    @Test
    public void checkBlackListError() {
        Parameters parameters = bluePrepaidWithCustomPrice(new BigDecimal("2500"));
        parameters.setPaymentMethod(CREDIT);
        parameters.setCheckCartErrors(false);

        CreditInfo creditInfo = createCreditDenialBlacklisted(new ArrayList<String>() {{
            add(DEFAULT_WARE_MD5);
        }});
        parameters.getReportParameters().setCreditInfo(creditInfo);

        MultiCart cart = orderCreateHelper.cart(parameters);
        List<ValidationResult> errors = cart.getCarts().get(0).getValidationErrors();
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getCode(), equalTo(PaymentMethodNotApplicableError.CODE));

        Set<PaymentMethod> deliveryPaymentOptions =
                cart.getCarts().get(0).getDeliveryOptions().get(0).getPaymentOptions();
        assertThat(deliveryPaymentOptions, not(hasItem(CREDIT)));
        assertThat(deliveryPaymentOptions, not(hasItem(INSTALLMENT)));

        Set<CreditError> creditErrors = new HashSet<>(cart.getCreditInformation().getCreditErrors());
        assertThat(creditErrors, hasSize(1));
        assertThat(creditErrors.iterator().next().getErrorCode(), is("CREDIT_NOT_AVAILABLE_FOR_ITEMS"));
        assertThat(creditErrors.iterator().next().getInvalidItems(), is(hasSize(1)));
    }

    @Test
    public void cannotChangeCreditOrder() throws Exception {
        Parameters parameters = bluePrepaidWithCustomPrice(new BigDecimal("4000"));
        parameters.getOrders().get(0).getItems().forEach(item -> {
            item.setCount(2);
        });
        parameters.setPaymentMethod(PaymentMethod.CREDIT);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        Collection<OrderItem> newItems = order.getItems();
        newItems.forEach(item -> item.setCount(1));
        ResultActions response = changeOrderItemsHelper.changeOrderItems(newItems, ClientHelper.crmRobotFor(order),
                order.getId());
        String stringResponse = response
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();
        assertThat(stringResponse, containsString("Cannot change items, order was paid by credit or installment"));
    }

    @Test
    public void checkInvalidItemsTest() {

        final long firstSupplierId = RandomUtils.nextLong();
        final long secondSupplierId = RandomUtils.nextLong();
        assertNotEquals(firstSupplierId, secondSupplierId);

        Parameters anotherOrderParameters = bluePrepaidWithCustomPrice(CREDIT_ITEM_PRICE);
        anotherOrderParameters.addOtherItem();
        anotherOrderParameters.addAnotherItem();
        anotherOrderParameters.getOrder().getItems().iterator().next().setSupplierId(firstSupplierId);
        anotherOrderParameters.getReportParameters().getOrderItemsOverride().clear();

        Parameters multiOrderParameters = bluePrepaidWithCustomPrice(CREDIT_ITEM_PRICE);
        multiOrderParameters.getOrder().getItems().iterator().next().setSupplierId(secondSupplierId);
        String invalidItemWareMd5 = multiOrderParameters.getOrder().getItems().iterator().next().getWareMd5();
        multiOrderParameters.getReportParameters().getOrderItemsOverride().clear();
        multiOrderParameters.addOrder(anotherOrderParameters);
        multiOrderParameters.setPaymentMethod(CREDIT);
        multiOrderParameters.setCheckCartErrors(false);

        multiOrderParameters.addShopMetaData(firstSupplierId, ShopSettingsHelper.getDefaultMeta());
        multiOrderParameters.addShopMetaData(secondSupplierId, ShopSettingsHelper.getPostpayMeta());

        MultiCart cart = orderCreateHelper.cart(multiOrderParameters);
        List<ValidationResult> errors = cart.getCarts().get(0).getValidationErrors();
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getCode(), equalTo(PaymentMethodNotApplicableError.CODE));

        Set<PaymentMethod> deliveryPaymentOptions =
                cart.getCarts().get(0).getDeliveryOptions().get(0).getPaymentOptions();
        assertThat(deliveryPaymentOptions, not(hasItem(CREDIT)));
        assertThat(deliveryPaymentOptions, not(hasItem(INSTALLMENT)));

        assertThat(cart.getCreditInformation().getCreditErrors(), hasSize(1));
        CreditError creditError = Iterables.getOnlyElement(cart.getCreditInformation().getCreditErrors());
        assertThat(
                creditError.getErrorCode(),
                is("CREDIT_NOT_AVAILABLE_FOR_ITEMS")
        );
        assertThat(
                Iterables.getOnlyElement(creditError.getInvalidItems()).getWareMd5(),
                is(invalidItemWareMd5)
        );
    }

    @Test
    public void checkMultipleErrors() {
        final long firstSupplierId = RandomUtils.nextLong();
        final long secondSupplierId = RandomUtils.nextLong();
        assertNotEquals(firstSupplierId, secondSupplierId);

        Parameters anotherOrderParameters = bluePrepaidWithCustomPrice(CREDIT_ITEM_PRICE);
        anotherOrderParameters.addOtherItem();
        anotherOrderParameters.addAnotherItem();
        anotherOrderParameters.getOrder().getItems().iterator().next().setSupplierId(firstSupplierId);
        anotherOrderParameters.getReportParameters().getOrderItemsOverride().clear();

        Parameters multiOrderParameters = bluePrepaidWithCustomPrice(CREDIT_ITEM_PRICE);
        multiOrderParameters.getOrder().getItems().iterator().next().setSupplierId(secondSupplierId);
        multiOrderParameters.getReportParameters().getOrderItemsOverride().clear();
        multiOrderParameters.addOrder(anotherOrderParameters);
        multiOrderParameters.setPaymentMethod(CREDIT);
        multiOrderParameters.setCheckCartErrors(false);

        multiOrderParameters.addShopMetaData(firstSupplierId, ShopSettingsHelper.getDefaultMeta());
        multiOrderParameters.addShopMetaData(secondSupplierId, ShopSettingsHelper.getPostpayMeta());

        multiOrderParameters.configuration().cart().mockConfigurations().forEach(
                (label, configuration) -> configuration.getReportParameters()
                        .setCreditInfo(createCreditDenialBlacklisted(List.of(DEFAULT_WARE_MD5))));

        MultiCart cart = orderCreateHelper.cart(multiOrderParameters);
        List<ValidationResult> errors = cart.getCarts().get(0).getValidationErrors();
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getCode(), equalTo(PaymentMethodNotApplicableError.CODE));

        Set<PaymentMethod> deliveryPaymentOptions =
                cart.getCarts().get(0).getDeliveryOptions().get(0).getPaymentOptions();
        assertThat(deliveryPaymentOptions, not(hasItem(CREDIT)));
        assertThat(deliveryPaymentOptions, not(hasItem(INSTALLMENT)));

        assertThat(cart.getCreditInformation().getCreditErrors(), hasSize(1));
        List<CreditError> creditErrors = cart.getCreditInformation().getCreditErrors();
        assertThat(
                creditErrors.stream().map(CreditError::getErrorCode).collect(Collectors.toList()),
                containsInAnyOrder("CREDIT_NOT_AVAILABLE_FOR_ITEMS")
        );
    }

    @Nonnull
    private CreditInfo createCreditDenial() {
        CreditDenial creditDenial = new CreditDenial();
        creditDenial.setReason("TOO_CHEAP");
        CreditInfo creditInfo = new CreditInfo();
        creditInfo.setCreditDenial(creditDenial);
        return creditInfo;
    }

    @Nonnull
    private CreditInfo createCreditDenialBlacklisted(List<String> blackList) {
        CreditDenial creditDenial = new CreditDenial();
        creditDenial.setBlackListOffers(blackList);

        creditDenial.setReason("BLACKLIST_CATEGORY");
        CreditInfo creditInfo = new CreditInfo();
        creditInfo.setCreditDenial(creditDenial);
        return creditInfo;
    }

    @Test
    public void checkPaymentOptionsInCartResponse() {
        Parameters parameters = bluePrepaidWithCustomPrice(CREDIT_ITEM_PRICE);
        //обнуляем все поля в доставке, чтобы получить методы оплаты по умолчанию.
        parameters.getOrder().setDelivery(new Delivery());
        parameters.getOrder().getDelivery().setRegionId(213L);

        MultiCart response = orderCreateHelper.cart(parameters);
        assertTrue(response.getPaymentOptions().contains(CREDIT));
        assertThat(response.getPriceForCreditAllowed(), is(BigDecimal.valueOf(3500)));
        assertThat(response.getCreditInformation().getPriceForCreditAllowed(), is(BigDecimal.valueOf(3500)));
        assertThat(response.getCreditMonthlyPayment(), is(BigDecimal.valueOf(605)));
        assertThat(response.getCreditInformation().getCreditMonthlyPayment(), is(BigDecimal.valueOf(605)));
    }

    @Test
    public void checkPaymentOptionsInCartResponsePreorder() {
        Parameters parameters = getDefaultOrderParams(CREDIT_ITEM_PRICE, true);
        //обнуляем все поля в доставке, чтобы получить методы оплаты по умолчанию.
        parameters.getOrder().setDelivery(new Delivery());
        parameters.getOrder().getDelivery().setRegionId(213L);

        MultiCart response = orderCreateHelper.cart(parameters);
        assertFalse(response.getPaymentOptions().contains(CREDIT));
    }

    @Test
    public void checkPaymentOptionsInCartResponseCrossborder() throws IOException {
        Parameters parameters = blueCrossborderOrderHelper.createDefaultParameters();
        parameters.getOrder().getItems().forEach(
                oi -> {
                    oi.setPrice(CREDIT_ITEM_PRICE);
                    oi.setBuyerPrice(CREDIT_ITEM_PRICE);
                }
        );
        parameters.getOrder().setDelivery(new Delivery());
        parameters.getOrder().getDelivery().setRegionId(213L);
        final MultiCart response = blueCrossborderOrderHelper.doCartBlueWithoutFulfilment(parameters);
        assertFalse(response.getPaymentOptions().contains(CREDIT));
    }

    @Test
    public void multiCartWithCreditCartAndWithoutPaymentMethod() {
        Parameters parameters = bluePrepaidWithCustomPrice(new BigDecimal("3508"));
        parameters.setPaymentType(null);
        parameters.setPaymentMethod(null);
        parameters.setCheckCartErrors(false);

        final Order order = parameters.getOrder();
        // Все параметры выставляем для конкретной корзины
        order.setPaymentMethod(CREDIT);
        order.setPaymentType(PaymentType.PREPAID);
        // Добавляем скидку, чтобы товар нельзя было купить в кредит
        parameters.getBuiltMultiCart().setPromoCode("SOME_PROMO");
        parameters.setMockLoyalty(true);
        LoyaltyParameters loyaltyParameters = parameters.getLoyaltyParameters();
        loyaltyParameters.clearDiscounts();
        order.getItems().forEach(
                item -> loyaltyParameters.addLoyaltyDiscount(item,
                        new LoyaltyDiscount(new BigDecimal(1000L), PromoType.MARKET_COUPON))
        );
        // Замокаем ответ репорта
        parameters.getReportParameters().setCreditInfo(createCreditDenial());

        MultiCart response = orderCreateHelper.cart(parameters);
        List<ValidationResult> errors = response.getCarts().get(0).getValidationErrors();
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getCode(), equalTo(PaymentMethodNotApplicableError.CODE));

        assertEquals(1, response.getCreditInformation().getCreditErrors().size());
        assertFalse(response.getPaymentOptions().contains(CREDIT));
    }

    private void checkOrderInDb(Order order, PaymentMethod method) {
        assertThat(order.getId(), notNullValue());
        assertThat(order.isFulfilment(), is(true));
        Order orderFromDb = orderService.getOrder(order.getId());
        assertThat(orderFromDb.getPaymentMethod(), equalTo(method));
        assertThat(orderFromDb.getStatus(), equalTo(OrderStatus.UNPAID));
        assertThat(orderFromDb.getSubstatus(), equalTo(OrderSubstatus.WAITING_USER_INPUT));
    }

    private Parameters getDefaultOrderParams(BigDecimal price, boolean isPreorder) {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setColor(Color.BLUE);
        parameters.getOrder().getItems().forEach(item -> {
            item.setBuyerPrice(price);
            item.setPrice(price);
            item.setPreorder(isPreorder);
        });
        return parameters;
    }
}
