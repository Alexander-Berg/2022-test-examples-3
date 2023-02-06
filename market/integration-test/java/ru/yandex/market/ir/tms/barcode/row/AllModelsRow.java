package ru.yandex.market.ir.tms.barcode.row;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.misc.bender.annotation.Bendable;

public class AllModelsRow {
    private final ModelStorage.Model data;

    public AllModelsRow(ModelStorage.Model data) {
        this.data = data;
    }

    /**
     * Странно, что в {@link CategoriesRow} получилось использовать {@link Bendable} на поля, а здесь нет:
     * кластер даже на select * выбрасывал "library/cpp/protobuf/yql/descriptor.cpp:113: can't parse protobin message".
     */
    public YTreeMapNode toNode() {
        return YTree.mapBuilder()
            .key("data").value(data.toByteArray())
            .key("model_id").value(data.getId())
            .key("parent_id").value(data.getParentId())
            .key("current_type").value(data.getCurrentType())
            .buildMap();
    }
}
