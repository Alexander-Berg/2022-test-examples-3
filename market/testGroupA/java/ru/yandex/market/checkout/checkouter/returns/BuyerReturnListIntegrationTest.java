package ru.yandex.market.checkout.checkouter.returns;

import java.math.BigDecimal;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.archive.OrderArchiveService;
import ru.yandex.market.checkout.checkouter.order.archive.OrderMovingService;
import ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingRequest;
import ru.yandex.market.checkout.checkouter.storage.returns.buyers.filters.BuyerReturnFilter;
import ru.yandex.market.checkout.checkouter.viewmodel.returns.BuyerReturnViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.returns.BuyerReturnViewModelCollection;
import ru.yandex.market.checkout.checkouter.viewmodel.returns.delivery.BuyerReturnCourierDeliveryViewModel;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingDirection.BASIC_TO_ARCHIVE;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParametersWithItems;

public class BuyerReturnListIntegrationTest extends AbstractReturnTestBase {

    private final Long buyerUid = 20220705L;

    @Autowired
    private BuyerReturnService buyerReturnService;
    @Autowired
    private OrderArchiveService orderArchiveService;
    @Autowired
    private OrderMovingService orderMovingService;
    @Autowired
    private CheckouterFeatureWriter featureWriter;

    @Test
    @DisplayName("Информация по доставке возврата курьером должна отобразиться на списке возвратов")
    void courierDeliveryInformationMustExist() {
        Order order = orderCreateHelper.createOrder(defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        returnHelper.mockActualDelivery(order);
        Return returnTemplate = ReturnProvider.generateReturn(order);
        setDefaultDelivery(order, returnTemplate, DeliveryType.DELIVERY);
        returnHelper.initReturn(order.getId(), returnTemplate);
        BuyerReturnFilter filter = BuyerReturnFilter.builder(order.getBuyer().getUid()).build();

        BuyerReturnViewModelCollection resultCollection = buyerReturnService.getBuyerReturn(filter);

        assertThat(resultCollection.getSize()).isEqualTo(1);
        BuyerReturnCourierDeliveryViewModel result =
                (BuyerReturnCourierDeliveryViewModel) resultCollection.getValues().get(0).getDelivery();
        assertThat(result).isNotNull();
        assertThat(result.getDates()).isNotNull();
        assertThat(result.getSenderAddress()).isNotNull();
    }

    @Test
    @DisplayName("Неархивные возвраты отображаются раньше архивных")
    void returnPaginatedReturnInfo() {
        featureWriter.writeValue(BooleanFeatureType.ENABLE_GETTING_ARCHIVED_RETURNS, true);
        var item = OrderItemProvider.getOrderItem();
        item.setQuantity(BigDecimal.TEN);
        var params = defaultBlueOrderParametersWithItems(item);
        params.setBuyer(getBuyer());
        Order orderToBeArchived = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(orderToBeArchived, OrderStatus.DELIVERED);

        returnHelper.mockActualDelivery(orderToBeArchived);
        Return returnTemplate1 = ReturnProvider.generateReturn(orderToBeArchived);
        returnTemplate1.setDelivery(null);
        returnTemplate1.getItems().get(0).setQuantity(BigDecimal.ONE);
        returnHelper.initReturn(orderToBeArchived.getId(), returnTemplate1);

        Order actualOrder = orderCreateHelper.createOrder(
                defaultBlueOrderParameters(getBuyer()));
        orderStatusHelper.proceedOrderToStatus(actualOrder, OrderStatus.DELIVERED);
        returnHelper.mockActualDelivery(actualOrder);
        Return returnTemplate2 = ReturnProvider.generateReturn(actualOrder);
        setDefaultDelivery(actualOrder, returnTemplate2, DeliveryType.DELIVERY);
        returnHelper.initReturn(actualOrder.getId(), returnTemplate2);

        var archived = orderArchiveService.archiveOrders(Set.of(orderToBeArchived.getId()), false);
        assertEquals(1, archived.size());
        orderMovingService.moveOrder(new OrderMovingRequest(orderToBeArchived.getId(), BASIC_TO_ARCHIVE));

        BuyerReturnFilter filter = BuyerReturnFilter.builder(buyerUid).build();

        BuyerReturnViewModelCollection resultCollection = buyerReturnService.getBuyerReturn(filter);

        assertThat(resultCollection.getSize()).isEqualTo(2);
        BuyerReturnViewModel actualReturnResult = resultCollection.getValues().get(0);
        assertThat(actualReturnResult).isNotNull();
        assertThat(actualReturnResult.getOrderId()).isEqualTo(actualOrder.getId());

        BuyerReturnViewModel archivedResult = resultCollection.getValues().get(1);
        assertThat(archivedResult).isNotNull();
        assertThat(archivedResult.getOrderId()).isEqualTo(orderToBeArchived.getId());

        featureWriter.writeValue(BooleanFeatureType.ENABLE_GETTING_ARCHIVED_RETURNS, false);
    }

    private Buyer getBuyer() {
        Buyer buyer = new Buyer(buyerUid);
        buyer.setFirstName(RandomStringUtils.randomAlphabetic(10));
        buyer.setLastName(RandomStringUtils.randomAlphabetic(10));
        buyer.setPhone("+79000010101");
        buyer.setEmail("a@a.ru");
        buyer.setRegionId(213L);
        buyer.setYandexEmployee(false);
        return buyer;
    }
}
