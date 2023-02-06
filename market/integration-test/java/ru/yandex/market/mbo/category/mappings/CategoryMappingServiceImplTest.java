package ru.yandex.market.mbo.category.mappings;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.dbselector.DbType;
import ru.yandex.market.mbo.configs.TestConfiguration;
import ru.yandex.market.mbo.db.MboDbSelector;
import ru.yandex.market.mbo.db.errors.CategoryNotFoundException;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Тесты проверяют корректность ${@link CategoryMappingServiceImpl}.
 * Тесты не валидируют данные, они всего лишь проверяют, что запросы отрабатывают корректно.
 *
 * @author s-ermakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class})
public class CategoryMappingServiceImplTest {

    private static final Logger log = LogManager.getLogger(CategoryMappingServiceImplTest.class);
    private static final long UNREAL_CATEGORY_ID = 12345678910L;
    private static final long UNREAL_GURU_CATEGORY_ID = 12345678910L;

    @Resource
    private CategoryMappingServiceImpl categoryMappingService;

    @Resource(name = "contentJdbcTemplate")
    private JdbcTemplate contentJdbcTemplate;

    @Resource(name = "categoryMappingServiceDbSelector")
    private MboDbSelector categoryMappingServiceDbSelector;

    @Test
    public void testGetCategoryIdByGuruCategoryId() {
        long guruCategoryId = getGuruCategoryId();
        long categoryId = categoryMappingService.getCategoryIdByGuruCategoryId(guruCategoryId);
        Assertions.assertThat(categoryId).isGreaterThan(0);
    }

    @Test(expected = CategoryNotFoundException.class)
    public void testGetCategoryIdByGuruCategoryIdWithUnRealCategoryId() {
        categoryMappingService.getCategoryIdByGuruCategoryId(UNREAL_GURU_CATEGORY_ID);
    }

    @Test
    public void testGetGuruCategoryByCategoryId() {
        long categoryId = getCategoryId();
        Long guruCategoryId = categoryMappingService.getGuruCategoryByCategoryId(categoryId);
        Assertions.assertThat(guruCategoryId).isGreaterThan(0);
    }

    @Test
    public void testGetGuruCategoryByCategoryIdWithEmptyGuruCategory() {
        long categoryId = getCategoryIdWithEmptyGuruId();
        Long guruCategoryId = categoryMappingService.getGuruCategoryByCategoryId(categoryId);
        Assertions.assertThat(guruCategoryId).isNull();
    }

    @Test(expected = CategoryNotFoundException.class)
    public void testGetGuruCategoryByCategoryIdWithUnRealCategoryId() {
        categoryMappingService.getGuruCategoryByCategoryId(UNREAL_CATEGORY_ID);
    }

    @Test
    public void testGetAllGuruCategoryMappings() {
        List<CategoryMapping> mappings = categoryMappingService.getAllGuruCategoryMappings();
        Assertions.assertThat(mappings).isNotEmpty();
    }

    @Test
    public void testGetCategoryMappingsByCategoryIds() {
        List<Long> categoryIds = Arrays.asList(getCategoryId(), getCategoryIdWithEmptyGuruId());
        List<CategoryMapping> mappings = categoryMappingService.getCategoryMappingsByCategoryIds(categoryIds);

        Assertions.assertThat(mappings).extracting(CategoryMapping::getCategoryId)
            .containsOnlyElementsOf(categoryIds);
    }

    @Test
    public void testEmptyResultInGetCategoryMappingsByCategoryIds() {
        List<CategoryMapping> mappings = categoryMappingService
            .getCategoryMappingsByCategoryIds(Collections.emptyList());

        Assertions.assertThat(mappings).isEmpty();
    }

    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void testEmptyResultInGetCategoryMappingsByCategoryIdsWithWrongId() {
        List<CategoryMapping> mappings = categoryMappingService
            .getCategoryMappingsByCategoryIds(Collections.singleton(-188181L));

        Assertions.assertThat(mappings).isEmpty();
    }

    @Test
    public void testGetCategoryMappingsByGuruCategoryIds() {
        List<Long> guruCategoryIds = Arrays.asList(getGuruCategoryId(), getOtherGuruCategoryId());
        List<CategoryMapping> mappings = categoryMappingService
            .getCategoryMappingsByGuruCategoryIds(guruCategoryIds);

        Assertions.assertThat(mappings).extracting(CategoryMapping::getGuruCategoryId)
            .containsOnlyElementsOf(guruCategoryIds);
    }

