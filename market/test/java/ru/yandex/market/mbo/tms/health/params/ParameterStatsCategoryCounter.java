package ru.yandex.market.mbo.tms.health.params;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.db.CachedSizeMeasureService;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"checkstyle:LineLength", "checkstyle:MagicNumber"})
public class ParameterStatsCategoryCounter extends ParametersCounterTest {
    private static final long CATEGORY_ID = 1L;


    @Test
    public void addParametersForCategory() {
        ParameterStatsCounter counter = createCounter();
        counter.addParametersForCategory(getParams(), CATEGORY_ID);
        Assert.assertEquals(3, counter.getParametersCount());
    }

    private List<CategoryParam> getParams() {
        CategoryParamBuilder paramBuilder = CategoryParamBuilder.newBuilder();
        return Arrays.asList(
                paramBuilder
                        .setId(PARAM_ID)
                        .setCategoryHid(CATEGORY_ID)
                        .setUseForGuru(Boolean.TRUE)
                        .build(),
                paramBuilder
                        .setId(PARAM_ID + 1)
                        .setCategoryHid(CATEGORY_ID)
                        .setUseForGuru(Boolean.TRUE)
                        .build(),
                paramBuilder
                        .setId(PARAM_ID + 2)
                        .setCategoryHid(CATEGORY_ID + 1)
                        .setUseForGuru(Boolean.TRUE)
                        .build(),
                paramBuilder
                        .setId(PARAM_ID + 3)
                        .setCategoryHid(CATEGORY_ID)
                        .setUseForGuru(Boolean.TRUE)
                        .build(),
                paramBuilder
                        .setId(PARAM_ID + 4)
                        .setCategoryHid(CATEGORY_ID + 2)
                        .setUseForGuru(Boolean.TRUE)
                        .build(),
                paramBuilder
                        .setId(SIZE_MEASURE_PARAM_ID)
                        .setCategoryHid(CATEGORY_ID + 2)
                        .setUseForGuru(Boolean.FALSE)
                        .build()
        );
    }

    private ParameterStatsCounter createCounter() {
        CachedSizeMeasureService cachedSizeMeasureService =
                mockSizeMeasureService(SIZE_MEASURE_PARAM_ID);
        ParameterStatsCounter counter = new ParameterStatsCounter(cachedSizeMeasureService);
        return counter;
    }
}
