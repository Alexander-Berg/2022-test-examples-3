package ru.yandex.market.checkout.helpers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.controllers.checkouter.PaymentInfoMultiOrderRequest;
import ru.yandex.market.checkout.checkouter.controllers.checkouter.PaymentInfoOrderRequest;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.BnplInfoRequest;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.configuration.ActualizationRequestConfiguration;
import ru.yandex.market.checkout.providers.v2.multicart.request.BuyerRequestProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.PLATFORM;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SHOW_CREDITS;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SHOW_CREDIT_BROKER;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SHOW_INSTALLMENTS;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SHOW_VAT;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.UID;

@WebTestHelper
public class ActualizePaymentInfoHelper extends MockMvcAware {

    public ActualizePaymentInfoHelper(WebApplicationContext webApplicationContext,
                                      TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public MultiOrder actualizePaymentInfo(MultiOrder multiOrder, Parameters parameters) throws Exception {
        PaymentInfoMultiOrderRequest requestBody = mapToPaymentInfoMultiOrder(multiOrder, parameters);
        MockHttpServletRequestBuilder builder = post("/actualize-payment-info");
        prepareMockBuilder(multiOrder, parameters, builder);
        builder.content(testSerializationService.serializeCheckouterObject(requestBody));
        return performApiRequest(builder, MultiOrder.class);
    }

    public MultiOrder changePayment(MultiOrder multiOrder, Parameters parameters) throws Exception {
        PaymentInfoMultiOrderRequest requestBody = mapToPaymentInfoMultiOrder(multiOrder, parameters);
        MockHttpServletRequestBuilder builder = post("/change-payment-method");
        prepareMockBuilder(multiOrder, parameters, builder);
        builder.content(testSerializationService.serializeCheckouterObject(requestBody));
        return performApiRequest(builder, MultiOrder.class);
    }

    private void prepareMockBuilder(MultiOrder multiOrder, Parameters parameters,
                                    MockHttpServletRequestBuilder builder) {
        builder.contentType(MediaType.APPLICATION_JSON_UTF8);
        builder.param(PLATFORM, Platform.DESKTOP.name());
        if (parameters.configuration().cart().request().isShowCredits()) {
            builder.param(SHOW_CREDITS, Boolean.TRUE.toString());
        }
        if (parameters.configuration().cart().request().isShowVat()) {
            builder.param(SHOW_VAT, Boolean.TRUE.toString());
        }
        if (parameters.configuration().cart().request().isShowInstallments()) {
            builder.param(SHOW_INSTALLMENTS, Boolean.TRUE.toString());
        }
        if (parameters.configuration().cart().request().isShowCreditBroker()) {
            builder.param(SHOW_CREDIT_BROKER, Boolean.TRUE.toString());
        }
        builder.param(UID, Objects.toString(
                Stream.of(
                                Optional.of(multiOrder)
                                        .map(MultiCart::getBuyer)
                                        .map(Buyer::getUid),
                                multiOrder.getCarts().stream()
                                        .findFirst()
                                        .map(Order::getBuyer)
                                        .map(Buyer::getUid),
                                Optional.of(parameters)
                                        .map(Parameters::getBuyer)
                                        .map(Buyer::getUid))
                        .filter(Optional::isPresent)
                        .findFirst()
                        .orElse(Optional.empty())
                        .orElse(null)));
    }

    private PaymentInfoMultiOrderRequest mapToPaymentInfoMultiOrder(MultiOrder multiOrder,
                                                                    Parameters parameters) {
        List<PaymentInfoOrderRequest> orderRequests = multiOrder.getCarts().stream()
                .map(order -> mapToPaymentInfoOrder(order, parameters))
                .collect(Collectors.toList());
        return PaymentInfoMultiOrderRequest.builder()
                .withOrders(orderRequests)
                .withBuyer(BuyerRequestProvider.fromBuyer(
                        new ActualizationRequestConfiguration(), parameters.getBuiltMultiCart())
                )
                .withSelectedCashbackOption(multiOrder.getSelectedCashbackOption())
                .withBnplInfo(
                        parameters.getBuiltMultiCart().getBnplInfo() == null
                                ? null
                                : BnplInfoRequest.builder()
                                .withSelected(parameters.getBuiltMultiCart().getBnplInfo().isSelected())
                                .build()
                )
                .build();
    }

    private PaymentInfoOrderRequest mapToPaymentInfoOrder(Order order, Parameters parameters) {
        return PaymentInfoOrderRequest.builder()
                .withId(order.getId())
                .withPaymentSystem(order.getPaymentSystem())
                .withPaymentMethod(parameters.getBuiltMultiCart().getPaymentMethod())
                .withPaymentType(parameters.getBuiltMultiCart().getPaymentType())
                .withProperties(order.getPropertiesForJson())
                .build();
    }
}
