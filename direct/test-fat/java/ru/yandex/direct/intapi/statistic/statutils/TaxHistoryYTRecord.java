package ru.yandex.direct.intapi.statistic.statutils;

import java.util.List;

import javax.annotation.Nullable;

import ru.yandex.direct.intapi.utils.ColumnInfo;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

public class TaxHistoryYTRecord {
    static final List<ColumnInfo> YT_COLUMNS = List.of(
            new ColumnInfo("YTHash", "int64", "int64(farm_hash(TaxID,StartDate))", true),
            new ColumnInfo("TaxID", "int64", true),
            new ColumnInfo("StartDate", "int64", true),
            new ColumnInfo("Percent", "int64", false)
    );

    private long taxID;
    private long startDate;
    private long percent;

    public YTreeMapNode buildMapNode() {
        YTreeBuilder builder = YTree.mapBuilder();
        setColumn(builder, "TaxID", taxID);
        setColumn(builder, "StartDate", startDate);
        setColumn(builder, "Percent", percent);
        return builder.buildMap();
    }

    private <T> void setColumn(YTreeBuilder builder, String field, @Nullable T value) {
        if (value == null) {
            builder.key(field).entity();
        } else {
            builder.key(field).value(value);
        }
    }

    public TaxHistoryYTRecord withTaxID(long taxID) {
        this.taxID = taxID;
        return this;
    }

    public TaxHistoryYTRecord withStartDate(long startDate) {
        this.startDate = startDate;
        return this;
    }

    public TaxHistoryYTRecord withPercent(long percent) {
        this.percent = percent;
        return this;
    }
}
