package ru.yandex.market.mbo.db.modelstorage.index.yt;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import ru.yandex.market.mbo.db.modelstorage.index.GenericField;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author apluhin
 * @created 11/9/20
 */
@SuppressWarnings("checkstyle:magicnumber")
public class GroupYtIndexQueryTest {

    @Test
    public void shouldPrintCorrectGroupModelIndexQuery() {
        MboIndexesFilter mboIndexesFilter = new MboIndexesFilter().setModelIds(Arrays.asList(1L))
            .setGroupModelIds(Arrays.asList(3L, 4L))
            .setDeleted(false)
            .setParentIds(Collections.singleton(15L))
            .setParentIdExists(false)
            .setCurrentTypes(Arrays.asList(CommonModel.Source.CLUSTER, CommonModel.Source.TOLOKA))
            .setModelIdCursor(15L)
            .addOrderBy(GenericField.MODEL_ID, Sort.Direction.ASC);
        GroupYtIndexQuery groupYtIndexQuery = new GroupYtIndexQuery(mboIndexesFilter);
        Assertions.assertThat(String.format(YtIndexQuery.INDEX_QUERY, "/test/test/test",
            groupYtIndexQuery.query())).isEqualTo("* FROM [/test/test/test] WHERE " +
            "group_model_id in (3,4) AND " +
            "model_id in (1) AND " +
            "deleted != true AND " +
            "parent_id in (15) AND " +
            "parent_id = NULL AND " +
            "current_type in (2,4) AND " +
            "model_id > 15 " +
            "ORDER BY model_id ASC");
    }

    @Test
    public void shouldFailedWithUnsupportedField() {
        MboIndexesFilter mboIndexesFilter = new MboIndexesFilter().setModelIds(Arrays.asList(1L))
            .setGroupModelIds(Arrays.asList(3L, 4L))
            .setDeleted(false)
            .setParentIds(Collections.singleton(15L))
            .setParentIdExists(false)
            .setCurrentTypes(Arrays.asList(CommonModel.Source.CLUSTER, CommonModel.Source.TOLOKA))
            .setModelIdCursor(15L)
            .addOrderBy(GenericField.MODEL_ID, Sort.Direction.ASC);
        Assert.assertTrue(GroupYtIndexQuery.isSupportFilter(mboIndexesFilter));
        mboIndexesFilter.setVendorId(1L);
        Assert.assertFalse(GroupYtIndexQuery.isSupportFilter(mboIndexesFilter));
    }

    @Test
    public void shouldFailedWithUnsupportedSort() {
        MboIndexesFilter mboIndexesFilter = new MboIndexesFilter().setModelIds(Arrays.asList(1L))
            .setGroupModelIds(Arrays.asList(3L, 4L))
            .setCurrentTypes(Arrays.asList(CommonModel.Source.CLUSTER, CommonModel.Source.TOLOKA))
            .addOrderBy(GenericField.CLUSTERIZER_OFFER_COUNT, Sort.Direction.ASC);
        Assert.assertFalse(GroupYtIndexQuery.isSupportFilter(mboIndexesFilter));
    }

    @Test
    public void shouldReturnCorrectGroupIndexUsageAvailability() {
        MboIndexesFilter filter = new MboIndexesFilter().setModelIds(Arrays.asList(1L))
            .setGroupModelIds(Arrays.asList(2L))
            .setCategoryId(3L)
            .setVendorId(4L)
            .setDeleted(true)
            .setIsSku(false)
            .setParentIds(Collections.singleton(15L))
            .setParentIdExists(true)
            .setCurrentTypes(Arrays.asList(CommonModel.Source.GURU, CommonModel.Source.SKU))
            .setPublished(true);
        Assertions.assertThat(GroupYtIndexQuery.isSupportFilter(filter)).isFalse();

        MboIndexesFilter emptyFilter = new MboIndexesFilter()
            .setGroupModelIds(Collections.emptyList());
        Assertions.assertThat(GroupYtIndexQuery.isSupportFilter(emptyFilter)).isFalse();

        filter = new MboIndexesFilter().setModelIds(Arrays.asList(1L))
            .setGroupModelIds(Arrays.asList(2L))
            .setDeleted(true)
            .setParentIds(Collections.singleton(15L))
            .setParentIdExists(true)
            .setCurrentTypes(Arrays.asList(CommonModel.Source.GURU, CommonModel.Source.SKU));
        Assertions.assertThat(GroupYtIndexQuery.isSupportFilter(filter)).isTrue();
    }

    @Test
    public void shouldReturnCorrectQueryWithoutGroupIn() {
        MboIndexesFilter filter = new MboIndexesFilter().setCategoryId(1L);
        GroupYtIndexQuery groupYtIndexQuery = new GroupYtIndexQuery(filter);
        Assertions.assertThat(groupYtIndexQuery.query()).isEqualTo("category_id in (1)");
    }
}
