package ru.yandex.market.logistics.lrm.tasks.returns;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.base.EventPayload;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.dto.TplClientDto;
import ru.yandex.market.logistics.les.dto.TplDimensionsDto;
import ru.yandex.market.logistics.les.dto.TplLocationDto;
import ru.yandex.market.logistics.les.dto.TplRequestIntervalDto;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressItemDto;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressReasonType;
import ru.yandex.market.logistics.les.tpl.TplReturnAtClientAddressCreateRequestEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnIdPayload;
import ru.yandex.market.logistics.lrm.queue.processor.CreateClientCourierReturnProcessor;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.DetachedTypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.EntityMetaService;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientItemMeta;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta.Address;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta.Client;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta.Interval;
import ru.yandex.market.logistics.lrm.service.personal.model.PersonalDataKey;
import ru.yandex.market.personal.client.api.DefaultPersonalMultiTypesRetrieveApi;
import ru.yandex.market.personal.client.model.CommonType;
import ru.yandex.market.personal.client.model.CommonTypeEnum;
import ru.yandex.market.personal.client.model.FullName;
import ru.yandex.market.personal.client.model.GpsCoordV2;
import ru.yandex.market.personal.client.model.MultiTypeRetrieveRequestItem;
import ru.yandex.market.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.personal.client.model.PersonalMultiTypeRetrieveRequest;
import ru.yandex.market.personal.client.model.PersonalMultiTypeRetrieveResponse;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lrm.config.LocalsConfiguration.TEST_UUID;
import static ru.yandex.market.logistics.personal.model.Address.COMMENT_KEY;
import static ru.yandex.market.logistics.personal.model.Address.FLOOR_KEY;
import static ru.yandex.market.logistics.personal.model.Address.HOUSE_KEY;
import static ru.yandex.market.logistics.personal.model.Address.INTERCOM_KEY;
import static ru.yandex.market.logistics.personal.model.Address.PORCH_KEY;
import static ru.yandex.market.logistics.personal.model.Address.ROOM_KEY;
import static ru.yandex.market.logistics.personal.model.Address.SETTLEMENT_KEY;
import static ru.yandex.market.logistics.personal.model.Address.STREET_KEY;

@DisplayName("Создание клиентского возврата курьером в TPL")
class CreateClientCourierReturnProcessorTest extends AbstractIntegrationYdbTest {

    private static final long RETURN_ID = 1;
    private static final long ITEM_ID = 10;
    private static final Instant NOW = Instant.parse("2021-12-11T10:09:08.00Z");

