package ru.yandex.market.logistics.nesu.api.order;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import one.util.streamex.IntStreamEx;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.CredentialsDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.MdsFileDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderLabelDto;
import ru.yandex.market.logistics.lom.model.dto.RecipientDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.FileType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.nesu.api.AbstractApiTest;
import ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics.werewolf.model.entity.LabelInfo;
import ru.yandex.market.logistics.werewolf.model.entity.OrderLabels;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.binaryContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение ярлыка заказа")
@DatabaseSetup("/controller/order/get/data.xml")
@ParametersAreNonnullByDefault
class ApiOrderLabelTest extends AbstractApiTest {

    private static final long ORDER_ID = 100L;
    private static final long SENDER_ID = 11L;
    private static final long SHOP_ID = 1L;
    private static final String TEST_FILE = "controller/api/order/label/test.gif";
    private static final LocalDate SHIPMENT_DATE = LocalDate.of(2020, 12, 1);

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private WwClient wwClient;

    @Autowired
    private Validator validator;

    @BeforeEach
    void setup() {
        authHolder.mockAccess(mbiApiClient, SHOP_ID);
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(lomClient, wwClient);
    }

    @Test
    @DisplayName("Неизвестный заказ")
    void unknownOrder() throws Exception {
        getLabel()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with ids [100]"));

        verifyGetOrder();
    }

