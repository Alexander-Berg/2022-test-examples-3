package ru.yandex.direct.intapi.statistic.statutils;

import java.util.List;

import javax.annotation.Nullable;

import ru.yandex.direct.intapi.utils.ColumnInfo;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

public class OrderInfoYTRecord {
    static final List<ColumnInfo> YT_COLUMNS = List.of(
            new ColumnInfo("YTHash", "int64", "int64(farm_hash(OrderID))", true),
            new ColumnInfo("OrderID", "int64", true),
            new ColumnInfo("TaxID", "int64", false),
            new ColumnInfo("CurrencyID", "int64", false)
    );

    private long orderID;
    private long taxID;
    private long currencyID;

    public YTreeMapNode buildMapNode() {
        YTreeBuilder builder = YTree.mapBuilder();
        setColumn(builder, "OrderID", orderID);
        setColumn(builder, "TaxID", taxID);
        setColumn(builder, "CurrencyID", currencyID);
        return builder.buildMap();
    }

    private <T> void setColumn(YTreeBuilder builder, String field, @Nullable T value) {
        if (value == null) {
            builder.key(field).entity();
        } else {
            builder.key(field).value(value);
        }
    }

    public OrderInfoYTRecord withOrderID(long orderID) {
        this.orderID = orderID;
        return this;
    }

    public OrderInfoYTRecord withTaxID(long taxID) {
        this.taxID = taxID;
        return this;
    }

    public OrderInfoYTRecord withCurrencyID(long currencyID) {
        this.currencyID = currencyID;
        return this;
    }
}
