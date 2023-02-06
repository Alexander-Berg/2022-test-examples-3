package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class CommonCampaignConverterSetScaleTest {

    @Test
    @Parameters(method = "testData")
    public void commonCampaignConvertet_setScale(BigDecimal input, String expected) {
        //noinspection ConstantConditions
        assertThat(CommonCampaignConverter.setScaleForCore(input).toPlainString())
                .isEqualTo(expected);
    }

    Iterable<Object[]> testData() {
        return asList(new Object[][]{
                {BigDecimal.ZERO, "0.00"},
                {BigDecimal.TEN, "10.00"},
                {BigDecimal.valueOf(123.1234), "123.12"},
                {BigDecimal.valueOf(123.1294), "123.12"},
                {BigDecimal.valueOf(123.1), "123.10"},
        });
    }

}
