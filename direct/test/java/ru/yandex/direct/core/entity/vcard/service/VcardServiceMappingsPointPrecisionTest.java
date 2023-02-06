package ru.yandex.direct.core.entity.vcard.service;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.vcard.model.PointPrecision;
import ru.yandex.direct.geosearch.model.Precision;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.vcard.service.VcardServiceMappings.precisionFromGeocoder;

@RunWith(Parameterized.class)
public class VcardServiceMappingsPointPrecisionTest {

    @Parameterized.Parameter(0)
    public Precision sourcePrecision;

    @Parameterized.Parameter(1)
    public PointPrecision expectedPointPrecision;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {Precision.EXACT, PointPrecision.EXACT},
                {Precision.NUMBER, PointPrecision.NUMBER},
                {Precision.NEAR, PointPrecision.NEAR},
                {Precision.STREET, PointPrecision.STREET},
                {Precision.OTHER, PointPrecision.OTHER},
                {Precision.UNKNOWN, null},
                {null, null}
        });
    }

    @Test
    public void geocoderPointPrecisionIsConvertedToInternalPointPrecisionFine() {
        assertThat("результат конвертации точности точки геокодера в enum ядра не соответствует ожидаемому",
                precisionFromGeocoder(sourcePrecision), equalTo(expectedPointPrecision));
    }
}
