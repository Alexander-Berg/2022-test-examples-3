package ru.yandex.market.checkout.checkouter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.checkouter.balance.trust.model.BalanceOrderParams;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BalanceRefund;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BasicResponse;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BasketLineState;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BasketLineUpdate;
import ru.yandex.market.checkout.checkouter.balance.trust.model.CreateAccountResponse;
import ru.yandex.market.checkout.checkouter.balance.trust.model.CreateBasketLine;
import ru.yandex.market.checkout.checkouter.balance.trust.model.CreateBasketRequest;
import ru.yandex.market.checkout.checkouter.balance.trust.model.CreateBasketResponse;
import ru.yandex.market.checkout.checkouter.balance.trust.model.CreateRefundResponse;
import ru.yandex.market.checkout.checkouter.balance.trust.model.GatewayInfoResponse;
import ru.yandex.market.checkout.checkouter.balance.trust.model.RefundLine;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.BalancePaymentMethod;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.BatchCreateBalanceOrderRequest;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.CreateAccountRequest;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.CreateRefundRequest;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.ListPaymentMethodsResponse;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.ListWalletBalanceResponse;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.MarkupBasketRequest;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.Product;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.ResizeBasketLineRequest;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.RestBasketResponse;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.TopupRequestBody;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.UnbindCardRequest;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.WalletBalance;
import ru.yandex.market.checkout.checkouter.balance.trust.xmlrpc.XmlRpcBasketResponse;
import ru.yandex.market.checkout.checkouter.controllers.OrderMigrateController;
import ru.yandex.market.checkout.checkouter.log.cart.DiffLog;
import ru.yandex.market.checkout.checkouter.log.cart.serialization.mixin.AddressImplMixin;
import ru.yandex.market.checkout.checkouter.log.cart.serialization.mixin.BuyerMixin;
import ru.yandex.market.checkout.checkouter.log.cart.serialization.mixin.DeliveryMixin;
import ru.yandex.market.checkout.checkouter.log.cart.serialization.mixin.OrderItemMixin;
import ru.yandex.market.checkout.checkouter.log.cart.serialization.mixin.OrderMixin;
import ru.yandex.market.checkout.checkouter.order.MoveOrderParams;
import ru.yandex.market.checkout.checkouter.pay.PaymentPartition;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplItem;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrder;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrderCreateRequestBody;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrderCreateResponse;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrderService;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplPlan;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplPlanCheckRequestBody;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplPlanCheckResponse;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplPlanDetails;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplRefundRequestBody;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplRefundResponseBody;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplRegularPayment;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUser;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.ConsumerMeta;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.OrderMeta;
import ru.yandex.market.checkout.checkouter.pay.cashier.model.CashierPaymentStateResponse;
import ru.yandex.market.checkout.checkouter.saturn.ScoringRequest;
import ru.yandex.market.checkout.checkouter.saturn.ScoringRequestBasket;
import ru.yandex.market.checkout.checkouter.sberbank.model.AdditionalJsonParams;
import ru.yandex.market.checkout.checkouter.sberbank.model.CartItem;
import ru.yandex.market.checkout.checkouter.sberbank.model.CartItems;
import ru.yandex.market.checkout.checkouter.sberbank.model.Installments;
import ru.yandex.market.checkout.checkouter.sberbank.model.OrderAttribute;
import ru.yandex.market.checkout.checkouter.sberbank.model.OrderBundle;
import ru.yandex.market.checkout.checkouter.sberbank.model.PaymentAmountInfo;
import ru.yandex.market.checkout.checkouter.sberbank.model.Quantity;
import ru.yandex.market.checkout.checkouter.sberbank.model.RegisterOrderRequest;
import ru.yandex.market.checkout.checkouter.sberbank.model.SberOrderStatusResponse;
import ru.yandex.market.checkout.checkouter.sberbank.model.SberRegisterOrderResponse;
import ru.yandex.market.checkout.checkouter.sberbank.model.SberResponse;
import ru.yandex.market.checkout.checkouter.tasks.eventinspector.YtTableMeta;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.order.ItemServiceRegistrationPayload;
import ru.yandex.market.checkout.checkouter.trust.client.ReceiptRow;
import ru.yandex.market.checkout.checkouter.trust.client.TrustReceipt;
import ru.yandex.market.checkout.checkouter.warranty.Warranty;
import ru.yandex.market.checkout.checkouter.warranty.WarrantyBuyer;
import ru.yandex.market.checkout.checkouter.warranty.WarrantyDelivery;
import ru.yandex.market.checkout.checkouter.warranty.WarrantyItem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class CheckouterDocumentationTest {

    private static final Logger log = LoggerFactory.getLogger(CheckouterDocumentationTest.class);

    private static final Set<Class<?>> CLASSES_TO_SKIP = Set.of(
            // cart diff log
            OrderMixin.class,
            DiffLog.class,
            AddressImplMixin.class,
            DeliveryMixin.class,
            OrderItemMixin.class,
            BuyerMixin.class,
            // yt client
            YtTableMeta.class,
            // receipt task
            ReceiptRow.class,
            TrustReceipt.class,
            Warranty.class,
            WarrantyBuyer.class,
            WarrantyDelivery.class,
            WarrantyItem.class,
            // новое апи траста
            BasicResponse.class,
            CreateBasketRequest.class,
            Product.class,
            CreateBasketResponse.class,
            BalanceRefund.class,
            CreateBasketLine.class,
            BasketLineUpdate.class,
            RestBasketResponse.class,
            CashierPaymentStateResponse.class,
            XmlRpcBasketResponse.class,
            BalanceOrderParams.class,
            BasketLineState.class,
            CreateRefundResponse.class,
            ItemServiceRegistrationPayload.class,
            MarkupBasketRequest.class,
            RefundLine.class,
            BalancePaymentMethod.class,
            ScoringRequest.class,
            ScoringRequestBasket.class,
            CreateRefundRequest.class,
            ListPaymentMethodsResponse.class,
            ResizeBasketLineRequest.class,
            UnbindCardRequest.class,
            BatchCreateBalanceOrderRequest.class,
            CreateAccountRequest.class,
            ListWalletBalanceResponse.class,
            WalletBalance.class,
            // sberbank api
            SberRegisterOrderResponse.class,
            RegisterOrderRequest.class,
            SberOrderStatusResponse.class,
            AdditionalJsonParams.class,
            OrderBundle.class,
            CartItems.class,
            CartItem.class,
            Quantity.class,
            Installments.class,
            OrderAttribute.class,
            PaymentAmountInfo.class,
            SberResponse.class,
            // logging helpers
            MoveOrderParams.class,
            // from cpa-common-client
            PaymentPartition.class,
            TopupRequestBody.class,
            CreateAccountResponse.class,
            GatewayInfoResponse.class,
            BnplOrderCreateRequestBody.class,
            BnplOrderCreateResponse.class,
            OrderMeta.class,
            BnplOrder.class,
            BnplUser.class,
            BnplPlan.class,
            BnplPlanCheckRequestBody.class,
            BnplPlanCheckResponse.class,
            BnplOrderService.class,
            BnplItem.class,
            BnplPlanDetails.class,
            BnplRegularPayment.class,
            BnplRefundRequestBody.class,
            BnplRefundResponseBody.class,
            ConsumerMeta.class,
            OrderMigrateController.OrderMigrateResponse.class
    );

    @Test
    public void jsonPropertyShouldBeMarkedWithApiModelProperty() {
        Reflections reflections = new Reflections(
                "ru.yandex.market.checkout.checkouter",
                new MethodAnnotationsScanner()
        );

        List<Method> methodsWithoutApiModelProperty = reflections.getMethodsAnnotatedWith(JsonProperty.class)
                .stream()
                .filter(method -> !method.getDeclaringClass().getSimpleName().endsWith("Builder"))
                .filter(method -> !CLASSES_TO_SKIP.contains(method.getDeclaringClass()))
                .filter(method -> method.getAnnotation(ApiModelProperty.class) == null)
                .sorted(Comparator.comparing(m -> m.getDeclaringClass().getCanonicalName()))
                .collect(Collectors.toList());

        methodsWithoutApiModelProperty
                .forEach(m -> log.error("Found property without @ApiModelProperty: {}", m));

        String message = "Found errors in following methods: " + methodsWithoutApiModelProperty.stream()
                .map(method -> method.getDeclaringClass().getSimpleName() + "::" + method.getName())
                .collect(Collectors.toList())
                .toString();
        assertThat(message, methodsWithoutApiModelProperty, hasSize(0));
    }

    @Test
    public void jsonFieldShouldBeMarkedWithApiModelProperty() {
        Reflections reflections = new Reflections(
                "ru.yandex.market.checkout.checkouter",
                new FieldAnnotationsScanner()
        );

        List<Field> fieldsWithoutApiModelProperty = reflections.getFieldsAnnotatedWith(JsonProperty.class)
                .stream()
                .filter(field -> !CLASSES_TO_SKIP.contains(field.getDeclaringClass()))
                .filter(field -> field.getAnnotation(ApiModelProperty.class) == null)
                .sorted(Comparator.comparing(m -> m.getDeclaringClass().getCanonicalName()))
                .collect(Collectors.toList());

        fieldsWithoutApiModelProperty
                .forEach(f -> log.error("Found field without @ApiModelProperty: {}", f));

        String message = "Found errors in following fields: " + fieldsWithoutApiModelProperty.stream()
                .map(field -> field.getDeclaringClass().getSimpleName() + "." + field.getName())
                .collect(Collectors.toList())
                .toString();
        assertThat(message, fieldsWithoutApiModelProperty, hasSize(0));
    }
}
