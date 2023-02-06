package ru.yandex.market.pvz.core.service.delivery.order;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.delivery.request.UpdateOrderTransferCodesRequest;
import ru.yandex.market.logistic.api.model.delivery.response.UpdateOrderTransferCodesResponse;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.service.delivery.DsApiBaseTest;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.ds.exception.DsApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UpdateOrderTransferCodesDsApiProcessorTest  extends DsApiBaseTest {

    private static final String NEW_VERIFICATION_CODE = "22222";

    private final TestDeliveryServiceFactory deliveryServiceFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;

    private final OrderRepository orderRepository;

    private final UpdateOrderTransferCodesDsApiProcessor updateOrderTransferCodesDsApiProcessor;

    @Test
    void updateTransferCodes() {
        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder
                .builder()
                .deliveryService(deliveryService)
                .build());
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        var request = readRequest("/ds/order/update_order_transfer_codes.xml",
                UpdateOrderTransferCodesRequest.class,
                Map.of("token", deliveryService.getToken(), "external_id", order.getExternalId(),
                        "verification_code", NEW_VERIFICATION_CODE));

        var actualResponse = updateOrderTransferCodesDsApiProcessor.apiCall(request, deliveryService);

        var expectedResponse = new UpdateOrderTransferCodesResponse();
        assertThat(actualResponse).isEqualTo(expectedResponse);

        var updatedOrder = orderRepository.findByExternalIdAndPickupPointIdOrThrow(order.getExternalId(),
                pickupPoint.getId());

        assertThat(updatedOrder.getOrderVerification()).isNotNull();
        assertThat(updatedOrder.getOrderVerification().getVerificationCode()).isEqualTo("22222");
    }

    @Test
    void updateTransferCodesForOrderWithoutCodeBefore() {
        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder
                .builder()
                .deliveryService(deliveryService)
                .build());
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .verificationCode(null)
                        .build())
                .build());

        var request = readRequest("/ds/order/update_order_transfer_codes.xml",
                UpdateOrderTransferCodesRequest.class,
                Map.of("token", deliveryService.getToken(), "external_id", order.getExternalId(),
                        "verification_code", NEW_VERIFICATION_CODE));

        var actualResponse = updateOrderTransferCodesDsApiProcessor.apiCall(request, deliveryService);

        var expectedResponse = new UpdateOrderTransferCodesResponse();
        assertThat(actualResponse).isEqualTo(expectedResponse);

        var updatedOrder = orderRepository.findByExternalIdAndPickupPointIdOrThrow(order.getExternalId(),
                pickupPoint.getId());

        assertThat(updatedOrder.getOrderVerification()).isNotNull();
        assertThat(updatedOrder.getOrderVerification().getVerificationCode()).isEqualTo("22222");
    }

    @Test
    void tryToUpdateTransferCodesWithoutOutboundCode() {
        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder
                .builder()
                .deliveryService(deliveryService)
                .build());
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        var request = readRequest("/ds/order/update_order_transfer_codes_without_outbound.xml",
                UpdateOrderTransferCodesRequest.class,
                Map.of("token", deliveryService.getToken(), "external_id", order.getExternalId()));

        assertThatThrownBy(() -> updateOrderTransferCodesDsApiProcessor.apiCall(request, deliveryService))
                .isExactlyInstanceOf(DsApiException.class);
    }

    @Test
    void tryToUpdateTransferCodesWithInvalidCode() {
        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder
                .builder()
                .deliveryService(deliveryService)
                .build());
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        var request = readRequest("/ds/order/update_order_transfer_codes.xml",
                UpdateOrderTransferCodesRequest.class,
                Map.of("token", deliveryService.getToken(), "external_id", order.getExternalId(),
                        "verification_code", "2222"));

        assertThatThrownBy(() -> updateOrderTransferCodesDsApiProcessor.apiCall(request, deliveryService))
                .isExactlyInstanceOf(DsApiException.class);
    }

    @Test
    void tryToUpdateTransferCodesForNotExistentOrder() {
        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder
                .builder()
                .deliveryService(deliveryService)
                .build());
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        var request = readRequest("/ds/order/update_order_transfer_codes.xml",
                UpdateOrderTransferCodesRequest.class,
                Map.of("token", deliveryService.getToken(), "external_id", order.getExternalId() + 1,
                        "verification_code", NEW_VERIFICATION_CODE));

        assertThatThrownBy(() -> updateOrderTransferCodesDsApiProcessor.apiCall(request, deliveryService))
                .isExactlyInstanceOf(DsApiException.class);
    }
}
