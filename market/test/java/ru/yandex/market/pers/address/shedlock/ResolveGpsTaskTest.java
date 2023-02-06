package ru.yandex.market.pers.address.shedlock;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.test.SourceScanner;
import ru.yandex.market.pers.address.config.GeoCoderMock;
import ru.yandex.market.pers.address.dao.SettingsDao;
import ru.yandex.market.pers.address.factories.AddressFactory;
import ru.yandex.market.pers.address.model.Address;
import ru.yandex.market.pers.address.model.GeocoderStatus;
import ru.yandex.market.pers.address.model.Location;
import ru.yandex.market.pers.address.model.ShedlockHistory;
import ru.yandex.market.pers.address.model.identity.Identity;
import ru.yandex.market.pers.address.services.PresetService;
import ru.yandex.market.pers.address.services.ShedlockHistoryService;
import ru.yandex.market.pers.address.util.BaseWebTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static ru.yandex.market.pers.address.factories.AddressDtoFactory.PROFSOYUZNAYA_STREET;
import static ru.yandex.market.pers.address.factories.AddressDtoFactory.TOLSTOGO_STREET;
import static ru.yandex.market.pers.address.factories.AddressDtoFactory.ZARECHNAYA_STREET;

public class ResolveGpsTaskTest extends BaseWebTest {

    public static final Identity<?> DEFAULT_UID = Identity.Type.UID.buildIdentity("1000");

    @Autowired
    private PresetService presetService;
    @Autowired
    private GeoCoderMock geoCoderMock;
    @Autowired
    private SettingsDao settingsDao;
    @Autowired
    private FillGpsExecutor fillGpsExecutor;
    @Autowired
    private ShedlockHistoryService shedlockHistoryService;
    @Autowired
    private RestTemplate geobaseRestTemplate;

    @Test
    public void methodsWithSchedulerLockMustReturnVoid() {
        List<Method> methodsWithSchedulerLockThatReturnNotVoid = SourceScanner.findSpringBeans("ru.yandex.market.pers" +
                ".address.shedlock")
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .filter(method -> method.getDeclaredAnnotation(SchedulerLock.class) != null)
                        .filter(method -> method.getReturnType().equals(Void.class))
                )
                .collect(Collectors.toList());

