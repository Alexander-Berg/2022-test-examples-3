package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SchedulerJob {
    CALCULATE_ORDER_STATUS("CalculateOrdersStatus", "orders"),
    REMOVE_ITEM_CANCEL_ORDER("RemoveItemCancelOrder", "orders"),
    WITHDRAWAL_REPLENISHMENT("ReplenishmentWithdrawalStart", "replenishment"),
    BUILDING_MARKING_JOB("BuildingMarkingJob", "orders");
    private final String value;
    private final String group;
}
