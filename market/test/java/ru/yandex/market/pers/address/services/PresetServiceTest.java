package ru.yandex.market.pers.address.services;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.address.config.Blackbox;
import ru.yandex.market.pers.address.dao.DeletedPresetDao;
import ru.yandex.market.pers.address.dao.ObjectKey;
import ru.yandex.market.pers.address.factories.AddressFactory;
import ru.yandex.market.pers.address.factories.BlackboxUserFactory;
import ru.yandex.market.pers.address.factories.ContactFactory;
import ru.yandex.market.pers.address.model.Address;
import ru.yandex.market.pers.address.model.Contact;
import ru.yandex.market.pers.address.model.GeocoderStatus;
import ru.yandex.market.pers.address.model.Preset;
import ru.yandex.market.pers.address.model.identity.Identity;
import ru.yandex.market.pers.address.services.blackbox.UserInfoResponse;
import ru.yandex.market.pers.address.services.model.MarketDataSyncAddress;
import ru.yandex.market.pers.address.util.BaseWebTest;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.domain.UidType;
import ru.yandex.market.sdk.userinfo.service.UserInfoService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static ru.yandex.market.pers.address.factories.AddressDtoFactory.TOLSTOGO_STREET;

class PresetServiceTest extends BaseWebTest {
    public static final Identity<?> DEFAULT_UID = Identity.Type.UID.buildIdentity("1000");
    private static final Identity<?> DEFAULT_YANDEX_UID = Identity.Type.YANDEX_UID.buildIdentity("1000");
    @Autowired
    private PresetService presetService;
    @Autowired
    private MarketDataSyncClient marketDataSyncClient;
    @Autowired
    @Blackbox
    private RestTemplate blackboxRestTemplate;
    @Autowired
    @Blackbox
    private UserInfoService userInfoService;
    @Autowired
    private DeletedPresetDao deletedPresetsDao;


