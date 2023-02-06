package ru.yandex.market.checkout.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.common.WebTestHelper;

@WebTestHelper
/*
  Хелпер для использования удалений из заказа (частичного выкупа) в тестах
 */
public class RemoveItemsHelper {

    @Autowired
    protected CheckouterAPI client;

    public List<ChangeRequest> remove(@NotNull RemoveRequest remove) {
        Objects.requireNonNull(remove);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(remove.convertToNotification());
        return client.editOrder(remove.getOrderId(), remove.getRole(),
                remove.getClientId(), List.of(Color.BLUE,
                        Color.WHITE), editRequest);

    }

    public static class RemoveRequest {

        private Long orderId;
        private HistoryEventReason reason;
        private ClientRole role;
        private Long clientId;
        private boolean cancelDisabled;
        private boolean alreadyRemovedByWarehouse;
        private List<ItemInfo> itemInfos;

        public RemoveRequest(Order order, HistoryEventReason reason, ClientRole role) {
            this(order.getId(), reason, role);
        }

        public RemoveRequest(Long orderId, HistoryEventReason reason, ClientRole role) {
            this.orderId = orderId;
            this.reason = reason;
            this.role = role;
            this.cancelDisabled = true;
            this.alreadyRemovedByWarehouse = true;
            this.clientId = null;
            itemInfos = new ArrayList<>();
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public HistoryEventReason getReason() {
            return reason;
        }

        public void setReason(HistoryEventReason reason) {
            this.reason = reason;
        }

        public ClientRole getRole() {
            return role;
        }

        public Long getClientId() {
            return clientId;
        }

        public void setClientId(Long clientId) {
            this.clientId = clientId;
        }

        public void setRole(ClientRole role) {
            this.role = role;
        }

        public void removeItem(long itemId, int remainedCount) {
            itemInfos.add(new ItemInfo(itemId, remainedCount, null));
        }

        public MissingItemsNotification convertToNotification() {
            if (itemInfos.isEmpty()) {
                throw new IllegalStateException("itemsInfos was empty");
            }
            return new MissingItemsNotification(alreadyRemovedByWarehouse, itemInfos, reason, cancelDisabled);
        }
    }
}
