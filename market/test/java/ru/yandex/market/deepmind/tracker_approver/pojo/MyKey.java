package ru.yandex.market.deepmind.tracker_approver.pojo;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MyKey {
    private final int supplierId;
    private final String shopSku;

    @JsonCreator
    public MyKey(@JsonProperty("supplierId") int supplierId, @JsonProperty("shopSku") String shopSku) {
        this.supplierId = supplierId;
        this.shopSku = shopSku;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public String getShopSku() {
        return shopSku;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MyKey myKey = (MyKey) o;
        return supplierId == myKey.supplierId && Objects.equals(shopSku, myKey.shopSku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supplierId, shopSku);
    }

    @Override
    public String toString() {
        return "Key{" +
            "supplierId=" + supplierId +
            ", shopSku='" + shopSku + '\'' +
            '}';
    }
}
