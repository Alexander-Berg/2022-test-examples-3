package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import yandex.maps.proto.search.precision.PrecisionOuterClass;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.util.geocoder.GeocoderParameters;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class GeocoderEnrichmentTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private WireMockServer geocoderMock;

    @Test
    void shouldSetGpsAndPostcode() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndEmptyGps();

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();
        Address buyerAddress = AddressProvider.getAddressWithoutPostcode();
        // чтобы "заглушить" использование personal для postCode. В противном случае возьмется 347660
        ((AddressImpl) buyerAddress).setPersonalAddressId("tbdtg45g4g");
        parameters.getOrder().getDelivery().setBuyerAddress(buyerAddress);

        Order order = orderCreateHelper.createOrder(parameters);

        assertEquals(GeocoderParameters.DEFAULT_POSTAL_CODE, order.getDelivery().getBuyerAddress().getPostcode());
        assertEquals(GeocoderParameters.DEFAULT_GPS.replace(" ", ","), order.getDelivery().getBuyerAddress().getGps());
    }

    @Test
    void shouldNotSetPostcodeIfPresent() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndEmptyGps();

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddress());

        Order order = orderCreateHelper.createOrder(parameters);

        assertEquals(order.getDelivery().getBuyerAddress().getPostcode(), AddressProvider.POSTCODE);
    }

    @Test
    void shouldPassLrParameter() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndEmptyGps();

        checkouterProperties.setSplitAddressAndRegionInGeocoderRequest(true);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddressWithoutPostcode());

        orderCreateHelper.createOrder(parameters);

        MatcherAssert.assertThat(geocoderMock.getAllServeEvents(), hasSize(1));
        LoggedRequest request = geocoderMock.getAllServeEvents().get(0).getRequest();
        MatcherAssert.assertThat(request.getQueryParams(), hasKey("lr"));

        QueryParameter queryParameter = request.getQueryParams().get("lr");
        Assertions.assertTrue(queryParameter.isSingleValued());
        MatcherAssert.assertThat(queryParameter.values(), hasSize(1));
        MatcherAssert.assertThat(queryParameter.values().get(0), CoreMatchers.is("213"));
    }

    @Test
    void shouldPassLrParameter2() throws Exception {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndEmptyGps();

        checkouterProperties.setSplitAddressAndRegionInGeocoderRequest(true);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddressWithoutPostcode());

        orderCreateHelper.createOrder(parameters);

        MatcherAssert.assertThat(geocoderMock.getAllServeEvents(), hasSize(1));
        LoggedRequest request = geocoderMock.getAllServeEvents().get(0).getRequest();
        MatcherAssert.assertThat(request.getQueryParams(), hasKey("lr"));

        QueryParameter queryParameter = request.getQueryParams().get("lr");
        Assertions.assertTrue(queryParameter.isSingleValued());
        MatcherAssert.assertThat(queryParameter.values(), hasSize(1));
        MatcherAssert.assertThat(queryParameter.values().get(0), CoreMatchers.is("213"));
    }

    @Test
    void shouldPassCityInAddress() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndEmptyGps();

        checkouterProperties.setSplitAddressAndRegionInGeocoderRequest(true);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();

        AddressImpl address = new AddressImpl();
        address.setCountry("Россия");
        address.setCity("Зеленоград");
        address.setHouse("1519");
        address.setApartment("284");
        address.setRecipient("Someguy");
        address.setPhone("2234562");

        parameters.getOrder().getDelivery().setBuyerAddress(address);

        orderCreateHelper.createOrder(parameters);

        MatcherAssert.assertThat(geocoderMock.getAllServeEvents(), hasSize(1));
        LoggedRequest request = geocoderMock.getAllServeEvents().get(0).getRequest();
        MatcherAssert.assertThat(request.getQueryParams(), hasKey("text"));

        QueryParameter queryParameter = request.getQueryParams().get("text");
        Assertions.assertTrue(queryParameter.isSingleValued());
        MatcherAssert.assertThat(queryParameter.values(), hasSize(1));
        MatcherAssert.assertThat(queryParameter.values().get(0), CoreMatchers.containsString("Зеленоград"));
    }

    @Test
    void shouldNearPrecision() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndEmptyGps();

        checkouterProperties.setSplitAddressAndRegionInGeocoderRequest(true);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddressWithoutPostcode());
        parameters.configuration().cart().multiCartMocks().getGeocoderParameters()
                .setPrecision(PrecisionOuterClass.Precision.NEARBY.name());
        Order order = orderCreateHelper.createOrder(parameters);
        assertNotNull(order.getDelivery().getBuyerAddress().getGps());
    }

    @Test
    void shouldAddDistrictInGeocoderRqWithSplitAddress() {
        checkouterProperties.setSplitAddressAndRegionInGeocoderRequest(true);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        String district = "district123";
        Address address = parameters.getOrder().getDelivery().getBuyerAddress();
        ((AddressImpl) address).setDistrict(district);
        initSpecialDistrict(address, district);
        orderCreateHelper.cart(parameters);

        List<ServeEvent> allServeEvents = geocoderMock.getAllServeEvents();
        MatcherAssert.assertThat(allServeEvents, hasSize(1));
        LoggedRequest request = allServeEvents.get(0).getRequest();
        assertTrue(request.getUrl().contains(district));
    }

    @Test
    void shouldAddDistrictInGeocoderRq() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        String district = "district123";
        Address address = parameters.getOrder().getDelivery().getBuyerAddress();
        ((AddressImpl) address).setDistrict(district);
        initSpecialDistrict(address, district);
        orderCreateHelper.cart(parameters);

        List<ServeEvent> allServeEvents = geocoderMock.getAllServeEvents();
        MatcherAssert.assertThat(allServeEvents, hasSize(1));
        LoggedRequest request = allServeEvents.get(0).getRequest();
        assertTrue(request.getUrl().contains(district));
    }

    private void initSpecialDistrict(Address address, String district) {
        PersAddress persAddress = PersAddress.convertToPersonal(address);
        persAddress.setDistrict(district);
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps(
                "34251639gwbcesaqq239098jhcdxe453",
                persAddress,
                null,
                null);

    }
}
