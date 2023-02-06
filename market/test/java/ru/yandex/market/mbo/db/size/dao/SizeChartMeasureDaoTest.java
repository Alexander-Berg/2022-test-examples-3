package ru.yandex.market.mbo.db.size.dao;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.market.mbo.db.size.model.SizeChartMeasure;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;

@SuppressWarnings("checkstyle:magicNumber")
public class SizeChartMeasureDaoTest {

    private static final Long SIZE_ID = 2L;

    private SizeChartMeasureDao sizeChartMeasureDao;
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

        sizeChartMeasureDao = new SizeChartMeasureDao(jdbcTemplate);
    }

    @Test
    public void updateSizeChartMeasureTest() {
        SizeChartMeasure sizeChartMeasure = generateSizeChartMeasure(
            "32", SIZE_ID, 2L, true, 50L, false, 40, 45
        );

        sizeChartMeasureDao.insertSizeChartMeasures(Collections.singletonList(sizeChartMeasure));


        SizeChartMeasure forUpdate = generateSizeChartMeasure(
            "34", SIZE_ID + 1, 3L, false, 51L, true, 50, 70
        );
        forUpdate.setId(sizeChartMeasure.getId());

        SizeChartMeasure expected = generateSizeChartMeasure(
            forUpdate.getName(), SIZE_ID, sizeChartMeasure.getOptionId(), forUpdate.getConvertedToSize(),
            sizeChartMeasure.getMeasureId(), forUpdate.getUsedInFilter(), forUpdate.getMinValue(),
            forUpdate.getMaxValue()
        );

        sizeChartMeasureDao.updateSizeChartMeasures(Collections.singletonList(forUpdate));

        verifyMeasures(expected, getSizeChartMeasureFromDb(forUpdate.getId()));
    }

    @Test
    public void removeByIdsTest() {
        List<SizeChartMeasure> data = Stream.of(
            generateSizeChartMeasure(
                "32", SIZE_ID, 2L, true, 50L, false, 40, 45
            ),
            generateSizeChartMeasure(
                "33", SIZE_ID, 3L, true, 51L, false, 40, 45
            ),
            generateSizeChartMeasure(
                "34", SIZE_ID, 4L, true, 52L, false, 40, 45
            )
        ).collect(Collectors.toList());

        sizeChartMeasureDao.insertSizeChartMeasures(data);

        List<Long> idsForRemove = data.stream().map(SizeChartMeasure::getId).limit(2).collect(Collectors.toList());
        sizeChartMeasureDao.removeSizeChartMeasuresByIds(idsForRemove);

        assertEquals(1, getSizeChartIdsFromDb().size());
    }

    @Test
    public void getSizeChartMeasureByIdTest() {
        SizeChartMeasure sizeChartMeasure = generateSizeChartMeasure(
            "32", SIZE_ID, 2L, true, 50L, false, 40, 45
        );

        sizeChartMeasureDao.insertSizeChartMeasures(Collections.singletonList(sizeChartMeasure));
        Optional<SizeChartMeasure> sizeChartMeasureById =
            sizeChartMeasureDao.getSizeChartMeasureById(sizeChartMeasure.getId());

        verifyMeasures(sizeChartMeasure, sizeChartMeasureById.get());
    }



    private SizeChartMeasure getSizeChartMeasureFromDb(Long id) {
        final String removeSizeChartMeasuresBySizeChart = "SELECT * FROM SIZE_CHART_MEASURE " +
            "WHERE SIZE_CHART_MEASURE.id = (:id)";
        return jdbcTemplate.query(
            removeSizeChartMeasuresBySizeChart, Collections.singletonMap("id", id),
            (resultSet, i) -> {
                SizeChartMeasure sizeChartMeasure = new SizeChartMeasure();
                sizeChartMeasure.setId(resultSet.getLong("id"));
                sizeChartMeasure.setMeasureId(resultSet.getLong("size_measure_id"));
                sizeChartMeasure.setConvertedToSize(resultSet.getBoolean("converted_to_size"));
                sizeChartMeasure.setUsedInFilter(resultSet.getBoolean("used_in_filter"));
                sizeChartMeasure.setOptionId(resultSet.getLong("option_id"));
                sizeChartMeasure.setName(resultSet.getString("measure_size_name"));
                sizeChartMeasure.setMaxValue(resultSet.getInt("max_value"));
                sizeChartMeasure.setMinValue(resultSet.getInt("min_value"));
                return sizeChartMeasure;
            }).get(0);
    }

    private List<Long> getSizeChartIdsFromDb() {
        final String removeSizeChartMeasuresBySizeChart = "SELECT id FROM SIZE_CHART_MEASURE";
        return jdbcTemplate.query(
            removeSizeChartMeasuresBySizeChart,
            (resultSet, i) -> resultSet.getLong("id")
        );
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private SizeChartMeasure generateSizeChartMeasure(String name, Long sizeId, Long optionId, Boolean convertedToSize,
                                                      Long measureId, Boolean usedInFilter, Integer minValue,
                                                      Integer maxValue) {
        SizeChartMeasure measure = new SizeChartMeasure();
        measure.setName(name);
        measure.setSizeId(sizeId);
        measure.setOptionId(optionId);
        measure.setMeasureId(measureId);
        measure.setConvertedToSize(convertedToSize);
        measure.setUsedInFilter(usedInFilter);
        measure.setMinValue(minValue);
        measure.setMaxValue(maxValue);
        return measure;
    }

    private void verifyMeasures(SizeChartMeasure expected, SizeChartMeasure result) {
        assertEquals(expected.getName(), result.getName());
        assertEquals(expected.getOptionId(), result.getOptionId());
        assertEquals(expected.getMeasureId(), result.getMeasureId());
        assertEquals(expected.getConvertedToSize(), result.getConvertedToSize());
        assertEquals(expected.getMaxValue(), result.getMaxValue());
        assertEquals(expected.getMinValue(), result.getMinValue());
        assertEquals(expected.getSetNameByUser(), result.getSetNameByUser());
    }
}
