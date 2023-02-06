package ru.yandex.market.mbo.db.size;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.SizeMeasureService;
import ru.yandex.market.mbo.db.size.dao.SizeChartDao;
import ru.yandex.market.mbo.db.size.model.Size;
import ru.yandex.market.mbo.db.size.model.SizeChart;
import ru.yandex.market.mbo.gwt.models.gurulight.GLMeasure;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class SizeChartServiceImplTest {

    @Mock
    private SizeMeasureService sizeMeasureService;
    @Mock
    private SizeChartDao sizeChartDao;
    @InjectMocks
    private SizeChartServiceImpl sizeChartService;

    @Test
    public void getStandardSizeChartsBySizeParameterTest() {
        final long paramId = 120L;
        final long measureId = 90L;

        GLMeasure glMeasure = new GLMeasure();
        glMeasure.setId(measureId);

        List<SizeChart> expected = Stream.of(
            generateSizeChart(1L, "RU", 2L, null, Collections.emptyList()),
            generateSizeChart(11L, "EN", 12L, null, Collections.emptyList())
        ).collect(Collectors.toList());

        when(sizeMeasureService.getSizeMeasureByValueParamId(paramId)).thenReturn(glMeasure);
        when(sizeChartDao.getStandardSizeChartsByMeasureId(measureId)).thenReturn(expected);

        List<SizeChart> result = sizeChartService.getStandardSizeChartsBySizeParameter(paramId);

        assertEquals(expected, result);
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

}
