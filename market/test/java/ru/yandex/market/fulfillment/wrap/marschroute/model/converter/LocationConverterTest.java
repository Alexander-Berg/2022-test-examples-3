package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.cache.LoadingCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteLocation;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.cache.GeoFileCacheKey;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.model.GeoFile;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.model.GeoInformation;
import ru.yandex.market.logistic.api.model.fulfillment.Location;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(LocationConverter.class)
@MockBean(SystemPropertyService.class)
class LocationConverterTest extends BaseIntegrationTest {

    private static final Map<String, GeoInformation> GEO_INFORMATION_MAP = new HashMap<String, GeoInformation>() {{
        put("MOSCOW_GEO_INFORMATION", new GeoInformation(
            213L,
            null,
            "7700000000000",
            "locality",
            50L
        ));
        put("LEAF_GEO_INFORMATION", new GeoInformation(
            30L,
            null,
            "",
            null,
            50L
        ));
        put("PARENT_GEO_INFORMATION", new GeoInformation(
            50L,
            null,
            "111",
            null,
            null
        ));
        put("LEAF_NOKLADR_GEO_INFORMATION", new GeoInformation(
            40L,
            null,
            "",
            null,
            60L
        ));
        put("PARENT_NO_KLADR_GEO_INFORMATION", new GeoInformation(
            60L,
            null,
            "",
            null,
            null
        ));
        put("KLADR_ID_MARSCHROUTE_GEO_INFORMATION", new GeoInformation(
            214L,
            null,
            "7700000000000",
            "7800000000000",
            null,
            "locality",
            50L
        ));
        put("KLADR_ID_MARSCHROUTE_EMPTY_GEO_INFORMATION", new GeoInformation(
            215L,
            null,
            "7700000000000",
            "",
            null,
            "locality",
            50L
        ));
    }};

    @Autowired
    private LocationConverter locationConverter;

    @MockBean
    LoadingCache<GeoFileCacheKey, GeoFile> cache;

    @SpyBean
    private GeoInformationProvider geoInformationProvider;

    @BeforeEach
    void init() {
        GEO_INFORMATION_MAP.values().forEach(this::mockGeoInformationProvider);
    }


