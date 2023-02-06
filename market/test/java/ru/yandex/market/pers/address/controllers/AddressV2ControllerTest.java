package ru.yandex.market.pers.address.controllers;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.address.config.TestClient;
import ru.yandex.market.pers.address.controllers.model.AddressDtoResponse;
import ru.yandex.market.pers.address.controllers.model.NewAddressDtoRequest;
import ru.yandex.market.pers.address.dao.ObjectKey;
import ru.yandex.market.pers.address.factories.AddressDtoFactory;
import ru.yandex.market.pers.address.factories.TestPlatform;
import ru.yandex.market.pers.address.model.identity.Identity;
import ru.yandex.market.pers.address.util.BaseWebTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class AddressV2ControllerTest extends BaseWebTest {

    @Autowired
    private TestClient testClient;

    @Autowired
    private RestTemplate geobaseRestTemplate;

    private final Identity<?> identity = Identity.Type.UUID.buildIdentity("123");
    private final TestPlatform platform = TestPlatform.BLUE;

    @Test
    public void shouldAddAddress() throws Exception {
        NewAddressDtoRequest addressDtoRequest = AddressDtoFactory.tolstogoStreet().build();
        String gpsParams = "lat=" + addressDtoRequest.getLocation().getLatitude() +
                "&lon=" + addressDtoRequest.getLocation().getLongitude();
        Integer expectedPreciseRegionId = 120544;
        given(geobaseRestTemplate.getForObject(ArgumentMatchers.contains(gpsParams), any()))
                .willReturn(expectedPreciseRegionId);
        AddressDtoResponse addressDtoResponse =
                testClient.addAddressV2(identity, addressDtoRequest, platform, false, false);
        assertAddress(addressDtoRequest, addressDtoResponse);
        Assertions.assertEquals(expectedPreciseRegionId, addressDtoResponse.getPreciseRegionId());
        Assertions.assertEquals(213, addressDtoResponse.getRegionId());
        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);

        Assertions.assertEquals(1, addresses.size());
        assertAddress(addressDtoRequest, addresses.get(0));
    }

    @Test
    public void shouldAddAddressWithoutSave() throws Exception {
        NewAddressDtoRequest addressDtoRequest = AddressDtoFactory.tolstogoStreet().build();
        String gpsParams = "lat=" + addressDtoRequest.getLocation().getLatitude() +
                "&lon=" + addressDtoRequest.getLocation().getLongitude();
        Integer expectedPreciseRegionId = 117895;
        given(geobaseRestTemplate.getForObject(ArgumentMatchers.contains(gpsParams), any()))
                .willReturn(expectedPreciseRegionId);
        AddressDtoResponse addressDtoResponse =
                testClient.addAddressV2(identity, addressDtoRequest, platform, true, false);
        assertAddress(addressDtoRequest, addressDtoResponse);
        Assertions.assertEquals(expectedPreciseRegionId, addressDtoResponse.getPreciseRegionId());

        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
        Assertions.assertEquals(0, addresses.size());
    }

    @Test
    public void shouldNotAddAddress() throws Exception {
        NewAddressDtoRequest addressDtoRequest = AddressDtoFactory.tolstogoStreet().build();
        String gpsParams = "lat=" + addressDtoRequest.getLocation().getLatitude() +
                "&lon=" + addressDtoRequest.getLocation().getLongitude();
        given(geobaseRestTemplate.getForObject(ArgumentMatchers.contains(gpsParams), any()))
                .willReturn(null);
        testClient.addAddressV2Expected400(identity, addressDtoRequest, platform, true, false);
    }

    @Test
    public void shouldUpdateAddress() throws Exception {
        NewAddressDtoRequest.Builder builder = AddressDtoFactory.tolstogoStreet();
        NewAddressDtoRequest addressDtoRequest = builder.build();
        String gpsParams = "lat=" + addressDtoRequest.getLocation().getLatitude() +
                "&lon=" + addressDtoRequest.getLocation().getLongitude();
        Integer expectedPreciseRegionId = 117895;
        given(geobaseRestTemplate.getForObject(ArgumentMatchers.contains(gpsParams), any()))
                .willReturn(expectedPreciseRegionId);
        AddressDtoResponse addressDtoResponse =
                testClient.addAddressV2(identity, addressDtoRequest, platform, false, false);
        assertAddress(addressDtoRequest, addressDtoResponse);
        Assertions.assertEquals(expectedPreciseRegionId, addressDtoResponse.getPreciseRegionId());

        builder.setDistrict("districtNew");
        AddressDtoResponse addressUpdated = testClient.updateAddressV2(
                identity, new ObjectKey(addressDtoResponse.getAddressId()), platform, false, builder.build());
        assertAddress(builder.build(), addressUpdated);
    }


    private void assertAddress(NewAddressDtoRequest addressDtoRequest, AddressDtoResponse addressDtoResponse) {
        Assertions.assertEquals(addressDtoRequest.getLocation(), addressDtoResponse.getLocation());
        Assertions.assertEquals(addressDtoRequest.getCountry(), addressDtoResponse.getCountry());
        Assertions.assertEquals(addressDtoRequest.getCity(), addressDtoResponse.getCity());
        Assertions.assertEquals(addressDtoRequest.getDistrict(), addressDtoResponse.getDistrict());
        Assertions.assertEquals(addressDtoRequest.getStreet(), addressDtoResponse.getStreet());
        Assertions.assertEquals(addressDtoRequest.getBuilding(), addressDtoResponse.getBuilding());
        Assertions.assertEquals(addressDtoRequest.getZip(), addressDtoResponse.getZip());
        Assertions.assertEquals(addressDtoRequest.getFloor(), addressDtoResponse.getFloor());
        Assertions.assertEquals(addressDtoRequest.getRoom(), addressDtoResponse.getRoom());
        Assertions.assertEquals(addressDtoRequest.getEntrance(), addressDtoResponse.getEntrance());
    }

}
