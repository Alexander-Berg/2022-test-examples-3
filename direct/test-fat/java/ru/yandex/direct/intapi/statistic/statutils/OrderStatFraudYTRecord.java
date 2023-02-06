package ru.yandex.direct.intapi.statistic.statutils;

import java.util.List;

import javax.annotation.Nullable;

import ru.yandex.direct.intapi.utils.ColumnInfo;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static java.util.Arrays.asList;

public class OrderStatFraudYTRecord {
    static final List<ColumnInfo> YT_COLUMNS = asList(
            new ColumnInfo("YTHash", "int64", "int64(farm_hash(OrderID))", true),
            new ColumnInfo("OrderID", "int64", true),
            new ColumnInfo("UpdateTime", "int64", true),
            new ColumnInfo("FraudShows", "int64", false),
            new ColumnInfo("FraudClicks", "int64", false),
            new ColumnInfo("FraudCost", "int64", false),
            new ColumnInfo("GiftShows", "int64", false),
            new ColumnInfo("GiftClicks", "int64", false),
            new ColumnInfo("GiftCost", "int64", false),
            new ColumnInfo("TotalShows", "int64", false),
            new ColumnInfo("FraudShowsGeneral", "int64", false),
            new ColumnInfo("FraudShowsSophisticated", "int64", false)
    );

    private long orderID;
    private long updateTime;
    private Long fraudShows;
    private Long fraudClicks;
    private Long fraudCost;
    private Long giftShows;
    private Long giftClicks;
    private Long giftCost;
    private Long totalShows;
    private Long fraudShowsGeneral;
    private Long fraudShowsSophisticated;

    public YTreeMapNode buildMapNode() {
        YTreeBuilder builder = YTree.mapBuilder();
        setColumn(builder, "OrderID", orderID);
        setColumn(builder, "UpdateTime", updateTime);
        setColumn(builder, "FraudShows", fraudShows);
        setColumn(builder, "FraudClicks", fraudClicks);
        setColumn(builder, "FraudCost", fraudCost);
        setColumn(builder, "GiftShows", giftShows);
        setColumn(builder, "GiftClicks", giftClicks);
        setColumn(builder, "GiftCost", giftCost);
        setColumn(builder, "TotalShows", totalShows);
        setColumn(builder, "FraudShowsGeneral", fraudShowsGeneral);
        setColumn(builder, "FraudShowsSophisticated", fraudShowsSophisticated);
        return builder.buildMap();
    }

    private <T> void setColumn(YTreeBuilder builder, String field, @Nullable T value) {
        if (value == null) {
            builder.key(field).entity();
        } else {
            builder.key(field).value(value);
        }
    }

    public OrderStatFraudYTRecord withOrderID(long orderID) {
        this.orderID = orderID;
        return this;
    }

    public OrderStatFraudYTRecord withUpdateTime(long updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public OrderStatFraudYTRecord withFraudShows(Long fraudShows) {
        this.fraudShows = fraudShows;
        return this;
    }

    public OrderStatFraudYTRecord withFraudClicks(Long fraudClicks) {
        this.fraudClicks = fraudClicks;
        return this;
    }

    public OrderStatFraudYTRecord withFraudCost(Long fraudCost) {
        this.fraudCost = fraudCost;
        return this;
    }

    public OrderStatFraudYTRecord withGiftShows(Long giftShows) {
        this.giftShows = giftShows;
        return this;
    }

    public OrderStatFraudYTRecord withGiftClicks(Long giftClicks) {
        this.giftClicks = giftClicks;
        return this;
    }

    public OrderStatFraudYTRecord withGiftCost(Long giftCost) {
        this.giftCost = giftCost;
        return this;
    }

    public OrderStatFraudYTRecord withTotalShows(Long totalShows) {
        this.totalShows = totalShows;
        return this;
    }

    public OrderStatFraudYTRecord withFraudShowsGeneral(Long fraudShowsGeneral) {
        this.fraudShowsGeneral = fraudShowsGeneral;
        return this;
    }

    public OrderStatFraudYTRecord withFraudShowsSophisticated(Long fraudShowsSophisticated) {
        this.fraudShowsSophisticated = fraudShowsSophisticated;
        return this;
    }
}
