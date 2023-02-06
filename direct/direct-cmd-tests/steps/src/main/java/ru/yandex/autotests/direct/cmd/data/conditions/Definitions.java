package ru.yandex.autotests.direct.cmd.data.conditions;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Definitions {

    @SerializedName("itemsOrder")
    private List<String> itemsOrder;

    public List<String> getItemsOrder() {
        return itemsOrder;
    }

    public Definitions withItemsOrder(List<String> itemsOrder) {
        this.itemsOrder = itemsOrder;
        return this;
    }
}
