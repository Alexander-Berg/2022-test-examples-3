package ru.yandex.market.checkout.checkouter.order;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonInstance {

    private final String cis;
    private final String uit;
    private final String balanceOrderId;
    private final String imei;
    private final String sn;

    public JsonInstance(String cis) {
        this.cis = cis;
        this.balanceOrderId = null;
        uit = null;
        imei = null;
        sn = null;
    }

    public JsonInstance(String cis, String balanceOrderId) {
        this.cis = cis;
        this.balanceOrderId = balanceOrderId;
        uit = null;
        imei = null;
        sn = null;
    }

    public JsonInstance(String cis, String uit, String balanceOrderId) {
        this.cis = cis;
        this.uit = uit;
        this.balanceOrderId = balanceOrderId;
        imei = null;
        sn = null;
    }

    public JsonInstance(String cis, String uit, String balanceOrderId, String imei, String sn) {
        this.cis = cis;
        this.uit = uit;
        this.balanceOrderId = balanceOrderId;
        this.imei = imei;
        this.sn = sn;
    }

    public static String toNode(JsonInstance... instances) {
        return Arrays.stream(instances)
                .map(JsonInstance::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Override
    public String toString() {
        return Stream.of(
                fieldToStr(OrderItemInstance.InstanceType.CIS.getName(), cis),
                fieldToStr(OrderItemInstance.InstanceType.UIT.getName(), uit),
                fieldToStr(OrderItemInstance.InstanceType.BALANCE_ORDER_ID.getName(), balanceOrderId),
                fieldToStr(OrderItemInstance.InstanceType.IMEI.getName(), imei)
        )
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(",", "{", "}"));
    }

    private String fieldToStr(String fieldName, String value) {
        return value == null ? "" :
                "\"" + fieldName + "\":\"" + value + "\"";
    }
}
