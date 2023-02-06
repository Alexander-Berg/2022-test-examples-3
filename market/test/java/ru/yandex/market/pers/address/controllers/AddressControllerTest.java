package ru.yandex.market.pers.address.controllers;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.pers.address.config.Blackbox;
import ru.yandex.market.pers.address.config.GeoCoderMock;
import ru.yandex.market.pers.address.config.PassportDataSync;
import ru.yandex.market.pers.address.config.PersAddress;
import ru.yandex.market.pers.address.config.TestClient;
import ru.yandex.market.pers.address.controllers.model.AddressDtoResponse;
import ru.yandex.market.pers.address.controllers.model.AddressFullnessState;
import ru.yandex.market.pers.address.controllers.model.AddressType;
import ru.yandex.market.pers.address.controllers.model.ContactDto;
import ru.yandex.market.pers.address.controllers.model.ContactDtoResponse;
import ru.yandex.market.pers.address.controllers.model.FavouritePickpointRequest;
import ru.yandex.market.pers.address.controllers.model.LocationDto;
import ru.yandex.market.pers.address.controllers.model.NewAddressDtoRequest;
import ru.yandex.market.pers.address.controllers.model.NewPresetDtoRequest;
import ru.yandex.market.pers.address.controllers.model.PresetsResponse;
import ru.yandex.market.pers.address.controllers.model.SuggestResponse;
import ru.yandex.market.pers.address.dao.FavouritePickpointDao;
import ru.yandex.market.pers.address.dao.ObjectKey;
import ru.yandex.market.pers.address.factories.AddressDtoFactory;
import ru.yandex.market.pers.address.factories.AddressFactory;
import ru.yandex.market.pers.address.factories.BlackboxUserFactory;
import ru.yandex.market.pers.address.factories.ContactDtoFactory;
import ru.yandex.market.pers.address.factories.ContactFactory;
import ru.yandex.market.pers.address.factories.PassportDatasyncAddressFactory;
import ru.yandex.market.pers.address.factories.PresetDtoFactory;
import ru.yandex.market.pers.address.factories.PresetFactory;
import ru.yandex.market.pers.address.factories.TestPlatform;
import ru.yandex.market.pers.address.model.Address;
import ru.yandex.market.pers.address.model.Contact;
import ru.yandex.market.pers.address.model.FavouritePickpoint;
import ru.yandex.market.pers.address.model.Location;
import ru.yandex.market.pers.address.model.Preset;
import ru.yandex.market.pers.address.model.identity.Identity;
import ru.yandex.market.pers.address.model.identity.Uid;
import ru.yandex.market.pers.address.services.GeocoderService;
import ru.yandex.market.pers.address.services.MarketDataSyncClient;
import ru.yandex.market.pers.address.services.PresetService;
import ru.yandex.market.pers.address.services.SuggestService;
import ru.yandex.market.pers.address.services.blackbox.UserInfoResponse;
import ru.yandex.market.pers.address.services.model.MarketDataSyncAddress;
import ru.yandex.market.pers.address.services.model.PassportDataSyncAddress;
import ru.yandex.market.pers.address.services.model.PassportDataSyncAddressList;
import ru.yandex.market.pers.address.util.BaseWebTest;
import ru.yandex.market.sdk.userinfo.service.UserInfoService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;
import static ru.yandex.market.pers.address.config.TestClient.toAddresses;
import static ru.yandex.market.pers.address.config.TestClient.toContacts;
import static ru.yandex.market.pers.address.config.TestClient.toPresets;
import static ru.yandex.market.pers.address.factories.AddressDtoFactory.GRUZINSKAIA_STREET;
import static ru.yandex.market.pers.address.factories.AddressDtoFactory.TOLSTOGO_STREET;
import static ru.yandex.market.pers.address.factories.ContactFactory.DEFAULT_EMAIL;
import static ru.yandex.market.pers.address.factories.ContactFactory.DEFAULT_FIRST_NAME;
import static ru.yandex.market.pers.address.factories.ContactFactory.DEFAULT_LAST_NAME;
import static ru.yandex.market.pers.address.factories.ContactFactory.DEFAULT_PHONE_NUM;
import static ru.yandex.market.pers.address.factories.ContactFactory.DEFAULT_SECOND_NAME;
import static ru.yandex.market.pers.address.factories.TestPlatform.BLUE;
import static ru.yandex.market.pers.address.factories.TestPlatform.RED;
import static ru.yandex.market.pers.address.services.model.MarketDataSyncAddress.builderFrom;
import static ru.yandex.market.pers.address.util.PresetMatcher.sameAddressDto;
import static ru.yandex.market.pers.address.util.PresetMatcher.sameContactDto;
import static ru.yandex.market.pers.address.util.PresetMatcher.samePresetDto;
import static ru.yandex.market.pers.address.util.SamePropertyValuesAsExcept.samePropertyValuesAsExcept;

class AddressControllerTest extends BaseWebTest {

    private static final Uid UID = new Uid(1000537205013L);
    private static final Uid SBER_ID = new Uid((1L << 61) - 1L);
    private static final Identity<?> YANDEX_UID = Identity.Type.YANDEX_UID.buildIdentity("1000537205013");
    private static final Identity<?> UUID = Identity.Type.UUID.buildIdentity("1000537205013");
    private static final String TOLSTOGO_16_ZIP = "119021";
    private static final String GRUZINSKAYA_12_ZIP = "123242";
    private static final String TOLSTOGO_STREET_IN_GEOCODER = "улица Льва Толстого";
    private static final Long REGION_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @PersAddress
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PresetService presetService;

    @Autowired
    private FavouritePickpointDao pickpointDao;

    @Blackbox
    @Autowired
    private RestTemplate blackboxRestTemplate;

    @PassportDataSync
    @Autowired
    private RestTemplate passportDataSyncRestTemplate;

    @Autowired
    private GeoCoderMock geoCoderMock;

    @Blackbox
    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private CheckouterClient checkouterClientMock;

    @Autowired
    private MarketDataSyncClient marketDataSyncClient;

    @Autowired
    private TestClient testClient;

    @Autowired
    private RestTemplate geobaseRestTemplate;


    @BeforeEach
    void setUp() {
        mockUid();

        PagedOrders orders = new PagedOrders(Collections.emptyList(), new Pager());
        given(checkouterClientMock.getOrders(any(), any())).willReturn(orders);
    }

