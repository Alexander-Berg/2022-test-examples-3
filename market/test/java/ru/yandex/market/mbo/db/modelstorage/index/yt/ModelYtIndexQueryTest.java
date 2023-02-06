package ru.yandex.market.mbo.db.modelstorage.index.yt;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.gwt.models.ParamValueSearch;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author apluhin
 * @created 11/9/20
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ModelYtIndexQueryTest {

    @Test
    public void shouldReturnCorrectModelIndexUsageAvailability() {
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
        Assertions.assertThat(ModelYtIndexQuery.isSupportFilter(filter)).isFalse();

        MboIndexesFilter emptyFilter = new MboIndexesFilter()
            .setModelIds(Collections.emptyList());
        Assertions.assertThat(ModelYtIndexQuery.isSupportFilter(emptyFilter)).isFalse();

        filter = new MboIndexesFilter().setModelIds(Arrays.asList(1L))
            .setGroupModelIds(Arrays.asList(2L))
            .setCategoryId(3L)
            .setDeleted(true)
            .setIsSku(false)
            .setParentIds(Collections.singleton(15L))
            .setParentIdExists(true)
            .setCurrentTypes(Arrays.asList(CommonModel.Source.GURU, CommonModel.Source.SKU))
            .setPublished(true);
        Assertions.assertThat(ModelYtIndexQuery.isSupportFilter(filter)).isTrue();

        filter = new MboIndexesFilter().setModelIds(Arrays.asList(1L));
        Assertions.assertThat(ModelYtIndexQuery.isSupportFilter(filter)).isTrue();
    }


    @Test
    public void shouldPrintCorrectModelIndexQuery() {
        MboIndexesFilter filter = new MboIndexesFilter().setModelIds(Arrays.asList(1L, 2L))
            .setGroupModelIds(Arrays.asList(2L))
            .setCategoryId(3L)
            .setDeleted(true)
            .setIsSku(false)
            .setParentIds(Collections.singleton(15L))
            .setParentIdExists(true)
            .setCurrentTypes(Arrays.asList(CommonModel.Source.GURU, CommonModel.Source.SKU))
            .setPublished(true);
        ModelYtIndexQuery query = new ModelYtIndexQuery(filter);
        Assertions.assertThat(String.format(YtIndexQuery.INDEX_QUERY, "/test/test/test", query.query()))
            .isEqualTo("* FROM [/test/test/test] WHERE " +
                "model_id in (1,2) AND " +
                "category_id in (3) AND " +
                "deleted = true AND " +
                "group_model_id in (2) AND " +
                "parent_id in (15) AND " +
                "parent_id != NULL AND " +
                "current_type in (1,7) AND " +
                "is_sku = false AND " +
                "published = true");
    }

    @Test
    public void shouldPrintCorrectAttributesWithParam() {
        MboIndexesFilter filter = new MboIndexesFilter().setModelIds(Arrays.asList(1L, 2L))
            .addAttribute(new ParamValueSearch(XslNames.IS_SKU,
                ParameterValue.ValueBuilder.newBuilder().setBooleanValue(true)));
        ModelYtIndexQuery query = new ModelYtIndexQuery(filter);
        Assertions.assertThat(String.format(YtIndexQuery.INDEX_QUERY, "/test/test/test", query.query()))
            .isEqualTo("* FROM [/test/test/test] WHERE " +
                "model_id in (1,2) AND " +
                "is_sku = true");
        Assertions.assertThat(filter.getAttributes().size()).isEqualTo(1);
    }

}
