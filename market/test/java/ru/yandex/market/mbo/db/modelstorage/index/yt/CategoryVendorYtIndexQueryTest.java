package ru.yandex.market.mbo.db.modelstorage.index.yt;

import org.assertj.core.api.Assertions;
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
public class CategoryVendorYtIndexQueryTest {

    @Test
    public void shouldPrintCorrectCategoryVendorIndexQuery() {
        MboIndexesFilter mboIndexesFilter = new MboIndexesFilter().setModelIds(Arrays.asList(1L))
            .setCategoryId(3L)
            .setVendorId(4L)
            .setDeleted(true)
            .setParentIds(Collections.singleton(15L))
            .setParentIdExists(true)
            .setCurrentTypes(Arrays.asList(CommonModel.Source.GURU, CommonModel.Source.SKU));
        Assertions.assertThat(String.format(YtIndexQuery.INDEX_QUERY, "/test/test/test",
            new CategoryVendorYtIndexQuery(mboIndexesFilter).query())).isEqualTo("* FROM [/test/test/test] WHERE " +
            "category_id in (3) AND " +
            "vendor_id in (4) AND " +
            "current_type in (1,7) AND " +
            "model_id in (1) AND " +
            "deleted = true AND " +
            "parent_id in (15) AND " +
            "parent_id != NULL");
    }

    @Test
    public void shouldPrintCorrectCategoryVendorIndexQueryIgnoringOrderByForCount() {
        MboIndexesFilter mboIndexesFilter = new MboIndexesFilter().setModelIds(Arrays.asList(1L))
            .setCategoryId(3L)
            .setVendorId(4L)
            .setDeleted(true)
            .setParentIds(Collections.singleton(15L))
            .setParentIdExists(true)
            .setCurrentTypes(Arrays.asList(CommonModel.Source.GURU, CommonModel.Source.SKU))
            .setLimit(50)
            .setOffset(50)
            .addOrderBy(GenericField.MODEL_ID, Sort.Direction.ASC);
        Assertions.assertThat(String.format(YtIndexQuery.INDEX_QUERY_COUNT, "/test/test/test",
            new CategoryVendorYtIndexQuery(mboIndexesFilter).query(false))).isEqualTo(
            "sum(1) as counted  FROM [/test/test/test] WHERE " +
                "category_id in (3) AND " +
                "vendor_id in (4) AND " +
                "current_type in (1,7) AND " +
                "model_id in (1) AND " +
                "deleted = true AND " +
                "parent_id in (15) AND " +
                "parent_id != NULL " +
                "GROUP BY 1");
    }


    @Test
    public void shouldReturnCategoryVendorIndexUsageAvailability() {
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
        Assertions.assertThat(CategoryVendorYtIndexQuery.isSupportFilter(filter)).isFalse();

        MboIndexesFilter emptyFilter = new MboIndexesFilter()
            .setCategoryIds(Collections.emptyList());
        Assertions.assertThat(CategoryVendorYtIndexQuery.isSupportFilter(emptyFilter)).isFalse();

        filter = new MboIndexesFilter().setModelIds(Arrays.asList(1L))
            .setCategoryId(3L)
            .setVendorId(4L)
            .setDeleted(true)
            .setParentIds(Collections.singleton(15L))
            .setParentIdExists(true)
            .setCurrentTypes(Arrays.asList(CommonModel.Source.GURU, CommonModel.Source.SKU));
        Assertions.assertThat(CategoryVendorYtIndexQuery.isSupportFilter(filter)).isTrue();
    }

}
