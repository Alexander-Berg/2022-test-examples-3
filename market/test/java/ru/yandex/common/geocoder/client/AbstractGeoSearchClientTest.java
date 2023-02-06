package ru.yandex.common.geocoder.client;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.protobuf.ExtensionRegistry;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentMatcher;

import yandex.maps.proto.common2.response.ResponseOuterClass;
import yandex.maps.proto.search.geocoder.Geocoder;
import yandex.maps.proto.search.search.Search;

import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.Component;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.Precision;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.common.test.util.ProtoTestUtil;

import static org.mockito.Mockito.mock;

@DisplayName("Тесты на GeoSearchApiClient")
public class AbstractGeoSearchClientTest {

    protected static final String GEO_SEARCH_API_URL = "http://addrs-testing.search.yandex" +
            ".net/search/stable/yandsearch?origin=market-geocoder-lib&text=";

    protected static final String QUERY = "Россия,Новосибирск,Николаева,11";
    protected static final String ENCODED_QUERY;

    static {
        try {
            ENCODED_QUERY = URLEncoder.encode(QUERY, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected final TvmTicketProvider tvmTicketProvider = Optional::empty;
    protected final HttpClient client = mock(HttpClient.class);
    protected final Function<URI, ArgumentMatcher<HttpUriRequest>> uriMatcher =
            uri -> argument -> argument.getURI().equals(uri);

    protected HttpResponse createHttpResponse(ResponseOuterClass.Response response) {
        HttpResponse httpResponse = new DefaultHttpResponseFactory().newHttpResponse(
                new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null),
                null
        );
        BasicHttpEntity responseEntity = new BasicHttpEntity();
        responseEntity.setContent(new ByteArrayInputStream(response.toByteArray()));
        httpResponse.setEntity(responseEntity);
        return httpResponse;
    }

    protected ResponseOuterClass.Response createProtobufResponseObject(String templateProtobufFilename) {
        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        registry.add(Search.sEARCHRESPONSEMETADATA);
        registry.add(Geocoder.gEOOBJECTMETADATA);
        return ProtoTestUtil.getProtoMessageByJson(
                ResponseOuterClass.Response.class,
                registry,
                templateProtobufFilename,
                getClass()
        );
    }

    protected GeoObject createSingleGeoObject() {
        return SimpleGeoObject.newBuilder()
                .withBoundary(Boundary.newBuilder()
                        .withEnvelopeLower("83.106462 54.855662")
                        .withEnvelopeUpper("83.114672 54.860399")
                        .build())
                .withToponymInfo(ToponymInfo.newBuilder()
                        .withPrecision(Precision.EXACT)
                        .withKind(Kind.HOUSE)
                        .withGeocoderObjectId("771369235")
                        .withComponents(Arrays.asList(
                                        new Component("Россия", Collections.singletonList(Kind.COUNTRY)),
                                        new Component("Сибирский федеральный округ",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("Новосибирская область",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("городской округ Новосибирск",
                                                Collections.singletonList(Kind.AREA)),
                                        new Component("Новосибирск", Collections.singletonList(Kind.LOCALITY)),
                                        new Component("улица Николаева", Collections.singletonList(Kind.STREET)),
                                        new Component("11", Collections.singletonList(Kind.HOUSE))
                                )
                        )
                        .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                        .withAddressLine("Россия, Новосибирск, улица Николаева, 11")
                        .withCountryInfo(CountryInfo.newBuilder()
                                .withCountryName("Россия")
                                .withCountryCode("RU")
                                .build()
                        )
                        .withAreaInfo(AreaInfo.newBuilder()
                                .withAdministrativeAreaName("Новосибирская область")
                                .withSubAdministrativeAreaName("городской округ Новосибирск")
                                .build()
                        )
                        .withLocalityInfo(LocalityInfo.newBuilder()
                                .withLocalityName("Новосибирск")
                                .withThoroughfareName("улица Николаева")
                                .withPostalCode("630090")
                                .withPremiseNumber("11")
                                .build()
                        )
                        .build()
                )
                .build();
    }

    @SuppressWarnings("checkstyle:MethodLength")
    protected List<GeoObject> createMultiGeoObjects() {
        return Arrays.asList(
                SimpleGeoObject.newBuilder()
                        .withBoundary(Boundary.newBuilder()
                                .withEnvelopeLower("83.106462 54.855662")
                                .withEnvelopeUpper("83.114672 54.860399")
                                .build()
                        )
                        .withToponymInfo(ToponymInfo.newBuilder()
                                .withPrecision(Precision.EXACT)
                                .withKind(Kind.HOUSE)
                                .withGeocoderObjectId("771369235")
                                .withComponents(Arrays.asList(
                                        new Component("Россия", Collections.singletonList(Kind.COUNTRY)),
                                        new Component("Сибирский федеральный округ",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("Новосибирская область",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("городской округ Новосибирск",
                                                Collections.singletonList(Kind.AREA)),
                                        new Component("Новосибирск", Collections.singletonList(Kind.LOCALITY)),
                                        new Component("улица Николаева", Collections.singletonList(Kind.STREET)),
                                        new Component("11", Collections.singletonList(Kind.HOUSE))
                                        )
                                )
                                .build()
                        )
                        .withAddressInfo(AddressInfo.newBuilder()
                                .withAddressLine("Россия, Новосибирск, улица Николаева, 11")
                                .withCountryInfo(CountryInfo.newBuilder()
                                        .withCountryName("Россия")
                                        .withCountryCode("RU")
                                        .build()
                                )
                                .withAreaInfo(AreaInfo.newBuilder()
                                        .withAdministrativeAreaName("Новосибирская область")
                                        .withSubAdministrativeAreaName("городской округ Новосибирск")
                                        .build()
                                )
                                .withLocalityInfo(LocalityInfo.newBuilder()
                                        .withLocalityName("Новосибирск")
                                        .withThoroughfareName("улица Николаева")
                                        .withPostalCode("630090")
                                        .withPremiseNumber("11")
                                        .build()
                                )
                                .build()
                        )
                        .build(),
                SimpleGeoObject.newBuilder()
                        .withBoundary(Boundary.newBuilder()
                                .withEnvelopeLower("83.10171 54.853864")
                                .withEnvelopeUpper("83.115229 54.859735")
                                .build()
                        )
                        .withToponymInfo(ToponymInfo.newBuilder()
                                .withPrecision(Precision.STREET)
                                .withKind(Kind.STREET)
                                .withGeocoderObjectId("11156368")
                                .withComponents(Arrays.asList(
                                        new Component("Россия", Collections.singletonList(Kind.COUNTRY)),
                                        new Component("Сибирский федеральный округ",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("Новосибирская область",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("городской округ Новосибирск",
                                                Collections.singletonList(Kind.AREA)),
                                        new Component("Новосибирск", Collections.singletonList(Kind.LOCALITY)),
                                        new Component("улица Николаева", Collections.singletonList(Kind.STREET))
                                        )
                                )
                                .build()
                        )
                        .withAddressInfo(AddressInfo.newBuilder()
                                .withAddressLine("Россия, Новосибирск, улица Николаева")
                                .withCountryInfo(CountryInfo.newBuilder()
                                        .withCountryName("Россия")
                                        .withCountryCode("RU")
                                        .build()
                                )
                                .withAreaInfo(AreaInfo.newBuilder()
                                        .withAdministrativeAreaName("Новосибирская область")
                                        .withSubAdministrativeAreaName("городской округ Новосибирск")
                                        .build()
                                )
                                .withLocalityInfo(LocalityInfo.newBuilder()
                                        .withLocalityName("Новосибирск")
                                        .withThoroughfareName("улица Николаева")
                                        .build()
                                )
                                .build()
                        )
                        .build(),
                SimpleGeoObject.newBuilder()
                        .withBoundary(Boundary.newBuilder()
                                .withEnvelopeLower("83.076153 54.824871")
                                .withEnvelopeUpper("83.126009 54.863627")
                                .build())
                        .withToponymInfo(ToponymInfo.newBuilder()
                                .withComponents(Arrays.asList(
                                        new Component("Россия",
                                                Collections.singletonList(Kind.COUNTRY)),
                                        new Component("Сибирский федеральный округ",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("Новосибирская область",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("городской округ Новосибирск",
                                                Collections.singletonList(Kind.AREA)),
                                        new Component("Новосибирск",
                                                Collections.singletonList(Kind.LOCALITY)),
                                        new Component("Советский район",
                                                Collections.singletonList(Kind.DISTRICT)),
                                        new Component("микрорайон Академгородок",
                                                Collections.singletonList(Kind.DISTRICT)),
                                        new Component("квартал Верхняя Зона Академгородка",
                                                Collections.singletonList(Kind.DISTRICT))
                                        )
                                )
                                .withPrecision(Precision.OTHER)
                                .withKind(Kind.DISTRICT)
                                .withGeocoderObjectId("1508551048")
                                .build()
                        )
                        .withAddressInfo(AddressInfo.newBuilder()
                                .withAddressLine("Россия, Новосибирск, Советский район, микрорайон Академгородок, " +
                                        "квартал Верхняя Зона Академгородка")
                                .withCountryInfo(CountryInfo.newBuilder()
                                        .withCountryName("Россия")
                                        .withCountryCode("RU")
                                        .build()
                                )
                                .withAreaInfo(AreaInfo.newBuilder()
                                        .withAdministrativeAreaName("Новосибирская область")
                                        .withSubAdministrativeAreaName("городской округ Новосибирск")
                                        .build()
                                )
                                .withLocalityInfo(LocalityInfo.newBuilder()
                                        .withLocalityName("Новосибирск")
                                        .withDependentLocalityName("квартал Верхняя Зона Академгородка")
                                        .build()
                                )
                                .build()
                        )
                        .build(),
                SimpleGeoObject.newBuilder()
                        .withBoundary(Boundary.newBuilder()
                                .withEnvelopeLower("83.076153 54.824871")
                                .withEnvelopeUpper("83.126009 54.880821")
                                .build())
                        .withToponymInfo(ToponymInfo.newBuilder()
                                .withPrecision(Precision.OTHER)
                                .withKind(Kind.DISTRICT)
                                .withGeocoderObjectId("53179423")
                                .withComponents(Arrays.asList(
                                        new Component("Россия", Collections.singletonList(Kind.COUNTRY)),
                                        new Component("Сибирский федеральный округ",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("Новосибирская область",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("городской округ Новосибирск",
                                                Collections.singletonList(Kind.AREA)),
                                        new Component("Новосибирск",
                                                Collections.singletonList(Kind.LOCALITY)),
                                        new Component("Советский район",
                                                Collections.singletonList(Kind.DISTRICT)),
                                        new Component("микрорайон Академгородок",
                                                Collections.singletonList(Kind.DISTRICT))
                                        )
                                )
                                .build()
                        )
                        .withAddressInfo(AddressInfo.newBuilder()
                                .withAddressLine("Россия, Новосибирск, Советский район, микрорайон Академгородок")
                                .withCountryInfo(CountryInfo.newBuilder()
                                        .withCountryName("Россия")
                                        .withCountryCode("RU")
                                        .build()
                                )
                                .withAreaInfo(AreaInfo.newBuilder()
                                        .withAdministrativeAreaName("Новосибирская область")
                                        .withSubAdministrativeAreaName("городской округ Новосибирск")
                                        .build())
                                .withLocalityInfo(LocalityInfo.newBuilder()
                                        .withLocalityName("Новосибирск")
                                        .withDependentLocalityName("микрорайон Академгородок")
                                        .build())
                                .build()
                        )
                        .build(),
                SimpleGeoObject.newBuilder()
                        .withBoundary(Boundary.newBuilder()
                                .withEnvelopeLower("82.925056 54.803637")
                                .withEnvelopeUpper("83.132019 54.913722")
                                .build())
                        .withToponymInfo(ToponymInfo.newBuilder()
                                .withComponents(Arrays.asList(
                                        new Component("Россия", Collections.singletonList(Kind.COUNTRY)),
                                        new Component("Сибирский федеральный округ",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("Новосибирская область",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("городской округ Новосибирск",
                                                Collections.singletonList(Kind.AREA)),
                                        new Component("Новосибирск",
                                                Collections.singletonList(Kind.LOCALITY)),
                                        new Component("Советский район",
                                                Collections.singletonList(Kind.DISTRICT))
                                        )
                                )
                                .withPrecision(Precision.OTHER)
                                .withKind(Kind.DISTRICT)
                                .withGeocoderObjectId("53177220")
                                .build()
                        )
                        .withAddressInfo(AddressInfo.newBuilder()
                                .withAddressLine("Россия, Новосибирск, Советский район")
                                .withCountryInfo(CountryInfo.newBuilder()
                                        .withCountryName("Россия")
                                        .withCountryCode("RU")
                                        .build()
                                )
                                .withAreaInfo(AreaInfo.newBuilder()
                                        .withAdministrativeAreaName("Новосибирская область")
                                        .withSubAdministrativeAreaName("городской округ Новосибирск")
                                        .build())
                                .withLocalityInfo(LocalityInfo.newBuilder()
                                        .withLocalityName("Новосибирск")
                                        .withDependentLocalityName("Советский район")
                                        .build())
                                .build()
                        )
                        .build(),
                SimpleGeoObject.newBuilder()
                        .withBoundary(Boundary.newBuilder()
                                .withEnvelopeLower("82.751897 54.803637")
                                .withEnvelopeUpper("83.16019 55.199424")
                                .build()
                        )
                        .withToponymInfo(ToponymInfo.newBuilder()
                                .withPrecision(Precision.OTHER)
                                .withKind(Kind.LOCALITY)
                                .withGeocoderObjectId("53118058")
                                .withComponents(Arrays.asList(
                                        new Component("Россия", Collections.singletonList(Kind.COUNTRY)),
                                        new Component("Сибирский федеральный округ",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("Новосибирская область",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("городской округ Новосибирск",
                                                Collections.singletonList(Kind.AREA)),
                                        new Component("Новосибирск",
                                                Collections.singletonList(Kind.LOCALITY))
                                        )
                                )
                                .build()
                        )
                        .withAddressInfo(AddressInfo.newBuilder()
                                .withAddressLine("Россия, Новосибирск")
                                .withCountryInfo(CountryInfo.newBuilder()
                                        .withCountryName("Россия")
                                        .withCountryCode("RU")
                                        .build()
                                )
                                .withAreaInfo(AreaInfo.newBuilder()
                                        .withAdministrativeAreaName("Новосибирская область")
                                        .withSubAdministrativeAreaName("городской округ Новосибирск")
                                        .build()
                                )
                                .withLocalityInfo(LocalityInfo.newBuilder()
                                        .withLocalityName("Новосибирск")
                                        .build()
                                )
                                .build()
                        )
                        .build(),
                SimpleGeoObject.newBuilder()
                        .withBoundary(Boundary.newBuilder()
                                .withEnvelopeLower("82.751897 54.800917")
                                .withEnvelopeUpper("83.16019 55.199424")
                                .build()
                        )
                        .withToponymInfo(ToponymInfo.newBuilder()
                                .withComponents(Arrays.asList(
                                        new Component("Россия", Collections.singletonList(Kind.COUNTRY)),
                                        new Component("Сибирский федеральный округ",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("Новосибирская область",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("городской округ Новосибирск",
                                                Collections.singletonList(Kind.AREA))
                                        )
                                )
                                .withPrecision(Precision.OTHER)
                                .withKind(Kind.AREA)
                                .withGeocoderObjectId("53001924")
                                .build()
                        )
                        .withAddressInfo(AddressInfo.newBuilder()
                                .withAddressLine("Россия, городской округ Новосибирск")
                                .withCountryInfo(CountryInfo.newBuilder()
                                        .withCountryName("Россия")
                                        .withCountryCode("RU")
                                        .build()
                                )
                                .withAreaInfo(AreaInfo.newBuilder()
                                        .withAdministrativeAreaName("Новосибирская область")
                                        .withSubAdministrativeAreaName("городской округ Новосибирск")
                                        .build()
                                )
                                .withLocalityInfo(LocalityInfo.newBuilder().build())
                                .build()
                        )
                        .build(),
                SimpleGeoObject.newBuilder()
                        .withBoundary(Boundary.newBuilder()
                                .withEnvelopeLower("75.088441 53.290643")
                                .withEnvelopeUpper("85.115984 57.235356")
                                .build()
                        )
                        .withToponymInfo(ToponymInfo.newBuilder()
                                .withPrecision(Precision.OTHER)
                                .withKind(Kind.PROVINCE)
                                .withGeocoderObjectId("53000048")
                                .withComponents(Arrays.asList(
                                        new Component("Россия", Collections.singletonList(Kind.COUNTRY)),
                                        new Component("Сибирский федеральный округ",
                                                Collections.singletonList(Kind.PROVINCE)),
                                        new Component("Новосибирская область",
                                                Collections.singletonList(Kind.PROVINCE))
                                        )
                                )
                                .build()
                        )
                        .withAddressInfo(AddressInfo.newBuilder()
                                .withAddressLine("Россия, Новосибирская область")
                                .withCountryInfo(CountryInfo.newBuilder()
                                        .withCountryName("Россия")
                                        .withCountryCode("RU")
                                        .build()
                                )
                                .withAreaInfo(AreaInfo.newBuilder()
                                        .withAdministrativeAreaName("Новосибирская область")
                                        .build()
                                )
                                .withLocalityInfo(LocalityInfo.newBuilder().build())
                                .build()
                        )
                        .build(),
                SimpleGeoObject.newBuilder()
                        .withBoundary(Boundary.newBuilder()
                                .withEnvelopeLower("70.357401 49.070793")
                                .withEnvelopeUpper("119.139388 81.304665")
                                .build()
                        )
                        .withToponymInfo(ToponymInfo.newBuilder()
                                .withPrecision(Precision.OTHER)
                                .withKind(Kind.PROVINCE)
                                .withGeocoderObjectId("53000007")
                                .withComponents(Arrays.asList(
                                        new Component("Россия", Collections.singletonList(Kind.COUNTRY)),
                                        new Component("Сибирский федеральный округ",
                                                Collections.singletonList(Kind.PROVINCE))
                                        )
                                )
                                .build()
                        )
                        .withAddressInfo(AddressInfo.newBuilder()
                                .withAddressLine("Россия, Сибирский федеральный округ")
                                .withCountryInfo(CountryInfo.newBuilder()
                                        .withCountryName("Россия")
                                        .withCountryCode("RU")
                                        .build()
                                )
                                .withAreaInfo(AreaInfo.newBuilder()
                                        .withAdministrativeAreaName("Сибирский федеральный округ")
                                        .build()
                                )
                                .withLocalityInfo(LocalityInfo.newBuilder().build())
                                .build()
                        )
                        .build(),
                SimpleGeoObject.newBuilder()
                        .withBoundary(Boundary.newBuilder()
                                .withEnvelopeLower("19.484764 41.185996")
                                .withEnvelopeUpper("191.128012 81.886117")
                                .build()
                        )
                        .withToponymInfo(ToponymInfo.newBuilder()
                                .withPrecision(Precision.OTHER)
                                .withKind(Kind.COUNTRY)
                                .withGeocoderObjectId("53000001")
                                .withComponents(Collections.singletonList(
                                                new Component("Россия", Collections.singletonList(Kind.COUNTRY))
                                        )
                                )
                                .build()
                        )
                        .withAddressInfo(AddressInfo.newBuilder()
                                .withAddressLine("Россия")
                                .withCountryInfo(CountryInfo.newBuilder()
                                        .withCountryName("Россия")
                                        .withCountryCode("RU")
                                        .build()
                                )
                                .withAreaInfo(AreaInfo.newBuilder().build())
                                .withLocalityInfo(LocalityInfo.newBuilder().build())
                                .build()
                        )
                        .build()

        );
    }

}
