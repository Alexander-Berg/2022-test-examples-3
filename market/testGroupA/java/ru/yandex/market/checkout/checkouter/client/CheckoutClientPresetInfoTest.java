package ru.yandex.market.checkout.checkouter.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.CartPresetInfo;
import ru.yandex.market.checkout.checkouter.order.PresetInfo;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.common.report.model.AddressPreset;
import ru.yandex.market.common.report.model.DeliveryAvailable;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.PresetParcel;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CheckoutClientPresetInfoTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer geobaseMock;
    @Autowired
    private WireMockServer reportMock;

    @Test
    public void shouldActualPreset() {
        String presetId = UUID.randomUUID().toString();
        PresetInfo presetInfo = new PresetInfo();
        presetInfo.setPresetId(presetId);
        AddressImpl address = (AddressImpl) AddressProvider.getAddress();
        address.setGps("37.60312745,55.73999513");
        presetInfo.setBuyerAddress(address);
        presetInfo.setType(DeliveryType.DELIVERY);
        presetInfo.setRegionId(5000000000L);
        presetInfo.setOutletId(5000000000L);

        AddressPreset addressPreset = new AddressPreset();
        addressPreset.setId(presetId);
        addressPreset.setType(DeliveryType.DELIVERY.name());
        PresetParcel presetParcel = new PresetParcel();
        presetParcel.setParcelIndex(0);
        presetParcel.setDeliveryAvailable(DeliveryAvailable.AVAILABLE);
        presetParcel.setTryingAvailable(true);
        addressPreset.setParcels(Collections.singletonList(presetParcel));

        Parameters parameters = new Parameters();
        parameters.setPresets(Collections.singletonList(presetInfo));

        parameters.getReportParameters().getActualDelivery().setAddressPresets(
                Collections.singletonList(addressPreset));
        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertEquals(1, multiCart.getPresets().size());

        PresetInfo presetInfoActual = multiCart.getPresets().get(0);
        assertEquals(presetId, presetInfoActual.getPresetId());
        assertNull(presetInfoActual.getBuyerAddress());
        assertNull(presetInfoActual.getOutletId());
        assertNull(presetInfoActual.getRegionId());
        assertEquals(DeliveryType.DELIVERY, presetInfoActual.getType());
        assertEquals(1, presetInfoActual.getCarts().size());

        CartPresetInfo cartPresetInfo = presetInfoActual.getCarts().get(0);
        assertNotNull(cartPresetInfo.getLabel());
        assertEquals(true, cartPresetInfo.isDeliveryAvailable());
        assertEquals(true, cartPresetInfo.getTryingAvailable());
    }


    @Test
    public void shouldActualPresetWithUnavailable() {
        String presetId = UUID.randomUUID().toString();
        PresetInfo presetInfo = new PresetInfo();
        presetInfo.setPresetId(presetId);
        AddressImpl address = (AddressImpl) AddressProvider.getAddress();
        address.setGps("37.60312745,55.73999513");
        presetInfo.setBuyerAddress(address);
        presetInfo.setType(DeliveryType.POST);

        AddressPreset addressPreset = new AddressPreset();
        addressPreset.setId(presetId);
        addressPreset.setType(DeliveryType.POST.name());
        PresetParcel presetParcel = new PresetParcel();
        presetParcel.setParcelIndex(0);
        presetParcel.setDeliveryAvailable(DeliveryAvailable.UNAVAILABLE);
        addressPreset.setParcels(Collections.singletonList(presetParcel));

        Parameters parameters = new Parameters();
        parameters.setPresets(Collections.singletonList(presetInfo));

        parameters.getReportParameters().getActualDelivery().setAddressPresets(
                Collections.singletonList(addressPreset));
        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertEquals(1, multiCart.getPresets().size());

        PresetInfo presetInfoActual = multiCart.getPresets().get(0);
        assertEquals(presetId, presetInfoActual.getPresetId());
        assertNull(presetInfoActual.getBuyerAddress());
        assertNull(presetInfoActual.getOutletId());
        assertNull(presetInfoActual.getRegionId());
        assertEquals(DeliveryType.POST, presetInfoActual.getType());
        assertEquals(1, presetInfoActual.getCarts().size());

        CartPresetInfo cartPresetInfo = presetInfoActual.getCarts().get(0);
        assertNotNull(cartPresetInfo.getLabel());
        assertEquals(false, cartPresetInfo.isDeliveryAvailable());
    }

    @Test
    public void shouldActualPresetWithPrecisionRegion() {
        String presetId = UUID.randomUUID().toString();
        PresetInfo presetInfo = new PresetInfo();
        presetInfo.setPresetId(presetId);
        AddressImpl address = (AddressImpl) AddressProvider.getAddress();
        address.setGps("37.60312745,55.73999513");
        presetInfo.setBuyerAddress(address);
        presetInfo.setType(DeliveryType.POST);
        presetInfo.setRegionId(213L);
        presetInfo.setOutletId(5000000000L);

        Parameters parameters = new Parameters();
        parameters.setPresets(Collections.singletonList(presetInfo));

        String precisionRegionId = "120542";

        geobaseMock.stubFor(
                get(urlPathEqualTo("/v1/region_id_by_location"))
                        .willReturn(okJson(precisionRegionId)));

        orderCreateHelper.cart(parameters);

        Collection<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();
        Collection<ServeEvent> actualDeliveryCalls = serveEvents.stream()
                .filter(
                        se -> se.getRequest()
                                .queryParameter("place")
                                .containsValue(MarketReportPlace.ACTUAL_DELIVERY.getId())
                )
                .filter(
                        se -> se.getRequest()
                                .queryParameter("address")
                                .toString()
                                .contains("rid:" + precisionRegionId)
                )
                .collect(Collectors.toList());

        assertEquals(1, actualDeliveryCalls.size());
    }

    @Test
    public void shouldActualPresetWithPrecisionRegionFromRequest() {
        long preciseRegionExpected = 102133L;
        String presetId = UUID.randomUUID().toString();
        PresetInfo presetInfo = new PresetInfo();
        presetInfo.setPresetId(presetId);
        AddressImpl address = (AddressImpl) AddressProvider.getAddress();
        address.setPreciseRegionId(preciseRegionExpected);
        address.setGps("37.60312745,55.73999513");
        presetInfo.setBuyerAddress(address);
        presetInfo.setType(DeliveryType.POST);
        presetInfo.setRegionId(213L);
        presetInfo.setOutletId(5000000000L);

        Parameters parameters = new Parameters();
        parameters.setPresets(Collections.singletonList(presetInfo));
        orderCreateHelper.cart(parameters);

        Collection<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();
        Collection<ServeEvent> actualDeliveryCalls = serveEvents.stream()
                .filter(
                        se -> se.getRequest()
                                .queryParameter("place")
                                .containsValue(MarketReportPlace.ACTUAL_DELIVERY.getId())
                )
                .filter(
                        se -> se.getRequest()
                                .queryParameter("address")
                                .toString()
                                .contains("rid:" + preciseRegionExpected)
                )
                .collect(Collectors.toList());

        assertEquals(1, actualDeliveryCalls.size());
    }

    @Test
    public void shouldActualPresetWithoutPrecisionRegion() {
        String presetId = UUID.randomUUID().toString();
        PresetInfo presetInfo = new PresetInfo();
        presetInfo.setPresetId(presetId);
        AddressImpl address = (AddressImpl) AddressProvider.getAddress();
        address.setGps("37.60312745,55.73999513");
        presetInfo.setBuyerAddress(address);
        presetInfo.setType(DeliveryType.POST);
        presetInfo.setRegionId(213L);
        presetInfo.setOutletId(5000000000L);

        Parameters parameters = new Parameters();
        parameters.setPresets(Collections.singletonList(presetInfo));

        geobaseMock.stubFor(
                get(urlPathEqualTo("/v1/region_id_by_location"))
                        .willReturn(notFound()));

        orderCreateHelper.cart(parameters);

        Collection<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();
        Collection<ServeEvent> actualDeliveryCalls = serveEvents.stream()
                .filter(
                        se -> se.getRequest()
                                .queryParameter("place")
                                .containsValue(MarketReportPlace.ACTUAL_DELIVERY.getId())
                )
                .filter(
                        se -> se.getRequest()
                                .queryParameter("address")
                                .toString()
                                .contains("rid:213")
                )
                .collect(Collectors.toList());

        assertEquals(1, actualDeliveryCalls.size());
    }

    @ParameterizedTest
    @CsvSource({"-1,10,10", "0,10,0", "1,10,1", "10,10,10", "10,1,1", "10,0,0",
            "-1,10,10", "0,10,0", "1,10,1", "10,10,10", "10,1,1", "10,0,0"})
    public void cartShouldLimitPresetsWhenCountMoreThanLimit(int limit, int initialCount, int expectedCount) {
        checkouterProperties.setActualizedPresetsLimit(limit);
        List<PresetInfo> presetInfos = new ArrayList<>();
        List<AddressPreset> addressPresets = new ArrayList<>();
        for (int i = 0; i < initialCount; i++) {
            PresetInfo presetInfo = new PresetInfo();
            presetInfos.add(presetInfo);
            presetInfo.setPresetId(UUID.randomUUID().toString());
            presetInfo.setBuyerAddress(AddressProvider.getAddress());
            presetInfo.setType(DeliveryType.POST);
            presetInfo.setRegionId(5000000000L);
            presetInfo.setOutletId(5000000000L);

            AddressPreset addressPreset = new AddressPreset();
            addressPresets.add(addressPreset);
            addressPreset.setId(presetInfo.getPresetId());
            addressPreset.setType(DeliveryType.POST.name());
            PresetParcel presetParcel = new PresetParcel();
            presetParcel.setParcelIndex(0);
            presetParcel.setDeliveryAvailable(DeliveryAvailable.AVAILABLE);
            addressPreset.setParcels(Collections.singletonList(presetParcel));
        }
        Parameters parameters = new Parameters();
        parameters.setPresets(presetInfos);
        parameters.getReportParameters().getActualDelivery().setAddressPresets(addressPresets);
        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertEquals(expectedCount, multiCart.getPresets().size());
    }

}
