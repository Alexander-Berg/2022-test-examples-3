package ru.yandex.market.logistics.lrm.jobs;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.DetachedTypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.EntityMetaService;
import ru.yandex.market.logistics.lrm.service.meta.TypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientItemMeta;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta.Address.AddressBuilder;
import ru.yandex.market.personal.client.api.DefaultPersonalMultiTypesStoreApi;
import ru.yandex.market.personal.client.model.CommonType;
import ru.yandex.market.personal.client.model.FullName;
import ru.yandex.market.personal.client.model.GpsCoordV2;
import ru.yandex.market.personal.client.model.MultiTypeStoreResponseItem;
import ru.yandex.market.personal.client.model.PersonalMultiTypeStoreRequest;
import ru.yandex.market.personal.client.model.PersonalMultiTypeStoreResponse;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta.Client;
import static ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta.Client.ClientBuilder;
import static ru.yandex.market.logistics.personal.model.Address.COMMENT_KEY;
import static ru.yandex.market.logistics.personal.model.Address.FLOOR_KEY;
import static ru.yandex.market.logistics.personal.model.Address.HOUSE_KEY;
import static ru.yandex.market.logistics.personal.model.Address.INTERCOM_KEY;
import static ru.yandex.market.logistics.personal.model.Address.PORCH_KEY;
import static ru.yandex.market.logistics.personal.model.Address.ROOM_KEY;
import static ru.yandex.market.logistics.personal.model.Address.SETTLEMENT_KEY;
import static ru.yandex.market.logistics.personal.model.Address.STREET_KEY;


@DisplayName("Зашифровка незашифрованного в ydb")
class YdbPersonalDataEncodeExecutorTest extends AbstractIntegrationYdbTest {

    public static final TypedEntity RETURN_TYPED_ENTITY_MOCK = new DetachedTypedEntity(EntityType.RETURN, 1L);
    public static final TypedEntity ITEM_TYPED_ENTITY_MOCK = new DetachedTypedEntity(EntityType.RETURN_ITEM, 10L);

