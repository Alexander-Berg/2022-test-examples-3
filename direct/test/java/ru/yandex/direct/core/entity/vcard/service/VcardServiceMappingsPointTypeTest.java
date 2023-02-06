package ru.yandex.direct.core.entity.vcard.service;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.vcard.model.PointType;
import ru.yandex.direct.geosearch.model.Kind;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.vcard.service.VcardServiceMappings.pointTypeFromGeocoder;

@RunWith(Parameterized.class)
public class VcardServiceMappingsPointTypeTest {

    @Parameterized.Parameter(0)
    public Kind sourceKind;

    @Parameterized.Parameter(1)
    public PointType expectedPointType;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {Kind.HOUSE, PointType.HOUSE},
                {Kind.STREET, PointType.STREET},
                {Kind.METRO, PointType.METRO},
                {Kind.DISTRICT, PointType.DISTRICT},
                {Kind.LOCALITY, PointType.LOCALITY},
                {Kind.AREA, PointType.AREA},
                {Kind.PROVINCE, PointType.PROVINCE},
                {Kind.COUNTRY, PointType.COUNTRY},
                {Kind.HYDRO, PointType.HYDRO},
                {Kind.RAILWAY, PointType.RAILWAY},
                {Kind.ROUTE, PointType.ROUTE},
                {Kind.VEGETATION, PointType.VEGETATION},
                {Kind.CEMETERY, PointType.CEMETERY},
                {Kind.BRIDGE, PointType.BRIDGE},
                {Kind.KM, PointType.KM},
                {Kind.OTHER, PointType.OTHER},
                {Kind.UNKNOWN, null},
                {null, null}
        });
    }

    @Test
    public void geocoderPointKindIsConvertedToInternalPointTypeFine() {
        assertThat("результат конвертации типа точки геокодера в enum ядра не соответствует ожидаемому",
                pointTypeFromGeocoder(sourceKind), equalTo(expectedPointType));
    }
}
