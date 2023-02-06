package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.misc.io.http.UrlUtils;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.PRECISE_REGION_ENABLED;

public class CheckoutGeoClientTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer geocoderMock;
    @Autowired
    private WireMockServer geobaseMock;
    @Autowired
    private WireMockServer reportMock;

    @BeforeEach
    public void createOrder() {
        checkouterFeatureWriter.writeValue(PRECISE_REGION_ENABLED, true);
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndEmptyGps();
    }

    @AfterEach
    public void cleanUp() {
        geocoderMock.resetAll();
        geobaseMock.resetAll();
        reportMock.resetAll();
        checkouterFeatureWriter.writeValue(PRECISE_REGION_ENABLED, PRECISE_REGION_ENABLED.getDefaultValue());
    }

    @Test
    public void shouldPassSubjectFederationToGeocoder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getDelivery().setRegionId(120670L);

        orderCreateHelper.cart(parameters);

        List<ServeEvent> allServeEvents = geocoderMock.getAllServeEvents();

        assertThat(allServeEvents, hasSize(1));

        LoggedRequest request = allServeEvents.get(0).getRequest();
        String geoString = UrlUtils.urlDecode(request.getUrl());

        assertThat(geoString, allOf(
                CoreMatchers.containsString("Санкт-Петербург и Ленинградская область"),
                CoreMatchers.containsString("Санкт-Петербург"),
                CoreMatchers.containsString("Приморский район"),
                CoreMatchers.containsString("Лисий Нос"),
                CoreMatchers.not(CoreMatchers.containsString("Питер"))
        ));
    }

    @Test
    public void shouldNotPassSubjectOfFedereationIfPropertyIsDisabled() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getDelivery().setRegionId(120670L);

        orderCreateHelper.cart(parameters);

        List<ServeEvent> allServeEvents = geocoderMock.getAllServeEvents();

        assertThat(allServeEvents, hasSize(1));

        LoggedRequest request = allServeEvents.get(0).getRequest();
        String geoString = UrlUtils.urlDecode(request.getUrl());

        assertThat(geoString, CoreMatchers.not(CoreMatchers.containsString("Москва и Московская область")));
    }

    @Test
    public void shouldNotPassSubjectFederationToGeocoderForMoscow() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getDelivery().setRegionId(213L);

        orderCreateHelper.cart(parameters);

        List<ServeEvent> allServeEvents = geocoderMock.getAllServeEvents();

        assertThat(allServeEvents, hasSize(1));

        LoggedRequest request = allServeEvents.get(0).getRequest();
        String geoString = UrlUtils.urlDecode(request.getUrl());

        assertThat(geoString, CoreMatchers.containsString("Москва"));
        assertThat(geoString, not(CoreMatchers.containsString("Москва и Московская область")));
    }

    @Test
    public void shouldRetunPreciseRegionForMoscow() {
        geobaseMock.stubFor(
                get(urlPathEqualTo("/v1/region_id_by_location"))
                        .willReturn(okJson("213")));

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getDelivery().setRegionId(213L);

        orderCreateHelper.cart(parameters);

        checkActualDeliveryRegion("213");
    }

    @Test
    public void shouldRetunPreciseRegionForMoscowDistricts() {
        geobaseMock.stubFor(
                get(urlPathEqualTo("/v1/region_id_by_location"))
                        //Троицк, входит в состав Москвы
                        .willReturn(okJson("20674")));

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getDelivery().setRegionId(213L);

        orderCreateHelper.cart(parameters);

        checkActualDeliveryRegion("20674");
    }

    @Test
    public void shouldRetunMoscowForAnotherRegions() {
        geobaseMock.stubFor(
                get(urlPathEqualTo("/v1/region_id_by_location"))
                        //Городской округ Электросталь, Московская область
                        .willReturn(okJson("120993")));

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getDelivery().setRegionId(213L);

        orderCreateHelper.cart(parameters);

        checkActualDeliveryRegion("213");
    }

    private void checkActualDeliveryRegion(String region) {
        Collection<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();
        Collection<ServeEvent> actualDeliveryCalls = serveEvents.stream()
                .filter(
                        se -> se.getRequest()
                                .queryParameter("place")
                                .containsValue(MarketReportPlace.ACTUAL_DELIVERY.getId())
                )
                .filter(
                        se -> se.getRequest()
                                .queryParameter("rids")
                                .containsValue(region)
                )
                .collect(Collectors.toList());

        assertThat(actualDeliveryCalls, Matchers.hasSize(greaterThanOrEqualTo(1)));
    }
}
