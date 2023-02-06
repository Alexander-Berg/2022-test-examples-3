package ru.yandex.market.psku.postprocessor.common.db.dao;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.CategoryParametersCache;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.CATEGORY_PARAMETERS_CACHE;

public class CategoryParametersCacheDAOTest  extends BaseDBTest {

    private static final long DEFAULT_ID = 1L;

    @Autowired
    CategoryParametersCacheDAO categoryParametersCacheDAO;

    @Test
    public void testCheckSelectIds() {
        insertCategory();

        List<Long> ids = categoryParametersCacheDAO.getCachedCategoriesIds();

        Assertions.assertThat(ids).hasSize(1);
    }

    @Test
    public void testSelectById() {
        insertCategory();

        CategoryParametersCache categoryParametersCache = categoryParametersCacheDAO.fetchOneByCategoryId(DEFAULT_ID);

        Assertions.assertThat(categoryParametersCache).isNotNull();
        Assertions.assertThat(categoryParametersCache.getData()).isNotNull();
    }



    private void insertCategory() {
        dsl().insertInto(CATEGORY_PARAMETERS_CACHE).values(
            DEFAULT_ID,
            Timestamp.from(Instant.now()),
            MboParameters.GetCategoryParametersResponse.getDefaultInstance()
        ).execute();
    }
}
