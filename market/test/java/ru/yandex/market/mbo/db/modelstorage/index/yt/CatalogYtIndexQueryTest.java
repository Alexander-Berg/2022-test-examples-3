package ru.yandex.market.mbo.db.modelstorage.index.yt;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.db.modelstorage.index.FilterHelper;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.gwt.models.ParamValueSearch;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.utils.XslNames;

/**
 * @author apluhin
 * @created 1/15/21
 */
@SuppressWarnings("checkstyle:magicnumber")
public class CatalogYtIndexQueryTest {

    @Test
    public void convertParamValuesToIndexFields() {
        MboIndexesFilter filter = new MboIndexesFilter();
        filter
            .setModelId(1000L)
            .setVendorId(1001L)
            .setCategoryId(1002L)
            .addAttribute(new ParamValueSearch(XslNames.OPERATOR_SIGN,
                ParameterValue.ValueBuilder.newBuilder().setBooleanValue(false)))
            .addAttribute(FilterHelper.getOnlyOperatorQualityCriteria(true));

        String query = new CatalogYtIndexQuery(filter).query(false);
        Assertions.assertThat(
            query
        ).isEqualTo("category_id in (1002) AND vendor_id in (1001) AND model_id " +
            "in (1000) AND quality in (17693316) AND operator_sign = false");
        Assertions.assertThat(CatalogYtIndexQuery.isSupportFilter(filter));
        Assertions.assertThat(filter.getAttributes().size()).isEqualTo(2);
    }
}