    @Nested
    class CRUD {
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldSavePreset(@ConvertWith(ToPlatform.class) TestPlatform platform,
                              @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            NewPresetDtoRequest toSave = PresetDtoFactory.tolstogoStreet();

            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            ObjectKey generatedId = testClient.addPreset(identity, toSave, platform);

            List<Preset> presets = toPresets(testClient.getPresets(identity, platform));
            assertThat(presets, contains(samePresetDto(toSave, generatedId)));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldSaveAddress(@ConvertWith(ToPlatform.class) TestPlatform platform,
                               @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            NewAddressDtoRequest toSave = AddressDtoFactory.tolstogoStreet().build();

            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            ObjectKey generatedId = testClient.addAddress(identity, toSave, platform);

            List<Address> addresses = toAddresses(testClient.getAddresses(identity, platform));
            assertThat(addresses, contains(sameAddressDto(toSave, generatedId)));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldSaveContactBlue(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                   @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            ContactDto toSave = ContactDtoFactory.sample();

            ObjectKey generatedId = testClient.addContact(identity, toSave, platform);

            List<Contact> contacts = toContacts(testClient.getContacts(identity, platform));
            assertThat(contacts, contains(sameContactDto(toSave, generatedId)));
        }

        // TODO MARKETDISCOUNT-1174 проверить данные в DS
        @Disabled
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldNotSavePresetForAddressWhichIsNotHaveSameFieldsButSameGeocoderResponse(@ConvertWith(ToPlatform.class) TestPlatform platform, @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            NewPresetDtoRequest toSave = PresetDtoFactory.slightlyChangedTolstogoStreet();

            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            ObjectKey generatedId1 = testClient.addPreset(identity, toSave, platform);

            toSave = PresetDtoFactory.tolstogoStreet();
            ObjectKey generatedId2 = testClient.addPreset(identity, toSave, platform);

            List<Preset> presets = toPresets(testClient.getPresets(identity, platform));
            assertEquals(generatedId1, generatedId2);
            assertThat(presets, contains(samePresetDto(PresetDtoFactory.slightlyChangedTolstogoStreet(),
                    generatedId2)));
        }

        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldSavePresetForAddressWhichIsNotHaveSameFieldsButSameGeocoderResponse(@ConvertWith(ToPlatform.class) TestPlatform platform, @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            NewPresetDtoRequest toSave1 = PresetDtoFactory.tolstogoStreet();

            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            ObjectKey generatedId1 = testClient.addPreset(identity, toSave1, platform);

            NewPresetDtoRequest toSave2 = PresetDtoFactory.tolstogoStreetAnotherRoom();
            ObjectKey generatedId2 = testClient.addPreset(identity, toSave2, platform);

            List<Preset> presets = presetService.getPresets(identity);
            assertThat(presets, containsInAnyOrder(
                    samePresetDto(toSave1, generatedId1),
                    samePresetDto(toSave2, generatedId2)
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldSaveContact(@ConvertWith(ToPlatform.class) TestPlatform platform,
                               @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            ContactDto toSave = ContactDtoFactory.sample();

            testClient.addContact(identity, toSave, platform);

            Collection<Contact> contacts = presetService.getContacts(identity);
            assertThat(contacts.stream()
                            .map(AddressControllerTest::toContactDto)
                            .collect(Collectors.toList()),
                    contains(samePropertyValuesAsExcept(toSave))
            );
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldNotSaveContactDuplicate(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                           @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);

            ObjectKey presetId = testClient.addPreset(identity,
                    TestPlatform.newPresetDtoRequest(ContactDtoFactory.sample(),
                            AddressDtoFactory.tolstogoStreet().build()),
                    platform
            );

            assertThat(presetService.getContacts(identity), hasSize(1)/* one from address*/);

            testClient.addContact(identity, ContactDtoFactory.sample(), platform);
            assertThat(presetService.getContacts(identity), hasSize(2)/* one because duplicates was filtered*/);

            testClient.deletePreset(identity, presetId);
            assertThat(presetService.getContacts(identity), hasSize(1));
            assertThat(presetService.getPresets(identity), empty());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldSaveContactIfHasDuplicateInAddress(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                      @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            ContactDto toSave = ContactDtoFactory.sample();

            testClient.addContact(identity, toSave, platform);
            assertThat(presetService.getContacts(identity), hasSize(1));

            testClient.addContact(identity, toSave, platform);
            assertThat(presetService.getContacts(identity), hasSize(2));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldNotReturnPresetWithoutAddress(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                 @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            ContactDto toSave = ContactDtoFactory.sample();

            testClient.addContact(identity, toSave, platform);

            List<Preset> presets = presetService.getPresets(identity);
            assertThat(presets, empty());
        }


        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldUpdatePreset(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            ObjectKey generatedId = testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);

            String updatedPhoneNum = "+79169785465";
            NewPresetDtoRequest toUpdate = TestPlatform.newPresetDtoRequest(
                    ContactDtoFactory.sample((b) -> b.setPhoneNum(updatedPhoneNum)),
                    AddressDtoFactory
                            .tolstogoStreet()
                            .setBuilding("99")
                            .build()
            );
            testClient.updatePreset(identity, generatedId, platform, toUpdate);

            List<Preset> presets = presetService.getPresets(identity);
            final Preset updatedPreset =
                    presets.stream().filter(p -> Objects.equals(p.getId(), generatedId)).findFirst().orElseThrow(IllegalArgumentException::new);
            assertEquals(updatedPhoneNum, updatedPreset.getContact().getPhoneNum());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldUpdateAddress(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                 @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            ObjectKey generatedId = testClient.addAddress(identity, AddressDtoFactory.tolstogoStreet().build(),
                    platform);

            String building = "99";
            NewAddressDtoRequest toUpdate = AddressDtoFactory
                    .tolstogoStreet()
                    .setBuilding(building)
                    .build();
            testClient.updateAddress(identity, generatedId, platform, toUpdate);

            List<Address> addresses = presetService.getAddresses(identity);
            final Address updatedAddress =
                    addresses.stream().filter(p -> Objects.equals(p.getId(), generatedId)).findFirst().orElseThrow(IllegalArgumentException::new);
            assertEquals(building, updatedAddress.getBuilding());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldUpdateContact(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                 @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            ObjectKey generatedId = testClient.addContact(identity, ContactDtoFactory.sample(), platform);

            String updatedPhoneNum = "+79169785465";
            ContactDto toUpdate = ContactDtoFactory.sample(b -> b.setPhoneNum(updatedPhoneNum));
            testClient.updateContact(identity, generatedId, platform, toUpdate);

            List<Contact> contacts = presetService.getContacts(identity);
            final Contact updatedContact =
                    contacts.stream().filter(p -> Objects.equals(p.getId(), generatedId)).findFirst().orElseThrow(IllegalArgumentException::new);
            assertEquals(updatedPhoneNum, updatedContact.getPhoneNum());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldFixPersaddress62(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                    @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            ObjectKey generatedId = testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);

            String updatedPhoneNum = "+79169785465";
            NewPresetDtoRequest toUpdate = TestPlatform.newPresetDtoRequest(
                    ContactDtoFactory.sample((b) -> b.setPhoneNum(updatedPhoneNum)),
                    AddressDtoFactory
                            .tolstogoStreet()
                            .setCountry(null)
                            .setBuilding("99")
                            .build()
            );

            testClient.updatePreset(identity, generatedId, platform, toUpdate);

            List<Preset> presets = presetService.getPresets(identity);
            final Preset updatedPreset =
                    presets.stream().filter(p -> Objects.equals(p.getId(), generatedId)).findFirst().orElseThrow(IllegalArgumentException::new);
            assertEquals("Россия", updatedPreset.getAddress().getCountry());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldDeletePreset(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            given(geoCoderMock.find(contains(GRUZINSKAIA_STREET))).willReturn(GeoCoderMock.Response.GRUZINSKAYA_12);
            ObjectKey firstId = testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);
            ObjectKey secondId = testClient.addPreset(identity, PresetDtoFactory.gruzinskayaStreet(), platform);

            testClient.deletePreset(identity, firstId);

            List<Preset> remainedPresets = presetService.getPresets(identity);
            assertThat(remainedPresets, contains(hasProperty("id", equalTo(secondId))));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldDeleteContact(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                 @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            ObjectKey firstId = testClient.addContact(identity, ContactDtoFactory.sample(), platform);
            ObjectKey secondId = testClient.addContact(identity, ContactDtoFactory.anotherSample(), platform);

            testClient.deleteContact(identity, firstId);

            List<Contact> remainedContacts = presetService.getContacts(identity);
            assertThat(remainedContacts, contains(hasProperty("id", equalTo(secondId))));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldDeletContactByKey(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                     @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            marketDataSyncClient.saveNewAddress(identity, MarketDataSyncAddress.builder()
                    .setId(java.util.UUID.randomUUID().toString())
                    .setCountry("Россия")
                    .setCity("Воронеж")
                    .setStreet("Академика Сахарова")
                    .setBuilding("11к6")
                    .setFloor("5")
                    .setFlat("22")
                    .setRegionId("111")
                    .setRecipient("Дмитрий Владимирович Селезнев")
                    .setPhone("+79507778899")
                    .setEmail("seleznev@mail.nowhere")
                    .build());

            List<ContactDtoResponse> contacts = testClient.getContacts(identity, platform);
            MatcherAssert.assertThat(contacts, hasSize(1));

            ObjectKey firstKey = new ObjectKey(contacts.get(0).getContactId());

            testClient.deleteContact(identity, firstKey);

            List<Contact> remainedContacts = presetService.getContacts(identity);
            assertThat(remainedContacts, empty());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldDeleteAddress(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                 @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            given(geoCoderMock.find(contains(GRUZINSKAIA_STREET))).willReturn(GeoCoderMock.Response.GRUZINSKAYA_12);
            ObjectKey firstId = testClient.addAddress(identity, AddressDtoFactory.gruzinskayaStreet().build(),
                    platform);
            ObjectKey secondId = testClient.addAddress(identity, AddressDtoFactory.tolstogoStreet().build(), platform);

            testClient.deleteAddress(identity, firstId);

            List<Address> remainedContacts = presetService.getAddresses(identity);
            assertThat(remainedContacts, contains(hasProperty("id", equalTo(secondId))));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldDeleteAddressByKey(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                      @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            marketDataSyncClient.saveNewAddress(identity, MarketDataSyncAddress.builder()
                    .setId(java.util.UUID.randomUUID().toString())
                    .setCountry("Россия")
                    .setCity("Воронеж")
                    .setStreet("Академика Сахарова")
                    .setBuilding("11к6")
                    .setFloor("5")
                    .setFlat("22")
                    .setRegionId("111")
                    .setRecipient("Дмитрий Владимирович Селезнев")
                    .setPhone("+79507778899")
                    .setEmail("seleznev@mail.nowhere")
                    .build());

            List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
            MatcherAssert.assertThat(addresses, hasSize(1));

            ObjectKey firstKey = new ObjectKey(addresses.get(0).getAddressId());

            testClient.deleteAddress(identity, firstKey);

            List<Address> remainedContacts = presetService.getAddresses(identity);
            assertThat(remainedContacts, empty());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldFilterOutEmptyContact(
                @ConvertWith(ToPlatform.class) TestPlatform platform,
                @ConvertWith(ToIdentity.class) Identity<?> identity
        ) throws Exception {
            marketDataSyncClient.saveNewAddress(identity, MarketDataSyncAddress.builder()
                    .setId(java.util.UUID.randomUUID().toString())
                    .setCountry("Россия")
                    .setCity("Воронеж")
                    .setStreet("Академика Сахарова")
                    .setBuilding("11к6")
                    .setFloor("5")
                    .setFlat("22")
                    .setRegionId("111")
                    .build());

            List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
            MatcherAssert.assertThat(addresses, hasSize(1));

            List<ContactDtoResponse> contacts = testClient.getContacts(identity, platform);
            MatcherAssert.assertThat(contacts, empty());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldHideEditedDatasyncAddress(
                @ConvertWith(ToPlatform.class) TestPlatform platform,
                @ConvertWith(ToIdentity.class) Identity<?> identity
        ) throws Exception {
            marketDataSyncClient.saveNewAddress(identity, MarketDataSyncAddress.builder()
                    .setId(java.util.UUID.randomUUID().toString())
                    .setCountry("Россия")
                    .setCity("Воронеж")
                    .setStreet("Академика Сахарова")
                    .setBuilding("11к6")
                    .setFloor("5")
                    .setFlat("22")
                    .setRegionId("111")
                    .setRecipient("Дмитрий Владимирович Селезнев")
                    .setPhone("+79507778899")
                    .setEmail("seleznev@mail.nowhere")
                    .build());

            List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
            MatcherAssert.assertThat(addresses, hasSize(1));

            ObjectKey firstKey = new ObjectKey(addresses.get(0).getAddressId());

            NewAddressDtoRequest newAddressDtoRequest = AddressDtoFactory.gruzinskayaStreet().build();

            testClient.updateAddress(identity, firstKey, platform, newAddressDtoRequest);

            List<Address> remainedContacts = presetService.getAddresses(identity);
            assertThat(remainedContacts, hasSize(1));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldHideEditedDatasyncContact(
                @ConvertWith(ToPlatform.class) TestPlatform platform,
                @ConvertWith(ToIdentity.class) Identity<?> identity
        ) throws Exception {
            marketDataSyncClient.saveNewAddress(identity, MarketDataSyncAddress.builder()
                    .setId(java.util.UUID.randomUUID().toString())
                    .setCountry("Россия")
                    .setCity("Воронеж")
                    .setStreet("Академика Сахарова")
                    .setBuilding("11к6")
                    .setFloor("5")
                    .setFlat("22")
                    .setRegionId("111")
                    .setRecipient("Дмитрий Владимирович Селезнев")
                    .setPhone("+79507778899")
                    .setEmail("seleznev@mail.nowhere")
                    .build());

            List<ContactDtoResponse> contacts = testClient.getContacts(identity, platform);
            MatcherAssert.assertThat(contacts, hasSize(1));

            ObjectKey firstKey = new ObjectKey(contacts.get(0).getContactId());

            ContactDto contactDto = ContactDtoFactory.anotherSample();

            testClient.updateContact(identity, firstKey, platform, contactDto);

            List<Contact> remainedContacts = presetService.getContacts(identity);
            assertThat(remainedContacts, hasSize(1));
        }


        @ParameterizedTest
        @CsvSource(value = {"BLUE,YANDEX_UID", "BLUE,UUID"})
        public void shouldNotFailOnDuplicates(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                              @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            ObjectKey objectKey = new ObjectKey(java.util.UUID.randomUUID().toString());

            Address address = AddressDtoFactory.tolstogoStreet()
                    .build()
                    .toAddressBuilder()
                    .setId(objectKey)
                    .build();

            presetService.addAddress(identity, address, "");
            presetService.addAddress(identity, address, "");

            List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
            MatcherAssert.assertThat(addresses, hasSize(1));
        }
    }

    @Nested
    class SpecificFieldsInAddress {

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldSaveZipFieldInPreset(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                        @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);
            testClient.addPreset(identity, PresetDtoFactory.gruzinskayaStreet(), platform);

            assertThat(getPreset(identity, platform).getAddresses(), containsInAnyOrder(
                    hasProperty("zip", equalTo(PresetDtoFactory.tolstogoStreet().getAddress().getZip())),
                    hasProperty("zip", equalTo(PresetDtoFactory.gruzinskayaStreet().getAddress().getZip()))
            ));
        }


        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldSaveFloorFieldInPreset(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                          @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);
            testClient.addPreset(identity, PresetDtoFactory.gruzinskayaStreet(), platform);

            assertThat(getPreset(identity, platform).getAddresses(), containsInAnyOrder(
                    hasProperty("floor", equalTo(PresetDtoFactory.tolstogoStreet().getAddress().getFloor())),
                    hasProperty("floor", equalTo(PresetDtoFactory.gruzinskayaStreet().getAddress().getFloor()))
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldSaveEntranceFieldInPreset(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                             @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);
            testClient.addPreset(identity, PresetDtoFactory.gruzinskayaStreet(), platform);

            assertThat(getPreset(identity, platform).getAddresses(), containsInAnyOrder(
                    hasProperty("entrance", equalTo(PresetDtoFactory.tolstogoStreet().getAddress().getEntrance())),
                    hasProperty("entrance", equalTo(PresetDtoFactory.gruzinskayaStreet().getAddress().getEntrance()))
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldSaveIntercomFieldInPreset(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                             @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);
            testClient.addPreset(identity, PresetDtoFactory.gruzinskayaStreet(), platform);

            assertThat(getPreset(identity, platform).getAddresses(), containsInAnyOrder(
                    hasProperty("intercom", equalTo(PresetDtoFactory.tolstogoStreet().getAddress().getIntercom())),
                    hasProperty("intercom", equalTo(PresetDtoFactory.gruzinskayaStreet().getAddress().getIntercom()))
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldAllowToAddAddressWithIncorrectRegionId(
                @ConvertWith(ToPlatform.class) TestPlatform platform,
                @ConvertWith(ToIdentity.class) Identity<?> identity
        ) throws Exception {
            NewAddressDtoRequest build = AddressDtoFactory.tolstogoStreet()
                    .setCity("Воронеж")
                    .build();

            testClient.addAddress(identity, build, platform);
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldSaveComment(
                @ConvertWith(ToPlatform.class) TestPlatform platform,
                @ConvertWith(ToIdentity.class) Identity<?> identity
        ) throws Exception {
            NewAddressDtoRequest request = AddressDtoFactory.tolstogoStreet().build();

            testClient.addAddress(identity, request, platform);

            List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
            MatcherAssert.assertThat(addresses, hasSize(1));
            MatcherAssert.assertThat(addresses.get(0).getComment(), is(request.getComment()));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldSaveLocation(
                @ConvertWith(ToPlatform.class) TestPlatform platform,
                @ConvertWith(ToIdentity.class) Identity<?> identity
        ) throws Exception {
            NewAddressDtoRequest request = AddressDtoFactory.tolstogoStreet().build();

            testClient.addAddress(identity, request, platform);

            List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
            MatcherAssert.assertThat(addresses, hasSize(1));

            AddressDtoResponse address = addresses.get(0);
            MatcherAssert.assertThat(address.getLocation(), is(request.getLocation()));
        }


        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID"})
        void shouldUpdateLocation(
                @ConvertWith(ToPlatform.class) TestPlatform platform,
                @ConvertWith(ToIdentity.class) Identity<?> identity
        ) throws Exception {
            NewAddressDtoRequest request = AddressDtoFactory.tolstogoStreet().build();

            testClient.addAddress(identity, request, platform);

            List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
            MatcherAssert.assertThat(addresses, hasSize(1));

            AddressDtoResponse address = addresses.get(0);

            NewAddressDtoRequest updateRequest = AddressDtoFactory.tolstogoStreet()
                    .setLocation(new LocationDto(new BigDecimal("12.34"), new BigDecimal("56.78")))
                    .build();

            testClient.updateAddress(identity, new ObjectKey(address.getAddressId()), platform, updateRequest);

            addresses = testClient.getAddresses(identity, platform);
            MatcherAssert.assertThat(addresses, hasSize(1));

            address = addresses.get(0);
            MatcherAssert.assertThat(address.getLocation(), is(updateRequest.getLocation()));
        }


    }

    @Nested
    class ZipFieldInAddressFromGeocoder {
        @SuppressWarnings("unchecked")
        // TODO MARKETDISCOUNT-1174 проверить данные в DS
        @Disabled
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldAddZipFromGeocoder(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                      @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            given(geoCoderMock.find(contains(GRUZINSKAIA_STREET))).willReturn(GeoCoderMock.Response.GRUZINSKAYA_12);

            testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);
            testClient.addPreset(identity, PresetDtoFactory.gruzinskayaStreet(), platform);
            assertThat(getPreset(identity, platform).getAddresses(), containsInAnyOrder(
                    hasProperty("zip", equalTo(TOLSTOGO_16_ZIP)),
                    hasProperty("zip", equalTo(GRUZINSKAYA_12_ZIP))
            ));
        }

        @SuppressWarnings("unchecked")
        // TODO MARKETDISCOUNT-1174 проверить данные в DS
        @Disabled
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldHandleGeocoderFailOneRequest(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            given(geoCoderMock.find(contains(GRUZINSKAIA_STREET))).willReturn(GeoCoderMock.Response.FAIL_OR_NOTHING);

            testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);
            testClient.addPreset(identity, PresetDtoFactory.gruzinskayaStreet(), platform);

            assertThat(getPreset(identity, platform).getAddresses(), containsInAnyOrder(
                    hasProperty("zip", equalTo(TOLSTOGO_16_ZIP)),
                    hasProperty("zip", nullValue())
            ));
        }

        @SuppressWarnings("unchecked")
        // TODO MARKETDISCOUNT-1174 проверить данные в DS
        @Disabled
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldHandleGeocoderTimeoutOneRequest(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                   @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            given(geoCoderMock.find(contains(GRUZINSKAIA_STREET))).willAnswer((invocation) -> {
                Thread.sleep(GeocoderService.GEOCODER_TIMEOUT_MILLIS);
                return GeoCoderMock.Response.FAIL_OR_NOTHING;
            });

            testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);
            testClient.addPreset(identity, PresetDtoFactory.gruzinskayaStreet(), platform);

            assertThat(getPreset(identity, platform).getAddresses(), containsInAnyOrder(
                    hasProperty("zip", equalTo(TOLSTOGO_16_ZIP)),
                    hasProperty("zip", nullValue())
            ));
        }
    }

    @Nested
    class FetchPresets {
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldFetchEmptyResponse(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                      @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            PresetsResponse presetsResponse = getPreset(identity, platform);
            assertThat(presetsResponse.getAddresses(), empty());
            assertThat(presetsResponse.getContacts(), empty());
            assertThat(presetsResponse.getPresets(), empty());
        }

        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldFetchPresets(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            given(geoCoderMock.find(contains(GRUZINSKAIA_STREET))).willReturn(GeoCoderMock.Response.GRUZINSKAYA_12);

            NewPresetDtoRequest first = PresetDtoFactory.tolstogoStreet();
            ObjectKey firstId = testClient.addPreset(identity, first, platform);
            NewPresetDtoRequest second = PresetDtoFactory.gruzinskayaStreet();
            ObjectKey secondId = testClient.addPreset(identity, second, platform);

            PresetsResponse presetsResponse = getPreset(identity, platform);

            assertThat(presetsResponse.getAddresses(), hasSize(2));
            assertThat(presetsResponse.getContacts(), hasSize(2));

            List<Preset> presets = TestPlatform.toPresetModels(presetsResponse);

            assertThat(presets, containsInAnyOrder(
                    samePresetDto(first, firstId),
                    samePresetDto(second, secondId)
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldNormalizeSameAddressIfGeoCoderFails(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                       @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.FAIL_OR_NOTHING);

            testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);
            testClient.addPreset(identity,
                    TestPlatform.newPresetDtoRequest(
                            ContactDtoFactory.anotherSample(),
                            AddressDtoFactory.tolstogoStreet().build()
                    ),
                    platform
            );

            PresetsResponse presetsResponse = getPreset(identity, platform);

            assertThat(presetsResponse.getAddresses(), hasSize(1));

            assertThat(presetsResponse.getPresets(), hasSize(2));
        }

        // TODO MARKETDISCOUNT-1174 проверить данные в DS
        @Disabled
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldNormalizeSameAddressIfGeoSaysItsEquals(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                          @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            String anotherWriteOfTolstogoStreet = "улица Льва Толстого";
            assertNotEquals(anotherWriteOfTolstogoStreet, TOLSTOGO_STREET);
            given(geoCoderMock.find(contains(anotherWriteOfTolstogoStreet))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);

            testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);
            testClient.addPreset(identity, TestPlatform.newPresetDtoRequest(
                    ContactDtoFactory.anotherSample(),
                    AddressDtoFactory.tolstogoStreet().setStreet(anotherWriteOfTolstogoStreet).build()
            ), platform);

            PresetsResponse presetsResponse = getPreset(identity, platform);

            assertThat(presetsResponse.getAddresses(), hasSize(1));
            assertThat(presetsResponse.getContacts(), hasSize(2));

            assertThat(presetsResponse.getPresets(), hasSize(2));
        }

        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        public void shouldNotNormalizeSameAddressIfRoomIsNotEqualEvenIfGeoSaysAddressesAreEquals(@ConvertWith(ToPlatform.class) TestPlatform platform, @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            String anotherWriteOfTolstogoStreet = "улица Льва Толстого";
            assertNotEquals(anotherWriteOfTolstogoStreet, TOLSTOGO_STREET);
            given(geoCoderMock.find(contains(anotherWriteOfTolstogoStreet))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);

            String firstRoom = "413";
            testClient.addPreset(identity, TestPlatform.newPresetDtoRequest(
                    ContactDtoFactory.anotherSample(),
                    AddressDtoFactory.tolstogoStreet().setRoom(firstRoom).build()
            ), platform);
            String secondRoom = "312";
            testClient.addPreset(identity, TestPlatform.newPresetDtoRequest(
                    ContactDtoFactory.anotherSample(),
                    AddressDtoFactory.tolstogoStreet().setRoom(secondRoom).setStreet(anotherWriteOfTolstogoStreet).build()
            ), platform);

            PresetsResponse presetsResponse = getPreset(identity, platform);

            assertThat(presetsResponse.getAddresses(), containsInAnyOrder(
                    hasProperty("room", equalTo(firstRoom)),
                    hasProperty("room", equalTo(secondRoom))
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldNormalizeSameContact(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                        @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            given(geoCoderMock.find(contains(GRUZINSKAIA_STREET))).willReturn(GeoCoderMock.Response.GRUZINSKAYA_12);

            testClient.addPreset(identity, TestPlatform.newPresetDtoRequest(
                    ContactDtoFactory.sample(),
                    AddressDtoFactory.tolstogoStreet().build()
            ), platform);
            NewPresetDtoRequest second = TestPlatform.newPresetDtoRequest(
                    ContactDtoFactory.sample(),
                    AddressDtoFactory.gruzinskayaStreet().build()
            );
            testClient.addPreset(identity, second, platform);

            PresetsResponse presetsResponse = getPreset(identity, platform);

            assertThat(presetsResponse.getAddresses(), hasSize(2));
            assertThat(presetsResponse.getContacts(), hasSize(1));

            assertThat(presetsResponse.getPresets(), hasSize(2));
        }

        // TODO MARKETDISCOUNT-1174 проверить данные в DS
        @Disabled
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "BLUE,YANDEX_UID", "BLUE,UUID", "RED,UID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldNormalizeSameContactAndAddressIfGeoSaysItsEquals(@ConvertWith(ToPlatform.class) TestPlatform platform, @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            String anotherWriteOfTolstogoStreet = "улица Льва Толстого";
            assertNotEquals(anotherWriteOfTolstogoStreet, TOLSTOGO_STREET);
            given(geoCoderMock.find(contains(anotherWriteOfTolstogoStreet))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);

            testClient.addPreset(identity, PresetDtoFactory.tolstogoStreet(), platform);
            testClient.addPreset(identity, TestPlatform.newPresetDtoRequest(
                    ContactDtoFactory.sample(),
                    AddressDtoFactory.tolstogoStreet().setStreet(anotherWriteOfTolstogoStreet).build()
            ), platform);

            PresetsResponse presetsResponse = getPreset(identity, platform);

            assertThat(presetsResponse.getAddresses(), hasSize(1));
            assertThat(presetsResponse.getContacts(), hasSize(1));

            assertThat(presetsResponse.getPresets(), hasSize(1));
        }
    }

    @Nested
    class FetchSuggests {
        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID"})
        void shouldFetchNames(@ConvertWith(ToPlatform.class) TestPlatform platform,
                              @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport();

            String lastNameFromBlackBox = "Петрова";
            String firstNameFromBlackBox = "Мария";
            mockBlackboxResponse(identity, firstNameFromBlackBox, lastNameFromBlackBox);
            presetService.addContact(identity, ContactFactory.sample(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getRecipients(), containsInAnyOrder(
                    equalTo(firstNameFromBlackBox + ' ' + lastNameFromBlackBox),
                    equalTo(DEFAULT_FIRST_NAME + ' ' + DEFAULT_SECOND_NAME + ' ' + DEFAULT_LAST_NAME))
            );
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldSkipNameDuplicates(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                      @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport();

            mockBlackboxResponse(identity, DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME);
            presetService.addContact(identity, ContactFactory.sample(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getRecipients(),
                    contains(equalTo(DEFAULT_FIRST_NAME + ' ' + DEFAULT_SECOND_NAME + ' ' + DEFAULT_LAST_NAME)));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldHandleBlackboxError(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                       @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport();

            given(blackboxRestTemplate.getForObject(any(), eq(UserInfoResponse.class)))
                    .willThrow(new RestClientException("network error"));
            presetService.addContact(identity, ContactFactory.sample(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getRecipients(),
                    contains(equalTo(DEFAULT_FIRST_NAME + ' ' + DEFAULT_SECOND_NAME + ' ' + DEFAULT_LAST_NAME)));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldHandleBlackboxTimeout(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                         @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport();

            String lastNameFromBlackBox = "Петрова";
            String firstNameFromBlackBox = "Мария";

            mockBlackboxResponse(identity, firstNameFromBlackBox, lastNameFromBlackBox,
                    SuggestService.SUGGEST_TIMEOUT_MILLIS + 100);
            presetService.addContact(identity, ContactFactory.sample(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getRecipients(),
                    contains(equalTo(DEFAULT_FIRST_NAME + ' ' + DEFAULT_SECOND_NAME + ' ' + DEFAULT_LAST_NAME)));
        }

        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldFetchEmailsForUid(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                     @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport();

            String someEmail = "another.mail@yandex.ru";
            mockBlackboxResponse(identity, BlackboxUserFactory.builder().addEmail(someEmail));
            presetService.addContact(identity, ContactFactory.sample(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getEmails(), containsInAnyOrder(
                    equalTo(someEmail),
                    equalTo(DEFAULT_EMAIL)
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldFetchEmailsForYandexUidAndUUID(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                  @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport();

            presetService.addContact(identity, ContactFactory.sample(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getEmails(), contains(
                    equalTo(DEFAULT_EMAIL)
            ));
        }

        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldSkipEmailsDuplicateForUid(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                             @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport();

            String someEmail = "another.mail@rambler.ru";
            mockBlackboxResponse(identity, BlackboxUserFactory.builder()
                    .addEmail(someEmail)
                    .addEmail(DEFAULT_EMAIL));
            presetService.addContact(identity, ContactFactory.sample(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getEmails(), containsInAnyOrder(
                    equalTo(someEmail),
                    equalTo(DEFAULT_EMAIL)
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,YANDEX_UID", "BLUE,UUID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldSkipEmailsDuplicateForYandexUidAndUUID(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                          @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            final String anotherPhone = "+79999990000";
            presetService.addContact(identity, ContactFactory.sample(), "source");
            presetService.addContact(identity, ContactFactory.sampleBuilder().setPhoneNum(anotherPhone).build(),
                    "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getEmails(), contains(
                    equalTo(DEFAULT_EMAIL)
            ));
        }


        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldFetchPhonesForUid(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                     @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport();

            String somePhone = "89467895795";
            mockBlackboxResponse(identity, BlackboxUserFactory.builder()
                    .setPhone(somePhone));
            presetService.addContact(identity, ContactFactory.sample(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getPhoneNums(), containsInAnyOrder(
                    equalTo(somePhone),
                    equalTo(DEFAULT_PHONE_NUM)
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,SBER_ID", "RED,SBER_ID"})
        void shouldNotFetchPhoneFromPassportForSberId(
                @ConvertWith(ToPlatform.class) TestPlatform platform,
                @ConvertWith(ToIdentity.class) Identity<?> identity)
                throws Exception {
            mockPassport();

            final String somePhone = "89467895795";
            mockBlackboxResponse(identity, BlackboxUserFactory.builder()
                    .setPhone(somePhone));
            presetService.addContact(identity, ContactFactory.sample(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getPhoneNums(), containsInAnyOrder(DEFAULT_PHONE_NUM));
            assertFalse(suggest.getPhoneNums().contains(somePhone));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,YANDEX_UID", "BLUE,UUID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldFetchPhonesForYandexUidAndUUID(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                  @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            presetService.addContact(identity, ContactFactory.sample(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getPhoneNums(), contains(
                    equalTo(DEFAULT_PHONE_NUM)
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldSkipPhoneDuplicates(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                       @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport();

            mockBlackboxResponse(identity, BlackboxUserFactory.builder()
                    .setPhone(DEFAULT_PHONE_NUM));
            presetService.addContact(identity, ContactFactory.sample(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getPhoneNums(), contains(
                    equalTo(DEFAULT_PHONE_NUM)
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldFetchAddressFromDatasync(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                            @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport();
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);

            presetService.addPreset(identity, PresetFactory.tolstogoStreet(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getAddresses(), hasSize(1));
            AddressDtoResponse fromDatasync = suggest.getAddresses().get(0);
            assertThat(fromDatasync, samePropertyValuesAsExcept(AddressDtoResponse.fromAddress(
                    AddressFactory.tolstogoStreet().setType(AddressType.OTHER).build()).build(), "zip", "addressId"));
            assertEquals(TOLSTOGO_16_ZIP, fromDatasync.getZip());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldReturnPartOfAddressFromDatasyncIfGeocoderFails(@ConvertWith(ToPlatform.class) TestPlatform platform, @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport();
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.FAIL_OR_NOTHING);
            presetService.addPreset(identity, PresetFactory.tolstogoStreet(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getAddresses(), hasSize(1));
            AddressDtoResponse fromDatasync = suggest.getAddresses().get(0);
            assertThat(fromDatasync, samePropertyValuesAsExcept(AddressDtoResponse.fromAddress(
                    AddressFactory.tolstogoStreet().setType(AddressType.OTHER).build()).build(), "zip", "addressId",
                    "objectKey"));
            assertNull(fromDatasync.getZip());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID", "BLUE,SBER_ID", "RED,SBER_ID"})
        void shouldFetchAddressFromPassport(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                            @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport(PassportDatasyncAddressFactory.tolstogoStreet());
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);

            SuggestResponse suggest = getSuggest(identity, platform);
            assertThat(suggest.getAddresses(), hasSize(1));
            AddressDtoResponse fromPassport = suggest.getAddresses().get(0);
            assertEquals(TOLSTOGO_STREET_IN_GEOCODER, fromPassport.getStreet());
            assertEquals(TOLSTOGO_16_ZIP, fromPassport.getZip());
            assertEquals(213L, fromPassport.getRegionId().longValue());
            assertEquals("Россия", fromPassport.getCountry());
            assertEquals("Москва", fromPassport.getCity());
            assertEquals("16", fromPassport.getBuilding());
            assertEquals(AddressType.WORK, fromPassport.getType());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID", "BLUE,SBER_ID", "RED,SBER_ID"})
        void shouldNotFetchAddressFromPassportIfGeocoderFails(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                              @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport(PassportDatasyncAddressFactory.tolstogoStreet());
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.FAIL_OR_NOTHING);

            SuggestResponse suggest = getSuggest(identity, platform);
            assertThat(suggest.getAddresses(), empty());
        }

        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldFetchBothPassportAndDatasyncAddresses(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                         @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport(PassportDatasyncAddressFactory.tolstogoStreet());
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            given(geoCoderMock.find(contains(GRUZINSKAIA_STREET))).willReturn(GeoCoderMock.Response.GRUZINSKAYA_12);
            presetService.addPreset(identity, PresetFactory.grouzinskayaStreet(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);
            assertThat(suggest.getAddresses(), containsInAnyOrder(
                    allOf(
                            hasProperty("zip", equalTo(TOLSTOGO_16_ZIP)),
                            hasProperty("street", equalTo(TOLSTOGO_STREET_IN_GEOCODER)),
                            hasProperty("type", equalTo(AddressType.WORK))
                    ),
                    allOf(
                            hasProperty("zip", equalTo(GRUZINSKAYA_12_ZIP)),
                            hasProperty("street", equalTo(GRUZINSKAIA_STREET)),
                            hasProperty("type", equalTo(AddressType.OTHER))

                    )
            ));
        }


        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldFilterIncorrectPreset(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                         @ConvertWith(ToIdentity.class) Identity identity) throws Exception {
            final MarketDataSyncAddress incorrectAddress =
                    builderFrom(
                            new Preset(
                                    null,
                                    null,
                                    AddressFactory
                                            .grouzinskayaStreet()
                                            .setStreet(null)
                                            .setBuilding(null)
                                            .build(),
                                    ContactFactory.sample()
                            )
                    )
                            .build();

            marketDataSyncClient.saveNewAddress(identity, incorrectAddress);

            final PresetsResponse presets = getPreset(identity, platform);
            assertThat(presets.getAddresses(), empty());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldSubstituteCountryIfCountryAbsent(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                    @ConvertWith(ToIdentity.class) Identity identity) throws Exception {
            presetService.enablePeraddress67Workaround(false);

            final MarketDataSyncAddress incorrectAddress =
                    builderFrom(
                            new Preset(
                                    null,
                                    null,
                                    AddressFactory
                                            .grouzinskayaStreet()
                                            .setCountry(null)
                                            .build(),
                                    ContactFactory.sample()
                            )
                    )
                            .build();

            marketDataSyncClient.saveNewAddress(identity, incorrectAddress);

            final PresetsResponse presets = getPreset(identity, platform);
            assertThat(presets.getAddresses(), hasSize(1));
        }

        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldNotNormalizeSameAddressIfRoomIsNotEqualEvenIfGeoSaysAddressesAreEquals(@ConvertWith(ToPlatform.class) TestPlatform platform, @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport();

            String anotherWriteOfTolstogoStreet = "улица Льва Толстого";
            assertNotEquals(anotherWriteOfTolstogoStreet, TOLSTOGO_STREET);
            given(geoCoderMock.find(contains(anotherWriteOfTolstogoStreet))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);

            String firstRoom = "413";
            testClient.addPreset(identity, NewPresetDtoRequest.builder()
                            .setAddress(AddressDtoFactory.tolstogoStreet().setRoom(firstRoom).build())
                            .setContact(ContactDtoFactory.anotherSample()).build(),
                    BLUE);
            String secondRoom = "312";
            testClient.addPreset(identity, NewPresetDtoRequest.builder()
                            .setAddress(AddressDtoFactory.tolstogoStreet().setRoom(secondRoom).setStreet(anotherWriteOfTolstogoStreet).build())
                            .setContact(ContactDtoFactory.anotherSample()).build(),
                    BLUE);

            SuggestResponse suggest = getSuggest(identity, platform);

            assertThat(suggest.getAddresses(), containsInAnyOrder(
                    hasProperty("room", equalTo(firstRoom)),
                    hasProperty("room", equalTo(secondRoom))
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID", "RED,UID"})
        void shouldNormalizeAddressesIfGeoSaysItsEquals(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                        @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            mockPassport(PassportDatasyncAddressFactory.tolstogoStreet());
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);

            presetService.addPreset(identity, PresetFactory.tolstogoStreet(), "source");

            SuggestResponse suggest = getSuggest(identity, platform);
            assertThat(suggest.getAddresses(), hasSize(1));
            AddressDtoResponse fromDatasync = suggest.getAddresses().get(0);
            assertThat(fromDatasync, samePropertyValuesAsExcept(AddressDtoResponse.fromAddress(
                    AddressFactory.tolstogoStreet().setType(AddressType.OTHER).build()).build(), "zip", "addressId"));
            assertEquals(TOLSTOGO_16_ZIP, fromDatasync.getZip());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID"})
        void shouldFetchAddressesFromCheckouter(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            ru.yandex.market.checkout.checkouter.delivery.Address address = getCheckouterAddress(null, null);
            mockCheckouterClient(address);

            mockPassport();
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(null);

            SuggestResponse suggest = getSuggest(identity, platform);
            assertEquals(1, suggest.getAddresses().size());
            assertThat(suggest.getAddresses(), hasItem(allOf(
                    hasProperty("regionId", equalTo(REGION_ID.intValue())),
                    hasProperty("city", equalTo(address.getCity())),
                    hasProperty("country", equalTo(address.getCountry())),
                    hasProperty("building", equalTo(address.getHouse())),
                    hasProperty("intercom", equalTo(address.getEntryPhone())),
                    hasProperty("entrance", equalTo(address.getEntrance())),
                    hasProperty("floor", equalTo(address.getFloor())),
                    hasProperty("room", equalTo(address.getApartment())),
                    hasProperty("street", equalTo(address.getStreet())),
                    hasProperty("zip", equalTo(address.getPostcode())),
                    hasProperty("type", equalTo(AddressType.LAST_ORDER))
            )));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID"})
        void shouldFetchAddressesFromCheckouterAndOthers(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                         @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            ru.yandex.market.checkout.checkouter.delivery.Address address = getCheckouterAddress();
            mockCheckouterClient(address);

            mockPassport(PassportDatasyncAddressFactory.tolstogoStreet());
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);

            SuggestResponse suggest = getSuggest(identity, platform);
            assertEquals(2, suggest.getAddresses().size());

            assertThat(suggest.getAddresses(), containsInAnyOrder(
                    allOf(
                            hasProperty("regionId", equalTo(REGION_ID.intValue())),
                            hasProperty("city", equalTo(address.getCity())),
                            hasProperty("country", equalTo(address.getCountry())),
                            hasProperty("building", equalTo(address.getHouse() + ", к. " + address.getBlock()
                                    + ", стр. " + address.getBuilding())),
                            hasProperty("intercom", equalTo(address.getEntryPhone())),
                            hasProperty("entrance", equalTo(address.getEntrance())),
                            hasProperty("floor", equalTo(address.getFloor())),
                            hasProperty("room", equalTo(address.getApartment())),
                            hasProperty("street", equalTo(address.getStreet())),
                            hasProperty("zip", equalTo(address.getPostcode())),
                            hasProperty("type", equalTo(AddressType.LAST_ORDER))
                    ),
                    hasProperty("type", equalTo(AddressType.WORK))
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID"})
        void shouldFetchAddressesAndFilterDuplicates(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                     @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            AddressImpl tolstogoStAddress = new AddressImpl();
            tolstogoStAddress.setCountry("Россия");
            tolstogoStAddress.setCity("Москва");
            tolstogoStAddress.setStreet("улица Льва Толстого");
            tolstogoStAddress.setHouse("16");
            tolstogoStAddress.setEntryPhone("235");
            tolstogoStAddress.setEntrance("2");
            tolstogoStAddress.setApartment("109");
            tolstogoStAddress.setFloor("3");
            tolstogoStAddress.setPostcode("119021");

            mockCheckouterClient(tolstogoStAddress);

            mockPassport(PassportDatasyncAddressFactory.tolstogoStreet());
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);

            SuggestResponse suggest = getSuggest(identity, platform);
            assertEquals(1, suggest.getAddresses().size());

            assertThat(suggest.getAddresses(), hasItem(allOf(
                    hasProperty("regionId", equalTo(213)),
                    hasProperty("city", equalTo(tolstogoStAddress.getCity())),
                    hasProperty("country", equalTo(tolstogoStAddress.getCountry())),
                    hasProperty("building", equalTo(tolstogoStAddress.getHouse())),
                    hasProperty("intercom", equalTo(tolstogoStAddress.getEntryPhone())),
                    hasProperty("entrance", equalTo(tolstogoStAddress.getEntrance())),
                    hasProperty("floor", equalTo(tolstogoStAddress.getFloor())),
                    hasProperty("room", equalTo(tolstogoStAddress.getApartment())),
                    hasProperty("street", equalTo(tolstogoStAddress.getStreet())),
                    hasProperty("zip", equalTo(tolstogoStAddress.getPostcode())),
                    hasProperty("type", equalTo(AddressType.WORK))
            )));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID"})
        void shouldFetchAddressesFromCheckouterWithNullDelivery(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                                @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            Order order = mock(Order.class);
            given(order.getDelivery()).willReturn(null);
            PagedOrders pagedOrders = new PagedOrders(Arrays.asList(order), new Pager());
            given(checkouterClientMock.getOrders(any(OrderSearchRequest.class), eq(ClientRole.USER), anyLong()))
                    .willReturn(pagedOrders);

            mockPassport();
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(null);

            SuggestResponse suggest = getSuggest(identity, platform);
            assertEquals(0, suggest.getAddresses().size());
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,UID"})
        void shouldReturnNoAddressesIfCheckouterUnavailable(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                            @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(checkouterClientMock.getOrders(any(OrderSearchRequest.class), eq(ClientRole.USER), anyLong()))
                    .willThrow(new RestClientException("Oh no..."));

            mockPassport();
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(null);

            SuggestResponse suggest = getSuggest(identity, platform);
            assertEquals(0, suggest.getAddresses().size());
        }
    }

    @Nested
    class Merge {
        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource(value = {"BLUE,YANDEX_UID", "BLUE,UUID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldMovePresetToNewUid(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                      @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(GRUZINSKAIA_STREET))).willReturn(GeoCoderMock.Response.GRUZINSKAYA_12);
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            NewPresetDtoRequest presetOfUnauthorized = PresetDtoFactory.tolstogoStreet();
            testClient.addPreset(identity, presetOfUnauthorized, platform);

            NewPresetDtoRequest presetOfAuthorized = PresetDtoFactory.gruzinskayaStreet();
            testClient.addPreset(UID, presetOfAuthorized, platform);

            merge(identity, UID);

            List<Preset> presets = presetService.getPresets(UID);
            assertThat(presets, containsInAnyOrder(
                    samePresetDto(presetOfAuthorized),
                    samePresetDto(presetOfUnauthorized)
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,YANDEX_UID", "BLUE,UUID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldRemovePresetDuplicatesOnMerge(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                 @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            NewPresetDtoRequest somePreset = PresetDtoFactory.tolstogoStreet();
            testClient.addPreset(identity, somePreset, platform);
            testClient.addPreset(UID, somePreset, platform);

            merge(identity, UID);

            List<Preset> presets = presetService.getPresets(UID);
            assertThat(presets, contains(
                    samePresetDto(somePreset)
            ));
        }

        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource(value = {"BLUE,YANDEX_UID", "BLUE,UUID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldNotRemoveAddressDuplicateIfPresetIsNotDuplicateOnMerge(@ConvertWith(ToPlatform.class) TestPlatform platform, @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            NewPresetDtoRequest somePreset = TestPlatform.newPresetDtoRequest(
                    ContactDtoFactory.sample(),
                    AddressDtoFactory.tolstogoStreet().build()
            );
            NewPresetDtoRequest samePresetButAnotherContact = TestPlatform.newPresetDtoRequest(
                    ContactDtoFactory.anotherSample(),
                    AddressDtoFactory.tolstogoStreet().build()
            );
            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            testClient.addPreset(identity, somePreset, platform);
            testClient.addPreset(UID, samePresetButAnotherContact, platform);

            merge(identity, UID);

            List<Preset> presets = presetService.getPresets(UID);
            assertThat(presets, containsInAnyOrder(
                    samePresetDto(somePreset),
                    samePresetDto(samePresetButAnotherContact)
            ));
        }

        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource(value = {"BLUE,YANDEX_UID", "BLUE,UUID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldMoveContactToNewUid(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                       @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            ContactDto contactOfUnauthorized = ContactDtoFactory.sample();
            testClient.addContact(identity, contactOfUnauthorized, platform);

            ContactDto contactOfAuthorized = ContactDtoFactory.anotherSample();
            testClient.addContact(UID, contactOfAuthorized, platform);

            merge(identity, UID);

            Collection<Contact> contacts = presetService.getContacts(UID);
            assertThat(contacts, containsInAnyOrder(
                    equalTo(contactOfAuthorized.toContactBuilder().build()),
                    equalTo(contactOfUnauthorized.toContactBuilder().build())
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,YANDEX_UID", "BLUE,UUID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldMergeSameAddresses(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                      @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            ContactDto contact = ContactDtoFactory.sample();
            ContactDto contact2 = ContactDtoFactory.sample();
            testClient.addContact(identity, contact, platform);

            testClient.addContact(UID, contact2, platform);

            merge(identity, UID);

            Collection<Contact> contacts = presetService.getContacts(UID);
            assertThat(contacts, contains(
                    equalTo(contact.toContactBuilder().build())
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"BLUE,YANDEX_UID", "BLUE,UUID", "RED,YANDEX_UID", "RED,UUID"})
        void shouldRemoveContactDuplicatesOnMerge(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                  @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            ContactDto someContact = ContactDtoFactory.sample();
            testClient.addContact(identity, someContact, platform);
            testClient.addContact(UID, someContact, platform);

            merge(identity, UID);

            given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);
            Collection<Contact> contacts = presetService.getContacts(UID);
            assertThat(contacts, contains(
                    equalTo(someContact.toContactBuilder().build())
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"YANDEX_UID", "UUID", "UID"})
        void shouldMovePickpointToNewUid(@ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            final FavouritePickpointRequest pickpointOfUnauthorized = FavouritePickpointRequest.builder()
                    .setPickId("pic1").setRegionId(1).build();
            testClient.savePickpoint(identity, pickpointOfUnauthorized);

            final FavouritePickpointRequest pickpointOfAuthorized = FavouritePickpointRequest.builder()
                    .setPickId("pic2").setRegionId(2).build();
            testClient.savePickpoint(UID, pickpointOfAuthorized);

            merge(identity, UID);


            final Set<FavouritePickpoint> pickpoints = pickpointDao.getAll(UID, 100);
            Assertions.assertEquals(2, pickpoints.size());
            assertThat(pickpoints, contains(
                    samePickpointDto(pickpointOfAuthorized),
                    samePickpointDto(pickpointOfUnauthorized)
            ));
        }

        @ParameterizedTest
        @CsvSource(value = {"YANDEX_UID", "UUID", "UID"})
        void shouldRemovePickpointsDuplicatesOnMerge(@ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
            final FavouritePickpointRequest pickpoint = FavouritePickpointRequest.builder()
                    .setPickId("pic1").setRegionId(1).build();
            testClient.savePickpoint(identity, pickpoint);
            testClient.savePickpoint(UID, pickpoint);

            merge(identity, UID);


            final Set<FavouritePickpoint> pickpoints = pickpointDao.getAll(UID, 100);
            Assertions.assertEquals(1, pickpoints.size());
            assertThat(pickpoints, contains(
                    samePickpointDto(pickpoint)
            ));
        }
    }

    @Nested
    class Errors {
        @Test
        public void should400OnIncorrectUserType() throws Exception {
            getPreset("fake", "1", BLUE, status().isBadRequest());
        }

        @Test
        public void should400OnUnparsableUserId() throws Exception {
            getPreset("uid", "asdf1", BLUE, status().isBadRequest());
        }
    }


    @Test
    void shouldWorkaroundEmptyRegionId() {
        final Identity<?> identity = Identity.Type.UID.buildIdentity("1");
        final Preset preset = Preset.builder()
                .setAddress(AddressFactory.grouzinskayaStreet()
                        .setCity("Вологда")
                        .setRegionId(null)
                        .build())
                .setContact(ContactFactory.sample())
                .build();

        marketDataSyncClient.saveNewAddress(identity, builderFrom(preset).build());


        final List<Preset> presets = presetService.getPresets(identity);
        assertThat(presets.get(0).getAddress(), hasProperty("regionId", equalTo(21)));
    }

    @Test
    void shouldFilterAddressWithInvalidFlat() {
        final String exceed15CharsRoom = "офис чтобы пройти нужно разбежаться посильнее";

        final Identity<?> identity = Identity.Type.UID.buildIdentity("1");
        final Preset preset = Preset.builder()
                .setAddress(AddressFactory.grouzinskayaStreet()
                        .setRoom(exceed15CharsRoom)
                        .build())
                .setContact(ContactFactory.sample())
                .build();

        marketDataSyncClient.saveNewAddress(identity, builderFrom(preset).build());


        final List<Preset> presets = presetService.getPresets(identity);
        assertThat(presets, empty());
    }

    @Test
    void shouldValidateFlatWhenSavingAddress() {
        final Identity<?> identity = Identity.Type.UID.buildIdentity("1");
        final String exceed15CharsRoom = "офис чтобы пройти нужно разбежаться посильнее";
        final Preset preset = Preset.builder()
                .setAddress(AddressFactory.grouzinskayaStreet()
                        .setRoom(exceed15CharsRoom)
                        .build())
                .setContact(ContactFactory.sample())
                .build();

        marketDataSyncClient.saveNewAddress(identity, builderFrom(preset).build());

        assertThrows(IllegalArgumentException.class, () -> presetService.addPreset(identity, preset, "source"));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldDuplicateQueryGeocoder(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                      @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        mockPassport(new PassportDataSyncAddress(
                "work",
                "улица Льва Толстого, 16",
                "Россия, Земля Евразия Россия Центральный федеральный округ Москва и Московская область Москва, " +
                        "Москва, " + TOLSTOGO_STREET + ", 16",
                "37.588149",
                "55.733847"
        ));//Россия, Земля Евразия Россия Центральный федеральный округ Москва и Московская область Москва, Москва,
        // ул. Льва Толстого, 16
        given(geoCoderMock.find(contains(TOLSTOGO_STREET))).willReturn(GeoCoderMock.Response.TOLSTOGO_16);

        presetService.addPreset(identity, Preset.builder()
                        .setAddress(Address.builder()
                                .setRegionId(213)
                                .setCountry("Россия")
                                .setCity("Москва")
                                .setStreet(TOLSTOGO_STREET)
                                .setBuilding("16")
                                .build())
                        .setContact(ContactFactory.sample())
                        .build()
                , "source");

        SuggestResponse suggest = getSuggest(identity, platform);
        assertThat(suggest.getAddresses(), hasSize(1));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldAddAddressWithDistrict(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                      @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        String testDistrict = "testDistrict";
        NewAddressDtoRequest address = AddressDtoFactory.tolstogoStreet().setDistrict(testDistrict).build();

        testClient.addAddress(identity, address, platform);

        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);

        assertThat(addresses, contains(
                allOf(
                        hasProperty("district", equalTo(testDistrict)))));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldUpdateAddressWithDistrict(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                         @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        String testDistrict = "testDistrict";
        NewAddressDtoRequest.Builder addressBuilder = AddressDtoFactory.tolstogoStreet();

        ObjectKey objectKey = testClient.addAddress(identity, addressBuilder.build(), platform);

        testClient.updateAddress(identity, objectKey, platform, addressBuilder.setDistrict(testDistrict).build());

        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);

        assertThat(addresses, contains(
                allOf(
                        hasProperty("district", equalTo(testDistrict)))));
    }


    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldUpdateAddressWithoutDistrict(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                            @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        NewAddressDtoRequest.Builder addressBuilder = AddressDtoFactory.tolstogoStreet();

        ObjectKey objectKey = testClient.addAddress(identity, addressBuilder.build(), platform);

        testClient.updateAddress(identity, objectKey, platform, addressBuilder.build());

        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);

        assertThat(addresses, contains(
                allOf(
                        hasProperty("district", isEmptyOrNullString()))));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldGetAddressWithLastTouchedTime(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                             @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        NewAddressDtoRequest.Builder addressBuilder = AddressDtoFactory.tolstogoStreet();
        ObjectKey objectKey = testClient.addAddress(identity, addressBuilder.build(), platform);
        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);
        assertEquals(1, addresses.size());
        assertNotNull(addresses.get(0).getLastTouchedTime());
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldUpdateTouch(@ConvertWith(ToPlatform.class) TestPlatform platform,
                           @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        NewAddressDtoRequest.Builder addressBuilder = AddressDtoFactory.tolstogoStreet();
        ObjectKey objectKey = testClient.addAddress(identity, addressBuilder.build(), platform);
        TimeUnit.MILLISECONDS.sleep(5);
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        TimeUnit.MILLISECONDS.sleep(5);
        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);

        assertEquals(1, addresses.size());
        assertTrue(addresses.get(0).getLastTouchedTime().isBefore(offsetDateTime));

        testClient.touchAddress(identity, objectKey);
        addresses = testClient.getAddresses(identity, platform);
        assertEquals(1, addresses.size());
        assertTrue(addresses.get(0).getLastTouchedTime().isAfter(offsetDateTime));
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldUpdateTouchWithNull(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                   @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        Address.Builder addressBuilder = AddressFactory.tolstogoStreet();
        ObjectKey objectKey = presetService.addAddress(identity, addressBuilder.build(), "");
        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platform);

        assertEquals(1, addresses.size());
        assertNull(addresses.get(0).getLastTouchedTime());

        testClient.touchAddress(identity, objectKey);
        addresses = testClient.getAddresses(identity, platform);
        assertEquals(1, addresses.size());
        assertNotNull(addresses.get(0).getLastTouchedTime());
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldUpdateAddressWithoutUpdateLastTouchTime(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                       @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        NewAddressDtoRequest.Builder addressBuilder = AddressDtoFactory.tolstogoStreet();
        ObjectKey objectKey = testClient.addAddress(identity, addressBuilder.build(), platform);
        List<AddressDtoResponse> addressesBeforeUpdate = testClient.getAddresses(identity, platform);
        TimeUnit.MILLISECONDS.sleep(5);
        testClient.updateAddress(identity, objectKey, platform, addressBuilder.build());
        List<AddressDtoResponse> addressesAfterUpdate = testClient.getAddresses(identity, platform);
        assertEquals(addressesBeforeUpdate.get(0).getLastTouchedTime(),
                addressesAfterUpdate.get(0).getLastTouchedTime());
    }


    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldUpdateAddressWithNullLastTouchTime(@ConvertWith(ToPlatform.class) TestPlatform platform,
                                                  @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        Address.Builder addressBuilder = AddressFactory.tolstogoStreet();
        NewAddressDtoRequest.Builder addressDtoFactory = AddressDtoFactory.tolstogoStreet();
        ObjectKey objectKey = presetService.addAddress(identity, addressBuilder.build(), "");
        List<AddressDtoResponse> addressesBeforeUpdate = testClient.getAddresses(identity, platform);
        assertEquals(1, addressesBeforeUpdate.size());
        assertNull(addressesBeforeUpdate.get(0).getLastTouchedTime());

        testClient.updateAddress(identity, objectKey, platform, addressDtoFactory.build());
        List<AddressDtoResponse> addressesAfterUpdate = testClient.getAddresses(identity, platform);
        assertEquals(1, addressesAfterUpdate.size());
        assertNull(addressesAfterUpdate.get(0).getLastTouchedTime());
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldAddAddressWithPlatform(@ConvertWith(ToPlatform.class) TestPlatform platformRgb,
                                      @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        Address.Builder addressBuilder = AddressFactory.tolstogoStreet();
        String platform = "ios";
        addressBuilder.setPlatform(platform);
        ObjectKey objectKey = presetService.addAddress(identity, addressBuilder.build(), "");
        List<Address> addresses = presetService.getAddresses(identity);
        assertEquals(1, addresses.size());
        assertEquals(platform, addresses.get(0).getPlatform());
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldUpdateAddressWithPlatform(@ConvertWith(ToPlatform.class) TestPlatform platformRgb,
                                         @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        Address.Builder addressBuilder = AddressFactory.tolstogoStreet();
        ObjectKey objectKey = presetService.addAddress(identity, addressBuilder.build(), "");
        addressBuilder = addressBuilder.setId(objectKey);
        List<Address> addressesBeforeUpdate = presetService.getAddresses(identity);
        assertEquals(1, addressesBeforeUpdate.size());
        assertNull(addressesBeforeUpdate.get(0).getPlatform());

        String platform = "ios";
        presetService.updateAddress(
                identity, addressBuilder.setStreet("sdf").setPlatform(platform).build(), "");
        List<Address> addressesAfterUpdate = presetService.getAddresses(identity);
        assertEquals(1, addressesAfterUpdate.size());
        assertEquals(platform, addressesAfterUpdate.get(0).getPlatform());
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldNotUpdatePlatformAddress(@ConvertWith(ToPlatform.class) TestPlatform platformRgb,
                                        @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        Address.Builder addressBuilder = AddressFactory.tolstogoStreet();
        String platform = "ios";
        ObjectKey objectKey = presetService.addAddress(identity, addressBuilder.setPlatform(platform).build(), "");
        addressBuilder = addressBuilder.setId(objectKey);
        List<Address> addressesBeforeUpdate = presetService.getAddresses(identity);
        assertEquals(1, addressesBeforeUpdate.size());
        assertEquals(platform, addressesBeforeUpdate.get(0).getPlatform());


        presetService.updateAddress(identity,
                addressBuilder.setLastTouchedTime(OffsetDateTime.now()).setPlatform("1").build(), "");
        presetService.updateAddress(identity, addressBuilder.setComment("123").setPlatform("2").build(), "");
        presetService.updateAddress(identity, addressBuilder.setFloor("2123").setPlatform("3").build(), "");
        presetService.updateAddress(identity, addressBuilder.setCargoLift(false).setPlatform("4").build(), "");
        presetService.updateAddress(identity, addressBuilder.setEntrance("123").setPlatform("5").build(), "");
        List<Address> addressesAfterUpdate = presetService.getAddresses(identity);
        assertEquals(1, addressesAfterUpdate.size());
        assertEquals(platform, addressesAfterUpdate.get(0).getPlatform());
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldUpdatePlatformAddress(@ConvertWith(ToPlatform.class) TestPlatform platformRgb,
                                     @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        Address.Builder addressBuilder = AddressFactory.tolstogoStreet();
        String platform = "ios";
        ObjectKey objectKey = presetService.addAddress(identity, addressBuilder.setPlatform(platform).build(), "");
        addressBuilder = addressBuilder.setId(objectKey);
        List<Address> addressesBeforeUpdate = presetService.getAddresses(identity);
        assertEquals(1, addressesBeforeUpdate.size());
        assertEquals(platform, addressesBeforeUpdate.get(0).getPlatform());

        checkUpdate(identity, addressBuilder.setCountry("123").setPlatform("and1").build());
        checkUpdate(identity, addressBuilder.setCity("123").setPlatform("and2").build());
        checkUpdate(identity, addressBuilder.setDistrict("123").setPlatform("and3").build());
        checkUpdate(identity, addressBuilder.setStreet("123").setPlatform("and4").build());
        checkUpdate(identity, addressBuilder.setBuilding("123").setPlatform("and5").build());
        checkUpdate(identity, addressBuilder.setLocation(new Location(BigDecimal.ONE, BigDecimal.TEN))
                .setPlatform("and6").build());
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldHideAddressWithoutGps(@ConvertWith(ToPlatform.class) TestPlatform platformRgb,
                                     @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        Address.Builder addressBuilder = AddressFactory.tolstogoStreet();
        ObjectKey objectKey = presetService.addAddress(identity, addressBuilder.setLocation(null).build(), "");
        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platformRgb);
        assertEquals(1, addresses.size());
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("hide_without_gps", "true");
        List<AddressDtoResponse> addressesWithParam = testClient.getAddresses(identity, platformRgb, params);
        assertTrue(addressesWithParam.isEmpty());
    }

    @ParameterizedTest
    @CsvSource(value = {"BLUE,UID"})
    void shouldHideAddressWithoutGps2(@ConvertWith(ToPlatform.class) TestPlatform platformRgb,
                                      @ConvertWith(ToIdentity.class) Identity<?> identity) throws Exception {
        Address.Builder addressBuilder = AddressFactory.tolstogoStreet();
        presetService.addAddress(identity, addressBuilder.setLocation(null).build(), "");
        presetService.addAddress(identity,
                addressBuilder.setLocation(new Location(BigDecimal.ONE, BigDecimal.TEN)).build(), "");
        List<AddressDtoResponse> addresses = testClient.getAddresses(identity, platformRgb);
        assertEquals(2, addresses.size());
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("hide_without_gps", "true");
        List<AddressDtoResponse> addressesWithParam = testClient.getAddresses(identity, platformRgb, params);
        assertEquals(1, addressesWithParam.size());
        assertNotNull(addressesWithParam.get(0).getLocation());
        assertNotNull(addressesWithParam.get(0).getLocation().getLatitude());
        assertNotNull(addressesWithParam.get(0).getLocation().getLongitude());
    }



    @Test
    void shouldAddAddressWithPreciseRegion() throws Exception {
        Integer preciseRegionId = 999;
        NewAddressDtoRequest address = AddressDtoFactory.tolstogoStreet()
                .setPreciseRegionId(preciseRegionId)
                .build();

        testClient.addAddress(UID, address, BLUE);

        List<AddressDtoResponse> addresses = testClient.getAddresses(UID, BLUE);

        assertThat(addresses, contains(
                allOf(
                        hasProperty("preciseRegionId", equalTo(preciseRegionId)))));
    }

    @Test
    void shouldUpdateAddressWithPreciseRegion() throws Exception {
        Integer preciseRegionId = 999;
        NewAddressDtoRequest.Builder addressBuilder = AddressDtoFactory.tolstogoStreet();

        ObjectKey objectKey = testClient.addAddress(UID, addressBuilder.build(), BLUE);

        testClient.updateAddress(UID, objectKey, BLUE, addressBuilder.setPreciseRegionId(preciseRegionId).build());

        List<AddressDtoResponse> addresses = testClient.getAddresses(UID, BLUE);

        assertThat(addresses, contains(
                allOf(
                        hasProperty("preciseRegionId", equalTo(preciseRegionId)))));
    }

    @Test
    void shouldUpdateAddressWithoutPreciseRegion() throws Exception {
        NewAddressDtoRequest.Builder addressBuilder = AddressDtoFactory.tolstogoStreet();

        ObjectKey objectKey = testClient.addAddress(UID, addressBuilder.build(), BLUE);

        testClient.updateAddress(UID, objectKey, BLUE, addressBuilder.build());

        List<AddressDtoResponse> addresses = testClient.getAddresses(UID, BLUE);

        assertThat(addresses, contains(
                allOf(
                        hasProperty("preciseRegionId",  is(nullValue())))));
    }

    @Test
    void addShouldFetchPreciseRegionIdWhenGpsIsSet() throws Exception {
        NewAddressDtoRequest.Builder addressBuilder = AddressDtoFactory.tolstogoStreet();
        addressBuilder.setLocation(new LocationDto(BigDecimal.valueOf(55.73361), BigDecimal.valueOf(37.642556)));
        String gpsParams = "lat=55.73361&lon=37.642556";
        int expectedPreciseRegionId = 117067;
        given(geobaseRestTemplate.getForObject(ArgumentMatchers.contains(gpsParams), any()))
                .willReturn(expectedPreciseRegionId);

        testClient.addAddress(UID, addressBuilder.build(), BLUE);
        List<AddressDtoResponse> addresses = testClient.getAddresses(UID, BLUE);

        assertThat(addresses, contains(
                allOf(
                        hasProperty("preciseRegionId", equalTo(expectedPreciseRegionId)))));
    }

    @Test
    void updateShouldFetchPreciseRegionIdWhenGpsIsSet() throws Exception {
        NewAddressDtoRequest.Builder addressBuilder = AddressDtoFactory.tolstogoStreet();
        String gpsParams = "lat=55.73361&lon=37.642556";
        int expectedPreciseRegionId = 117067;
        given(geobaseRestTemplate.getForObject(ArgumentMatchers.contains(gpsParams), any()))
                .willReturn(expectedPreciseRegionId);
        NewAddressDtoRequest newAddressDtoRequest = addressBuilder.build();
        ObjectKey objectKey = testClient.addAddress(UID, newAddressDtoRequest, BLUE);
        List<AddressDtoResponse> addressesAfterAdd = testClient.getAddresses(UID, BLUE);
        assertThat(addressesAfterAdd, contains(
                allOf(
                        hasProperty("preciseRegionId", nullValue()))));
        addressBuilder.setLocation(new LocationDto(BigDecimal.valueOf(55.73361), BigDecimal.valueOf(37.642556)));

        testClient.updateAddress(UID, objectKey, BLUE, addressBuilder.build());
        List<AddressDtoResponse> addresses = testClient.getAddresses(UID, BLUE);

        assertThat(addresses, contains(
                allOf(
                        hasProperty("preciseRegionId", equalTo(expectedPreciseRegionId)))));
    }

    @Test
    void shouldAddAddressWithFullnessState() throws Exception {
        AddressFullnessState fullnessState = AddressFullnessState.NOT_FULLY_FILLED;
        NewAddressDtoRequest address = AddressDtoFactory.tolstogoStreet()
                .setFullnessState(fullnessState)
                .build();

        testClient.addAddress(UID, address, BLUE);

        List<AddressDtoResponse> addresses = testClient.getAddresses(UID, BLUE);

        assertThat(addresses, contains(
                allOf(
                        hasProperty("fullnessState", equalTo(fullnessState)))));
    }

    @Test
    void shouldUpdateAddressWithFullnessState() throws Exception {
        AddressFullnessState fullnessState = AddressFullnessState.NOT_FULLY_FILLED;
        NewAddressDtoRequest.Builder addressBuilder = AddressDtoFactory.tolstogoStreet();

        ObjectKey objectKey = testClient.addAddress(UID, addressBuilder.build(), BLUE);

        testClient.updateAddress(UID, objectKey, BLUE, addressBuilder.setFullnessState(fullnessState).build());
        List<AddressDtoResponse> addresses = testClient.getAddresses(UID, BLUE);

        assertThat(addresses, contains(
                allOf(
                        hasProperty("fullnessState", equalTo(fullnessState)))));
    }

    @Test
    void shouldAddAddressWithDefaultFullnessStateWhenNull() throws Exception {
        AddressFullnessState defaultFullnessState = AddressFullnessState.FULLY_FILLED;
        NewAddressDtoRequest.Builder addressBuilder = AddressDtoFactory.tolstogoStreet()
                .setFullnessState(null);

        testClient.addAddress(UID, addressBuilder.build(), BLUE);

        List<AddressDtoResponse> addresses = testClient.getAddresses(UID, BLUE);
        assertThat(addresses, contains(
                allOf(
                        hasProperty("fullnessState", is(defaultFullnessState)))));
    }

    @Test
    void shouldUpdateAddressWithDefaultFullnessStateWhenNull() throws Exception {
        AddressFullnessState defaultFullnessState = AddressFullnessState.FULLY_FILLED;
        NewAddressDtoRequest.Builder addressBuilder = AddressDtoFactory.tolstogoStreet()
                .setFullnessState(null);
        ObjectKey objectKey = testClient.addAddress(UID, addressBuilder.build(), BLUE);

        testClient.updateAddress(UID, objectKey, BLUE, addressBuilder.build());

        List<AddressDtoResponse> addresses = testClient.getAddresses(UID, BLUE);
        assertThat(addresses, contains(
                allOf(
                        hasProperty("fullnessState", is(defaultFullnessState)))));
    }

    private void checkUpdate(Identity<?> identity, Address address) {
        presetService.updateAddress(identity, address, "");
        List<Address> addressesAfterUpdate = presetService.getAddresses(identity);
        assertEquals(1, addressesAfterUpdate.size());
        assertEquals(address.getPlatform(), addressesAfterUpdate.get(0).getPlatform());
    }


    private void mockBlackboxResponse(Identity<?> identity, String firstNameFromBlackBox, String lastNameFromBlackBox) {
        mockBlackboxResponse(identity, firstNameFromBlackBox, lastNameFromBlackBox, 0);
    }

    private void mockBlackboxResponse(Identity<?> identity, String firstNameFromBlackBox, String lastNameFromBlackBox
            , long responseTime) {
        mockBlackboxResponse(identity, BlackboxUserFactory.builder()
                .setLastName(lastNameFromBlackBox)
                .setFirstName(firstNameFromBlackBox), responseTime);
    }

    private void mockBlackboxResponse(Identity<?> identity, BlackboxUserFactory builder) {
        mockBlackboxResponse(identity, builder, 0);
    }

    private void mockBlackboxResponse(Identity<?> identity, BlackboxUserFactory builder, long responseTime) {
        if (identity.getType() == Identity.Type.UID) {
            given(blackboxRestTemplate.getForObject(any(), eq(UserInfoResponse.class)))
                    .willAnswer(answer -> {
                        if (responseTime > 0) {
                            Thread.sleep(responseTime);
                        }
                        return builder.build();
                    });
        } else {
            throw new RuntimeException("should not called for this type of identity");
        }
    }

    private void mockPassport(PassportDataSyncAddress... addresses) {
        given(passportDataSyncRestTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                eq(PassportDataSyncAddressList.class)
        ))
                .willReturn(new ResponseEntity<>(new PassportDataSyncAddressList(Arrays.asList(addresses)),
                        HttpStatus.OK));
    }

    private void mockUid() {
        given(userInfoService.resolve(UID.getValue()))
                .willReturn(ru.yandex.market.sdk.userinfo.domain.Uid.ofPassport(UID.getValue()));
        given(userInfoService.resolve(SBER_ID.getValue()))
                .willReturn(ru.yandex.market.sdk.userinfo.domain.Uid.ofSberlog(SBER_ID.getValue()));
    }

    private void mockCheckouterClient(ru.yandex.market.checkout.checkouter.delivery.Address address) {
        PagedOrders checkouterResponse = createCheckouterResponse(address);
        given(checkouterClientMock.getOrders(any(), any())).willReturn(checkouterResponse);
    }

    private void merge(Identity<?> from, Uid to) throws Exception {
        mockMvc
                .perform(
                        post("/merge/{uid}/", to.getValue())
                                .param("userType", from.getType().getCode())
                                .param("userId", from.getStringValue())
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }

    private SuggestResponse getSuggest(Identity<?> uid, TestPlatform platform) throws Exception {
        String response = mockMvc
                .perform(
                        get("/suggests/{type}/{userId}/{platformType}", uid.getType().getCode(), uid.getStringValue()
                                , platform.path())
                )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, SuggestResponse.class);
    }

    private PresetsResponse getPreset(Identity<?> identity, TestPlatform platform) throws Exception {
        return getPreset(identity.getType().getCode(), identity.getStringValue(), platform);
    }

    private PresetsResponse getPreset(String userType, String userId, TestPlatform platform) throws Exception {
        String response = getPreset(userType, userId, platform, status().isOk());
        return objectMapper.readValue(response, PresetsResponse.class);
    }

    @NotNull
    private String getPreset(String userType, String userId, TestPlatform platform, ResultMatcher matcher) throws Exception {
        return mockMvc
                .perform(
                        get("/presets/{type}/{userId}/{platformType}", userType, userId, platform.path())
                                .param("regionId", "213")
                )
                .andDo(log())
                .andExpect(matcher)
                .andReturn().getResponse().getContentAsString();
    }


    private static ContactDto toContactDto(Contact c) {
        return ContactDto.toContactDtoBuilder(c).build();
    }

    private static PagedOrders createCheckouterResponse(ru.yandex.market.checkout.checkouter.delivery.Address address) {
        Order order = mock(Order.class);
        Delivery delivery = mock(Delivery.class);

        given(delivery.getBuyerAddress()).willReturn(address);
        given(delivery.getRegionId()).willReturn(REGION_ID);
        given(order.getDelivery()).willReturn(delivery);

        return new PagedOrders(Collections.singletonList(order), new Pager());
    }


    private static ru.yandex.market.checkout.checkouter.delivery.Address getCheckouterAddress() {
        return getCheckouterAddress("block", "building");
    }

    private static ru.yandex.market.checkout.checkouter.delivery.Address getCheckouterAddress(String block,
                                                                                              String building) {
        AddressImpl address = new AddressImpl();
        address.setSubway("subway");
        address.setCountry("country");
        address.setCity("city");
        address.setStreet("street");
        address.setHouse("house");
        address.setEntryPhone("entryPhone");
        address.setBlock(block);
        address.setBuilding(building);
        address.setEntrance("entrance");
        address.setApartment("apartment");
        address.setFloor("floor");
        address.setPostcode("postcode");

        return address;
    }

    static class ToIdentity extends SimpleArgumentConverter {
        private static final ImmutableMap<String, Identity<?>> USER_IDS =
                ImmutableMap.of("UID", UID, "YANDEX_UID", YANDEX_UID, "UUID", UUID, "SBER_ID", SBER_ID);

        @Override
        protected Object convert(Object source, Class targetType) throws ArgumentConversionException {
            //noinspection SuspiciousMethodCalls
            return USER_IDS.get(source);
        }
    }

    static class ToPlatform extends SimpleArgumentConverter {
        @Override
        protected Object convert(Object source, Class targetType) throws ArgumentConversionException {
            switch ((String) source) {
                case "BLUE":
                    return BLUE;
                case "RED":
                    return RED;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    public static Matcher<FavouritePickpoint> samePickpointDto(FavouritePickpointRequest expected) {
        return allOf(
                hasProperty("pickId", samePropertyValuesAsExcept(expected.getPickId())),
                hasProperty("regionId", samePropertyValuesAsExcept(expected.getRegionId()))
        );
    }
}
