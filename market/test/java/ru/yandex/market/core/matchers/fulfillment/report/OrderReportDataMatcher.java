package ru.yandex.market.core.matchers.fulfillment.report;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.core.fulfillment.report.excel.jxls.JxlsOrdersReportModel;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.Matchers.allOf;

public class OrderReportDataMatcher {
    /**
     * Проверка строки в колонке "Заказ" для отчета "виртуальный магазин"
     */
    public static Matcher<JxlsOrdersReportModel> virtualShopOrderReportMatcher(
            Long orderId,
            String offerName,
            String shopSku,
            Integer count,
            String supplierName,
            Long supplierId,
            @Nonnull MbiOrderStatus status,
            BigDecimal billingPrice,
            BigDecimal subsidy,
            String creationDate,
            String changedStatusTime,
            String paymentType,
            String deliveryServiceName,
            String regionToName
    ) {
        return allOf(
                hasOrderId(orderId),
                hasOfferName(offerName),
                hasShopSku(shopSku),
                hasCountInDelivery(count),
                hasSupplierName(supplierName),
                hasSupplierId(supplierId),
                hasStatus(status.getName()),
                hasBillingPrice(billingPrice),
                hasSubsidy(subsidy),
                hasCreationDate(creationDate),
                hasChangedStatusTime(changedStatusTime),
                hasPaymentType(paymentType),
                hasDeliveryServiceName(deliveryServiceName),
                hasRegionToName(regionToName)
        );
    }

    /**
     * Проверка, что строка с в колонке "Заказ" пустая для отчета "виртуальный магазин"
     */
    public static Matcher<JxlsOrdersReportModel> virtualShopOrderReportNullMatcher() {
        return allOf(
                orderReportNullMatcher(),
                hasSupplierName(null),
                hasSupplierId(null)
        );
    }

    /**
     * Проверка, что строка с в колонке "Заказ" пустая для фулфилмент отчета
     */
    public static Matcher<JxlsOrdersReportModel> orderReportNullMatcher() {
        return Matchers.allOf(
                hasOrderId(null),
                hasOfferName(null),
                hasShopSku(null),
                hasCountInDelivery(null),
                hasStatus(null),
                hasBillingPrice(null),
                hasSubsidy(null),
                hasSpasiboPerItem(null),
                hasCreationDate(null),
                hasChangedStatusTime(null),
                hasPaymentType(null),
                hasDeliveryServiceName(null),
                hasRegionToName(null)
        );
    }

    public static Matcher<JxlsOrdersReportModel> hasOrderId(Long expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getOrderId, expectedValue, "orderId")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasOrderNum(String expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getOrderNum, expectedValue, "orderNum")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasOfferName(String expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getOfferName, expectedValue, "offerName")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasShopSku(String expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getShopSku, expectedValue, "shopSku")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasCountInDelivery(Integer expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getCountInDelivery, expectedValue, "count")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasInitialCount(Integer expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getInitialCount, expectedValue, "initialCount")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasStatus(String expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getStatus, expectedValue, "status")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasBillingPrice(BigDecimal expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getBillingPrice, expectedValue, "billingPrice")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasSubsidy(BigDecimal expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getSubsidy, expectedValue, "subsidy")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasCreationDate(String expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getCreationDate, expectedValue, "creationDate")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasChangedStatusTime(String expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getChangedStatusTime, expectedValue, "changedStatusTime")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasPaymentType(String expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getPaymentType, expectedValue, "paymentType")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasDeliveryServiceName(String expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getDeliveryServiceName, expectedValue, "deliveryServiceName")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasRegionToName(String expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getRegionToName, expectedValue, "regionToName")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasSpasiboPerItem(BigDecimal expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getSpasibo, expectedValue, "spasiboPerItem")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasCashbackPerItem(BigDecimal expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getCashback, expectedValue, "cashbackPerItem")
                .build();
    }


    public static Matcher<JxlsOrdersReportModel> hasSupplierId(Long expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getSupplierId, expectedValue, "supplierId")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasSupplierName(String expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getSupplierName, expectedValue, "supplierName")
                .build();
    }

    public static Matcher<JxlsOrdersReportModel> hasCis(String expectedValue) {
        return MbiMatchers.<JxlsOrdersReportModel>newAllOfBuilder()
                .add(JxlsOrdersReportModel::getCis, expectedValue, "cis")
                .build();
    }

}