    @Test
    void testFullConversion() throws Exception {
        Location location = new Location.LocationBuilder(null, null, "Москва")
                .setLocationId(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION").getGeoId().intValue())
                .setStreet("Улица Пушкина")
                .setHouse("Дом Колотушкина")
                .setZipCode("123456")
                .setBuilding("building")
                .setHousing("housing")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        MarschrouteLocation marschrouteLocation = locationConverter.convert(location);

        softly.assertThat(marschrouteLocation.getCityId())
            .as("Asserting city id value")
            .isEqualTo(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION").getKladrId().substring(0, 11));

        softly.assertThat(marschrouteLocation.getStreet())
            .as("Asserting street value")
            .isEqualTo(location.getStreet());

        softly.assertThat(marschrouteLocation.getRoom())
            .as("Asserting room value")
            .isEqualTo(location.getRoom());

        softly.assertThat(marschrouteLocation.getIndex())
            .as("Asserting zip code")
            .isEqualTo(location.getZipCode());

        softly.assertThat(marschrouteLocation.getBuilding1())
            .as("Asserting building1 first part (8 symbols) of house")
            .isEqualTo("Дом Коло");

        softly.assertThat(marschrouteLocation.getBuilding2())
            .as("Asserting building2 second part of house + building + housing")
            .isEqualTo("тушкина " + location.getBuilding() + "/" + location.getHousing());

        softly.assertThat(marschrouteLocation.getEntrance())
            .as("Asserting porch value")
            .isEqualTo(location.getPorch());

        softly.assertThat(Integer.valueOf(marschrouteLocation.getFloor()))
            .as("Asserting floor value")
            .isEqualTo(location.getFloor());

        verifyGeoInformationMock(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION"));
    }

    @Test
    void testBuildingHousingConversionWhenBothNullAndHouseIsShort() {
        Location location = new Location.LocationBuilder(null, null, "Москва")
            .setLocationId(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION").getGeoId().intValue())
            .setStreet("Улица Пушкина")
            .setHouse("10Б")
            .setZipCode("123456")
            .setPorch("1")
            .setFloor(15)
            .setRoom("room")
            .build();

        MarschrouteLocation marschrouteLocation = locationConverter.convert(location);

        assertThat(marschrouteLocation.getBuilding2())
            .as("Building 2 should be null")
            .isNull();

        verifyGeoInformationMock(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION"));
    }

    @Test
    void testBuildingHousingConversionWhenBothNullAndHouseIsLong() {
        Location location = new Location.LocationBuilder(null, null, "Москва")
                .setLocationId(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION").getGeoId().intValue())
                .setStreet("Улица Пушкина")
                .setHouse("10Б корп. 12")
                .setZipCode("123456")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        MarschrouteLocation marschrouteLocation = locationConverter.convert(location);

        assertThat(marschrouteLocation.getBuilding1())
            .as("Building 1 should have first part of house")
            .isEqualTo("10Б");

        assertThat(marschrouteLocation.getBuilding2())
            .as("Building 2 should have second part of house")
            .isEqualTo("корп. 12");

        verifyGeoInformationMock(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION"));
    }

    @Test
    void testBuildingHousingConversionWhenBothNullAndHouseIsVeryLong() {
        Location location = new Location.LocationBuilder(null, null, "Москва")
            .setLocationId(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION").getGeoId().intValue())
            .setStreet("Улица Пушкина")
            .setHouse("10Б корп. 121234")
            .setZipCode("123456")
            .setPorch("1")
            .setFloor(15)
            .setRoom("room")
            .build();

        MarschrouteLocation marschrouteLocation = locationConverter.convert(location);

        assertThat(marschrouteLocation.getBuilding1())
            .as("Building 1 should have first part of house")
            .isEqualTo("10Б корп");

        assertThat(marschrouteLocation.getBuilding2())
            .as("Building 2 should have second part of house")
            .isEqualTo(". 121234");

        verifyGeoInformationMock(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION"));
    }

    @Test
    void testBuildingHousingConversionWhenBothNotNull() {
        Location location = new Location.LocationBuilder(null, null, "Москва")
            .setLocationId(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION").getGeoId().intValue())
            .setStreet("Улица Пушкина")
            .setHouse("10Б")
            .setBuilding("к. 1")
            .setHousing("8")
            .setZipCode("123456")
            .setPorch("1")
            .setFloor(15)
            .setRoom("room")
            .build();

        MarschrouteLocation marschrouteLocation = locationConverter.convert(location);

        assertThat(marschrouteLocation.getBuilding1())
            .as("Building 1 should have first part of house")
            .isEqualTo("10Б");

        assertThat(marschrouteLocation.getBuilding2())
            .as("Building 2 should have second part of house")
            .isEqualTo("к. 1/8");

        verifyGeoInformationMock(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION"));
    }

    @Test
    void testBuildingHousingConversionWhenBuildingIsNull() throws Exception {
        Location location = new Location.LocationBuilder(null, null, "Москва")
                .setLocationId(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION").getGeoId().intValue())
                .setStreet("Улица Пушкина")
                .setHouse("Дом Коло")
                .setZipCode("123456")
                .setHousing("housing")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        MarschrouteLocation marschrouteLocation = locationConverter.convert(location);

        assertThat(marschrouteLocation.getBuilding2())
            .as("Building 2 should have only housing value")
            .isEqualTo(location.getHousing());

        verifyGeoInformationMock(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION"));
    }


    @Test
    void testBuildingHousingConversionWhenHousingIsNull() throws Exception {
        Location location = new Location.LocationBuilder(null, null, "Москва")
                .setLocationId(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION").getGeoId().intValue())
                .setStreet("Улица Пушкина")
                .setHouse("Дом Коло")
                .setZipCode("123456")
                .setBuilding("building")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        MarschrouteLocation marschrouteLocation = locationConverter.convert(location);

        assertThat(marschrouteLocation.getBuilding2())
            .as("Building 2 should have only building value")
            .isEqualTo(location.getBuilding());

        verifyGeoInformationMock(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION"));
    }


    /**
     * Проверяет, что
     * -----
     * При наличии:
     * Названия города
     * Информации по нужному locationId
     * Названия улицы
     * -----
     * Поле street должно содержать изначальное значение улицы.
     */
    @Test
    void convertWithAllValidData() {
        Location location = new Location.LocationBuilder(null, null, "Москва")
                .setLocationId(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION").getGeoId().intValue())
                .setStreet("Улица Пушкина")
                .setHouse("Дом Колотушкина")
                .setZipCode("123456")
                .setBuilding("building")
                .setHousing("housing")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        MarschrouteLocation actualLocation = locationConverter.convert(location);

        assertThat(actualLocation.getStreet())
            .as("Assert that street contains expected street value")
            .isEqualTo(location.getStreet());

        verifyGeoInformationMock(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION"));
    }

    /**
     * Проверяем, что при условии, когда geoId в заказе и geoId выбранного GeoInformation не отличаются
     * при отсутствии улицы - в итоговое значение улицы будет вписано
     * {@value ru.yandex.market.fulfillment.wrap.marschroute.model.converter.LocationConverter#MISSING_STREET_REPLACEMENT}
     */
    @Test
    void convertWithMissingStreetValue() {
        Location location = new Location.LocationBuilder(null, null, "Москва")
                .setLocationId(GEO_INFORMATION_MAP.get("MOSCOW_GEO_INFORMATION").getGeoId().intValue())
                .setStreet(null)
                .setHouse("Дом Колотушкина")
                .setZipCode("123456")
                .setBuilding("building")
                .setHousing("housing")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        MarschrouteLocation actualLocation = locationConverter.convert(location);

        assertThat(actualLocation.getStreet())
            .as("Asserting resulting street value")
            .isEqualTo(LocationConverter.MISSING_STREET_REPLACEMENT);

        verifyGeoInformationMock(location.getLocationId());
    }


    /**
     * Проверяет, что
     * -----
     * При наличии:
     * Названия города
     * Названия улицы
     * -----
     * И отсутствии:
     * Значения КЛАДР у непосредственного locationId  (был взять родительский)
     * -----
     * Поле street должно содержать комбинацию из значений город + улицу через запятую.
     */
    @Test
    void convertWithParentKladr() {
        Location location = new Location.LocationBuilder(null, null, "Москва")
                .setLocationId(GEO_INFORMATION_MAP.get("LEAF_GEO_INFORMATION").getGeoId().intValue())
                .setStreet("Улица Пушкина")
                .setHouse("Дом Колотушкина")
                .setZipCode("123456")
                .setBuilding("building")
                .setHousing("housing")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        MarschrouteLocation actualLocation = locationConverter.convert(location);

        assertThat(actualLocation.getStreet())
            .as("Street value should contain both locality and street values")
            .isEqualTo(location.getLocality() + ", " + location.getStreet());

        verifyGeoInformationMock(location.getLocationId());
    }

    /**
     * При отсутствии:
     * Значения КЛАДР у непосредственного locationId, а также у всех родительских - выдается пустой CityId
     * -----
     * Поле street должно содержать комбинацию из значений город + улицу через запятую.
     */
    @Test
    void convertWithoutParentKladr() {
        Location location = new Location.LocationBuilder(null, null, "Москва")
                .setLocationId(GEO_INFORMATION_MAP.get("LEAF_NOKLADR_GEO_INFORMATION").getGeoId().intValue())
                .setStreet("Улица Пушкина")
                .setHouse("Дом Колотушкина")
                .setZipCode("123456")
                .setBuilding("building")
                .setHousing("housing")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        MarschrouteLocation actualLocation = locationConverter.convert(location);

        assertThat(actualLocation.getCityId()).isEmpty();

        assertThat(actualLocation.getStreet())
            .as("Street value should contain both locality and street values")
            .isEqualTo(location.getLocality() + ", " + location.getStreet());

        verifyGeoInformationMock(location.getLocationId());
    }

    /**
     * Проверяет, что
     * -----
     * При отсутствии:
     * Названия города
     * Названия улицы
     * Значения КЛАДР у непосредственного locationId  (был взять родительский)
     * -----
     * Поле street должно содержать
     * {@value ru.yandex.market.fulfillment.wrap.marschroute.model.converter.LocationConverter#MISSING_STREET_REPLACEMENT}
     */
    @Test
    void convertWithParentKladrAndNoStreetOrCity() {
        Location location = new Location.LocationBuilder(null, null, null)
                .setLocationId(GEO_INFORMATION_MAP.get("PARENT_GEO_INFORMATION").getGeoId().intValue())
                .setStreet(null)
                .setHouse("Дом Колотушкина")
                .setZipCode("123456")
                .setBuilding("building")
                .setHousing("housing")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        MarschrouteLocation actualLocation = locationConverter.convert(location);

        assertThat(actualLocation.getStreet())
            .as("Street value should contain missing street replacement")
            .isEqualTo(LocationConverter.MISSING_STREET_REPLACEMENT);

        verifyGeoInformationMock(location.getLocationId());
    }


    /**
     * Проверяем, что при условии, когда geoId в заказе и geoId выбранного GeoInformation отличаются,
     * но при отсутствии значения улицы - в итоговое значение улицы будет вписано значение
     * locality + {@value ru.yandex.market.fulfillment.wrap.marschroute.model.converter.LocationConverter#MISSING_STREET_REPLACEMENT}
     */
    @Test
    void convertWithMissingStreetValueCombined() {
        Location location = new Location.LocationBuilder(null, null, "Москва")
                .setLocationId(100500)
                .setStreet(null)
                .setHouse("Дом Колотушкина")
                .setZipCode("123456")
                .setBuilding("building")
                .setHousing("housing")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        mockGeoInformationProviderReturnsEmptyResult(100500L);

        MarschrouteLocation actualLocation = locationConverter.convert(location);

        assertThat(actualLocation.getStreet())
            .as("Street value should only contain street value")
            .isEqualTo(location.getLocality() + ", " + LocationConverter.MISSING_STREET_REPLACEMENT);

        verifyGeoInformationMock(location.getLocationId());
    }


    /**
     * Проверяет, что при отствии гео информации
     * -----
     * Поле street должно содержать  название улицы и город.
     */
    @Test
    void convertGeoInfoNotFound() {
        Location location = new Location.LocationBuilder(null, null, "Москва")
                .setLocationId(100500)
                .setStreet("Улица Пушкина")
                .setHouse("Дом Колотушкина")
                .setZipCode("123456")
                .setBuilding("building")
                .setHousing("housing")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        mockGeoInformationProviderReturnsEmptyResult(100500L);
        MarschrouteLocation actualLocation = locationConverter.convert(location);

        assertThat(actualLocation.getStreet())
            .as("Street value should only contain street value")
            .isEqualTo(location.getLocality() + ", " + location.getStreet());

        verifyGeoInformationMock(location.getLocationId());
    }

    /**
     * Проверяет, что
     * -----
     * При наличии:
     * kladrIdMarschroute
     * -----
     * Поле cityId должно быть равно первым 11 цифрам kladrIdMarschroute.
     */
    @Test
    void convertWithKladrIdMarschroute() {
        Location location = new Location.LocationBuilder(null, null, "Москва")
                .setLocationId(GEO_INFORMATION_MAP.get("KLADR_ID_MARSCHROUTE_GEO_INFORMATION").getGeoId().intValue())
                .setStreet("Улица Пушкина")
                .setHouse("Дом Колотушкина")
                .setZipCode("123456")
                .setBuilding("building")
                .setHousing("housing")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        MarschrouteLocation marschrouteLocation = locationConverter.convert(location);

        softly.assertThat(marschrouteLocation.getCityId())
            .as("Asserting city id value")
            .isEqualTo(GEO_INFORMATION_MAP.get("KLADR_ID_MARSCHROUTE_GEO_INFORMATION")
                .getKladrIdMarschroute().substring(0, 11));
    }

    /**
     * Проверяет, что
     * -----
     * При наличии:
     * kladrIdMarschroute
     * Равном пустой строке
     * И наличии:
     * непустого kladrId
     * -----
     * Поле cityId должно быть равно первым 11 цифрам kladrId.
     */
    @Test
    void convertWithEmptyKladrIdMarschroute() {
        Location location = new Location.LocationBuilder(null, null, "Москва")
                .setLocationId(GEO_INFORMATION_MAP.get("KLADR_ID_MARSCHROUTE_EMPTY_GEO_INFORMATION").getGeoId().intValue())
                .setStreet("Улица Пушкина")
                .setHouse("Дом Колотушкина")
                .setZipCode("123456")
                .setBuilding("building")
                .setHousing("housing")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        MarschrouteLocation marschrouteLocation = locationConverter.convert(location);

        softly.assertThat(marschrouteLocation.getCityId())
            .as("Asserting city id value")
            .isEqualTo(GEO_INFORMATION_MAP.get("KLADR_ID_MARSCHROUTE_EMPTY_GEO_INFORMATION")
                .getKladrId().substring(0, 11));
    }


    /**
     * Проверяет, что
     * -----
     * При наличии:
     * Названия улицы
     * -----
     * При отсутствии:
     * Названия города
     * Значения КЛАДР у непосредственного locationId  (был взять родительский)
     * -----
     * Поле street должно содержать только название улицы.
     */
    @Test
    void convertWithParentKladrAndOnlyStreet() {
        Location location = new Location.LocationBuilder(null, null, null)
                .setLocationId(GEO_INFORMATION_MAP.get("LEAF_GEO_INFORMATION").getGeoId().intValue())
                .setStreet("Улица Пушкина")
                .setHouse("Дом Колотушкина")
                .setZipCode("123456")
                .setBuilding("building")
                .setHousing("housing")
                .setPorch("1")
                .setFloor(15)
                .setRoom("room")
                .build();

        MarschrouteLocation actualLocation = locationConverter.convert(location);

        assertThat(actualLocation.getStreet())
            .as("Street value should only contain street value")
            .isEqualTo(location.getStreet());

        verifyGeoInformationMock(location.getLocationId());
    }

    private void mockGeoInformationProvider(@Nonnull GeoInformation geoInformation) {
        mockGeoInformationProvider(geoInformation.getGeoId(), geoInformation);
    }

    private void mockGeoInformationProvider(Long geoId, @Nonnull GeoInformation geoInformation) {
        doReturn(Optional.of(geoInformation)).when(geoInformationProvider).provide(geoId);
    }

    private void mockGeoInformationProviderReturnsEmptyResult(Long geoId) {
        doReturn(Optional.empty()).when(geoInformationProvider).provide(geoId);
    }

    private void verifyGeoInformationMock(@Nonnull GeoInformation geoInformation) {
        verifyGeoInformationMock(geoInformation.getGeoId().intValue());
    }

    private void verifyGeoInformationMock(Integer geoId) {
        verify(geoInformationProvider).findWithKladr(Long.valueOf(geoId));
    }
}