    @Autowired
    private EntityMetaTableDescription metaTableDescription;
    @Autowired
    private EntityMetaService metaService;
    @Autowired
    private CreateClientCourierReturnProcessor processor;
    @Autowired
    private LesProducer lesProducer;
    @Autowired
    private DefaultPersonalMultiTypesRetrieveApi personalMultiTypesRetrieveApi;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(metaTableDescription);
    }

    @BeforeEach
    void setup() {
        clock.setFixed(NOW, DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(lesProducer, personalMultiTypesRetrieveApi);
    }

    @Test
    @DisplayName("Минимальный набор данных")
    @DatabaseSetup("/database/tasks/returns/create-client-courier-tpl/before/minimal.xml")
    void minimal() {
        metaService.save(
            new DetachedTypedEntity(EntityType.RETURN, RETURN_ID),
            CourierClientReturnMeta.builder().build()
        );
        metaService.save(
            new DetachedTypedEntity(EntityType.RETURN_ITEM, ITEM_ID),
            CourierClientItemMeta.builder().build()
        );

        execute();

        var payload = new TplReturnAtClientAddressCreateRequestEvent(
            "test-request-id/1",
            "order-external-id",
            "1",
            "return-external-id",
            null,
            null,
            List.of(emptyItem()),
            null,
            null
        );

        verify(lesProducer).send(event(payload), OUT_LES_QUEUE);
    }

    @Test
    @DisplayName("Данные товара")
    @DatabaseSetup("/database/tasks/returns/create-client-courier-tpl/before/item.xml")
    void itemMeta() {
        metaService.save(
            new DetachedTypedEntity(EntityType.RETURN, RETURN_ID),
            CourierClientReturnMeta.builder().build()
        );
        metaService.save(
            new DetachedTypedEntity(EntityType.RETURN_ITEM, ITEM_ID),
            CourierClientItemMeta.builder()
                .name("item-name")
                .categoryName("item-category-name")
                .description("item-description")
                .previewPhotoUrl("item-preview-url")
                .itemDetailsUrl("item-details-url")
                .clientPhotoUrls(List.of("item-client-photo"))
                .dimensions(
                    CourierClientItemMeta.Dimensions.builder()
                        .weight(100L)
                        .length(200L)
                        .width(300L)
                        .height(400L)
                        .build()
                )
                .buyerPrice(new BigDecimal("123.45"))
                .clientPhotoUrls(List.of("item-photo-url"))
                .build()
        );

        execute();

        var payload = new TplReturnAtClientAddressCreateRequestEvent(
            "test-request-id/1",
            "order-external-id",
            "1",
            "return-external-id",
            null,
            null,
            List.of(
                new TplReturnAtClientAddressItemDto(
                    200L,
                    "item-vendor-code",
                    "item-name",
                    "item-category-name",
                    "item-description",
                    "item-preview-url",
                    "item-details-url",
                    new TplDimensionsDto(100L, 200L, 300L, 400L),
                    TplReturnAtClientAddressReasonType.BAD_QUALITY,
                    "item-return-reason",
                    new BigDecimal("123.45"),
                    List.of("item-photo-url")
                )
            ),
            null,
            null
        );

        verify(lesProducer).send(event(payload), OUT_LES_QUEUE);
    }

    @Test
    @DisplayName("Данные возврата")
    @DatabaseSetup("/database/tasks/returns/create-client-courier-tpl/before/return.xml")
    void returnMeta() {
        PersonalMultiTypeRetrieveRequest request = personalRequestWithMinimalAddress();
        when(personalMultiTypesRetrieveApi.v1MultiTypesRetrievePost(request))
            .thenReturn(personalResponseWithMinimalAddress());
        metaService.save(
            new DetachedTypedEntity(EntityType.RETURN, RETURN_ID),
            CourierClientReturnMeta.builder()
                .deliveryServiceId(100L)
                .client(
                    Client.builder()
                        .fullName("client full name")
                        .phone("8(999)123-45-67")
                        .email("client-email")
                        .personalFullNameId("personal-client-full-name-id")
                        .personalPhoneId("personal-phone-id")
                        .build()
                )
                .address(
                    Address.builder()
                        .city("address-city")
                        .street("address-street")
                        .house("address-house")
                        .entrance("address-entrance")
                        .apartment("address-apartment")
                        .floor("address-floor")
                        .entryPhone("address-entry-phone")
                        .lat(new BigDecimal("12.34"))
                        .lon(new BigDecimal("56.78"))
                        .comment("address-comment")
                        .personalAddressId("personal-address-id")
                        .personalGpsCoordId("personal-gps-id")
                        .build()
                )
                .interval(
                    Interval.builder()
                        .dateFrom(LocalDate.of(2022, 3, 4))
                        .timeFrom(LocalTime.of(10, 11))
                        .dateTo(LocalDate.of(2022, 3, 4))
                        .timeTo(LocalTime.of(11, 12))
                        .build()
                )
                .build()
        );
        metaService.save(
            new DetachedTypedEntity(EntityType.RETURN_ITEM, ITEM_ID),
            CourierClientItemMeta.builder().build()
        );

        execute();

        var payload = new TplReturnAtClientAddressCreateRequestEvent(
            "test-request-id/1",
            "order-external-id",
            "1",
            "return-external-id",
            100L,
            new TplRequestIntervalDto(
                LocalDate.of(2022, 3, 4),
                LocalTime.of(10, 11),
                LocalDate.of(2022, 3, 4),
                LocalTime.of(11, 12)
            ),
            List.of(emptyItem()),
            new TplLocationDto(
                "address-city",
                "address-street",
                "address-house",
                "address-entrance",
                "address-apartment",
                "address-floor",
                "address-entry-phone",
                new BigDecimal("12.34"),
                new BigDecimal("56.78"),
                "address-comment",
                "personal-address-id",
                "personal-gps-id"
            ),
            new TplClientDto(
                "client full name", // значение из Personal совпадает с оригиналом
                "client-email", // не заменено, т.к. нет id
                "+79991234567", // значение из Personal совпадает с оригиналом (в нормализованном виде)
                "personal-client-full-name-id",
                null,
                "personal-phone-id"
            )
        );

        verify(lesProducer).send(event(payload), OUT_LES_QUEUE);
        verify(personalMultiTypesRetrieveApi).v1MultiTypesRetrievePost(request);
    }

    @Test
    @DisplayName("Замена на значения из Personal: нет оригинальных данных")
    @DatabaseSetup("/database/tasks/returns/create-client-courier-tpl/before/return.xml")
    void noOriginals() {
        PersonalMultiTypeRetrieveRequest request = personalRequest();
        when(personalMultiTypesRetrieveApi.v1MultiTypesRetrievePost(request)).thenReturn(personalResponse());
        saveToMetaExecuteAndVerifyEventSentToTpl(
            Client.builder()
                .personalFullNameId("personal-client-full-name-id")
                .personalEmailId("personal-client-email-id")
                .personalPhoneId("personal-client-phone-id")
                .build(),
            new TplClientDto(
                "client full name",
                "client email",
                "client phone",
                "personal-client-full-name-id",
                "personal-client-email-id",
                "personal-client-phone-id"
            ),
            Address.builder()
                .personalAddressId("personal-address-id")
                .personalGpsCoordId("personal-gps-id")
                .build(),
            new TplLocationDto(
                "client city",
                "client street",
                "client house",
                "client porch",
                "client room",
                "1",
                "client intercom",
                new BigDecimal("12.34"),
                new BigDecimal("56.78"),
                "client comment",
                "personal-address-id",
                "personal-gps-id"
            )
        );
        verify(personalMultiTypesRetrieveApi).v1MultiTypesRetrievePost(request);
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Замена на значения из Personal: нет ни оригинальных данных ни зашифрованных")
    @DatabaseSetup("/database/tasks/returns/create-client-courier-tpl/before/return.xml")
    void noPersonalIdAnNoOriginals(String ignored, Client clientToSave, TplClientDto clientSentToTpl) {
        saveToMetaExecuteAndVerifyEventSentToTpl(clientToSave, clientSentToTpl, null, null);
    }

    @Nonnull
    private static Stream<Arguments> noPersonalIdAnNoOriginals() {
        return Stream.of(
            Arguments.of(
                "Клиент без полей",
                Client.builder().build(),
                new TplClientDto(null, null, null, null, null, null)
            ),
            Arguments.of(
                "Клиент отсутствует",
                null,
                null
            )
        );
    }

    @MethodSource("metaWithoutOriginals")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Замена на значения из Personal: нет значения в Personal, нет оригиналов")
    @DatabaseSetup("/database/tasks/returns/create-client-courier-tpl/before/return.xml")
    void noValueInPersonalAndNoOriginals(
        CommonTypeEnum type, Function<String, CourierClientReturnMeta> metaBuilder
    ) {
        when(personalMultiTypesRetrieveApi.v1MultiTypesRetrievePost(any()))
            .thenReturn(new PersonalMultiTypeRetrieveResponse().items(List.of()));
        String id = "personal-id";
        metaService.save(
            new DetachedTypedEntity(EntityType.RETURN, RETURN_ID),
            metaBuilder.apply(id)
        );
        metaService.save(
            new DetachedTypedEntity(EntityType.RETURN_ITEM, ITEM_ID),
            CourierClientItemMeta.builder().build()
        );

        PersonalDataKey personalDataKey = new PersonalDataKey(type, id);
        softly.assertThatThrownBy(this::execute)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Value not found in Personal: %s".formatted(personalDataKey));

        verify(personalMultiTypesRetrieveApi).v1MultiTypesRetrievePost(
            getPersonalRequest(getRequestItem(type, id))
        );
    }

    @Nonnull
    private static Stream<Arguments> metaWithoutOriginals() {
        return Stream.of(
            Arguments.of(
                CommonTypeEnum.FULL_NAME,
                (Function<String, CourierClientReturnMeta>) id -> CourierClientReturnMeta.builder()
                    .client(Client.builder().personalFullNameId(id).build())
                    .build()
            ),
            Arguments.of(
                CommonTypeEnum.ADDRESS,
                (Function<String, CourierClientReturnMeta>) id -> CourierClientReturnMeta.builder()
                    .address(Address.builder().personalAddressId(id).build())
                    .build()
            )
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Замена на значения из Personal: нет значения в Personal, только опциональные поля")
    @DatabaseSetup("/database/tasks/returns/create-client-courier-tpl/before/return.xml")
    void noValueInPersonalOnlyOptionalFields(
        CommonTypeEnum type,
        Function<String, List<MultiTypeRetrieveResponseItem>> responseItemsBuilder,
        Function<String, Address> addressBuilder,
        BiConsumer<TplLocationDto, String> tplLocationDtoBuilder
    ) {
        String id = "personal-id";
        when(personalMultiTypesRetrieveApi.v1MultiTypesRetrievePost(any()))
            .thenReturn(new PersonalMultiTypeRetrieveResponse().items(responseItemsBuilder.apply(id)));

        TplLocationDto tplLocationDto = new TplLocationDto();
        tplLocationDtoBuilder.accept(tplLocationDto, id);
        saveToMetaExecuteAndVerifyEventSentToTpl(null, null, addressBuilder.apply(id), tplLocationDto);

        verify(personalMultiTypesRetrieveApi).v1MultiTypesRetrievePost(
            getPersonalRequest(getRequestItem(type, id))
        );
    }

    @Nonnull
    private static Stream<Arguments> noValueInPersonalOnlyOptionalFields() {
        return Stream.of(
            Arguments.of(
                CommonTypeEnum.ADDRESS,
                (Function<String, List<MultiTypeRetrieveResponseItem>>) id -> List.of(
                    new MultiTypeRetrieveResponseItem()
                        .type(CommonTypeEnum.ADDRESS)
                        .id(id)
                        .value(new CommonType().address(Map.of(SETTLEMENT_KEY, "clint city")))
                ),
                (Function<String, Address>) id -> Address.builder().personalAddressId(id).build(),
                (BiConsumer<TplLocationDto, String>) (dto, id) -> {
                    dto.setPersonalAddressId(id);
                    dto.setCity("clint city");
                }
            ),
            Arguments.of(
                CommonTypeEnum.GPS,
                (Function<String, List<MultiTypeRetrieveResponseItem>>) id -> List.of(),
                (Function<String, Address>) id -> Address.builder().personalGpsCoordId(id).build(),
                (BiConsumer<TplLocationDto, String>) TplLocationDto::setPersonalGpsCoordId
            )
        );
    }

    @MethodSource("metaWithoutOriginals")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Замена на значения из Personal: исключение при походе в Personal, нет оригинальных данных")
    @DatabaseSetup("/database/tasks/returns/create-client-courier-tpl/before/return.xml")
    void exceptionWhenGoingToPersonalAndNoOriginals(
        CommonTypeEnum type, Function<String, CourierClientReturnMeta> metaBuilder
    ) {
        when(personalMultiTypesRetrieveApi.v1MultiTypesRetrievePost(any()))
            .thenThrow(new RuntimeException("Some exception"));
        String id = "personal-id";
        metaService.save(
            new DetachedTypedEntity(EntityType.RETURN, RETURN_ID),
            metaBuilder.apply(id)
        );
        metaService.save(
            new DetachedTypedEntity(EntityType.RETURN_ITEM, ITEM_ID),
            CourierClientItemMeta.builder().build()
        );

        PersonalDataKey personalDataKey = new PersonalDataKey(type, id);
        softly.assertThatThrownBy(this::execute)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Value not found in Personal: %s".formatted(personalDataKey));

        verify(personalMultiTypesRetrieveApi).v1MultiTypesRetrievePost(
            getPersonalRequest(getRequestItem(type, id))
        );
    }

    @Test
    @DisplayName("Замена на значения из Personal: исключение при походе в Personal, есть оригинальные данные")
    @DatabaseSetup("/database/tasks/returns/create-client-courier-tpl/before/return.xml")
    void exceptionWhenGoingToPersonalAndThereAreOriginals() {
        when(personalMultiTypesRetrieveApi.v1MultiTypesRetrievePost(any()))
            .thenThrow(new RuntimeException("Some exception"));
        saveToMetaExecuteAndVerifyEventSentToTpl(
            Client.builder()
                .phone("client phone")
                .personalPhoneId("personal-client-phone-id")
                .build(),
            new TplClientDto(
                null,
                null,
                "client phone", // фоллбэк на оригинал
                null,
                null,
                "personal-client-phone-id"
            ),
            Address.builder()
                .city("clint city")
                .personalAddressId("personal-address-id")
                .build(),
            new TplLocationDto(
                "clint city", // фоллбэк на оригинал
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "personal-address-id",
                null
            )
        );
        verify(personalMultiTypesRetrieveApi).v1MultiTypesRetrievePost(
            getPersonalRequest(
                getRequestItem(CommonTypeEnum.PHONE, "personal-client-phone-id"),
                getRequestItem(CommonTypeEnum.ADDRESS, "personal-address-id")
            )
        );
    }

    @Nonnull
    private PersonalMultiTypeRetrieveRequest getPersonalRequest(MultiTypeRetrieveRequestItem... requestItem) {
        return new PersonalMultiTypeRetrieveRequest().items(List.of(requestItem));
    }

    @Nonnull
    private MultiTypeRetrieveRequestItem getRequestItem(CommonTypeEnum type, String id) {
        return new MultiTypeRetrieveRequestItem()
            .type(type)
            .id(id);
    }

    private void saveToMetaExecuteAndVerifyEventSentToTpl(
        Client clientToSave,
        TplClientDto clientSentToTpl,
        Address addressToSave,
        TplLocationDto addressSentToTpl
    ) {
        metaService.save(
            new DetachedTypedEntity(EntityType.RETURN, RETURN_ID),
            CourierClientReturnMeta.builder()
                .deliveryServiceId(100L)
                .client(clientToSave)
                .address(addressToSave)
                .build()
        );
        metaService.save(
            new DetachedTypedEntity(EntityType.RETURN_ITEM, ITEM_ID),
            CourierClientItemMeta.builder().build()
        );

        execute();

        var payload = new TplReturnAtClientAddressCreateRequestEvent(
            "test-request-id/1",
            "order-external-id",
            "1",
            "return-external-id",
            100L,
            null,
            List.of(emptyItem()),
            addressSentToTpl,
            clientSentToTpl
        );

        verify(lesProducer).send(event(payload), OUT_LES_QUEUE);
    }

    @Nonnull
    private PersonalMultiTypeRetrieveRequest personalRequest() {
        return new PersonalMultiTypeRetrieveRequest().items(
            List.of(
                getRequestItem(CommonTypeEnum.FULL_NAME, "personal-client-full-name-id"),
                getRequestItem(CommonTypeEnum.EMAIL, "personal-client-email-id"),
                getRequestItem(CommonTypeEnum.PHONE, "personal-client-phone-id"),
                getRequestItem(CommonTypeEnum.ADDRESS, "personal-address-id"),
                getRequestItem(CommonTypeEnum.GPS, "personal-gps-id")
            )
        );
    }

    @Nonnull
    private PersonalMultiTypeRetrieveRequest personalRequestWithMinimalAddress() {
        return new PersonalMultiTypeRetrieveRequest().items(
            List.of(
                getRequestItem(CommonTypeEnum.FULL_NAME, "personal-client-full-name-id"),
                getRequestItem(CommonTypeEnum.PHONE, "personal-phone-id"),
                getRequestItem(CommonTypeEnum.ADDRESS, "personal-address-id"),
                getRequestItem(CommonTypeEnum.GPS, "personal-gps-id")
            )
        );
    }

    @Nonnull
    private PersonalMultiTypeRetrieveResponse personalResponse() {
        return new PersonalMultiTypeRetrieveResponse().items(
            List.of(
                new MultiTypeRetrieveResponseItem()
                    .type(CommonTypeEnum.FULL_NAME)
                    .id("personal-client-full-name-id")
                    .value(new CommonType().fullName(
                        new FullName().surname("client").forename("full").patronymic("name")
                    )),
                new MultiTypeRetrieveResponseItem()
                    .type(CommonTypeEnum.EMAIL)
                    .id("personal-client-email-id")
                    .value(new CommonType().email("client email")),
                new MultiTypeRetrieveResponseItem()
                    .type(CommonTypeEnum.PHONE)
                    .id("personal-client-phone-id")
                    .value(new CommonType().phone("client phone")),
                new MultiTypeRetrieveResponseItem()
                    .type(CommonTypeEnum.ADDRESS)
                    .id("personal-address-id")
                    .value(new CommonType().address(
                        Map.of(
                            SETTLEMENT_KEY, "client city",
                            STREET_KEY, "client street",
                            HOUSE_KEY, "client house",
                            PORCH_KEY, "client porch",
                            ROOM_KEY, "client room",
                            FLOOR_KEY, "1",
                            INTERCOM_KEY, "client intercom",
                            COMMENT_KEY, "client comment"
                        ))
                    ),
                new MultiTypeRetrieveResponseItem()
                    .type(CommonTypeEnum.GPS)
                    .id("personal-gps-id")
                    .value(new CommonType().gps(
                        new GpsCoordV2().latitude("12.34").longitude("56.78"))
                    )
            )
        );
    }

    @Nonnull
    private PersonalMultiTypeRetrieveResponse personalResponseWithMinimalAddress() {
        return new PersonalMultiTypeRetrieveResponse().items(
            List.of(
                new MultiTypeRetrieveResponseItem()
                    .type(CommonTypeEnum.FULL_NAME)
                    .id("personal-client-full-name-id")
                    .value(new CommonType().fullName(
                        new FullName().surname("client").forename("full").patronymic("name")
                    )),
                new MultiTypeRetrieveResponseItem()
                    .type(CommonTypeEnum.PHONE)
                    .id("personal-phone-id")
                    .value(new CommonType().phone("+79991234567")),
                new MultiTypeRetrieveResponseItem()
                    .type(CommonTypeEnum.ADDRESS)
                    .id("personal-address-id")
                    .value(new CommonType().address(Map.of("settlement", "clint city"))),
                new MultiTypeRetrieveResponseItem()
                    .type(CommonTypeEnum.GPS)
                    .id("personal-gps-id")
                    .value(new CommonType().gps(new GpsCoordV2().latitude("12.34").longitude("56.78")))
            )
        );
    }

    @Nonnull
    private TplReturnAtClientAddressItemDto emptyItem() {
        return new TplReturnAtClientAddressItemDto(
            200L,
            "item-vendor-code",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    @Nonnull
    private Event event(EventPayload payload) {
        return new Event(
            SOURCE_FOR_LES,
            TEST_UUID,
            NOW.toEpochMilli(),
            "TPL_RETURN_AT_CLIENT_ADDRESS_CREATE_REQUEST",
            payload,
            ""
        );
    }

    private void execute() {
        processor.execute(ReturnIdPayload.builder().returnId(RETURN_ID).build());
    }

}
