package ru.yandex.market.checkout.util.geocoder;

import org.apache.commons.lang3.StringUtils;

import ru.yandex.common.geocoder.model.response.Precision;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;

/**
 * Параметры мока для gecoder. Большинство значений имеют формат, описанный в
 * <a href="https://tech.yandex.ru/maps/doc/geocoder/desc/concepts/About-docpage/">geocoder API</a>
 *
 * @author sergeykoles
 * Created on: 19.07.18
 */
public class GeocoderParameters {

    public static final String DEFAULT_POSTAL_CODE = "119021";
    public static final String DEFAULT_PRECISION = Precision.EXACT.name();
    private static final String COORDINATES_SEPARATOR = " ";
    private static final String DEFAULT_LONGITUDE = "46.135321";
    private static final String DEFAULT_LATTITUDE = "48.218271";
    public static final String DEFAULT_GPS = DEFAULT_LONGITUDE + COORDINATES_SEPARATOR + DEFAULT_LATTITUDE;
    /**
     * должен ли {@link OrderCreateHelper} автоматически конфигурировать мок для геокодера.
     */
    private boolean autoMock = true;

    private String longitude = DEFAULT_LONGITUDE;
    private String latitude = DEFAULT_LATTITUDE;

    private String gps = DEFAULT_GPS;
    private String postalCode = DEFAULT_POSTAL_CODE;
    private String precision = DEFAULT_PRECISION;

    public String getLongitude() {
        return longitude;
    }

    public String getLatitude() {
        return latitude;

    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getGps() {
        return gps;
    }

    public void setGps(String gps) {
        String[] coordinates = StringUtils.split(gps, COORDINATES_SEPARATOR);
        if (coordinates == null || coordinates.length != 2) {
            throw new IllegalArgumentException("GPS must be in format: '<lon> <lat>'");
        }

        this.longitude = coordinates[0];
        this.latitude = coordinates[1];
    }

    public String getPrecision() {
        return precision;
    }

    public void setPrecision(String precision) {
        this.precision = precision;
    }

    public boolean isAutoMock() {
        return autoMock;
    }

    public void setAutoMock(boolean autoMock) {
        this.autoMock = autoMock;
    }

    public void setDefault() {
        longitude = DEFAULT_LONGITUDE;
        latitude = DEFAULT_LATTITUDE;
        gps = DEFAULT_GPS;
        postalCode = DEFAULT_POSTAL_CODE;
        precision = DEFAULT_PRECISION;
    }
}
