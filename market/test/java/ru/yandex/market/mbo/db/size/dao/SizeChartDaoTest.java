package ru.yandex.market.mbo.db.size.dao;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.market.mbo.db.size.model.Size;
import ru.yandex.market.mbo.db.size.model.SizeChart;
import ru.yandex.market.mbo.db.size.model.SizeChartMeasure;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
public class SizeChartDaoTest {

    private SizeChartDao sizeChartDao;
    private final SizeChartWithSizeAndMeasuresResultSetExtractor extractor =
        new SizeChartWithSizeAndMeasuresResultSetExtractor();
    private final SizeChartSimpleRowMapper sizeChartSimpleRowMapper =
        new SizeChartSimpleRowMapper();
    private SizeChartMeasureDao sizeChartMeasureDao;
    private SizeChartSizeDao sizeChartSizeDao;

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
        NamedParameterJdbcTemplate jdbcTemplate = spy(namedTemplate);

        sizeChartDao = new SizeChartDao(
            jdbcTemplate,
            sizeChartSimpleRowMapper,
            extractor
        );
        sizeChartMeasureDao = new SizeChartMeasureDao(jdbcTemplate);
        sizeChartSizeDao = new SizeChartSizeDao(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void getSizeChartTest() {
        List<SizeChart> expectedResult = Stream.of(
            generateSizeChart(null, "name", 2L, null, null),
            generateSizeChart(null, "name1", 12L, 13L, null)
        ).collect(Collectors.toList());

        expectedResult.forEach(sizeChartDao::insertSizeChart);

        List<SizeChart> result = sizeChartDao.getSizeChartsSimple();

        assertEquals(2, result.size());
        assertTrue(expectedResult.containsAll(result) && result.containsAll(expectedResult));
    }

    @Test
    public void updateOnlySizeChartTest() {
        SizeChart sizeChart = generateSizeChart(0L, "name", 2L, null, null);
        sizeChartDao.insertSizeChart(sizeChart);

        SizeChart updatedSizeChart = generateSizeChart(sizeChart.getId(), "name2", 3L, 4L, null);
        sizeChartDao.updateOnlySizeChart(updatedSizeChart);

        SizeChart fromDb = sizeChartDao.getSizeChartById(updatedSizeChart.getId()).get();

        verifyOnlySizeCharts(updatedSizeChart, fromDb);
    }

    @Test
    public void insertSizeChartTest() {
        SizeChart sizeChart = generateSizeChart(1L, "name", 2L, null, null);
        sizeChartDao.insertSizeChart(sizeChart);

        assertNotNull(sizeChart.getId());
        SizeChart fromDb = sizeChartDao.getSizeChartById(sizeChart.getId()).get();

        verifyOnlySizeCharts(sizeChart, fromDb);
    }

    @Test
    public void getAllSizeChartTest() {
        SizeChartMeasure sizeChartMeasure = generateSizeChartMeasure(
            "32", 2L, true, 50L, false, 40, 45
        );
        List<Long> categoryIds = Stream.of(2L, 3L).collect(Collectors.toList());
        Size size = generateSize("32/34", 32L, categoryIds, Collections.singletonList(sizeChartMeasure));

        SizeChart sizeChart = generateSizeChart(null, "Adidas", 1L, 123L,
            Collections.singletonList(size));

        sizeChartDao.insertSizeChart(sizeChart);
        sizeChartSizeDao.insertSizeChartSizes(Collections.singletonList(size), sizeChart.getId());
        sizeChartMeasure.setSizeId(size.getId());
        sizeChartMeasureDao.insertSizeChartMeasures(Collections.singletonList(sizeChartMeasure));

        assertNotNull(sizeChart.getId());

        SizeChart fromDb = sizeChartDao.getSizeChartById(sizeChart.getId()).get();

        verifyOnlySizeCharts(sizeChart, fromDb);
        assertEquals(1, sizeChart.getSizes().size());
        verifySize(size, fromDb.getSizes().get(0));
        assertEquals(1, sizeChart.getSizes().get(0).getMeasures().size());
        verifyMeasures(sizeChartMeasure, fromDb.getSizes().get(0).getMeasures().get(0));
    }

    @Test
    public void getAllSizeChartIfMeasuresNullTest() {
        SizeChartMeasure sizeChartMeasure = generateSizeChartMeasure(
            "32", 2L, true, 50L, false, null, null
        );
        List<Long> categoryIds = Stream.of(2L, 3L).collect(Collectors.toList());
        Size size = generateSize("32/34", 32L, categoryIds, Collections.singletonList(sizeChartMeasure));

        SizeChart sizeChart = generateSizeChart(null, "Adidas", 1L, 123L,
            Collections.singletonList(size));

        sizeChartDao.insertSizeChart(sizeChart);
        sizeChartSizeDao.insertSizeChartSizes(Collections.singletonList(size), sizeChart.getId());
        sizeChartMeasure.setSizeId(size.getId());
        sizeChartMeasureDao.insertSizeChartMeasures(Collections.singletonList(sizeChartMeasure));

        assertNotNull(sizeChart.getId());

        SizeChart fromDb = sizeChartDao.getSizeChartById(sizeChart.getId()).get();

        verifyOnlySizeCharts(sizeChart, fromDb);
        assertEquals(1, sizeChart.getSizes().size());
        verifySize(size, fromDb.getSizes().get(0));
        assertEquals(1, sizeChart.getSizes().get(0).getMeasures().size());
        verifyMeasures(sizeChartMeasure, fromDb.getSizes().get(0).getMeasures().get(0));
    }

    @Test
    public void removeSizeChartTest() {
        List<SizeChart> forInsert = Stream.of(
            generateSizeChart(null, "name", 2L, null, null),
            generateSizeChart(null, "name1", 12L, 13L, null)
        ).collect(Collectors.toList());

        forInsert.forEach(sizeChartDao::insertSizeChart);
        assertEquals(2, sizeChartDao.getSizeChartsSimple().size());

        forInsert.stream().map(SizeChart::getId).forEach(sizeChartDao::removeSizeChart);
        assertTrue(sizeChartDao.getSizeChartsSimple().isEmpty());
    }

    @Test
    public void getStandardSizeChartsByMeasureIdTest() {
        final Long measureId = 12L;

        SizeChartMeasure sizeChartMeasure1 = generateSizeChartMeasure(
            "32", 2L, true, measureId, false, null, null
        );
        SizeChartMeasure sizeChartMeasure2 = generateSizeChartMeasure(
            "32", 2L, true, measureId + 1, false, null, null
        );
        Size size1 = generateSize(
            "32/34", 32L, Collections.emptyList(), Collections.singletonList(sizeChartMeasure1)
        );
        Size size2 = generateSize(
            "42", 33L, Collections.emptyList(), Collections.singletonList(sizeChartMeasure1)
        );
        Size size3 = generateSize(
            "44", 34L, Collections.emptyList(), Collections.singletonList(sizeChartMeasure2)
        );

        SizeChart sizeChart1 = generateSizeChart(null, "RU", 1L, null,
            Collections.singletonList(size1));
        SizeChart sizeChart2 = generateSizeChart(null, "EN", 2L, null,
            Collections.singletonList(size2));
        SizeChart sizeChart3 = generateSizeChart(null, "INT", 3L, null,
            Collections.singletonList(size3));

        insertFullStructure(sizeChart1);
        insertFullStructure(sizeChart2);
        insertFullStructure(sizeChart3);


        List<SizeChart> expectedChart = Stream.of(sizeChart1, sizeChart2).collect(Collectors.toList());

        List<SizeChart> standardSizeChartsByMeasureId = sizeChartDao.getStandardSizeChartsByMeasureId(measureId);

        assertEquals(expectedChart, standardSizeChartsByMeasureId);
    }

    private SizeChart generateSizeChart(Long id, String chartName, Long optionId, Long vendorId, List<Size> sizes) {
        SizeChart result = new SizeChart();
        result.setId(id);
        result.setName(chartName);
        result.setOptionId(optionId);
        result.setVendorId(vendorId);
        result.setSizes(sizes);
        return result;
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

    private SizeChartMeasure generateSizeChartMeasure(String name, Long optionId, Boolean convertedToSize,
                                                      Long measureId, Boolean usedInFilter, Integer minValue,
                                                      Integer maxValue) {
        SizeChartMeasure measure = new SizeChartMeasure();
        measure.setName(name);
        measure.setOptionId(optionId);
        measure.setMeasureId(measureId);
        measure.setConvertedToSize(convertedToSize);
        measure.setUsedInFilter(usedInFilter);
        measure.setMinValue(minValue);
        measure.setMaxValue(maxValue);
        return measure;
    }

    private void verifyOnlySizeCharts(SizeChart expected, SizeChart result) {
        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getName(), result.getName());
        assertEquals(expected.getOptionId(), result.getOptionId());
        assertEquals(expected.getVendorId(), result.getVendorId());
    }

    private void verifySize(Size expected, Size result) {
        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getSizeName(), result.getSizeName());
        assertEquals(expected.getSizeOptionId(), result.getSizeOptionId());
        assertTrue(expected.getCategoryIds().containsAll(result.getCategoryIds()));
    }

    private void verifyMeasures(SizeChartMeasure expected, SizeChartMeasure result) {
        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getName(), result.getName());
        assertEquals(expected.getOptionId(), result.getOptionId());
        assertEquals(expected.getMeasureId(), result.getMeasureId());
        assertEquals(expected.getConvertedToSize(), result.getConvertedToSize());
        assertEquals(expected.getMaxValue(), result.getMaxValue());
        assertEquals(expected.getMinValue(), result.getMinValue());
        assertEquals(expected.getSizeId(), result.getSizeId());
    }

    private void insertFullStructure(SizeChart sizeChart) {
        sizeChartDao.insertSizeChart(sizeChart);
        sizeChart.getSizes().forEach(
            size -> sizeChartSizeDao.insertSizeChartSizes(Collections.singletonList(size), sizeChart.getId())
        );

        sizeChart.getSizes().forEach(size -> {
            size.getMeasures().forEach(measure -> measure.setSizeId(size.getId()));
        });

        sizeChart.getSizes().stream().map(Size::getMeasures).flatMap(Collection::stream)
            .forEach(sizeChartMeasure ->
                sizeChartMeasureDao.insertSizeChartMeasures(Collections.singletonList(sizeChartMeasure))
            );
    }

}
