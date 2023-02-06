package ru.yandex.market.mbo.db.size.validation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.size.SizeChartStorageServiceException;
import ru.yandex.market.mbo.db.size.dao.SizeChartDao;
import ru.yandex.market.mbo.db.size.model.SizeChart;

import java.util.Collections;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SizeChartUniqueNameValidatorTest {
    private static final long SIZE_CHART_ID = 1L;

    @Mock
    private SizeChartDao sizeChartDao;

    @InjectMocks
    private SizeChartUniqueNameValidator sizeChartUniqueNameValidator;

    @Test
    public void sizeChartUniqueNameValidationOKTest() {
        final String chartNameInDb = "ADIDAS";
        final String chartName = "NIKE";

        SizeChart sizeChartFromDb = new SizeChart();
        sizeChartFromDb.setName(chartNameInDb);
        sizeChartFromDb.setId(SIZE_CHART_ID);

        SizeChart sizeChart = new SizeChart();
        sizeChart.setName(chartName);

        when(sizeChartDao.getSizeChartsSimple()).thenReturn(Collections.singletonList(sizeChartFromDb));

        sizeChartUniqueNameValidator.validate(sizeChart);
    }

    @Test(expected = SizeChartStorageServiceException.class)
    public void sizeChartUniqueNameValidationFailedTest() {
        final String chartName = "NIKE";

        SizeChart sizeChartFromDb = new SizeChart();
        sizeChartFromDb.setName(chartName);
        sizeChartFromDb.setId(SIZE_CHART_ID);

        SizeChart sizeChart = new SizeChart();
        sizeChart.setName(chartName);

        when(sizeChartDao.getSizeChartsSimple()).thenReturn(Collections.singletonList(sizeChartFromDb));

        sizeChartUniqueNameValidator.validate(sizeChart);
    }

    @Test(expected = SizeChartStorageServiceException.class)
    public void sizeChartEmptyName() {
        SizeChart sizeChart = new SizeChart();
        sizeChartUniqueNameValidator.validate(sizeChart);
    }

}
