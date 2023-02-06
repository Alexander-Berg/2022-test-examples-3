package ru.yandex.market.mbo.db.size.dao;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.market.mbo.db.size.model.Size;
import ru.yandex.market.mbo.db.size.model.SizeChartMeasure;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

@SuppressWarnings("checkstyle:magicNumber")
public class SizeChartSizeDaoTest {

    private SizeChartSizeDao sizeChartSizeDao;
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        String dbName = getClass().getSimpleName() + UUID.randomUUID();

        dataSource.setUrl(
            "jdbc:h2:mem:" + dbName +
                ";INIT=RUNSCRIPT FROM 'classpath:ru/yandex/market/mbo/db/size/init_size_chart.sql'" +
                ";MODE=Oracle"
        );

        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(dataSource);
        jdbcTemplate = spy(namedTemplate);

        sizeChartSizeDao = new SizeChartSizeDao(jdbcTemplate);
    }

    @Test
    public void removeSizeChartSizeByIdsTest() {
        List<Size> data = Stream.of(
            generateSize("42", 1L, null, null),
            generateSize("43", 2L, null, null),
            generateSize("44", 3L, null, null)
        ).collect(Collectors.toList());

        sizeChartSizeDao.insertSizeChartSizes(data, 1L);

        List<Long> idsForRemove = data.stream().map(Size::getId).limit(2).collect(Collectors.toList());
        sizeChartSizeDao.removeSizeChartSizeByIds(idsForRemove);

        assertEquals(1, getSizeChartIdsFromDb().size());
    }

    @Test
    public void updateSizeChartTest() {
        List<Long> categories = Stream.of(1L, 2L).collect(Collectors.toList());
        List<Long> newCategories = Stream.of(33L, 44L).collect(Collectors.toList());

        Size size = generateSize("42", 1L, categories, null);

        sizeChartSizeDao.insertSizeChartSizes(Collections.singletonList(size), 1L);
        Size updatedSize = generateSize("44", 2L, newCategories, null);
        updatedSize.setId(size.getId());

        sizeChartSizeDao.updateSizeChartSizes(Collections.singletonList(updatedSize));

        Size expectedSize = generateSize(updatedSize.getSizeName(), size.getSizeOptionId(), newCategories, null);

        verifySize(expectedSize, getSizeChartMeasureFromDb(size.getId()));
    }

    @Test
    public void getSizeByIdTest() {
        List<Long> categories = Stream.of(1L, 2L).collect(Collectors.toList());
        Size size = generateSize("42", 1L, categories, null);

        sizeChartSizeDao.insertSizeChartSizes(Collections.singletonList(size), 1L);
        Size res = sizeChartSizeDao.getSize(size.getId()).get();

        assertEquals(size.getSizeName(), res.getSizeName());
        assertEquals(size.getSizeOptionId(), res.getSizeOptionId());

    }

    private List<Long> getSizeChartIdsFromDb() {
        final String select = "SELECT id FROM SIZE_CHART_SIZE";
        return jdbcTemplate.query(
            select,
            (resultSet, i) -> resultSet.getLong("id")
        );
    }

    private Size generateSize(String sizeName, Long sizeOptionId,
                              List<Long> categoryIds, List<SizeChartMeasure> measures) {
        Size size = new Size();
        size.setSizeName(sizeName);
        size.setSizeOptionId(sizeOptionId);
        size.setCategoryIds(categoryIds);
        size.setMeasures(measures);
        return size;
    }

    private Size getSizeChartMeasureFromDb(Long id) {
        final String removeSizeChartMeasuresBySizeChart = "SELECT * FROM SIZE_CHART_SIZE " +
            "WHERE id = (:id)";

        Size result = jdbcTemplate.query(
            removeSizeChartMeasuresBySizeChart, Collections.singletonMap("id", id),
            (resultSet, i) -> {
                Size size = new Size();
                size.setId(resultSet.getLong("id"));
                size.setSizeName(resultSet.getString("size_name"));
                size.setSizeOptionId(resultSet.getLong("size_option_id"));
                return size;
            }).get(0);

        result.setCategoryIds(jdbcTemplate.query("SELECT category_id FROM SIZE_CHART_SIZE_CATEGORY " +
                "WHERE size_id = :size_id", Collections.singletonMap("size_id", id),
            (resultSet, i) -> resultSet.getLong("category_id")));

        return result;
    }

    private void verifySize(Size expected, Size result) {
        assertEquals(expected.getSizeName(), result.getSizeName());
        assertEquals(expected.getSizeOptionId(), result.getSizeOptionId());
        assertTrue(expected.getCategoryIds().containsAll(result.getCategoryIds()));
    }
}