    @Test
    @DisplayName("Недоступный сендер")
    void inaccessibleSender() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, SHOP_ID);
        mockLomGetOrder();

        getLabel()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [11]"));

        verifyGetOrder();
    }

    @Test
    @DisplayName("Неактивный сендер")
    void deletedSender() throws Exception {
        mockLomGetOrder(order -> order.setSenderId(12L));

        getLabel()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [12]"));

        verifyGetOrder();
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = "PROCESSING"
    )
    @DisplayName("Заказ в некорректном статусе")
    void incorrectStatus(OrderStatus status) throws Exception {
        mockLomGetOrder(order -> order.setStatus(status));

        getLabel()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER_LABEL] with ids [100]"));

        verifyGetOrder();
    }

    @Test
    @DisplayName("Заказ собственной доставки (fake)")
    void fakeOrder() throws Exception {
        mockLomGetOrder(order -> order.setFake(true));

        getLabel()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER_LABEL] with ids [100]"));

        verifyGetOrder();
    }

    @Test
    @DisplayName("Ярлык не найден")
    void labelNotFound() throws Exception {
        mockLomGetOrder();

        getLabel()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER_LABEL] for order 100"));

        verifyGetOrder();
        verify(lomClient).getLabel(ORDER_ID);
    }

    @Test
    @DisplayName("Неправильный адрес файла")
    void invalidFileUrl() throws Exception {
        mockLomGetOrder();
        mockLomGetLabel("invalid:file");

        getLabel()
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("unknown protocol: invalid"));

        verifyGetOrder();
        verify(lomClient).getLabel(ORDER_ID);
    }

    @Test
    @DisplayName("Получение ярлыка без указания конкретного медиа типа")
    void success() throws Exception {
        mockLomGetOrder();
        mockLomGetLabel(ClassLoader.getSystemResource(TEST_FILE).toString());

        getLabel(MediaType.ALL)
            .andExpect(status().isOk())
            .andExpect(checkHeaders(DocumentFormat.PDF))
            .andExpect(binaryContent(TEST_FILE));

        verifyGetOrder();
        verify(lomClient).getLabel(ORDER_ID);
    }

    @Test
    @DisplayName("Получение ярлыка без указания заголовка Accept")
    void noAcceptHeader() throws Exception {
        mockLomGetOrder();
        mockLomGetLabel(ClassLoader.getSystemResource(TEST_FILE).toString());

        getLabel()
            .andExpect(status().isOk())
            .andExpect(checkHeaders(DocumentFormat.PDF))
            .andExpect(binaryContent(TEST_FILE));

        verifyGetOrder();
        verify(lomClient).getLabel(ORDER_ID);
    }

    @Test
    @DisplayName("Запрошен только неподдерживаемый формат")
    void noSuitableFormat() throws Exception {
        getLabel(MediaType.IMAGE_GIF)
            .andExpect(status().isNotAcceptable())
            .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Запрошен неподдерживаемый формат")
    void noSuitableFormatWithErrorBody() throws Exception {
        getLabel(MediaType.IMAGE_GIF, MediaType.APPLICATION_JSON)
            .andExpect(status().isNotAcceptable())
            .andExpect(errorMessage(
                "Could not resolve format from accept headers [image/gif, application/json]. "
                    + "Supported types: [application/pdf, text/html]"
            ));
    }

    @Test
    @DisplayName("Запрошен только json")
    void acceptedJson() throws Exception {
        getLabel(MediaType.APPLICATION_JSON)
            .andExpect(status().isNotAcceptable())
            .andExpect(errorMessage(
                "Could not resolve format from accept headers [application/json]. "
                    + "Supported types: [application/pdf, text/html]"
            ));
    }

    @Test
    @DisplayName("Явно запрошен PDF")
    void acceptPdf() throws Exception {
        mockLomGetOrder();
        mockLomGetLabel(ClassLoader.getSystemResource(TEST_FILE).toString());

        getLabel(MediaType.APPLICATION_PDF)
            .andExpect(status().isOk())
            .andExpect(checkHeaders(DocumentFormat.PDF))
            .andExpect(binaryContent(TEST_FILE));

        verifyGetOrder();
        verify(lomClient).getLabel(ORDER_ID);
    }

    @Test
    @DisplayName("Явно запрошен HTML")
    void acceptHtml() throws Exception {
        mockLomGetOrder();
        List<LabelInfo> expectedLabelInfo = createLabelInfo();
        mockWwGenerateLabel(expectedLabelInfo, DocumentFormat.HTML);

        getLabel(MediaType.TEXT_HTML)
            .andExpect(status().isOk())
            .andExpect(checkHeaders(DocumentFormat.HTML))
            .andExpect(binaryContent(TEST_FILE));

        verifyGetOrder();
        verifyWwCalled(expectedLabelInfo, DocumentFormat.HTML);
    }

    @Test
    @DisplayName("Заказ собственной доставки (fake), запрошен html")
    void fakeOrderAcceptHtmlOnly() throws Exception {
        mockLomGetOrder(order -> order.setFake(true));

        getLabel(MediaType.TEXT_HTML)
            .andExpect(status().isNotFound())
            .andExpect(content().string(""));

        verifyGetOrder();
    }

    @Test
    @DisplayName("WW ответил ошибкой валидации")
    void wwAnsweredWithValidationError() throws Exception {
        mockLomGetOrder();
        List<LabelInfo> expectedLabelInfo = createLabelInfo();
        String validationErrorMessage = "Following validation errors occurred:\n"
            + "Field: 'labels', message: 'must not be empty'";

        when(wwClient.generateLabels(expectedLabelInfo, DocumentFormat.HTML, PageSize.A6))
            .thenThrow(new HttpTemplateException(
                HttpStatus.BAD_REQUEST.value(),
                String.format("{\"message\":\"%s\"}", validationErrorMessage)
            ));

        getLabel(MediaType.TEXT_HTML, MediaType.APPLICATION_JSON)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(validationErrorMessage));

        verifyGetOrder();
        verifyWwCalled(expectedLabelInfo, DocumentFormat.HTML);
    }

    @Test
    @DisplayName("WW ответил ошибкой валидации, запрошен только html")
    void wwAnsweredWithValidationErrorAcceptsOnlyHtml() throws Exception {
        mockLomGetOrder();
        List<LabelInfo> expectedLabelInfo = createLabelInfo();
        String validationErrorMessage = "Following validation errors occurred:\n"
            + "Field: 'labels', message: 'must not be empty'";

        when(wwClient.generateLabels(expectedLabelInfo, DocumentFormat.HTML, PageSize.A6))
            .thenThrow(new HttpTemplateException(
                HttpStatus.BAD_REQUEST.value(),
                String.format("{\"message\":\"%s\"}", validationErrorMessage)
            ));

        getLabel(MediaType.TEXT_HTML)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(validationErrorMessage));

        verifyGetOrder();
        verifyWwCalled(expectedLabelInfo, DocumentFormat.HTML);
    }

    @Test
    @DisplayName("Несколько грузомест")
    void severalPlaces() throws Exception {
        mockLomGetOrder(
            order -> order.setUnits(List.of(
                OrderDtoFactory.createRootUnit(),
                createPlaceUnit(1, "1"),
                createPlaceUnit(2, "2"),
                createPlaceUnit(3, "3")
            ))
        );
        List<LabelInfo> expectedLabelInfo = createLabelInfo(3);
        mockWwGenerateLabel(expectedLabelInfo, DocumentFormat.HTML);

        getLabel(MediaType.TEXT_HTML)
            .andExpect(status().isOk())
            .andExpect(checkHeaders(DocumentFormat.HTML))
            .andExpect(binaryContent(TEST_FILE));

        verifyGetOrder();
        verifyWwCalled(expectedLabelInfo, DocumentFormat.HTML);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("correctOrdersSource")
    @DisplayName("Варианты допустимых к генерации ярлыков заказов")
    void correctOrders(
        @SuppressWarnings("unused") String displayName,
        UnaryOperator<OrderDto> lomOrderModifier,
        UnaryOperator<LabelInfo.LabelInfoBuilder> labelInfoModifier
    ) throws Exception {
        mockLomGetOrder(lomOrderModifier);
        List<LabelInfo> expectedLabelInfo = createLabelInfo(1, labelInfoModifier);
        mockWwGenerateLabel(expectedLabelInfo, DocumentFormat.HTML);

        getLabel(MediaType.TEXT_HTML)
            .andExpect(status().isOk())
            .andExpect(checkHeaders(DocumentFormat.HTML))
            .andExpect(binaryContent(TEST_FILE));

        verifyGetOrder();
        verifyWwCalled(expectedLabelInfo, DocumentFormat.HTML);
    }

    @Nonnull
    private static Stream<Arguments> correctOrdersSource() {
        return Stream.<Triple<String, UnaryOperator<OrderDto>, UnaryOperator<LabelInfo.LabelInfoBuilder>>>of(
            Triple.of(
                "ExternalId не обязателен",
                order -> order.setExternalId(null),
                label -> label.seller(
                    LabelInfo.SellerInfo.builder()
                        .legalName("test-sender-legal-name")
                        .readableName("test-sender-name")
                        .build()
                )
            ),
            Triple.of(
                "Идентификатор грузоместа не обязателен",
                order -> order.setUnits(List.of(
                    OrderDtoFactory.createRootUnit(),
                    OrderDtoFactory.createPlaceUnitBuilder()
                        .externalId(null)
                        .dimensions(OrderDtoFactory.createKorobyte(10, 10, 10, 10))
                        .build()
                )),
                label -> label.place(
                    LabelInfo.PlaceInfo.builder()
                        .placeNumber(1)
                        .placesCount(1)
                        .weight(new BigDecimal("10"))
                        .build()
                )
            ),
            Triple.of(
                "Отчество получателя не указано",
                order -> order.setRecipient(
                    RecipientDto.builder()
                        .firstName("first-name")
                        .lastName("last-name")
                        .address(createAddress())
                        .build()
                ),
                label -> label.recipient(
                    LabelInfo.RecipientInfo.builder()
                        .firstName("first-name")
                        .lastName("last-name")
                        .phoneNumber("+79998886655")
                        .build()
                )
            ),
            Triple.of(
                "Заказ может ехать без СЦ",
                order -> order.setWaybill(List.of(
                    createWaybillSegmentWithShipmentDate(PartnerType.DELIVERY, 2L)
                )),
                label -> label.sortingCenter(null)
            ),
            Triple.of(
                "Заказ с отдельным возвратным СЦ",
                order -> order.setWaybill(List.of(
                    createWaybillSegmentWithShipmentDate(PartnerType.SORTING_CENTER, 1L),
                    createWaybillSegment(PartnerType.DELIVERY, 2L),
                    createWaybillSegment(PartnerType.SORTING_CENTER, 3L)
                )),
                null
            ),
            Triple.of(
                "Заказ с комбинированным маршрутом через несколько СЦ",
                order -> order.setWaybill(List.of(
                    createWaybillSegmentWithShipmentDate(PartnerType.SORTING_CENTER, 1L),
                    createWaybillSegment(PartnerType.SORTING_CENTER, 3L),
                    createWaybillSegment(PartnerType.DELIVERY, 2L)
                )),
                label -> label.sortingCenter(null)
            ),
            Triple.of(
                "Заказ с типом доставки до ПВЗ",
                order -> order
                    .setWaybill(List.of(
                        createWaybillSegmentWithShipmentDate(PartnerType.SORTING_CENTER, 1L),
                        createWaybillSegment(PartnerType.DELIVERY, 2L, null, createAddress())
                    ))
                    .setRecipient(
                        RecipientDto.builder()
                            .firstName("first-name")
                            .lastName("last-name")
                            .middleName("middle-name")
                            .build()
                    )
                    .setDeliveryType(DeliveryType.PICKUP),
                null
            ),
            Triple.of(
                "Заказ с типом доставки почтой",
                order -> order.setDeliveryType(DeliveryType.POST),
                null
            )
        ).map(t -> Arguments.of(t.getLeft(), t.getMiddle(), t.getRight()));
    }

    private void mockLomGetOrder() {
        mockLomGetOrder(UnaryOperator.identity());
    }

    private void mockLomGetOrder(UnaryOperator<OrderDto> modification) {
        OrderDto order = OrderDtoFactory.createLomOrder()
            .setId(ORDER_ID)
            .setSenderId(SENDER_ID)
            .setExternalId("external-id")
            .setBarcode("barcode")
            .setStatus(OrderStatus.PROCESSING)
            .setUnits(List.of(
                OrderDtoFactory.createRootUnit(),
                createPlaceUnit(1, "1")
            ))
            .setCredentials(
                CredentialsDto.builder()
                    .incorporation("test-sender-legal-name")
                    .build()
            )
            .setRecipient(
                RecipientDto.builder()
                    .firstName("first-name")
                    .lastName("last-name")
                    .middleName("middle-name")
                    .address(createAddress())
                    .build()
            )
            .setWaybill(List.of(
                createWaybillSegmentWithShipmentDate(PartnerType.SORTING_CENTER, 1L),
                createWaybillSegment(PartnerType.DELIVERY, 2L),
                createWaybillSegment(PartnerType.DELIVERY, 3L)
            ));

        when(lomClient.getOrder(ORDER_ID, Set.of()))
            .thenReturn(Optional.of(modification.apply(order)));
    }

    private void mockLomGetLabel(String labelUrl) {
        when(lomClient.getLabel(ORDER_ID))
            .thenReturn(Optional.of(
                OrderLabelDto.builder()
                    .orderId(ORDER_ID)
                    .labelFile(
                        MdsFileDto.builder()
                            .url(labelUrl)
                            .fileType(FileType.ORDER_LABEL)
                            .build()
                    )
                    .build()
            ));
    }

    private void mockWwGenerateLabel(List<LabelInfo> labelInfo, DocumentFormat documentFormat) {
        when(wwClient.generateLabels(anyList(), eq(documentFormat), eq(PageSize.A6)))
            .thenAnswer(invocation -> {
                // Отлаживаться намного проще, глядя на дифф через софт
                softly.assertThat(invocation.<List<LabelInfo>>getArgument(0))
                    .isEqualTo(labelInfo);

                Set<ConstraintViolation<OrderLabels>> violations = validator.validate(
                    OrderLabels.builder()
                        .labels(invocation.getArgument(0))
                        .build()
                );

                softly.assertThat(violations).isEmpty();

                return extractBytes(TEST_FILE);
            });
    }

    private void verifyWwCalled(List<LabelInfo> labelInfo, DocumentFormat documentFormat) {
        verify(wwClient).generateLabels(labelInfo, documentFormat, PageSize.A6);
    }

    @Nonnull
    private static List<LabelInfo> createLabelInfo() {
        return createLabelInfo(1);
    }

    @Nonnull
    private static List<LabelInfo> createLabelInfo(int numberOfPlaces) {
        return createLabelInfo(numberOfPlaces, null);
    }

    @Nonnull
    private static List<LabelInfo> createLabelInfo(
        int numberOfPlaces,
        @Nullable UnaryOperator<LabelInfo.LabelInfoBuilder> modifier
    ) {
        return IntStreamEx.rangeClosed(1, numberOfPlaces)
            .mapToObj(
                iter -> {
                    LabelInfo.LabelInfoBuilder builder = LabelInfo.builder()
                        .platformClientId(3L)
                        .barcode("barcode")
                        .seller(
                            LabelInfo.SellerInfo.builder()
                                .number("external-id")
                                .legalName("test-sender-legal-name")
                                .readableName("test-sender-name")
                                .build()
                        )
                        .place(
                            LabelInfo.PlaceInfo.builder()
                                .placeNumber(iter)
                                .placesCount(numberOfPlaces)
                                .externalId("place-id-" + iter)
                                .weight(new BigDecimal(iter))
                                .build()
                        )
                        .shipmentDate(SHIPMENT_DATE)
                        .recipient(
                            LabelInfo.RecipientInfo.builder()
                                .firstName("first-name")
                                .lastName("last-name")
                                .middleName("middle-name")
                                .phoneNumber("+79998886655")
                                .build()
                        )
                        .address(
                            LabelInfo.AddressInfo.builder()
                                .country("recipient_country")
                                .federalDistrict("recipient_federal_district")
                                .region("recipient_region")
                                .locality("recipient_locality")
                                .subRegion("recipient_sub_region")
                                .settlement("recipient_settlement")
                                .street("recipient_street")
                                .house("recipient_house")
                                .building("recipient_building")
                                .housing("recipient_housing")
                                .room("recipient_room")
                                .zipCode("recipient_zip")
                                .build()
                        )
                        .deliveryService(
                            LabelInfo.PartnerInfo.builder()
                                .readableName("partner-name-2")
                                .legalName("partner-legal-name-2")
                                .build()
                        )
                        .sortingCenter(
                            LabelInfo.PartnerInfo.builder()
                                .readableName("partner-name-1")
                                .legalName("partner-legal-name-1")
                                .build()
                        );
                    return Optional.ofNullable(modifier)
                        .orElse(UnaryOperator.identity())
                        .andThen(LabelInfo.LabelInfoBuilder::build)
                        .apply(builder);
                }
            )
            .toList();
    }

    @Nonnull
    private static AddressDto createAddress() {
        return AddressDto.builder()
            .geoId(1000)
            .country("recipient_country")
            .region("recipient_region")
            .locality("recipient_locality")
            .zipCode("recipient_zip")
            .street("recipient_street")
            .house("recipient_house")
            .building("recipient_building")
            .housing("recipient_housing")
            .room("recipient_room")
            .federalDistrict("recipient_federal_district")
            .subRegion("recipient_sub_region")
            .settlement("recipient_settlement")
            .build();
    }

    @Nonnull
    private static StorageUnitDto createPlaceUnit(int id, String weight) {
        return OrderDtoFactory.createPlaceUnitBuilder()
            .externalId("place-id-" + id)
            .dimensions(
                KorobyteDto.builder()
                    .height(10)
                    .width(10)
                    .length(10)
                    .weightGross(new BigDecimal(weight))
                    .build()
            )
            .build();
    }

    @Nonnull
    private static WaybillSegmentDto createWaybillSegment(
        PartnerType partnerType,
        Long partnerId,
        @Nullable LocalDate shipmentDate,
        @Nullable AddressDto shipmentToAddress
    ) {
        WaybillSegmentDto.WaybillSegmentDtoBuilder segmentBuilder = WaybillSegmentDto.builder()
            .partnerType(partnerType)
            .partnerId(partnerId)
            .partnerName("partner-name-" + partnerId)
            .partnerLegalName("partner-legal-name-" + partnerId);

        if (shipmentDate != null || shipmentToAddress != null) {
            WaybillSegmentDto.ShipmentDto.ShipmentDtoBuilder shipmentBuilder = WaybillSegmentDto.ShipmentDto.builder();

            Optional.ofNullable(shipmentDate).ifPresent(shipmentBuilder::date);
            Optional.ofNullable(shipmentToAddress).ifPresent(
                address -> shipmentBuilder.locationTo(LocationDto.builder().address(address).build())
            );

            segmentBuilder.shipment(shipmentBuilder.build());
        }

        return segmentBuilder.build();
    }

    @Nonnull
    private static WaybillSegmentDto createWaybillSegmentWithShipmentDate(PartnerType partnerType, Long partnerId) {
        return createWaybillSegment(partnerType, partnerId, ApiOrderLabelTest.SHIPMENT_DATE, null);
    }

    @Nonnull
    private static WaybillSegmentDto createWaybillSegment(PartnerType partnerType, Long partnerId) {
        return createWaybillSegment(partnerType, partnerId, null, null);
    }

    @Nonnull
    private ResultActions getLabel(MediaType... acceptHeaders) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/orders/100/label").headers(authHeaders());
        if (acceptHeaders.length > 0) {
            request.accept(acceptHeaders);
        }
        return mockMvc.perform(request);
    }

    @Nonnull
    private static ResultMatcher checkHeaders(DocumentFormat documentFormat) {
        switch (documentFormat) {
            case PDF:
                return checkHeaders("application/pdf", "pdf");
            case HTML:
                return checkHeaders("text/html;charset=UTF-8", "html");
            default:
                throw new RuntimeException("Unknown document format: " + documentFormat);
        }
    }

    @Nonnull
    private static ResultMatcher checkHeaders(String contentType, String extension) {
        return ResultMatcher.matchAll(
            content().contentType(contentType),
            header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                String.format("attachment; filename="
                    + "\"order_label_100.%s\"", extension)
            )
        );
    }

    @Nonnull
    private static byte[] extractBytes(String path) {
        try (InputStream is = ClassLoader.getSystemResourceAsStream(path)) {
            return IOUtils.toByteArray(Objects.requireNonNull(is));
        } catch (IOException e) {
            throw new RuntimeException("Could not extract content", e);
        }
    }

    private void verifyGetOrder() {
        verify(lomClient).getOrder(ORDER_ID, Set.of());
    }
}
