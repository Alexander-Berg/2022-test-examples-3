package ru.yandex.market.logistics.werewolf.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.common.util.collections.Triple;
import ru.yandex.market.logistics.werewolf.config.properties.MarginsProperties;
import ru.yandex.market.logistics.werewolf.dto.document.WriterOptions;
import ru.yandex.market.logistics.werewolf.model.entity.LabelInfo;
import ru.yandex.market.logistics.werewolf.model.entity.OrderLabels;
import ru.yandex.market.logistics.werewolf.model.enums.PageOrientation;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContentInBytes;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Генерация ярлыков для заказов")
class LabelsGeneratorTest extends AbstractDocumentGeneratorTest {

    @Autowired
    private MarginsProperties labelsMarginsProperties;

    @ParameterizedTest
    @MethodSource("labelGenerationSource")
    @DisplayName("Успешная генерация ярлыков в HTML")
    void generateLabelsSuccess(
        String requestPath,
        String responsePath
    ) throws Exception {
        performAndDispatch(
            requestPath,
            request -> request.accept(MediaType.TEXT_HTML)
        )
            .andExpect(status().isOk())
            .andExpect(content().contentType(TEXT_HTML_UTF_8))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"labels.html\""))
            .andExpect(content().string(extractFileContent(responsePath)));
    }

    @Nonnull
    private static Stream<Arguments> labelGenerationSource() {
        return Stream.of(
            Arguments.of(
                "controller/documents/request/single_label.json",
                "controller/documents/response/single_label_delivery.html"
            ),
            Arguments.of(
                "controller/documents/request/single_label_no_sc.json",
                "controller/documents/response/single_label_no_sc.html"
            ),
            Arguments.of(
                "controller/documents/request/single_label_no_place_id.json",
                "controller/documents/response/single_label_no_place_id.html"
            ),
            Arguments.of(
                "controller/documents/request/multiple_labels.json",
                "controller/documents/response/multiple_labels.html"
            ),
            Arguments.of(
                "controller/documents/request/single_label_no_seller_number.json",
                "controller/documents/response/single_label_no_seller_number.html"
            ),
            Arguments.of(
                "controller/documents/request/single_label_no_weight.json",
                "controller/documents/response/single_label_no_weight.html"
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource("validationSource")
    @DisplayName("Валидация входных параметров")
    void validation(
        Consumer<OrderLabels.OrderLabelsBuilder> builderConsumer,
        String field,
        String message
    ) throws Exception {
        OrderLabels.OrderLabelsBuilder labelsInfoBuilder = labelsInfoBuilder();
        builderConsumer.accept(labelsInfoBuilder);
        performWithBody(
            objectMapper.writeValueAsString(labelsInfoBuilder.build()),
            request -> request.accept(MediaType.TEXT_HTML, APPLICATION_JSON_Q_09)
        )
            .andExpect(status().isBadRequest())
            .andExpect(fieldError(field, message));
    }

    @Nonnull
    private static Stream<Arguments> validationSource() {
        return Stream.of(
            validationGeneral(),
            validationSC(),
            validationDS(),
            validationPlace(),
            validationRecipient(),
            validationSeller()
        )
            .flatMap(Function.identity())
            .map(t -> Arguments.of(t.first, t.second, t.third));
    }

    @Nonnull
    private static Stream<Triple<Consumer<OrderLabels.OrderLabelsBuilder>, String, String>> validationGeneral() {
        return Stream.concat(
            Stream.of(
                Triple.of(
                    b -> b.labels(null),
                    "labels",
                    "must not be empty"
                ),
                Triple.of(
                    b -> b.labels(List.of()),
                    "labels",
                    "must not be empty"
                ),
                Triple.of(
                    b -> b.labels(Collections.singletonList(null)),
                    "labels[0]",
                    "must not be null"
                )
            ),
            Stream.<Triple<UnaryOperator<LabelInfo.LabelInfoBuilder>, String, String>>of(
                Triple.of(
                    b -> b.platformClientId(null),
                    "labels[0].platformClientId",
                    "must not be null"
                ),
                Triple.of(
                    b -> b.barcode(null),
                    "labels[0].barcode",
                    "must not be blank"
                ),
                Triple.of(
                    b -> b.barcode(""),
                    "labels[0].barcode",
                    "must not be blank"
                ),
                Triple.of(
                    b -> b.barcode("  "),
                    "labels[0].barcode",
                    "must not be blank"
                ),
                Triple.of(
                    b -> b.deliveryService(null),
                    "labels[0].deliveryService",
                    "must not be null"
                ),
                Triple.of(
                    b -> b.place(null),
                    "labels[0].place",
                    "must not be null"
                ),
                Triple.of(
                    b -> b.recipient(null),
                    "labels[0].recipient",
                    "must not be null"
                ),
                Triple.of(
                    b -> b.address(null),
                    "labels[0].address",
                    "must not be null"
                ),
                Triple.of(
                    b -> b.shipmentDate(null),
                    "labels[0].shipmentDate",
                    "must not be null"
                ),
                Triple.of(
                    b -> b.seller(null),
                    "labels[0].seller",
                    "must not be null"
                )
            ).map(
                t -> Triple.of(
                    b -> b.labels(List.of(t.first.apply(labelInfoBuilder()).build())),
                    t.second,
                    t.third
                )
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<OrderLabels.OrderLabelsBuilder>, String, String>> validationSC() {
        return Stream.<Triple<UnaryOperator<LabelInfo.PartnerInfo.PartnerInfoBuilder>, String, String>>of(
            Triple.of(
                b -> b.legalName(null),
                "labels[0].sortingCenter.legalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.legalName(""),
                "labels[0].sortingCenter.legalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.legalName("  "),
                "labels[0].sortingCenter.legalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.readableName(null),
                "labels[0].sortingCenter.readableName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.readableName(""),
                "labels[0].sortingCenter.readableName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.readableName("  "),
                "labels[0].sortingCenter.readableName",
                "must not be blank"
            )
        ).map(
            t -> Triple.of(
                b -> b.labels(List.of(
                    labelInfoBuilder()
                        .sortingCenter(t.first.apply(partnerInfoBuilder()).build())
                        .build()
                )),
                t.second,
                t.third
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<OrderLabels.OrderLabelsBuilder>, String, String>> validationDS() {
        return Stream.<Triple<UnaryOperator<LabelInfo.PartnerInfo.PartnerInfoBuilder>, String, String>>of(
            Triple.of(
                b -> b.legalName(null),
                "labels[0].deliveryService.legalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.legalName(""),
                "labels[0].deliveryService.legalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.legalName("  "),
                "labels[0].deliveryService.legalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.readableName(null),
                "labels[0].deliveryService.readableName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.readableName(""),
                "labels[0].deliveryService.readableName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.readableName("  "),
                "labels[0].deliveryService.readableName",
                "must not be blank"
            )
        ).map(
            t -> Triple.of(
                b -> b.labels(List.of(
                    labelInfoBuilder()
                        .deliveryService(t.first.apply(partnerInfoBuilder()).build())
                        .build()
                )),
                t.second,
                t.third
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<OrderLabels.OrderLabelsBuilder>, String, String>> validationPlace() {
        return Stream.<Triple<UnaryOperator<LabelInfo.PlaceInfo.PlaceInfoBuilder>, String, String>>of(
            Triple.of(
                b -> b.placeNumber(null),
                "labels[0].place.placeNumber",
                "must not be null"
            ),
            Triple.of(
                b -> b.placeNumber(0),
                "labels[0].place.placeNumber",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.placeNumber(-3),
                "labels[0].place.placeNumber",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.placesCount(null),
                "labels[0].place.placesCount",
                "must not be null"
            ),
            Triple.of(
                b -> b.placesCount(0),
                "labels[0].place.placesCount",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.placesCount(-3),
                "labels[0].place.placesCount",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.weight(BigDecimal.valueOf(-3)),
                "labels[0].place.weight",
                "must be greater than or equal to 0"
            )
        ).map(
            t -> Triple.of(
                b -> b.labels(List.of(
                    labelInfoBuilder()
                        .place(t.first.apply(placeInfoBuilder()).build())
                        .build()
                )),
                t.second,
                t.third
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<OrderLabels.OrderLabelsBuilder>, String, String>> validationRecipient() {
        return Stream.<Triple<UnaryOperator<LabelInfo.RecipientInfo.RecipientInfoBuilder>, String, String>>of(
            Triple.of(
                b -> b.firstName(null),
                "labels[0].recipient.firstName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.firstName(""),
                "labels[0].recipient.firstName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.firstName("  "),
                "labels[0].recipient.firstName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.lastName(null),
                "labels[0].recipient.lastName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.lastName(""),
                "labels[0].recipient.lastName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.lastName("  "),
                "labels[0].recipient.lastName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.phoneNumber(null),
                "labels[0].recipient.phoneNumber",
                "must not be blank"
            ),
            Triple.of(
                b -> b.phoneNumber(""),
                "labels[0].recipient.phoneNumber",
                "must not be blank"
            ),
            Triple.of(
                b -> b.phoneNumber("  "),
                "labels[0].recipient.phoneNumber",
                "must not be blank"
            )
        ).map(
            t -> Triple.of(
                b -> b.labels(List.of(
                    labelInfoBuilder()
                        .recipient(t.first.apply(recipientInfoBuilder()).build())
                        .build()
                )),
                t.second,
                t.third
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<OrderLabels.OrderLabelsBuilder>, String, String>> validationSeller() {
        return Stream.<Triple<UnaryOperator<LabelInfo.SellerInfo.SellerInfoBuilder>, String, String>>of(
            Triple.of(
                b -> b.legalName(null),
                "labels[0].seller.legalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.legalName(""),
                "labels[0].seller.legalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.legalName("  "),
                "labels[0].seller.legalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.readableName(null),
                "labels[0].seller.readableName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.readableName(""),
                "labels[0].seller.readableName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.readableName("  "),
                "labels[0].seller.readableName",
                "must not be blank"
            )
        ).map(
            t -> Triple.of(
                b -> b.labels(List.of(
                    labelInfoBuilder()
                        .seller(t.first.apply(sellerInfoBuilder()).build())
                        .build()
                )),
                t.second,
                t.third
            )
        );
    }

    @ParameterizedTest
    @EnumSource(PageSize.class)
    @DisplayName("Генерация PDF с указанием размера страницы")
    void generatePdfA4Format(PageSize pageSize) throws Exception {
        mockConverterWithChecks(
            extractFileContentInBytes(defaultHtmlResponseBodyPath()),
            defaultWriterOptions().setPageSize(pageSize),
            MOCK_PDF_CONTENT.getBytes()
        );

        performAndDispatch(
            defaultRequestBodyPath(),
            request -> request
                .accept(MediaType.APPLICATION_PDF)
                .param("pageSize", pageSize.name())
        )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"labels.pdf\""))
            .andExpect(content().string(MOCK_PDF_CONTENT));
    }

    @Nonnull
    @Override
    protected String defaultRequestBodyPath() {
        return "controller/documents/request/single_label.json";
    }

    @Nonnull
    @Override
    protected String defaultHtmlResponseBodyPath() {
        return "controller/documents/response/single_label_delivery.html";
    }

    @Nonnull
    @Override
    protected String defaultFilename() {
        return "labels";
    }

    @Nonnull
    @Override
    protected String requestPath() {
        return "/document/label/generate";
    }

    @Nonnull
    @Override
    protected WriterOptions defaultWriterOptions() {
        return new WriterOptions(PageSize.A6, PageOrientation.PORTRAIT)
            .setMargins(labelsMarginsProperties.asWriterOptionsMargins());
    }

    @Nonnull
    public static OrderLabels.OrderLabelsBuilder labelsInfoBuilder() {
        return OrderLabels.builder()
            .labels(List.of(labelInfoBuilder().build()));
    }

    @Nonnull
    public static LabelInfo.LabelInfoBuilder labelInfoBuilder() {
        return LabelInfo.builder()
            .platformClientId(1L)
            .address(LabelInfo.AddressInfo.builder().build())
            .barcode("order-barcode")
            .sortingCenter(partnerInfoBuilder().build())
            .deliveryService(partnerInfoBuilder().build())
            .place(placeInfoBuilder().build())
            .recipient(recipientInfoBuilder().build())
            .shipmentDate(LocalDate.parse("2020-06-06"))
            .seller(sellerInfoBuilder().build());
    }

    @Nonnull
    public static LabelInfo.PartnerInfo.PartnerInfoBuilder partnerInfoBuilder() {
        return LabelInfo.PartnerInfo.builder()
            .legalName("partner-legal-name")
            .readableName("partner-readable-name");
    }

    @Nonnull
    public static LabelInfo.PlaceInfo.PlaceInfoBuilder placeInfoBuilder() {
        return LabelInfo.PlaceInfo.builder()
            .externalId("place-ext-id")
            .placeNumber(1)
            .placesCount(2)
            .weight(BigDecimal.valueOf(2.3));
    }

    @Nonnull
    public static LabelInfo.RecipientInfo.RecipientInfoBuilder recipientInfoBuilder() {
        return LabelInfo.RecipientInfo.builder()
            .firstName("recipient-first-name")
            .lastName("recipient-last-name")
            .middleName("recipient-middle-name")
            .phoneNumber("recipient-phone");
    }

    @Nonnull
    public static LabelInfo.SellerInfo.SellerInfoBuilder sellerInfoBuilder() {
        return LabelInfo.SellerInfo.builder()
            .number("seller-id")
            .legalName("seller-legal-name")
            .readableName("seller-readable-name");
    }
}
