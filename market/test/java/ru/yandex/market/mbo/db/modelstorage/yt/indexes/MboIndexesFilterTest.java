package ru.yandex.market.mbo.db.modelstorage.yt.indexes;

import java.util.Arrays;
import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.index.Operation;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

@SuppressWarnings("checkstyle:magicnumber")
public class MboIndexesFilterTest {

    @Test
    public void shouldBeCopiedCorrectly() {
        MboIndexesFilter filter = new MboIndexesFilter().setModelIds(Arrays.asList(1L))
            .setGroupModelIds(Arrays.asList(2L))
            .setCategoryId(3L)
            .setVendorId(4L)
            .setDeleted(true)
            .setIsSku(false)
            .setParentIds(Collections.singleton(15L))
            .setParentIdExists(true)
            .setSourceTypes(Arrays.asList(CommonModel.Source.GURU, CommonModel.Source.SKU))
            .setCreatedDate(1000000L, Operation.EQ)
            .setDeletedDate(1000000L, Operation.EQ)
            .setModifiedDate(1000000L, Operation.EQ)
            .setChecked(true, Operation.EQ)
            .setDoubtful(true, Operation.EQ)
            .setHasSyncTarget(true, Operation.EQ)
            .setAlias("alias", Operation.EQ)
            .setTitle("title", Operation.EQ)
            .setPublished(true);
        MboIndexesFilter copy = MboIndexesFilter.copyOf(filter);
        Assertions.assertThat(filter == copy).isFalse();
        Assertions.assertThat(filter).isEqualTo(copy);
    }

}
