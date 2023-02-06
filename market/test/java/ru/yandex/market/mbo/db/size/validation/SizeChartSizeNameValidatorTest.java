package ru.yandex.market.mbo.db.size.validation;

import org.junit.Test;
import ru.yandex.market.mbo.db.size.SizeChartStorageServiceException;
import ru.yandex.market.mbo.db.size.model.Size;
import ru.yandex.market.mbo.db.size.model.SizeChart;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("checkstyle:MagicNumber")
public class SizeChartSizeNameValidatorTest {

    private final SizeChartSizeNameValidator sizeChartSizeNameValidator = new SizeChartSizeNameValidator();

    @Test
    public void duplicateSizeNamesOKTest() {
        Size size1 = new Size();
        size1.setSizeName("A");
        size1.setId(1L);
        Size size2 = new Size();
        size2.setSizeName("B");
        size2.setId(2L);

        Size testSize = new Size();
        testSize.setSizeName("D");
        testSize.setId(3L);

        List<Size> sizes = Stream.of(size1, size2).collect(Collectors.toList());
        SizeChart sizeChart = new SizeChart();
        sizeChart.setSizes(sizes);

        sizeChartSizeNameValidator.validate(sizeChart, testSize);
    }

    @Test
    public void duplicateSizeNamesOKTestIfIdsEqual() {
        Size size1 = new Size();
        size1.setSizeName("A");
        size1.setId(1L);
        Size size2 = new Size();
        size2.setSizeName("B");
        size2.setId(2L);

        Size testSize = new Size();
        testSize.setSizeName("B");
        testSize.setId(2L);

        List<Size> sizes = Stream.of(size1, size2).collect(Collectors.toList());
        SizeChart sizeChart = new SizeChart();
        sizeChart.setSizes(sizes);

        sizeChartSizeNameValidator.validate(sizeChart, testSize);
    }

    @Test(expected = SizeChartStorageServiceException.class)
    public void duplicateSizeNamesFailedTest() {
        Size size1 = new Size();
        size1.setSizeName("A");
        size1.setId(1L);
        Size size2 = new Size();
        size2.setSizeName("B");
        size2.setId(2L);

        Size testSize = new Size();
        testSize.setSizeName("B");
        testSize.setId(3L);

        List<Size> sizes = Stream.of(size1, size2).collect(Collectors.toList());
        SizeChart sizeChart = new SizeChart();
        sizeChart.setSizes(sizes);

        sizeChartSizeNameValidator.validate(sizeChart, testSize);
    }

}