    @Autowired
    private YdbPersonalDataEncodeExecutor executor;
    @Autowired
    private EntityMetaService metaService;
    @Autowired
    private DefaultPersonalMultiTypesStoreApi storeApi;
    @Autowired
    private EntityMetaTableDescription entityMetaTable;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTable);
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(storeApi);
    }

    @Test
    @DisplayName("Успех: все параметры")
    void success() {
        metaService.save(
            RETURN_TYPED_ENTITY_MOCK,
            CourierClientReturnMeta.builder()
                .deliveryServiceId(100L)
                .address(
                    fullAddressBuilder().build()
                )
                .client(
                    fullClientBuilder().build()
                )
                .build()
        );
        metaService.save(ITEM_TYPED_ENTITY_MOCK, CourierClientItemMeta.builder().build());
        PersonalMultiTypeStoreRequest storeRequest = new PersonalMultiTypeStoreRequest()
            .validate(false)
            .items(
                List.of(
                    new CommonType().fullName(new FullName().surname("client").forename("full").patronymic("name")),
                    new CommonType().phone("client phone"),
                    new CommonType().email("client email"),
                    fullAddress(),
                    fullGpsCoord()
                )
            );
        when(storeApi.v1MultiTypesStorePost(storeRequest)).thenReturn(
            new PersonalMultiTypeStoreResponse().items(
                List.of(
                    new MultiTypeStoreResponseItem()
                        .id("personal-client-full-name-id")
                        .value(new CommonType().fullName(
                            new FullName().surname("client").forename("full").patronymic("name")
                        )),
                    new MultiTypeStoreResponseItem()
                        .id("personal-client-phone-id")
                        .value(new CommonType().phone("client phone")),
                    new MultiTypeStoreResponseItem()
                        .id("personal-client-email-id")
                        .value(new CommonType().email("client email")),
                    new MultiTypeStoreResponseItem()
                        .id("personal-address-id")
                        .value(fullAddress()),
                    new MultiTypeStoreResponseItem()
                        .id("personal-gps-coord-id")
                        .value(fullGpsCoord())
                )
            )
        );

        executor.execute(null);

        CourierClientReturnMeta returnMeta = metaService.get(RETURN_TYPED_ENTITY_MOCK, CourierClientReturnMeta.class);
        softly.assertThat(returnMeta).isEqualTo(
            CourierClientReturnMeta.builder()
                .deliveryServiceId(100L)
                .address(
                    addressWithIdentifiers(CourierClientReturnMeta.Address.builder())
                )
                .client(
                    clientWithIdentifiers(Client.builder())
                )
                .build()
        );

        verify(storeApi).v1MultiTypesStorePost(storeRequest);
    }

    @Test
    @DisplayName("Успех: все параметры и идентификаторы")
    void successWithIdentifiers() {
        metaService.save(
            RETURN_TYPED_ENTITY_MOCK,
            CourierClientReturnMeta.builder()
                .deliveryServiceId(100L)
                .address(
                    addressWithIdentifiers(fullAddressBuilder())
                )
                .client(
                    clientWithIdentifiers(fullClientBuilder())
                )
                .build()
        );
        metaService.save(ITEM_TYPED_ENTITY_MOCK, CourierClientItemMeta.builder().build());

        executor.execute(null);

        CourierClientReturnMeta returnMeta = metaService.get(RETURN_TYPED_ENTITY_MOCK, CourierClientReturnMeta.class);
        softly.assertThat(returnMeta).isEqualTo(
            CourierClientReturnMeta.builder()
                .deliveryServiceId(100L)
                .address(
                    addressWithIdentifiers(CourierClientReturnMeta.Address.builder())
                )
                .client(
                    clientWithIdentifiers(Client.builder())
                )
                .build()
        );
    }

    @DisplayName("Успех: параметры клиента по-одному")
    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void successClientParametersOneByOne(
        @SuppressWarnings("unused") String displayName,
        Function<ClientBuilder, ClientBuilder> presentClientBuilder,
        Function<CommonType, CommonType> requestItemBuilder,
        String responseItemId,
        BiFunction<ClientBuilder, String, ClientBuilder> resultClientBuilder
    ) {
        metaService.save(
            RETURN_TYPED_ENTITY_MOCK,
            CourierClientReturnMeta.builder()
                .deliveryServiceId(100L)
                .client(
                    presentClientBuilder.apply(Client.builder()).build()
                )
                .build()
        );
        metaService.save(ITEM_TYPED_ENTITY_MOCK, CourierClientItemMeta.builder().build());
        PersonalMultiTypeStoreRequest storeRequest = new PersonalMultiTypeStoreRequest()
            .validate(false)
            .items(
                List.of(
                    requestItemBuilder.apply(new CommonType())
                )
            );
        when(storeApi.v1MultiTypesStorePost(storeRequest)).thenReturn(
            new PersonalMultiTypeStoreResponse().items(
                List.of(
                    new MultiTypeStoreResponseItem()
                        .id(responseItemId)
                        .value(requestItemBuilder.apply(new CommonType()))
                )
            )
        );

        executor.execute(null);

        CourierClientReturnMeta returnMeta = metaService.get(RETURN_TYPED_ENTITY_MOCK, CourierClientReturnMeta.class);
        softly.assertThat(returnMeta).isEqualTo(
            CourierClientReturnMeta.builder()
                .deliveryServiceId(100L)
                .client(
                    resultClientBuilder.apply(Client.builder(), responseItemId).build()
                )
                .build()
        );

        verify(storeApi).v1MultiTypesStorePost(storeRequest);
    }

    @Test
    @DisplayName("Успех: нет параметров")
    void successNoParameters() {
        CourierClientReturnMeta initialReturnMeta = CourierClientReturnMeta.builder()
            .deliveryServiceId(100L)
            .address(
                addressWithIdentifiers(CourierClientReturnMeta.Address.builder())
            )
            .client(
                clientWithIdentifiers(Client.builder())
            )
            .build();
        metaService.save(
            RETURN_TYPED_ENTITY_MOCK,
            initialReturnMeta
        );
        metaService.save(ITEM_TYPED_ENTITY_MOCK, CourierClientItemMeta.builder().build());

        executor.execute(null);

        CourierClientReturnMeta returnMeta = metaService.get(RETURN_TYPED_ENTITY_MOCK, CourierClientReturnMeta.class);
        softly.assertThat(returnMeta).isEqualTo(initialReturnMeta);
    }

    @Nonnull
    private static Stream<Arguments> successClientParametersOneByOne() {
        return Stream.of(
            Arguments.of(
                "fullName -> personalFullNameId",
                (Function<ClientBuilder, ClientBuilder>) builder -> builder.fullName("client full name"),
                (Function<CommonType, CommonType>) commonType -> commonType.fullName(
                    new FullName().surname("client").forename("full").patronymic("name")
                ),
                "personal-client-full-name-id",
                (BiFunction<ClientBuilder, String, ClientBuilder>) ClientBuilder::personalFullNameId
            ),
            Arguments.of(
                "phone -> personalPhoneId",
                (Function<ClientBuilder, ClientBuilder>) builder -> builder.phone("client phone"),
                (Function<CommonType, CommonType>) commonType -> commonType.phone("client phone"),
                "personal-client-phone-id",
                (BiFunction<ClientBuilder, String, ClientBuilder>) ClientBuilder::personalPhoneId
            ),
            Arguments.of(
                "email -> personalEmailId",
                (Function<ClientBuilder, ClientBuilder>) builder -> builder.email("client email"),
                (Function<CommonType, CommonType>) commonType -> commonType.email("client email"),
                "personal-client-email-id",
                (BiFunction<ClientBuilder, String, ClientBuilder>) ClientBuilder::personalEmailId
            )
        );
    }

    @DisplayName("Успех: параметры адреса по-одному")
    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void successAddressParametersOneByOne(
        @SuppressWarnings("unused") String displayName,
        Function<AddressBuilder, AddressBuilder> presentAddressBuilder,
        Function<CommonType, CommonType> requestItemBuilder,
        BiFunction<AddressBuilder, String, AddressBuilder> resultAddressBuilder
    ) {
        String responseItemId = "personal-id";
        metaService.save(
            RETURN_TYPED_ENTITY_MOCK,
            CourierClientReturnMeta.builder()
                .deliveryServiceId(100L)
                .address(
                    presentAddressBuilder.apply(CourierClientReturnMeta.Address.builder()).build()
                )
                .build()
        );
        metaService.save(ITEM_TYPED_ENTITY_MOCK, CourierClientItemMeta.builder().build());
        PersonalMultiTypeStoreRequest storeRequest = new PersonalMultiTypeStoreRequest()
            .validate(false)
            .items(
                List.of(
                    requestItemBuilder.apply(new CommonType())
                )
            );
        when(storeApi.v1MultiTypesStorePost(storeRequest)).thenReturn(
            new PersonalMultiTypeStoreResponse().items(
                List.of(
                    new MultiTypeStoreResponseItem()
                        .id(responseItemId)
                        .value(requestItemBuilder.apply(new CommonType()))
                )
            )
        );

        executor.execute(null);

        CourierClientReturnMeta returnMeta = metaService.get(RETURN_TYPED_ENTITY_MOCK, CourierClientReturnMeta.class);
        softly.assertThat(returnMeta).isEqualTo(
            CourierClientReturnMeta.builder()
                .deliveryServiceId(100L)
                .address(
                    resultAddressBuilder.apply(CourierClientReturnMeta.Address.builder(), responseItemId).build()
                )
                .build()
        );

        verify(storeApi).v1MultiTypesStorePost(storeRequest);
    }

    @Nonnull
    private static Stream<Arguments> successAddressParametersOneByOne() {
        return Stream.of(
            Arguments.of(
                "address.city -> personalAddressId",
                (Function<AddressBuilder, AddressBuilder>) builder -> builder.city("client city"),
                (Function<CommonType, CommonType>) commonType -> commonType.address(
                    Map.of(SETTLEMENT_KEY, "client city")
                ),
                (BiFunction<AddressBuilder, String, AddressBuilder>) AddressBuilder::personalAddressId
            ),
            Arguments.of(
                "address.street -> personalAddressId",
                (Function<AddressBuilder, AddressBuilder>) builder -> builder.street("client street"),
                (Function<CommonType, CommonType>) commonType -> commonType.address(
                    Map.of(STREET_KEY, "client street")
                ),
                (BiFunction<AddressBuilder, String, AddressBuilder>) AddressBuilder::personalAddressId
            ),
            Arguments.of(
                "address.house -> personalAddressId",
                (Function<AddressBuilder, AddressBuilder>) builder -> builder.house("client house"),
                (Function<CommonType, CommonType>) commonType -> commonType.address(
                    Map.of(HOUSE_KEY, "client house")
                ),
                (BiFunction<AddressBuilder, String, AddressBuilder>) AddressBuilder::personalAddressId
            ),
            Arguments.of(
                "address.porch -> personalAddressId",
                (Function<AddressBuilder, AddressBuilder>) builder -> builder.entrance("client porch"),
                (Function<CommonType, CommonType>) commonType -> commonType.address(
                    Map.of(PORCH_KEY, "client porch")
                ),
                (BiFunction<AddressBuilder, String, AddressBuilder>) AddressBuilder::personalAddressId
            ),
            Arguments.of(
                "address.room -> personalAddressId",
                (Function<AddressBuilder, AddressBuilder>) builder -> builder.apartment("client room"),
                (Function<CommonType, CommonType>) commonType -> commonType.address(
                    Map.of(ROOM_KEY, "client room")
                ),
                (BiFunction<AddressBuilder, String, AddressBuilder>) AddressBuilder::personalAddressId
            ),
            Arguments.of(
                "address.floor -> personalAddressId",
                (Function<AddressBuilder, AddressBuilder>) builder -> builder.floor("client floor"),
                (Function<CommonType, CommonType>) commonType -> commonType.address(
                    Map.of(FLOOR_KEY, "client floor")
                ),
                (BiFunction<AddressBuilder, String, AddressBuilder>) AddressBuilder::personalAddressId
            ),
            Arguments.of(
                "address.intercom -> personalAddressId",
                (Function<AddressBuilder, AddressBuilder>) builder -> builder.entryPhone("client intercom"),
                (Function<CommonType, CommonType>) commonType -> commonType.address(
                    Map.of(INTERCOM_KEY, "client intercom")
                ),
                (BiFunction<AddressBuilder, String, AddressBuilder>) AddressBuilder::personalAddressId
            ),
            Arguments.of(
                "address.comment -> personalAddressId",
                (Function<AddressBuilder, AddressBuilder>) builder -> builder.comment("client comment"),
                (Function<CommonType, CommonType>) commonType -> commonType.address(
                    Map.of(COMMENT_KEY, "client comment")
                ),
                (BiFunction<AddressBuilder, String, AddressBuilder>) AddressBuilder::personalAddressId
            ),
            Arguments.of(
                "address.lat -> personalGpsCoordId",
                (Function<AddressBuilder, AddressBuilder>) builder -> builder.lat(new BigDecimal("12.34")),
                (Function<CommonType, CommonType>) commonType -> commonType.gps(
                    new GpsCoordV2().latitude("12.34")
                ),
                (BiFunction<AddressBuilder, String, AddressBuilder>) AddressBuilder::personalGpsCoordId
            ),
            Arguments.of(
                "address.lon -> personalGpsCoordId",
                (Function<AddressBuilder, AddressBuilder>) builder -> builder.lon(new BigDecimal("56.78")),
                (Function<CommonType, CommonType>) commonType -> commonType.gps(
                    new GpsCoordV2().longitude("56.78")
                ),
                (BiFunction<AddressBuilder, String, AddressBuilder>) AddressBuilder::personalGpsCoordId
            )
        );
    }

    @Nonnull
    private CommonType fullAddress() {
        return new CommonType().address(
            Map.of(
                SETTLEMENT_KEY, "client city",
                STREET_KEY, "client street",
                HOUSE_KEY, "client house",
                PORCH_KEY, "client porch",
                ROOM_KEY, "client room",
                FLOOR_KEY, "1",
                INTERCOM_KEY, "client intercom",
                COMMENT_KEY, "client comment"
            )
        );
    }

    @Nonnull
    private CommonType fullGpsCoord() {
        return new CommonType().gps(
            new GpsCoordV2().latitude("12.34").longitude("56.78")
        );
    }

    @Nonnull
    private AddressBuilder fullAddressBuilder() {
        return CourierClientReturnMeta.Address.builder()
            .city("client city")
            .street("client street")
            .house("client house")
            .entrance("client porch")
            .apartment("client room")
            .floor("1")
            .entryPhone("client intercom")
            .lat(new BigDecimal("12.34"))
            .lon(new BigDecimal("56.78"))
            .comment("client comment");
    }

    @Nonnull
    private CourierClientReturnMeta.Address addressWithIdentifiers(AddressBuilder builder) {
        return builder
            .personalAddressId("personal-address-id")
            .personalGpsCoordId("personal-gps-coord-id")
            .build();
    }

    @Nonnull
    private ClientBuilder fullClientBuilder() {
        return Client.builder()
            .fullName("client full name")
            .phone("client phone")
            .email("client email");
    }

    @Nonnull
    private Client clientWithIdentifiers(ClientBuilder builder) {
        return builder
            .personalFullNameId("personal-client-full-name-id")
            .personalPhoneId("personal-client-phone-id")
            .personalEmailId("personal-client-email-id")
            .build();
    }
}
