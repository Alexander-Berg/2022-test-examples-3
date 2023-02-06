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

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.werewolf.dto.document.WriterOptions;
import ru.yandex.market.logistics.werewolf.model.entity.DocOrder;
import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;
import ru.yandex.market.logistics.werewolf.model.enums.PageOrientation;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

abstract class AbstractRTAGeneratorTest extends AbstractDocumentGeneratorTest {

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource
    @DisplayName("Валидация входных параметров")
    void validation(
        Consumer<RtaOrdersData.RtaOrdersDataBuilder> builderConsumer,
        String field,
        String message
    ) throws Exception {
        RtaOrdersData.RtaOrdersDataBuilder rtaOrdersDataBuilder = ordersDataBuilder();
        builderConsumer.accept(rtaOrdersDataBuilder);
        performWithBody(
            objectMapper.writeValueAsString(rtaOrdersDataBuilder.build()),
            request -> request.accept(MediaType.TEXT_HTML, APPLICATION_JSON_Q_09)
        )
            .andExpect(status().isBadRequest())
            .andExpect(fieldError(field, message));
    }

    @Nonnull
    private static Stream<Arguments> validation() {
        return Stream.of(
                validationCommon(),
                validationDocOrder()
            )
            .flatMap(Function.identity())
            .map(t -> Arguments.of(t.getLeft(), t.getMiddle(), t.getRight()));
    }

    @Nonnull
    private static Stream<Triple<Consumer<RtaOrdersData.RtaOrdersDataBuilder>, String, String>> validationCommon() {
        return Stream.of(
            Triple.of(
                b -> b.orders(null),
                "orders",
                "must not be empty"
            ),
            Triple.of(
                b -> b.orders(List.of()),
                "orders",
                "must not be empty"
            ),
            Triple.of(
                b -> b.orders(Collections.singletonList(null)),
                "orders[0]",
                "must not be null"
            ),
            Triple.of(
                b -> b.shipmentId(null),
                "shipmentId",
                "must not be empty"
            ),
            Triple.of(
                b -> b.shipmentId(""),
                "shipmentId",
                "must not be empty"
            ),
            Triple.of(
                b -> b.shipmentDate(null),
                "shipmentDate",
                "must not be null"
            ),
            Triple.of(
                b -> b.senderLegalName(null),
                "senderLegalName",
                "must not be null"
            ),
            Triple.of(
                b -> b.partnerLegalName(null),
                "partnerLegalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.partnerLegalName(""),
                "partnerLegalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.partnerLegalName("   "),
                "partnerLegalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.senderId(null),
                "senderId",
                "must not be empty"
            ),
            Triple.of(
                b -> b.senderId(""),
                "senderId",
                "must not be empty"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<RtaOrdersData.RtaOrdersDataBuilder>, String, String>> validationDocOrder() {
        return Stream.<Triple<UnaryOperator<DocOrder.DocOrderBuilder>, String, String>>of(
            Triple.of(
                b -> b.yandexId(null),
                "orders[0].yandexId",
                "must not be blank"
            ),
            Triple.of(
                b -> b.yandexId(""),
                "orders[0].yandexId",
                "must not be blank"
            ),
            Triple.of(
                b -> b.yandexId("   "),
                "orders[0].yandexId",
                "must not be blank"
            ),
            Triple.of(
                b -> b.assessedCost(null),
                "orders[0].assessedCost",
                "must not be null"
            ),
            Triple.of(
                b -> b.assessedCost(BigDecimal.valueOf(-1)),
                "orders[0].assessedCost",
                "must be greater than or equal to 0"
            ),
            Triple.of(
                b -> b.weight(null),
                "orders[0].weight",
                "must not be null"
            ),
            Triple.of(
                b -> b.weight(BigDecimal.ZERO),
                "orders[0].weight",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.weight(BigDecimal.valueOf(-1)),
                "orders[0].weight",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.placesCount(null),
                "orders[0].placesCount",
                "must not be null"
            ),
            Triple.of(
                b -> b.placesCount(0),
                "orders[0].placesCount",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.placesCount(-1),
                "orders[0].placesCount",
                "must be greater than 0"
            )
        ).map(
            t -> Triple.of(
                b -> b.orders(List.of(t.getLeft().apply(docOrderBuilder()).build())),
                t.getMiddle(),
                t.getRight()
            )
        );
    }

    @Nonnull
    @Override
    protected String defaultRequestBodyPath() {
        return "controller/documents/request/RTA_single_order.json";
    }

    @Nonnull
    @Override
    protected String defaultFilename() {
        return "2020-01-10-test";
    }

    @Nonnull
    @Override
    protected WriterOptions defaultWriterOptions() {
        return new WriterOptions(PageSize.A4, PageOrientation.PORTRAIT);
    }

    @Nonnull
    private static RtaOrdersData.RtaOrdersDataBuilder ordersDataBuilder() {
        return RtaOrdersData.builder()
            .orders(List.of(docOrderBuilder().build()))
            .shipmentId("shipment-id")
            .shipmentDate(LocalDate.of(2020, 8, 20))
            .senderLegalName("sender-legal-name")
            .partnerLegalName("partner-legal-name")
            .senderId("sender-id");
    }

    @Nonnull
    private static DocOrder.DocOrderBuilder docOrderBuilder() {
        return DocOrder.builder()
            .yandexId("yandex-id")
            .partnerId("parter-id")
            .assessedCost(BigDecimal.TEN)
            .weight(BigDecimal.ONE)
            .placesCount(1);
    }
}
