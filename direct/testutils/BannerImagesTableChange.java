package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BannerImagesTableChange extends BaseTableChange {
    public long imageId;

    public static BinlogEvent createBannerImagesEvent(List<BannerImagesTableChange> bannerImagesTableChanges,
                                                      Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(BANNER_IMAGES.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = bannerImagesTableChanges.stream()
                .map(bannerImagesTableChange -> createBannerImagesTableRow(bannerImagesTableChange, operation))
                .collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createBannerImagesTableRow(BannerImagesTableChange bannerImagesTableChange,
                                                              Operation operation) {
        Map<String, Object> primaryKeys = Map.of(BANNER_IMAGES.IMAGE_ID.getName(), bannerImagesTableChange.imageId);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, bannerImagesTableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public BannerImagesTableChange withImageId(long imageId) {
        this.imageId = imageId;
        return this;
    }
}
