package ru.yandex.direct.grid.processing.service.tools;

import java.math.BigDecimal;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.geosearch.model.GeoObject;
import ru.yandex.direct.grid.processing.model.GdCoordinates;
import ru.yandex.direct.grid.processing.model.GdGeoToolsResponse;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@ParametersAreNonnullByDefault
public class GeoToolsConverterTest {
    private static final BigDecimal POSITIVE_COORDINATE = BigDecimal.valueOf(34.32234863434);
    private static final BigDecimal NEGATIVE_COORDINATE = BigDecimal.valueOf(-23.2345678345);
    private static final String POSITIVE_COORDINATE_STRING = "34.322349";
    private static final String NEGATIVE_COORDINATE_STRING = "-23.234568";

    @Test
    public void test_convertToRequestCoordinatesPositiveValue() {
        assertThat(GeoToolsConverter.toRequestCoordinate(POSITIVE_COORDINATE)).isEqualTo(POSITIVE_COORDINATE_STRING);
    }

    @Test
    public void test_convertToRequestCoordinatesNegativeValue() {
        assertThat(GeoToolsConverter.toRequestCoordinate(NEGATIVE_COORDINATE)).isEqualTo(NEGATIVE_COORDINATE_STRING);
    }

    @Test
    public void test_convertCoordinatesToString() {
        GdCoordinates coordinates = new GdCoordinates()
                .withX(POSITIVE_COORDINATE)
                .withY(NEGATIVE_COORDINATE);
        String expected = POSITIVE_COORDINATE_STRING + "," + NEGATIVE_COORDINATE_STRING;
        assertThat(GeoToolsConverter.coordinatesToString(coordinates)).isEqualTo(expected);
    }

    @Test
    public void test_convertToGdGeoToolsResponse() {
        Long geoId = RandomNumberUtils.nextPositiveLong();
        GeoObject geoObject = mock(GeoObject.class);
        doReturn(geoId).when(geoObject).getGeoId();
        doReturn("Country").when(geoObject).getCountry();
        doReturn("AdministrativeArea").when(geoObject).getAdministrativeArea();
        doReturn("City").when(geoObject).getCity();
        doReturn("Street").when(geoObject).getStreet();
        doReturn("House").when(geoObject).getHouse();
        GdGeoToolsResponse expected = new GdGeoToolsResponse()
                .withGeoId(geoId)
                .withCountry("Country")
                .withAdministrativeArea("AdministrativeArea")
                .withCity("City")
                .withStreet("Street")
                .withHouse("House");

        assertThat(GeoToolsConverter.toGdGeoToolsResponse(geoObject)).is(matchedBy(beanDiffer(expected)));
    }

}
