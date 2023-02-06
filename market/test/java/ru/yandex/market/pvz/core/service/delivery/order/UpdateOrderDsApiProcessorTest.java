package ru.yandex.market.pvz.core.service.delivery.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.delivery.request.CreateOrderRequest;
import ru.yandex.market.logistic.api.model.delivery.request.UpdateOrderRequest;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.place.OrderPlace;
import ru.yandex.market.pvz.core.domain.order.model.place.OrderPlaceItem;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.service.delivery.DsApiBaseTest;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.ds.exception.DsApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.EXTERNAL_ID_NOT_UNIQUE_ENABLED;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UpdateOrderDsApiProcessorTest extends DsApiBaseTest {

    private final DsOrderManager dsOrderManager;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;
    private final OrderRepository orderRepository;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    private PickupPoint pickupPoint;

    private long orderId;

    @BeforeEach
    public void setup() {
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, true);

        pickupPoint = pickupPointFactory.createPickupPoint();

        CreateOrderRequest request = readRequest(
                "/ds/order/create_order_without_places_with_multiple_items.xml",
                CreateOrderRequest.class, Map.of(
                        "pickup_point_code", pickupPoint.getId(),
                        "orderId", orderId));

        var createResponse = dsOrderManager.createOrder(request.getOrder(),
                pickupPoint.getLegalPartner().getDeliveryService());

        orderId = Long.parseLong(createResponse.getOrderId().getPartnerId());

        transactionTemplate.execute(ts -> {
            Order order = orderRepository.findByIdOrThrow(Long.parseLong(createResponse.getOrderId().getPartnerId()));

            assertThat(order.getPlaces()).isNotNull();
            assertThat(order.getPlaces()).isEmpty();
            return null;
        });
    }

    @Test
    void updateOrderMultiplePlacesFromOurWarehouse() {
        UpdateOrderRequest updateRequest = readRequest(
                "/ds/order/update_order_multiple_places.xml",
                UpdateOrderRequest.class, Map.of(
                        "pickup_point_code", pickupPoint.getId(),
                        "orderId", orderId
                ));

        var updateResponse = dsOrderManager.updateOrder(updateRequest.getOrder(),
                pickupPoint.getLegalPartner().getDeliveryService());
        transactionTemplate.execute(ts -> {
            Order orderAfterUpdate =
                    orderRepository.findByIdOrThrow(Long.parseLong(updateResponse.getOrderId().getPartnerId()));

            assertThat(orderAfterUpdate.getPlaces()).hasSize(2);
            assertThat(orderAfterUpdate.getPlaces())
                    .extracting(OrderPlace::getBarcode)
                    .containsExactlyInAnyOrder("12981801-1", "12981801-2");

            for (OrderPlace place : orderAfterUpdate.getPlaces()) {
                assertThat(place.getItems()).hasSize(1);
                OrderPlaceItem placeItem = place.getItems().iterator().next();
                assertThat(placeItem.getCount()).isEqualTo(1);
            }

            assertThat(orderAfterUpdate.getPlaces().get(0).getCreatedAt()).isNotNull();
            assertThat(orderAfterUpdate.getPlaces().get(0).getUpdatedAt()).isNotNull();

            assertThat(orderAfterUpdate.getOrderAdditionalInfo()).isNotNull();
            assertThat(orderAfterUpdate.getOrderAdditionalInfo().getPlacesCount()).isEqualTo((short) 2);
            assertThat(orderAfterUpdate.getOrderAdditionalInfo().getPlaceCodes())
                    .containsExactlyInAnyOrder("12981801-1", "12981801-2");
            assertThat(orderAfterUpdate.getOrderAdditionalInfo().getTotalPrice())
                    .isEqualByComparingTo(BigDecimal.valueOf(3380));
            return null;
        });
    }

    @Test
    void updateOrderWithDuplicatePlacesFromNotOurWarehouse() {
        UpdateOrderRequest updateRequest = readRequest(
                "/ds/order/update_order_duplicate_places.xml",
                UpdateOrderRequest.class, Map.of("pickup_point_code", pickupPoint.getId(),
                        "orderId", orderId
                ));

        var updateResponse = dsOrderManager.updateOrder(updateRequest.getOrder(),
                pickupPoint.getLegalPartner().getDeliveryService());

        transactionTemplate.execute(ts -> {
            Order orderAfterUpdate =
                    orderRepository.findByIdOrThrow(Long.parseLong(updateResponse.getOrderId().getPartnerId()));

            assertThat(orderAfterUpdate.getPlaces()).hasSize(1);
            assertThat(orderAfterUpdate.getPlaces())
                    .extracting(OrderPlace::getBarcode)
                    .containsExactlyInAnyOrder("12981801-1");
            assertThat(orderAfterUpdate.getPlaces().get(0).getItems()).hasSize(2);

            assertThat(orderAfterUpdate.getOrderAdditionalInfo()).isNotNull();
            assertThat(orderAfterUpdate.getOrderAdditionalInfo().getPlacesCount()).isEqualTo((short) 1);
            assertThat(orderAfterUpdate.getOrderAdditionalInfo().getPlaceCodes())
                    .containsExactlyInAnyOrder("12981801-1");
            assertThat(orderAfterUpdate.getOrderAdditionalInfo().getTotalPrice())
                    .isEqualByComparingTo(BigDecimal.valueOf(3380));
            return null;
        });
    }

    @Test
    void updateOrderPlaceTwiceWithDifferentBarcodes() {
        UpdateOrderRequest firstUpdateRequest = readRequest(
                "/ds/order/update_order_place_with_barcode.xml",
                UpdateOrderRequest.class, Map.of("partner_code_value", "111",
                        "orderId", orderId));

        var firstUpdateResponse = dsOrderManager.updateOrder(firstUpdateRequest.getOrder(),
                pickupPoint.getLegalPartner().getDeliveryService());

        transactionTemplate.execute(ts -> {
            Order orderAfterUpdate =
                    orderRepository.findByIdOrThrow(Long.parseLong(firstUpdateResponse.getOrderId().getPartnerId()));

            assertThat(orderAfterUpdate.getPlaces()).hasSize(1);
            assertThat(orderAfterUpdate.getPlaces().get(0).getBarcode())
                    .isEqualTo("111");
            assertThat(orderAfterUpdate.getPlaces().get(0).getItems()).hasSize(2);

            assertThat(orderAfterUpdate.getOrderAdditionalInfo()).isNotNull();
            assertThat(orderAfterUpdate.getOrderAdditionalInfo().getPlacesCount()).isEqualTo((short) 1);
            assertThat(orderAfterUpdate.getOrderAdditionalInfo().getPlaceCodes()).containsExactlyInAnyOrder("111");
            assertThat(orderAfterUpdate.getOrderAdditionalInfo().getTotalPrice())
                    .isEqualByComparingTo(BigDecimal.valueOf(3380));
            return null;
        });

        UpdateOrderRequest secondUpdateRequest = readRequest(
                "/ds/order/update_order_place_with_barcode.xml",
                UpdateOrderRequest.class, Map.of("partner_code_value", "222",
                        "orderId", orderId));

        var secondUpdateResponse = dsOrderManager.updateOrder(secondUpdateRequest.getOrder(),
                pickupPoint.getLegalPartner().getDeliveryService());

        transactionTemplate.execute(ts -> {
            Order orderAfterUpdate =
                    orderRepository.findByIdOrThrow(Long.parseLong(secondUpdateResponse.getOrderId().getPartnerId()));

            assertThat(orderAfterUpdate.getPlaces()).hasSize(1);
            assertThat(orderAfterUpdate.getPlaces().get(0).getBarcode())
                    .isEqualTo("222");
            assertThat(orderAfterUpdate.getPlaces().get(0).getItems()).hasSize(2);

            assertThat(orderAfterUpdate.getOrderAdditionalInfo()).isNotNull();
            assertThat(orderAfterUpdate.getOrderAdditionalInfo().getPlacesCount()).isEqualTo((short) 1);
            assertThat(orderAfterUpdate.getOrderAdditionalInfo().getPlaceCodes()).containsExactlyInAnyOrder("222");
            assertThat(orderAfterUpdate.getOrderAdditionalInfo().getTotalPrice())
                    .isEqualByComparingTo(BigDecimal.valueOf(3380));
            return null;
        });
    }


    @Test
    void updateOrderInWrongUpdateStatus() {
        UpdateOrderRequest updateRequest = readRequest("/ds/order/update_order_multiple_places.xml",
                UpdateOrderRequest.class, Map.of("orderId", orderId));

        transactionTemplate.execute(ts -> {
            Order order = orderRepository.findByExternalIdAndPickupPointIdOrThrow(
                    updateRequest.getOrder().getOrderId().getYandexId(), pickupPoint.getId());
            orderFactory.forceDeliver(order.getId(), LocalDate.of(2021, 10, 19));
            return null;
        });

        assertThatThrownBy(() -> dsOrderManager.updateOrder(updateRequest.getOrder(),
                pickupPoint.getLegalPartner().getDeliveryService())).isExactlyInstanceOf(DsApiException.class);
    }
}
