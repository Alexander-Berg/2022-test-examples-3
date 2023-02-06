package ru.yandex.market.pers.address.shedlock;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.address.config.GeoCoderMock;
import ru.yandex.market.pers.address.dao.SettingsDao;
import ru.yandex.market.pers.address.factories.AddressFactory;
import ru.yandex.market.pers.address.model.Address;
import ru.yandex.market.pers.address.model.GeocoderStatus;
import ru.yandex.market.pers.address.model.Location;
import ru.yandex.market.pers.address.model.identity.Identity;
import ru.yandex.market.pers.address.services.PresetService;
import ru.yandex.market.pers.address.util.BaseWebTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static ru.yandex.market.pers.address.factories.AddressDtoFactory.TOLSTOGO_STREET;

public class UpdateToNewStatusTest extends BaseWebTest {

    public static final Identity<?> DEFAULT_UID = Identity.Type.UID.buildIdentity("1000");

    @Autowired
    private PresetService presetService;
    @Autowired
    private GeoCoderMock geoCoderMock;
    @Autowired
    private SettingsDao settingsDao;
    @Autowired
    private FillGpsExecutor fillGpsExecutor;

    @BeforeEach
    public void init() {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
    }

    @Test
    public void shouldUpdateFromOldStatusToExact() throws InterruptedException {
        generateAddressWithOldStatus();
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        addresses.forEach(address -> assertEquals(GeocoderStatus.GEOCODER_EXACT, address.getGeocoderStatus()));
    }


    @Test
    public void shouldUpdateFromOldStatusToNotFound() throws InterruptedException {
        generateAddressWithOldStatus();
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.FAIL_OR_NOTHING);
        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        addresses.forEach(address -> assertEquals(GeocoderStatus.GEOCODER_NOT_FOUND, address.getGeocoderStatus()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateFromOldStatusToLowerNear() throws InterruptedException {
        generateAddressWithOldStatus();
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.BAD_PRECISION);
        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        addresses.forEach(address -> assertEquals(GeocoderStatus.GEOCODER_LOWER_NEAR, address.getGeocoderStatus()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateFromOldStatusToNumber() throws InterruptedException {
        generateAddressWithOldStatus();
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.PROFSOYUZNAYA_146);
        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        addresses.forEach(address -> assertEquals(GeocoderStatus.GEOCODER_NUMBER, address.getGeocoderStatus()));
    }

    private void generateAddressWithOldStatus() {
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .setLocation(new Location(new BigDecimal("1.1"), new BigDecimal("1.2")))
                        .setGeocoderStatus(GeocoderStatus.SUCCESS_CONFIRMED)
                        .build(), "source");
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .setLocation(new Location(new BigDecimal("1.1"), new BigDecimal("1.2")))
                        .setGeocoderStatus(GeocoderStatus.GEOCODER_NUMBER_PRECISION)
                        .build(), "source");
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .setGeocoderStatus(GeocoderStatus.GEOCODER_LOW_PRECISION)
                        .build(), "source");
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .setGeocoderStatus(GeocoderStatus.NOT_FOUND)
                        .build(), "source");
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .setGeocoderStatus(GeocoderStatus.FROM_REQUEST_CONFIRMED)
                        .build(), "source");
    }


}
