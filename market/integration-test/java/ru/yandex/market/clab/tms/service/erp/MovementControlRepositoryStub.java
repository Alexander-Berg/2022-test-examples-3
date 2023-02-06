package ru.yandex.market.clab.tms.service.erp;

import ru.yandex.market.clab.common.service.ShopSkuKey;
import ru.yandex.market.clab.tms.service.erp.data.TransferOrder;
import ru.yandex.market.clab.tms.service.erp.data.TransferOrderError;
import ru.yandex.market.clab.tms.service.erp.data.TransferOrderItem;
import ru.yandex.market.clab.tms.service.erp.data.TransferOrderItemStatus;
import ru.yandex.market.clab.tms.service.erp.data.TransferOrderState;
import ru.yandex.market.clab.tms.service.erp.data.TransferOrderStatus;
import ru.yandex.market.clab.tms.service.erp.data.TransferOrderStatusEvent;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MovementControlRepositoryStub implements MovementControlRepository {
    private Map<String, TransferOrderInternal> transferOrders = new LinkedHashMap<>();

    @Override
    public List<TransferOrder> getTransferOrders(List<String> orderIds) {
        return transferOrders.entrySet().stream()
            .filter(e -> orderIds.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .map(TransferOrderInternal::getOrder)
            .collect(Collectors.toList());
    }

    @Override
    public void insertTransferOrder(TransferOrder order) {
        order.setState(TransferOrderState.NEW);
        order.setCreatedDate(Timestamp.from(Instant.now()));
        transferOrders.put(order.getOrderId(), new TransferOrderInternal(order));
    }

    @Override
    public void insertTransferOrderItems(List<TransferOrderItem> items) {
        items.forEach(i -> {
            TransferOrderInternal transferOrderInternal = transferOrders.get(i.getOrderId());
            transferOrderInternal.getItems().add(i);
        });
    }

    @Override
    public List<TransferOrderStatusEvent> getOrderStatusEvents(List<String> orderIds) {
        return orderIds.stream()
            .flatMap(id -> {
                TransferOrderInternal transferOrderInternal = transferOrders.get(id);
                if (transferOrderInternal == null) {
                    return Stream.empty();
                }
                return transferOrderInternal.getStatuses().stream();
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<TransferOrderItemStatus> getOrderItemStatuses(List<String> orderIds) {
        return orderIds.stream().flatMap(id -> {
            TransferOrderInternal transferOrderInternal = transferOrders.get(id);
            if (transferOrderInternal == null) {
                return Stream.empty();
            }
            return transferOrderInternal.getItemStatuses().values().stream();
        }).collect(Collectors.toList());
    }

    @Override
    public List<TransferOrderError> getOrderErrors(List<String> orderIds) {
        return orderIds.stream().flatMap(id -> {
            TransferOrderInternal transferOrderInternal = transferOrders.get(id);
            if (transferOrderInternal == null) {
                return Stream.empty();
            }
            return transferOrderInternal.getErrors().stream();
        }).collect(Collectors.toList());
    }

    public List<TransferOrderItem> getOrderItems(List<String> orderIds) {
        return orderIds.stream().flatMap(id -> {
            TransferOrderInternal transferOrderInternal = transferOrders.get(id);
            if (transferOrderInternal == null) {
                return Stream.empty();
            }
            return transferOrderInternal.getItems().stream();
        }).collect(Collectors.toList());
    }

    public void startProcessing(String orderId) {
        TransferOrderInternal transferOrderInternal = getTransferOrderInternal(orderId);
        transferOrderInternal.getOrder().setState(TransferOrderState.PROCESSED);
        setStatus(orderId, TransferOrderStatus.NEW, "New");
    }

    public void setStatus(String orderId,
                          TransferOrderStatus status,
                          String text) {
        setStatus(orderId, status, text, null, null);
    }

    public void setStatus(String orderId,
                          TransferOrderStatus status,
                          String text,
                          String axOrderId,
                          String ffOrderId) {
        TransferOrderInternal transferOrderInternal = getTransferOrderInternal(orderId);
        List<TransferOrderStatusEvent> beforeStatuses = transferOrderInternal.getStatuses();
        String newAxOrderId = axOrderId == null ?
            beforeStatuses.stream()
                .map(TransferOrderStatusEvent::getAxOrderId)
                .filter(Objects::nonNull)
                .findFirst().orElse(null) :
            axOrderId;
        String newFfOrderId = ffOrderId == null ?
            beforeStatuses.stream()
                .map(TransferOrderStatusEvent::getFfOrderId)
                .filter(Objects::nonNull)
                .findFirst().orElse(null) :
            ffOrderId;
        transferOrderInternal.addStatus(createStatusEvent(
            transferOrderInternal.getOrder(), status, text, newAxOrderId, newFfOrderId));
        if (status == TransferOrderStatus.FAILED) {
            transferOrderInternal.getErrors().add(createError(transferOrderInternal.getOrder(), text));
        }
    }

    public void createItemStatus(TransferOrderItem item, boolean success) {
        TransferOrderInternal transferOrderInternal = getTransferOrderInternal(item.getOrderId());
        ShopSkuKey key = new ShopSkuKey(Long.valueOf(item.getSupplierId()), item.getSupplierSkuId());
        transferOrderInternal.getItemStatuses().computeIfAbsent(
            key,
            k -> createStatus(item, success)
        );
        if (!success) {
            addError(item, "Товар отсутствует на складе");
        }
    }

    public void addError(String orderId, String error) {
        TransferOrderInternal transferOrderInternal = getTransferOrderInternal(orderId);
        setStatus(orderId, TransferOrderStatus.FAILED, error);
        transferOrderInternal.getErrors().add(createError(transferOrderInternal.getOrder(), error));
    }

    public void addError(TransferOrderItem item, String error) {
        TransferOrderInternal transferOrderInternal = getTransferOrderInternal(item.getOrderId());
        transferOrderInternal.getErrors().add(createError(item, error));
    }

    private TransferOrderInternal getTransferOrderInternal(String orderId) {
        TransferOrderInternal transferOrderInternal = transferOrders.get(orderId);
        if (transferOrderInternal == null) {
            throw new IllegalArgumentException();
        }
        return transferOrderInternal;
    }

    private TransferOrderItemStatus createStatus(TransferOrderItem item, boolean success) {
        TransferOrderItemStatus status = new TransferOrderItemStatus();
        status.setSystemType(item.getSystemType());
        status.setOrderId(item.getOrderId());
        status.setMskuId(item.getMskuId());
        status.setSupplierId(item.getSupplierId());
        status.setSupplierSkuId(item.getSupplierSkuId());
        status.setQuantityOrig(1);
        status.setQuantityReserved(success ? 1 : 0);
        status.setQuantityPicked(success ? 1 : 0);
        status.setQuantitySent(success ? 1 : 0);
        status.setQuantityReceived(success ? 1 : 0);
        return status;
    }

    private TransferOrderStatusEvent createStatusEvent(TransferOrder item,
                                                       TransferOrderStatus status,
                                                       String text,
                                                       String axOrderId,
                                                       String ffOrderId) {
        TransferOrderStatusEvent statusEvent = new TransferOrderStatusEvent();
        statusEvent.setSystemType(item.getSystemType());
        statusEvent.setOrderId(item.getOrderId());
        statusEvent.setStatus(status);
        statusEvent.setStatusDate(Timestamp.from(Instant.now()));
        statusEvent.setStatusText(text);
        statusEvent.setStatus(status);
        statusEvent.setAxOrderId(axOrderId);
        statusEvent.setFfOrderId(ffOrderId);
        return statusEvent;
    }

    private TransferOrderError createError(TransferOrder item,
                                           String text) {
        TransferOrderError error = new TransferOrderError();
        error.setSystemType(item.getSystemType());
        error.setOrderId(item.getOrderId());
        error.setErrorNum(0);
        error.setCreatedDate(Timestamp.from(Instant.now()));
        error.setErrorText(text);
        return error;
    }

    private TransferOrderError createError(TransferOrderItem item,
                                           String text) {
        TransferOrderError error = new TransferOrderError();
        error.setSystemType(item.getSystemType());
        error.setOrderId(item.getOrderId());
        error.setSupplierId(item.getSupplierId());
        error.setSupplierSkuId(item.getSupplierSkuId());
        error.setMskuId(item.getMskuId());
        error.setErrorNum(0);
        error.setCreatedDate(Timestamp.from(Instant.now()));
        error.setErrorText(text);
        return error;
    }

    public class TransferOrderInternal {
        TransferOrder order;
        List<TransferOrderItem> items = new ArrayList<>();
        List<TransferOrderStatusEvent> statuses = new ArrayList<>();
        Map<ShopSkuKey, TransferOrderItemStatus> itemStatuses = new LinkedHashMap<>();
        List<TransferOrderError> errors = new ArrayList<>();

        public TransferOrderInternal(TransferOrder order) {
            this.order = order;
        }

        public TransferOrder getOrder() {
            return order;
        }

        public void setOrder(TransferOrder order) {
            this.order = order;
        }

        public List<TransferOrderItem> getItems() {
            return items;
        }

        public void setItems(List<TransferOrderItem> items) {
            this.items = items;
        }

        public List<TransferOrderStatusEvent> getStatuses() {
            return statuses;
        }

        public void addStatus(TransferOrderStatusEvent status) {
            this.statuses.add(status);
        }

        public Map<ShopSkuKey, TransferOrderItemStatus> getItemStatuses() {
            return itemStatuses;
        }

        public void setItemStatuses(Map<ShopSkuKey, TransferOrderItemStatus> itemStatuses) {
            this.itemStatuses = itemStatuses;
        }

        public List<TransferOrderError> getErrors() {
            return errors;
        }

        public void setErrors(List<TransferOrderError> errors) {
            this.errors = errors;
        }
    }
}
