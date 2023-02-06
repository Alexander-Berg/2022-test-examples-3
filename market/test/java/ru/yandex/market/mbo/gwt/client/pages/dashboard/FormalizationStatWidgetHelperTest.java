package ru.yandex.market.mbo.gwt.client.pages.dashboard;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author yuramalinov
 * @created 19.04.18
 */
public class FormalizationStatWidgetHelperTest {
    @Test
    public void testConvert() {
        assertEquals("Нет сессий", FormalizationStatWidgetHelper.convert(Collections.emptyList()));
        assertEquals("20180418_0022",
            FormalizationStatWidgetHelper.convert(Collections.singletonList("20180418_0022")));
        assertEquals("20180418_0022 +1",
            FormalizationStatWidgetHelper.convert(Arrays.asList("20180418_0022", "20180416_2031")));
        assertEquals("20180418_0022 +1",
            FormalizationStatWidgetHelper.convert(Arrays.asList("20180416_2031", "20180418_0022")));
    }
}