        assertThat(methodsWithSchedulerLockThatReturnNotVoid, is(empty()));
    }

    @Test
    public void methodsWithSchedulerLockMoreThanOne() {
        List<Method> methodsWithSchedulerLock = SourceScanner.findSpringBeans("ru.yandex.market.pers.address.shedlock")
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .filter(method -> method.getDeclaredAnnotation(SchedulerLock.class) != null)
                )
                .collect(Collectors.toList());

        assertTrue(methodsWithSchedulerLock.size() >= 1);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void shouldFillGpsWithStatusSuccess() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);
        fillGpsExecutor.processResolvingGps();


        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.GEOCODER_EXACT))
                        )
                ));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFillGpsWithStatusFromRequest() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");

        String latitude = "55.733969";
        String longitude = "37.587093";
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .setLocation(new Location(new BigDecimal(latitude), new BigDecimal(longitude)))
                        .build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.FAIL_OR_NOTHING);
        given(geoCoderMock.find(ArgumentMatchers.contains(latitude)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);
        fillGpsExecutor.processResolvingGps();


        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.FRONT_CONFIRMED))
                        )
                ));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFillGpsWithIncorrectRegion() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");

        String latitude = "55.733969";
        String longitude = "37.587093";
        presetService.addAddress(DEFAULT_UID,
                Address.builder()
                        .setCountry("Россия")
                        .setCity("Воронеж")
                        .setStreet(TOLSTOGO_STREET)
                        .setBuilding("6")
                        .setFloor("1")
                        .setRoom("67")
                        .setRegionId(111)
                        .setLocation(new Location(new BigDecimal(latitude), new BigDecimal(longitude)))
                        .build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.FAIL_OR_NOTHING);
        given(geoCoderMock.find(ArgumentMatchers.contains(latitude)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);
        fillGpsExecutor.processResolvingGps();


        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.GEOCODER_NOT_FOUND))
                        )
                ));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFillGpsWithStatusNotFound() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .build(), "source");

        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.FAIL_OR_NOTHING);
        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.GEOCODER_NOT_FOUND))
                        )
                ));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFillGpsWithStatusInit() {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .build(), "source");

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.INIT))
                        )
                ));
    }


    @Test
    public void shouldFillGpsIfEmptyAddress() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        fillGpsExecutor.processResolvingGps();
    }

    @Test
    public void shouldFillGpsIfEmptyAddressLimitOne() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "1");
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .build(), "source");
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .build(), "source");
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.FAIL_OR_NOTHING);
        fillGpsExecutor.processResolvingGps();
        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        List<Address> addressList = addresses
                .stream()
                .filter(a -> GeocoderStatus.GEOCODER_NOT_FOUND == a.getGeocoderStatus())
                .collect(Collectors.toList());
        assertEquals(1, addressList.size());
    }

    @Test
    public void shouldFillGpsWithManyAddress() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "25");
        settingsDao.put("thread_count_for_shedlock_fill_gps", "10");
        for (int i = 0; i < 100; i++) {
            presetService.addAddress(DEFAULT_UID,
                    Address.builder()
                            .setCountry("Россия")
                            .setCity("Воронеж")
                            .setStreet(TOLSTOGO_STREET)
                            .setBuilding("6" + i)
                            .setFloor("1" + i)
                            .setRoom("67" + i)
                            .setRegionId(111 + i)
                            .build(), "source");
        }
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.FAIL_OR_NOTHING);
        fillGpsExecutor.processResolvingGps();
        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        List<Address> addressList = addresses
                .stream()
                .filter(a -> GeocoderStatus.GEOCODER_NOT_FOUND == a.getGeocoderStatus())
                .collect(Collectors.toList());
        assertEquals(25, addressList.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFillGpsWithStatusNumber() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "25");
        settingsDao.put("thread_count_for_shedlock_fill_gps", "2");
        presetService.addAddress(DEFAULT_UID,
                Address.builder()
                        .setCountry("Россия")
                        .setCity("Воронеж")
                        .setStreet(PROFSOYUZNAYA_STREET)
                        .setBuilding("6")
                        .setFloor("1")
                        .setRoom("67")
                        .setRegionId(111)
                        .build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(PROFSOYUZNAYA_STREET)))
                .willReturn(GeoCoderMock.Response.PROFSOYUZNAYA_146);
        fillGpsExecutor.processResolvingGps();
        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.GEOCODER_NUMBER))
                        )
                ));
    }


    @SuppressWarnings("unchecked")
    @Test
    public void shouldFillGpsWithStatusLowPrecision() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "25");
        settingsDao.put("thread_count_for_shedlock_fill_gps", "2");
        presetService.addAddress(DEFAULT_UID, AddressFactory.tolstogoStreet().build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.BAD_PRECISION);
        fillGpsExecutor.processResolvingGps();
        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.GEOCODER_LOWER_NEAR))
                        )
                ));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateFromRequestStatus() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        String latitude = "55.733969";
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .setLocation(new Location(new BigDecimal(latitude), new BigDecimal("37.587093")))
                        .setGeocoderStatus(GeocoderStatus.FROM_REQUEST)
                        .build(), "source");

        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);
        given(geoCoderMock.find(ArgumentMatchers.contains(latitude)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);
        fillGpsExecutor.processResolvingGps();


        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.GEOCODER_EXACT))
                        )
                ));
    }

    @Test
    public void shouldRemoveLocation() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        String latitude = "55.733969";
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .setLocation(new Location(new BigDecimal(latitude), new BigDecimal("37.587093")))
                        .setGeocoderStatus(GeocoderStatus.FROM_REQUEST)
                        .build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.BAD_PRECISION);
        given(geoCoderMock.find(ArgumentMatchers.contains(latitude)))
                .willReturn(GeoCoderMock.Response.FAIL_OR_NOTHING);
        fillGpsExecutor.processResolvingGps();


        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertEquals(1, addresses.size());
        assertEquals(GeocoderStatus.GEOCODER_LOWER_NEAR, addresses.get(0).getGeocoderStatus());
        assertNull(addresses.get(0).getLocation());
    }


    @Test
    public void shouldSaveLocation() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        String latitude = "55.733969";
        Location location = new Location(new BigDecimal(latitude), new BigDecimal("37.587093"));
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .setLocation(location)
                        .setGeocoderStatus(GeocoderStatus.FROM_REQUEST)
                        .build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.BAD_PRECISION);
        given(geoCoderMock.find(ArgumentMatchers.contains(latitude)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);
        fillGpsExecutor.processResolvingGps();


        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertEquals(1, addresses.size());
        assertEquals(GeocoderStatus.FRONT_CONFIRMED, addresses.get(0).getGeocoderStatus());
        assertEquals(location, addresses.get(0).getLocation());
    }

    @Test
    public void shouldAddDistrictInGeocoderRq() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        String district = "district";
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .setDistrict(district)
                        .build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(district)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);
        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.GEOCODER_EXACT))
                        )
                ));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSaveShedlockHistory() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet().build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);
        fillGpsExecutor.processResolvingGps();

        List<ShedlockHistory> shedlockHistories = shedlockHistoryService.getShedlockHistories();

        assertThat(shedlockHistories, hasSize(1));
    }

    @Test
    public void shouldNotUpdatePlatform() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        String platform = "ios1";
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .setPlatform(platform)
                        .build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);
        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertEquals(1, addresses.size());
        assertEquals(platform, addresses.get(0).getPlatform());
    }

    @Test
    public void shouldGetFirstGeoObject() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        presetService.addAddress(DEFAULT_UID, AddressFactory.tolstogoStreet().build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.TWO_GEO_OBJECTS);
        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertEquals(1, addresses.size());
        assertEquals(GeocoderStatus.GEOCODER_LOWER_NEAR, addresses.get(0).getGeocoderStatus());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFillZipWithStatusSuccess() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        Address address = AddressFactory.tolstogoStreet().build();
        assertNull(address.getZip());
        presetService.addAddress(DEFAULT_UID, address, "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);
        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.GEOCODER_EXACT)),
                                hasProperty("zip", equalTo("119021")
                                )
                        )));
    }

    @Test
    public void shouldFillGpsWithStatusFront() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        String latitude = "1.1";
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet()
                        .setLocation(new Location(new BigDecimal(latitude), new BigDecimal("1.2")))
                        .build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.NEAR_PRECISION);
        given(geoCoderMock.find(ArgumentMatchers.contains(latitude)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);
        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertEquals(1, addresses.size());
        Address result = addresses.get(0);
        assertEquals(GeocoderStatus.FRONT_CONFIRMED, result.getGeocoderStatus());
        assertNotNull(result.getLocation());
        assertNotNull(result.getLocation().getLatitude());
        assertNotNull(result.getLocation().getLongitude());
    }

    @Test
    public void shouldFillGpsWithStatusNear() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        presetService.addAddress(DEFAULT_UID,
                AddressFactory.tolstogoStreet().build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.NEAR_PRECISION);
        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertEquals(1, addresses.size());
        Address result = addresses.get(0);
        assertEquals(GeocoderStatus.GEOCODER_NEAR, result.getGeocoderStatus());
        assertNotNull(result.getLocation());
        assertNotNull(result.getLocation().getLatitude());
        assertNotNull(result.getLocation().getLongitude());
    }


    @Test
    public void shouldFillPreciseRegionId() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        Address address = AddressFactory.tolstogoStreet().build();
        String gpsParams = "lat=55.73361&lon=37.642556";
        int expectedPreciseRegionId = 117067;
        given(geobaseRestTemplate.getForObject(ArgumentMatchers.contains(gpsParams), any()))
                .willReturn(expectedPreciseRegionId);
        presetService.addAddress(DEFAULT_UID, address, "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);

        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.GEOCODER_EXACT)),
                                hasProperty("preciseRegionId", equalTo(expectedPreciseRegionId)
                                )
                        )));
    }

    @Test
    public void shouldFillPreciseRegionIdAsNullWhenGeobaseDontReturnRegionId() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "100");
        Address address = AddressFactory.tolstogoStreet().build();
        given(geobaseRestTemplate.getForObject(any(), any())).willReturn(-1);
        presetService.addAddress(DEFAULT_UID, address, "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(TOLSTOGO_STREET)))
                .willReturn(GeoCoderMock.Response.TOLSTOGO_16);

        fillGpsExecutor.processResolvingGps();

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.GEOCODER_EXACT)),
                                hasProperty("preciseRegionId", nullValue())
                        )
                ));
    }

    @Test
    public void shouldNotFillPreciseRegionIdWhenLowPrecision() throws InterruptedException {
        settingsDao.put("address_limit_for_shedlock_fill_gps", "25");
        settingsDao.put("thread_count_for_shedlock_fill_gps", "2");
        presetService.addAddress(DEFAULT_UID,
                Address.builder()
                        .setCountry("Россия")
                        .setCity("Воронеж")
                        .setStreet(ZARECHNAYA_STREET)
                        .setBuilding("6")
                        .setFloor("1")
                        .setRoom("67")
                        .setRegionId(111)
                        .build(), "source");
        given(geoCoderMock.find(ArgumentMatchers.contains(ZARECHNAYA_STREET)))
                .willReturn(GeoCoderMock.Response.BAD_PRECISION);
        fillGpsExecutor.processResolvingGps();
        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertThat(addresses,
                contains(
                        allOf(
                                hasProperty("geocoderStatus", equalTo(GeocoderStatus.GEOCODER_LOWER_NEAR)),
                                hasProperty("preciseRegionId", nullValue())
                        )
                ));
    }
}
