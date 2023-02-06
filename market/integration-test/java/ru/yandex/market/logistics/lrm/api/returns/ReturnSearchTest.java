package ru.yandex.market.logistics.lrm.api.returns;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi.SearchReturnsOper;
import ru.yandex.market.logistics.lrm.client.model.LogisticPointType;
import ru.yandex.market.logistics.lrm.client.model.ReturnBox;
import ru.yandex.market.logistics.lrm.client.model.ReturnBoxStatus;
import ru.yandex.market.logistics.lrm.client.model.ReturnItem;
import ru.yandex.market.logistics.lrm.client.model.ReturnLogisticPoint;
import ru.yandex.market.logistics.lrm.client.model.ReturnSegment;
import ru.yandex.market.logistics.lrm.client.model.ReturnSegmentShipment;
import ru.yandex.market.logistics.lrm.client.model.ReturnSegmentStatus;
import ru.yandex.market.logistics.lrm.client.model.ReturnStatus;
import ru.yandex.market.logistics.lrm.client.model.SearchReturn;
import ru.yandex.market.logistics.lrm.client.model.SearchReturnsRequest;
import ru.yandex.market.logistics.lrm.client.model.SearchReturnsResponse;
import ru.yandex.market.logistics.lrm.client.model.ShipmentDestination;
import ru.yandex.market.logistics.lrm.client.model.ShipmentDestinationType;
import ru.yandex.market.logistics.lrm.client.model.ValidationError;
import ru.yandex.market.logistics.lrm.client.model.ValidationViolation;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Поиск возвратов")
class ReturnSearchTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("По штрихкодам коробок")
    @DatabaseSetup("/database/api/returns/search/before/box_external_ids.xml")
    void boxesExternalIds() {
        SearchReturnsResponse response = searchReturns(
            new SearchReturnsRequest()
                .boxExternalIds(Set.of("box-external-id-1", "box-external-id-2"))
        )
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response.getReturns())
            .containsExactly(
                new SearchReturn()
                    .id(1L)
                    .status(ReturnStatus.CREATED)
                    .externalId("return-external-id")
                    .orderExternalId("order-external-id")
                    .commitedTs(1639267200L)
                    .addBoxesItem(
                        new ReturnBox()
                            .status(ReturnBoxStatus.CREATED)
                            .externalId("box-external-id-1")
                            .segments(List.of(
                                new ReturnSegment()
                                    .uniqueId("segment-id-1")
                                    .logisticPoint(
                                        new ReturnLogisticPoint()
                                            .logisticPointId(1001L)
                                            .partnerId(2001L)
                                            .type(LogisticPointType.SORTING_CENTER)
                                    )
                                    .shipment(
                                        new ReturnSegmentShipment()
                                            .destination(
                                                new ShipmentDestination()
                                                    .logisticPointId(1002L)
                                                    .partnerId(2002L)
                                                    .type(ShipmentDestinationType.FULFILLMENT)
                                            )
                                    )
                                    .status(ReturnSegmentStatus.CREATED),
                                new ReturnSegment()
                                    .uniqueId("segment-id-2")
                                    .logisticPoint(
                                        new ReturnLogisticPoint()
                                            .type(LogisticPointType.SORTING_CENTER)
                                    )
                                    .shipment(new ReturnSegmentShipment())
                                    .status(ReturnSegmentStatus.CREATED)
                            ))
                    )
                    .addItemsItem(
                        new ReturnItem()
                            .boxExternalId("box-external-id-1")
                            .supplierId(200L)
                            .vendorCode("item-vendor-code")
                            .instances(Map.of(
                                "CIS", "987-wer",
                                "uit", "lkj-6875"
                            ))
                    ),
                new SearchReturn()
                    .id(2L)
                    .orderExternalId("order-external-id-2")
                    .addBoxesItem(
                        new ReturnBox()
                            .externalId("box-external-id-2")
                            .segments(List.of())
                    )
                    .items(List.of())
            );
    }

    @Test
    @DisplayName("По внешнему идентификатору")
    @DatabaseSetup("/database/api/returns/search/before/external_id.xml")
    void externalId() {
        SearchReturnsResponse response = searchReturns(
            new SearchReturnsRequest()
                .externalId("first-external-id")
        )
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response.getReturns())
            .containsExactly(
                new SearchReturn()
                    .id(1L)
                    .externalId("first-external-id")
                    .orderExternalId("987654")
                    .boxes(List.of())
                    .items(List.of())
            );
    }

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource
    @DisplayName("Валидация запроса")
    void requestValidation(SearchReturnsRequest request, String field, String message) {
        ValidationError error = searchReturns(request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getErrors())
            .containsExactly(
                new ValidationViolation()
                    .field(field)
                    .message(message)
            );
    }

    @Nonnull
    public static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of(
                new SearchReturnsRequest().boxExternalIds(Set.of()),
                "boxExternalIds",
                "size must be between 1 and 100"
            )
        );
    }

    @Nonnull
    private SearchReturnsOper searchReturns(SearchReturnsRequest request) {
        return apiClient.returns().searchReturns().body(request);
    }

}