    @SuppressWarnings("unchecked")
    @Test
    public void shouldMergeAddressesFromDatasyncAndInternalDatabase() {
        // часть адресов в пг часть в датасинке
        marketDataSyncClient.saveNewAddress(
                DEFAULT_UID,
                MarketDataSyncAddress.builder()
                        .setId("10000")
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

        presetService.addPreset(
                DEFAULT_UID,
                Preset.builder()
                        .setId(new ObjectKey("50000"))
                        .setContact(Contact.builder()
                                .setRecipient("Дмитрий Владимирович Селезнев")
                                .setPhoneNum("+79507778899")
                                .setEmail("seleznev@mail.nowhere")
                                .build())
                        .setAddress(Address.builder()
                                .setCountry("Россия")
                                .setCity("Воронеж")
                                .setStreet("Александра Твардовского")
                                .setBuilding("6")
                                .setFloor("1")
                                .setRoom("67")
                                .setRegionId(193)
                                .build())
                        .build(), "source");

        final List<Preset> presets = presetService.getPresets(DEFAULT_UID);

        assertThat(
                presets,
                containsInAnyOrder(
                        hasProperty("id", equalTo(new ObjectKey("10000"))),
                        hasProperty("id", equalTo(new ObjectKey("50000")))
                )
        );

    }


    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterPersaddress67PresetsFromDatasync() {
        mockBlackboxResponse(DEFAULT_UID, BlackboxUserFactory.builder()
                .addEmail("not_seleznev@mail.nowhere"));

        // часть адресов в пг часть в датасинке
        final String id1 = marketDataSyncClient.saveNewAddress(
                DEFAULT_UID,
                MarketDataSyncAddress.builder()
                        .setId("10000")
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

        final String id2 = marketDataSyncClient.saveNewAddress(
                DEFAULT_UID,
                MarketDataSyncAddress.builder()
                        .setId("10001")
                        .setCountry("Россия")
                        .setCity("Москва")
                        .setStreet("Сухого")
                        .setBuilding("1")
                        .setFloor("1")
                        .setFlat("3")
                        .setRegionId("213")
                        .setRecipient("Антон Николаевич Старенко")
                        .setPhone("+79991118877")
                        .setEmail("starenko@mail.nowhere")
                        .build());

        final List<Preset> presets = presetService.getPresets(DEFAULT_UID);

        assertThat(
                presets,
                empty()
        );

        // пресеты должны отфильтроваться через бб
        final List<String> deletedPresets = deletedPresetsDao.getDeletedPresets(DEFAULT_UID);

        assertThat(deletedPresets, containsInAnyOrder(equalTo(id1), equalTo(id2)));


        // теперь пресеты должны отфильтроваться через deleted_presets
        final List<Preset> presets2 = presetService.getPresets(DEFAULT_UID);

        assertThat(
                presets2,
                empty()
        );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterPersaddress67PresetsFromDatasyncIfUserNoAuth() {
        final String id1 = marketDataSyncClient.saveNewAddress(
                DEFAULT_YANDEX_UID,
                MarketDataSyncAddress.builder()
                        .setId("10000")
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

        final String id2 = marketDataSyncClient.saveNewAddress(
                DEFAULT_YANDEX_UID,
                MarketDataSyncAddress.builder()
                        .setId("10001")
                        .setCountry("Россия")
                        .setCity("Москва")
                        .setStreet("Сухого")
                        .setBuilding("1")
                        .setFloor("1")
                        .setFlat("3")
                        .setRegionId("213")
                        .setRecipient("Антон Николаевич Старенко")
                        .setPhone("+79991118877")
                        .setEmail("starenko@mail.nowhere")
                        .build());

        final List<Preset> presets = presetService.getPresets(DEFAULT_YANDEX_UID);

        assertThat(
                presets,
                empty()
        );

        // пресеты должны отфильтроваться через бб
        final List<String> deletedPresets = deletedPresetsDao.getDeletedPresets(DEFAULT_YANDEX_UID);

        assertThat(deletedPresets, containsInAnyOrder(equalTo(id1), equalTo(id2)));


        // теперь пресеты должны отфильтроваться через deleted_presets
        final List<Preset> presets2 = presetService.getPresets(DEFAULT_UID);

        assertThat(
                presets2,
                empty()
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotFilterPersaddress67PresetsFromDatasyncIfUserNoAuthButSamePhone() {
        marketDataSyncClient.saveNewAddress(
                DEFAULT_YANDEX_UID,
                MarketDataSyncAddress.builder()
                        .setId("10000")
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

        marketDataSyncClient.saveNewAddress(
                DEFAULT_YANDEX_UID,
                MarketDataSyncAddress.builder()
                        .setId("10001")
                        .setCountry("Россия")
                        .setCity("Москва")
                        .setStreet("Сухого")
                        .setBuilding("1")
                        .setFloor("1")
                        .setFlat("3")
                        .setRegionId("213")
                        .setRecipient("Дмитрий Владимирович Селезнев")
                        .setPhone("+79507778899")
                        .setEmail("starenko@mail.nowhere")
                        .build());

        final List<Preset> presets = presetService.getPresets(DEFAULT_YANDEX_UID);

        assertThat(
                presets,
                hasSize(2)
        );
    }

    @Test
    public void shouldNotFilterPersaddress67PresetsFromDatabase() {
        mockBlackboxResponse(DEFAULT_UID, BlackboxUserFactory.builder()
                .addEmail("not_seleznev@mail.nowhere"));

        presetService.addPreset(
                DEFAULT_UID,
                Preset.builder()
                        .setAddress(AddressFactory.tolstogoStreet().build())
                        .setContact(ContactFactory.sampleBuilder()
                                .setEmail("seleznev@mail.nowhere").build())
                        .build(),
                null
        );

        presetService.addPreset(
                DEFAULT_UID,
                Preset.builder()
                        .setAddress(AddressFactory.grouzinskayaStreet().build())
                        .setContact(ContactFactory.sampleBuilder()
                                .setEmail("starenko@mail.nowhere").build())
                        .build(),
                null
        );

        final List<Preset> presets = presetService.getPresets(DEFAULT_UID);

        assertThat(
                presets,
                hasSize(2)
        );
    }

    @Test
    public void shouldNotFilterPersaddress67PresetsFromDatasyncIfEmailMatch() {
        mockBlackboxResponse(DEFAULT_UID, BlackboxUserFactory.builder()
                .addEmail("seleznev@mail.nowhere"));

        // часть адресов в пг часть в датасинке
        marketDataSyncClient.saveNewAddress(
                DEFAULT_UID,
                MarketDataSyncAddress.builder()
                        .setId("10000")
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

        marketDataSyncClient.saveNewAddress(
                DEFAULT_UID,
                MarketDataSyncAddress.builder()
                        .setId("10001")
                        .setCountry("Россия")
                        .setCity("Москва")
                        .setStreet("Сухого")
                        .setBuilding("1")
                        .setFloor("1")
                        .setFlat("3")
                        .setRegionId("213")
                        .setRecipient("Антон Николаевич Старенко")
                        .setPhone("+79991118877")
                        .setEmail("starenko@mail.nowhere")
                        .build());

        final List<Preset> presets = presetService.getPresets(DEFAULT_UID);

        assertThat(
                presets,
                hasSize(1)
        );
    }


    @Test
    public void shouldSkipDatasyncPresetsIfInternalDatabaseHasSamePreset() {
        marketDataSyncClient.saveNewAddress(
                DEFAULT_UID,
                MarketDataSyncAddress.builder()
                        .setId("10000")
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

        presetService.addPreset(
                DEFAULT_UID,
                Preset.builder()
                        .setId(new ObjectKey("10000"))
                        .setContact(Contact.builder()
                                .setRecipient("Дмитрий Владимирович Селезнев")
                                .setPhoneNum("+79507778899")
                                .setEmail("seleznev@mail.nowhere")
                                .build())
                        .setAddress(Address.builder()
                                .setCountry("Россия")
                                .setCity("Воронеж")
                                .setStreet("Академика Сахарова")
                                .setBuilding("11к6")
                                .setFloor("5")
                                .setRoom("22")
                                .setRegionId(193)
                                .build())
                        .build(), "source");

        final List<Preset> presets = presetService.getPresets(DEFAULT_UID);

        assertThat(
                presets,
                contains(
                        hasProperty("id", equalTo(new ObjectKey("10000")))
                )
        );
    }

    @Test
    public void shouldFilterDeletedPresets() {
        marketDataSyncClient.saveNewAddress(
                DEFAULT_UID,
                MarketDataSyncAddress.builder()
                        .setId("10000")
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

        presetService.deletePreset(
                DEFAULT_UID,
                new ObjectKey("10000"));

        final List<Preset> presets = presetService.getPresets(DEFAULT_UID);

        assertThat(
                presets,
                empty()
        );
    }


    @SuppressWarnings("unchecked")
    @Test
    public void shouldPreferAddressFromInternalDatabaseAfterUpdate() {
        marketDataSyncClient.saveNewAddress(
                DEFAULT_UID,
                MarketDataSyncAddress.builder()
                        .setId("10000")
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
                        .build()
        );

        presetService.updatePreset(
                DEFAULT_UID,
                Preset.builder()
                        .setId(new ObjectKey("10000"))
                        .setContact(Contact.builder()
                                .setRecipient("Дмитрий Владимирович Селезнев")
                                .setPhoneNum("+79507778899")
                                .setEmail("seleznev@mail.nowhere")
                                .build())
                        .setAddress(Address.builder()
                                .setCountry("Россия")
                                .setCity("Воронеж")
                                .setStreet("Александра Твардовского")
                                .setBuilding("6")
                                .setFloor("1")
                                .setRoom("67")
                                .setRegionId(111)
                                .build())
                        .build(),
                "source"
        );

        final List<Preset> presets = presetService.getPresets(DEFAULT_UID);

        assertThat(
                presets,
                contains(
                        allOf(
                                hasProperty("id", equalTo(new ObjectKey("10000"))),
                                hasProperty("address",
                                        allOf(
                                                hasProperty("street", equalTo("Александра Твардовского")),
                                                hasProperty("country", equalTo("Россия")),
                                                hasProperty("city", equalTo("Воронеж")),
                                                hasProperty("street", equalTo("Александра Твардовского")),
                                                hasProperty("building", equalTo("6")),
                                                hasProperty("floor", equalTo("1")),
                                                hasProperty("room", equalTo("67")),
                                                hasProperty("regionId", equalTo(111))
                                        )
                                ),
                                hasProperty("contact",
                                        allOf(
                                                hasProperty("recipient", equalTo("Дмитрий Владимирович Селезнев")),
                                                hasProperty("phoneNum", equalTo("+79507778899")),
                                                hasProperty("email", equalTo("seleznev@mail.nowhere"))
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void shouldGetAddressWithoutStatusNotFound2() {
        presetService.addAddress(DEFAULT_UID,
                Address.builder()
                        .setCountry("Россия")
                        .setCity("Воронеж")
                        .setStreet(TOLSTOGO_STREET)
                        .setBuilding("6")
                        .setFloor("1")
                        .setRoom("67")
                        .setRegionId(111)
                        .setGeocoderStatus(GeocoderStatus.NOT_FOUND)
                        .build(), "source");

        presetService.addAddress(DEFAULT_UID,
                Address.builder()
                        .setCountry("Россия")
                        .setCity("Воронеж")
                        .setStreet(TOLSTOGO_STREET)
                        .setBuilding("6")
                        .setFloor("12")
                        .setRoom("67")
                        .setRegionId(111)
                        .setGeocoderStatus(GeocoderStatus.INIT)
                        .build(), "source");

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        List<Address> addressList = addresses
                .stream()
                .filter(a -> !GeocoderStatus.NOT_FOUND.equals(a.getGeocoderStatus()))
                .collect(Collectors.toList());
        assertEquals(1, addressList.size());
    }

    @Test
    public void shouldFilterIncorrectDataSyncAddresses() {
        marketDataSyncClient.saveNewAddress(
                DEFAULT_UID,
                MarketDataSyncAddress.builder()
                        .setId("10000")
                        .setCountry("Россия")
                        .setCity("Воронеж")
                        .setStreet("Академика Сахарова")
                        .setBuilding("11к6")
                        .build());
        marketDataSyncClient.saveNewAddress(
                DEFAULT_UID,
                MarketDataSyncAddress.builder()
                        .setId("10001")
                        .setCountry("Россия")
                        .setStreet("Академика Сахарова")
                        .setBuilding("11к6")
                        .build());
        marketDataSyncClient.saveNewAddress(
                DEFAULT_UID,
                MarketDataSyncAddress.builder()
                        .setId("10002")
                        .setCountry("Россия")
                        .setCity("Воронеж")
                        .setStreet("Академика Сахарова")
                        .build());

        marketDataSyncClient.saveNewAddress(
                DEFAULT_UID,
                MarketDataSyncAddress.builder()
                        .setId("10003")
                        .setAddressLine("Address Line")
                        .build());

        List<Address> addresses = presetService.getAddresses(DEFAULT_UID);
        assertEquals(1, addresses.size());
    }

    private void mockBlackboxResponse(Identity identity, BlackboxUserFactory builder) {
        mockBlackboxResponse(identity, builder, 0);
    }

    private void mockBlackboxResponse(Identity identity, BlackboxUserFactory builder, long responseTime) {
        if (identity.getType() == Identity.Type.UID) {
            given(blackboxRestTemplate.getForObject(any(), eq(UserInfoResponse.class)))
                    .willAnswer(answer -> {
                        if (responseTime > 0) {
                            Thread.sleep(responseTime);
                        }
                        return builder.build();
                    });
            given(
                    userInfoService.resolve(eq((long) identity.getValue())))
                    .willReturn(new Uid((long) identity.getValue(), UidType.PUID)
                    );
        } else {
            throw new RuntimeException("should not called for this type of identity");
        }
    }
}
