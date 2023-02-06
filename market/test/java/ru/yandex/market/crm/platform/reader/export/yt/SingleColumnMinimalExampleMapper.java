package ru.yandex.market.crm.platform.reader.export.yt;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.models.MinimalExample;

public class SingleColumnMinimalExampleMapper implements FactYtRowMapper<MinimalExample> {

    private int rowNumberOnError = -1;
    private AtomicInteger rowsRead = new AtomicInteger(0);

    @Override
    public MinimalExample map(YTreeMapNode row) {
        if (rowNumberOnError == rowsRead.incrementAndGet()) {
            // чтобы избежать перехвата в родителе
            throw new Error();
        }

        Map<String, YTreeNode> map = row.asMap();
        String testColumn = map.get("test_column").stringValue();

        return MinimalExample.newBuilder()
                .setUserIds(UserIds.newBuilder().setYandexuid(testColumn))
                .setTimestamp(System.currentTimeMillis())
                .build();
    }

    public void setRowNumberOnError(int rowNumberOnError) {
        this.rowNumberOnError = rowNumberOnError;
    }

    public void reset() {
        rowsRead.set(0);
        rowNumberOnError = -1;
    }
}
