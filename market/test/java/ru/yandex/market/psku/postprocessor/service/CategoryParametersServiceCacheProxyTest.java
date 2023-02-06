package ru.yandex.market.psku.postprocessor.service;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.CategoryParametersCacheDAO;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.CategoryParametersCache;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.CATEGORY_PARAMETERS_CACHE;

public class CategoryParametersServiceCacheProxyTest extends BaseDBTest {

    private static final long DEFAULT_ID = 1L;

    @Autowired
    private CategoryParametersCacheDAO categoryParametersCacheDAO;
    private CategoryParametersService categoryParametersService;

    @Before
    public void setup() {
         categoryParametersService = mock(CategoryParametersService.class);
         when(categoryParametersService.getParameters(any()))
                 .thenReturn(MboParameters.GetCategoryParametersResponse.getDefaultInstance());
    }


    @Test
    public void testGetFromCache() {
        insertCategory();

        CategoryParametersServiceCacheProxy cacheProxy =
            new CategoryParametersServiceCacheProxy(categoryParametersService, categoryParametersCacheDAO);

        MboParameters.GetCategoryParametersResponse response = cacheProxy.getParameters(
             MboParameters.GetCategoryParametersRequest
                 .newBuilder()
                 .setCategoryId(DEFAULT_ID)
                 .setTimestamp(0)
                 .build()
         );

        assertThat(response).isNotNull();
    }

    @Test
    public void testCacheValue() {
        CategoryParametersServiceCacheProxy cacheProxy =
            new CategoryParametersServiceCacheProxy(categoryParametersService,categoryParametersCacheDAO);

        MboParameters.GetCategoryParametersResponse response = cacheProxy.getParameters(
            MboParameters.GetCategoryParametersRequest
                .newBuilder()
                .setCategoryId(DEFAULT_ID)
                .setTimestamp(0)
                .build()
        );

        assertThat(response).isNotNull();

        List<Long> ids = categoryParametersCacheDAO.getCachedCategoriesIds();

        assertThat(ids).hasSize(1).contains(DEFAULT_ID);
    }

    @Test
    public void testUpdateCacheValue() {
        CategoryParametersServiceCacheProxy cacheProxy =
                new CategoryParametersServiceCacheProxy(categoryParametersService,categoryParametersCacheDAO);

        insertCategory(Timestamp.from(Instant.ofEpochMilli(0L)));

        cacheProxy.getParameters(
                MboParameters.GetCategoryParametersRequest
                        .newBuilder()
                        .setCategoryId(DEFAULT_ID)
                        .setTimestamp(0)
                        .build()
        );

        CategoryParametersCache cache = categoryParametersCacheDAO.fetchOneByCategoryId(DEFAULT_ID);

        assertThat(cache).isNotNull();
        assertThat(cache.getCreateTime().getTime()).isGreaterThan(0L);
    }

    private void insertCategory(Timestamp timestamp) {
        dsl().insertInto(CATEGORY_PARAMETERS_CACHE).values(
            DEFAULT_ID,
            timestamp,
            MboParameters.GetCategoryParametersResponse.getDefaultInstance()
        ).execute();
    }

    private void insertCategory() {
        insertCategory(Timestamp.from(Instant.now()));
    }

}
