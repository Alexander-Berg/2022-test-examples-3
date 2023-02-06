package ru.yandex.market.mbo.db.size.processor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.size.model.SizeChart;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SetVendorChartNameProcessorTest {

    @Mock
    private GlobalVendorService globalVendorService;

    @InjectMocks
    private SetVendorChartNameProcessor setVendorChartNameProcessor;

    @Test
    public void setChartNameTest() {
        final Long vendorId = 1L;
        final String vendorName = "ADIDAS";

        when(globalVendorService.loadVendorName(vendorId)).thenReturn(vendorName);

        SizeChart sizeChart = new SizeChart();
        sizeChart.setVendorId(vendorId);

        setVendorChartNameProcessor.process(sizeChart);

        assertEquals(vendorName, sizeChart.getName());
    }

    @Test
    public void notSetChartNameTest() {
        final Long vendorId = 1L;
        final String vendorName = "ADIDAS";

        when(globalVendorService.loadVendorName(vendorId)).thenReturn(vendorName);

        SizeChart sizeChart = new SizeChart();
        sizeChart.setVendorId(vendorId);
        sizeChart.setName("NIKE");

        setVendorChartNameProcessor.process(sizeChart);

        assertEquals("NIKE", sizeChart.getName());
    }

}
