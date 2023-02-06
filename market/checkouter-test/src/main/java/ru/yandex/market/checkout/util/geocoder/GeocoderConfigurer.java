package ru.yandex.market.checkout.util.geocoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.TextFormat;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import yandex.maps.proto.common2.response.ResponseOuterClass;
import yandex.maps.proto.entrance.EntranceOuterClass;
import yandex.maps.proto.search.business.Business;
import yandex.maps.proto.search.business_internal.BusinessInternal;
import yandex.maps.proto.search.experimental.Experimental;
import yandex.maps.proto.search.geocoder.Geocoder;
import yandex.maps.proto.search.geocoder_internal.GeocoderInternal;
import yandex.maps.proto.search.references.ReferencesOuterClass;
import yandex.maps.proto.search.search.Search;
import yandex.maps.proto.uri.Uri;
import ru.yandex.common.util.IOUtils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

/**
 * Конфигуратор мока геокодера. Шаблон ответа в файле /generators/geocoder/georesponse.txt.
 * Выполняет подстановку параметров вида <pre>${lon}</pre>
 *
 * @author sergeykoles
 * Created on: 19.07.18
 */
@TestComponent
public class GeocoderConfigurer {

    private static final ExtensionRegistry REGISTRY = ExtensionRegistry.newInstance();
    private static final Map<String, Function<GeocoderParameters, String>> MAPPINGS = new HashMap<>();
    private static final String TEMPLATE;

    static {
        REGISTRY.add(Uri.gEOOBJECTMETADATA);
        REGISTRY.add(Business.gEOOBJECTMETADATA);
        REGISTRY.add(ReferencesOuterClass.gEOOBJECTMETADATA);
        REGISTRY.add(Search.sEARCHMETADATA);
        REGISTRY.add(EntranceOuterClass.eNTRANCEMETADATA);
        REGISTRY.add(Experimental.gEOOBJECTMETADATA);
        REGISTRY.add(BusinessInternal.cOMPANYINFO);
        REGISTRY.add(Search.sEARCHRESPONSEMETADATA);
        REGISTRY.add(Geocoder.gEOOBJECTMETADATA);
        REGISTRY.add(Geocoder.rESPONSEMETADATA);
        REGISTRY.add(GeocoderInternal.rESPONSEINFO);
        REGISTRY.add(GeocoderInternal.tOPONYMINFO);


        // тут можно писать лямбды для заполнения плейсхолдеров
        MAPPINGS.put("${lon}", GeocoderParameters::getLongitude);
        MAPPINGS.put("${lat}", GeocoderParameters::getLatitude);

        MAPPINGS.put(
                // тут можно писать лямбды для заполнения плейсхолдеров
                "gps", GeocoderParameters::getGps
        );
        MAPPINGS.put(
                "${postalCode}", GeocoderParameters::getPostalCode
        );
        MAPPINGS.put(
                "${precision}", GeocoderParameters::getPrecision
        );
        try {
            TEMPLATE = IOUtils.readInputStream(GeocoderConfigurer.class
                    .getResourceAsStream("/generators/geocoder/georesponse.txt")
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read geocoder response template");
        }
    }

    @Autowired
    private WireMockServer geocoderMock;

    public void resetAll() {
        geocoderMock.resetAll();
    }

    public void mock(GeocoderParameters geocoderParameters) {
        geocoderMock.stubFor(
                get(anyUrl())
                        .willReturn(makeResponse(geocoderParameters))
        );
    }

    /**
     * Выполняет подстановки плейсхолдеров в шаблоне исходя из переданных мок-параметров.
     *
     * @param geocoderParameters параметры для подстановки в шаблон
     * @return текст ответа геокодера
     */
    private ResponseDefinitionBuilder makeResponse(GeocoderParameters geocoderParameters) {

        String result = TEMPLATE;
        for (String key : MAPPINGS.keySet()) {
            String replacement = MAPPINGS.get(key).apply(geocoderParameters);
            result = StringUtils.replace(result, key, replacement == null ? "" : replacement);
        }

        ResponseOuterClass.Response.Builder response = ResponseOuterClass.Response.newBuilder();
        try {
            TextFormat.merge(result, REGISTRY, response);
        } catch (TextFormat.ParseException ex) {
            throw new IllegalStateException("Error reading mock geo response from file", ex);
        }

        return aResponse()
                .withStatus(200)
                .withBody(response.build().toByteArray());
    }
}
