package ru.yandex.direct.intapi.statistic.statutils;

import java.util.List;

import javax.annotation.Nullable;

import ru.yandex.direct.intapi.utils.ColumnInfo;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

public class OrderStatDayYTRecord {
    static final List<ColumnInfo> YT_COLUMNS = List.of(
            new ColumnInfo("YTHash", "int64", "int64(farm_hash(OrderID))", true),
            new ColumnInfo("OrderID", "int64", true),
            new ColumnInfo("UpdateTime", "int64", true),
            new ColumnInfo("IsSearch", "boolean", true),
            new ColumnInfo("Shows", "int64", false),
            new ColumnInfo("OfferShows", "int64", false),
            new ColumnInfo("SmartTgoShows", "int64", false),
            new ColumnInfo("Clicks", "int64", false),
            new ColumnInfo("Cost", "int64", false),
            new ColumnInfo("CostCur", "int64", false),
            new ColumnInfo("SessionNum", "int64", false),
            new ColumnInfo("SessionLen", "int64", false),
            new ColumnInfo("SessionCost", "int64", false),
            new ColumnInfo("GoalsNum", "int64", false),
            new ColumnInfo("SessionDepth", "int64", false),
            new ColumnInfo("PriceCur", "int64", false)
    );

    private long orderID;
    private long updateTime;
    private boolean isSearch;
    private Long shows;
    private Long offerShows;
    private Long smartTgoShows;
    private Long clicks;
    private Long cost;
    private Long costCur;
    private Long sessionNum;
    private Long sessionLen;
    private Long sessionCost;
    private Long goalsNum;
    private Long sessionDepth;
    private Long priceCur;

    public YTreeMapNode buildMapNode() {
        YTreeBuilder builder = YTree.mapBuilder();
        setColumn(builder, "OrderID", orderID);
        setColumn(builder, "UpdateTime", updateTime);
        setColumn(builder, "IsSearch", isSearch);
        setColumn(builder, "Shows", shows);
        setColumn(builder, "OfferShows", offerShows);
        setColumn(builder, "SmartTgoShows", smartTgoShows);
        setColumn(builder, "Clicks", clicks);
        setColumn(builder, "Cost", cost);
        setColumn(builder, "CostCur", costCur);
        setColumn(builder, "SessionNum", sessionNum);
        setColumn(builder, "SessionLen", sessionLen);
        setColumn(builder, "SessionCost", sessionCost);
        setColumn(builder, "GoalsNum", goalsNum);
        setColumn(builder, "SessionDepth", sessionDepth);
        setColumn(builder, "PriceCur", priceCur);
        return builder.buildMap();
    }

    private <T> void setColumn(YTreeBuilder builder, String field, @Nullable T value) {
        if (value == null) {
            builder.key(field).entity();
        } else {
            builder.key(field).value(value);
        }
    }


    public OrderStatDayYTRecord withOrderID(long orderID) {
        this.orderID = orderID;
        return this;
    }

    public OrderStatDayYTRecord withUpdateTime(long updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public OrderStatDayYTRecord withIsSearch(boolean isSearch) {
        this.isSearch = isSearch;
        return this;
    }

    public OrderStatDayYTRecord withShows(Long shows) {
        this.shows = shows;
        return this;
    }

    public OrderStatDayYTRecord withOfferShows(Long offerShows) {
        this.offerShows = offerShows;
        return this;
    }

    public OrderStatDayYTRecord withSmartTgoShows(Long smartTgoShows) {
        this.smartTgoShows = smartTgoShows;
        return this;
    }

    public OrderStatDayYTRecord withClicks(Long clicks) {
        this.clicks = clicks;
        return this;
    }

    public OrderStatDayYTRecord withCost(Long cost) {
        this.cost = cost;
        return this;
    }

    public OrderStatDayYTRecord withCostCur(Long costCur) {
        this.costCur = costCur;
        return this;
    }

    public OrderStatDayYTRecord withSessionNum(Long sessionNum) {
        this.sessionNum = sessionNum;
        return this;
    }

    public OrderStatDayYTRecord withSessionLen(Long sessionLen) {
        this.sessionLen = sessionLen;
        return this;
    }

    public OrderStatDayYTRecord withSessionCost(Long sessionCost) {
        this.sessionCost = sessionCost;
        return this;
    }

    public OrderStatDayYTRecord withGoalsNum(Long goalsNum) {
        this.goalsNum = goalsNum;
        return this;
    }

    public OrderStatDayYTRecord withSessionDepth(Long sessionDepth) {
        this.sessionDepth = sessionDepth;
        return this;
    }

    public OrderStatDayYTRecord withPriceCur(Long priceCur) {
        this.priceCur = priceCur;
        return this;
    }
}
