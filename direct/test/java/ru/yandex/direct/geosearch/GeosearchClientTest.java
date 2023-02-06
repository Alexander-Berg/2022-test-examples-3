package ru.yandex.direct.geosearch;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.geosearch.model.Address;
import ru.yandex.direct.geosearch.model.AddressComponent;
import ru.yandex.direct.geosearch.model.GeoObject;
import ru.yandex.direct.geosearch.model.Kind;
import ru.yandex.direct.geosearch.model.Precision;
import ru.yandex.direct.utils.io.RuntimeIoException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class GeosearchClientTest extends GeosearchClientTestBase {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    /**
     * Чтобы получить содержимое файлов из validResponsePath в человеко-читаемом виде нужно сделать следующий запрос
     * <p>
     * curl "http://addrs-testing.search.yandex.net/search/stable/yandsearch?ms=pb&hr=yes&lang=ru_RU&origin=direct&text=xxxx&type=geo"
     * text - url-кодированный адрес вида "Россия Москва Красная площадь 1"
     **/
    @Parameterized.Parameter
    public String validResponsePath;
    @Parameterized.Parameter(value = 1)
    public String addressOrCoordnstes;
    @Parameterized.Parameter(value = 2)
    public GeoObject expectedGeoObject;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{
                        "/valid_full_response1",
                        new Address().withCountry("Россия").withCity("Москва")
                                .withStreet("Красная площадь").withBuilding("1").toString(),
                        new GeoObject.Builder()
                                .withGeoId(213L)
                                .withHouses(0L)
                                .withCountry("Россия")
                                .withCity("Москва")
                                .withStreet("Красная площадь")
                                .withHouse("1")
                                .withText("Россия, Москва, Красная площадь, 1")
                                .withName("Красная площадь, 1")
                                .withX(37.617716)
                                .withY(55.755322)
                                .withX1(37.613611)
                                .withY1(55.753007)
                                .withX2(37.621821)
                                .withY2(55.757637)
                                .withPrecision(Precision.EXACT)
                                .withKind(Kind.HOUSE)
                                .withComponents(asList(
                                        new AddressComponent(Kind.COUNTRY, "Россия"),
                                        new AddressComponent(Kind.PROVINCE, "Центральный федеральный округ"),
                                        new AddressComponent(Kind.PROVINCE, "Москва"),
                                        new AddressComponent(Kind.LOCALITY, "Москва"),
                                        new AddressComponent(Kind.STREET, "Красная площадь"),
                                        new AddressComponent(Kind.HOUSE, "1")
                                ))
                                .build()},
                new Object[]{
                        "/valid_full_response2",
                        new Address().withCountry("Россия").withCity("Воронеж")
                                .withStreet("Рижская").withBuilding("16к6").toString(),
                        new GeoObject.Builder()
                                .withGeoId(193L)
                                .withHouses(0L)
                                .withCountry("Россия")
                                .withAdministrativeArea("городской округ Воронеж")
                                .withCity("Воронеж")
                                .withStreet("Рижская улица")
                                .withHouse("16к6")
                                .withText("Россия, Воронеж, Рижская улица, 16к6")
                                .withName("Рижская улица, 16к6")
                                .withX(39.291331)
                                .withY(51.665663)
                                .withX1(39.287226)
                                .withY1(51.663110)
                                .withX2(39.295437)
                                .withY2(51.668216)
                                .withPrecision(Precision.EXACT)
                                .withKind(Kind.HOUSE)
                                .withComponents(asList(
                                        new AddressComponent(Kind.COUNTRY, "Россия"),
                                        new AddressComponent(Kind.PROVINCE, "Центральный федеральный округ"),
                                        new AddressComponent(Kind.PROVINCE, "Воронежская область"),
                                        new AddressComponent(Kind.AREA, "городской округ Воронеж"),
                                        new AddressComponent(Kind.LOCALITY, "Воронеж"),
                                        new AddressComponent(Kind.STREET, "Рижская улица"),
                                        new AddressComponent(Kind.HOUSE, "16к6")
                                ))
                                .build()},
                new Object[]{
                        "/valid_full_response_metro",
                        "37.587093,55.733969",
                        new GeoObject.Builder()
                                .withGeoId(20490L)
                                .withHouses(0L)
                                .withCountry("Россия")
                                .withCity("Москва")
                                .withStreet("Кольцевая линия")
                                .withText("Россия, Москва, Кольцевая линия, метро Парк культуры")
                                .withName("метро Парк культуры")
                                .withX(37.592869)
                                .withY(55.735302)
                                .withX1(37.58464)
                                .withY1(55.730659)
                                .withX2(37.601097)
                                .withY2(55.739944)
                                .withPrecision(Precision.EXACT)
                                .withKind(Kind.METRO)
                                .withComponents(asList(
                                        new AddressComponent(Kind.COUNTRY, "Россия"),
                                        new AddressComponent(Kind.PROVINCE, "Центральный федеральный округ"),
                                        new AddressComponent(Kind.PROVINCE, "Москва"),
                                        new AddressComponent(Kind.LOCALITY, "Москва"),
                                        new AddressComponent(Kind.ROUTE, "Кольцевая линия"),
                                        new AddressComponent(Kind.METRO, "метро Парк культуры")
                                ))
                                .build()},
                new Object[]{
                        "/valid_full_response_railway",
                        "Россия Москва платформа Битца",
                        new GeoObject.Builder()
                                .withGeoId(3L)
                                .withHouses(0L)
                                .withCountry("Россия")
                                .withStreet("Курское направление Московской железной дороги")
                                .withText("Россия, Курское направление Московской железной дороги, платформа Битца")
                                .withName("платформа Битца")
                                .withX(37.611554)
                                .withY(55.571245)
                                .withX1(37.603325)
                                .withY1(55.566583)
                                .withX2(37.619782)
                                .withY2(55.575907)
                                .withPrecision(Precision.EXACT)
                                .withKind(Kind.RAILWAY)
                                .withComponents(asList(
                                        new AddressComponent(Kind.COUNTRY, "Россия"),
                                        new AddressComponent(Kind.PROVINCE, "Центральный федеральный округ"),
                                        new AddressComponent(Kind.ROUTE, "Курское направление Московской железной дороги"),
                                        new AddressComponent(Kind.RAILWAY, "платформа Битца")
                                ))
                                .build()});
    }

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                try {
                    Buffer body = new Buffer().write(GeosearchClientTest.class.getResourceAsStream(validResponsePath).readAllBytes());
                    return new MockResponse().setBody(body);
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeIoException(e);
                }
            }
        };
    }


    @Test
    public void getGeoData_GetExactOneResult(){
        List<GeoObject> geoObjects = geosearchClient.searchAddress(addressOrCoordnstes);
        assertThat(geoObjects).contains(expectedGeoObject);
    }
}