    @Test
    public void testEmptyResultInGetCategoryMappingsByGuruCategoryIds() {
        List<CategoryMapping> mappings = categoryMappingService
            .getCategoryMappingsByGuruCategoryIds(Collections.emptyList());

        Assertions.assertThat(mappings).isEmpty();
    }

    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void testEmptyResultInGetCategoryMappingsByGuruCategoryIdsWithWrongId() {
        List<CategoryMapping> mappings = categoryMappingService
            .getCategoryMappingsByGuruCategoryIds(Collections.singleton(-188181L));

        Assertions.assertThat(mappings).isEmpty();
    }

    private long getGuruCategoryId() {
        JdbcTemplate jdbcTemplate = categoryMappingServiceDbSelector.getProxyingJdbcTemplate();
        Long guruCategoryId;
        if (categoryMappingServiceDbSelector.getDbType() == DbType.ORACLE) {
            guruCategoryId = jdbcTemplate.queryForObject(
                "select guru_category_id from mc_category WHERE guru_category_id is not null AND ROWNUM = 1",
                Long.class
            );
        } else if (categoryMappingServiceDbSelector.getDbType() == DbType.POSTGRESQL) {
            guruCategoryId = jdbcTemplate.queryForObject(
                "select guru_category_id from mc_category WHERE guru_category_id is not null limit 1",
                Long.class
            );
        } else  {
            throw new IllegalStateException(String.format("Unknown database type %s",
                categoryMappingServiceDbSelector.getDbType()));
        }
        log.info("guruCategoryId: " + guruCategoryId);
        return guruCategoryId;
    }

    private long getOtherGuruCategoryId() {
        JdbcTemplate jdbcTemplate = categoryMappingServiceDbSelector.getProxyingJdbcTemplate();
        Long guruCategoryId;
        if (categoryMappingServiceDbSelector.getDbType() == DbType.ORACLE) {
            guruCategoryId = jdbcTemplate.queryForObject(
                "select guru_category_id from (select guru_category_id, ROWNUM AS RN from mc_category " +
                    "WHERE guru_category_id is not null) WHERE RN = 2", Long.class
            );
        } else if (categoryMappingServiceDbSelector.getDbType() == DbType.POSTGRESQL) {
            guruCategoryId = jdbcTemplate.queryForObject(
                "select guru_category_id \n" +
                    "from (select guru_category_id, row_number() over () as rn \n" +
                    "      from market_content.mc_category\n" +
                    "      where guru_category_id is not null) guru_cat \n" +
                    "where rn= 2;", Long.class
            );
        } else  {
            throw new IllegalStateException(String.format("Unknown database type %s",
                categoryMappingServiceDbSelector.getDbType()));
        }
        log.info("other guruCategoryId: " + guruCategoryId);
        return guruCategoryId;
    }

    private long getCategoryId() {
        JdbcTemplate jdbcTemplate = categoryMappingServiceDbSelector.getProxyingJdbcTemplate();
        Long categoryId;
        if (categoryMappingServiceDbSelector.getDbType() == DbType.ORACLE) {
            categoryId = jdbcTemplate.queryForObject(
                "select hyper_id from mc_category WHERE guru_category_id is not null AND ROWNUM = 1", Long.class
            );
        } else if (categoryMappingServiceDbSelector.getDbType() == DbType.POSTGRESQL) {
            categoryId = jdbcTemplate.queryForObject(
                "select hyper_id from mc_category WHERE guru_category_id is not null limit 1", Long.class
            );
        } else  {
            throw new IllegalStateException(String.format("Unknown database type %s",
                categoryMappingServiceDbSelector.getDbType()));
        }
        log.info("categoryId: " + categoryId);
        return categoryId;
    }

    private long getCategoryIdWithEmptyGuruId() {
        JdbcTemplate jdbcTemplate = categoryMappingServiceDbSelector.getProxyingJdbcTemplate();
        Long categoryId;
        if (categoryMappingServiceDbSelector.getDbType() == DbType.ORACLE) {
            categoryId = categoryMappingServiceDbSelector.getProxyingJdbcTemplate().queryForObject(
                "select hyper_id from mc_category WHERE guru_category_id is null AND ROWNUM = 1", Long.class
            );
        } else if (categoryMappingServiceDbSelector.getDbType() == DbType.POSTGRESQL) {
            categoryId = categoryMappingServiceDbSelector.getProxyingJdbcTemplate().queryForObject(
                "select hyper_id from mc_category WHERE guru_category_id is null limit 1", Long.class
            );
        } else  {
            throw new IllegalStateException(String.format("Unknown database type %s",
                categoryMappingServiceDbSelector.getDbType()));
        }
        log.info("categoryId with empty guruCategoryId: " + categoryId);
        return categoryId;
    }
}
