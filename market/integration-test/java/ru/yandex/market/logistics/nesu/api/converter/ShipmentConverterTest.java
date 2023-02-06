package ru.yandex.market.logistics.nesu.api.converter;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus;
import ru.yandex.market.logistics.nesu.api.AbstractApiTest;
import ru.yandex.market.logistics.nesu.converter.ShipmentConverter;
import ru.yandex.market.logistics.nesu.dto.enums.DaasOrderStatus;
import ru.yandex.market.logistics.nesu.dto.filter.ShipmentSearchFilter;

@DisplayName("ShipmentConverter")
class ShipmentConverterTest extends AbstractApiTest {
    private static final long MARKET_ID = 1;

    @Autowired
    private ShipmentConverter converter;

    @DisplayName("SENDER_WAIT_*: ")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("senderWaitCases")
    void senderWait(
        String caseName,
        ShipmentSearchFilter inputFilter,
        ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter expectedFilter
    ) {
        ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter result =
            converter.toLomShipmentSearchFilter(inputFilter, MARKET_ID, null, null);

        softly.assertThat(result).isEqualTo(expectedFilter);
    }

    private static ShipmentSearchFilter createInputFilter(
        Set<DaasOrderStatus> orderStatuses,
        Set<ru.yandex.market.logistics.nesu.api.model.enums.ShipmentApplicationStatus> shipmentApplicationStatuses
    ) {
        return ShipmentSearchFilter.builder()
            .orderStatuses(orderStatuses)
            .statuses(shipmentApplicationStatuses)
            .build();
    }

    private static ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter createExpectedFilter(
        PartnerType partnerType,
        Set<ShipmentApplicationStatus> shipmentApplicationStatuses,
        Set<ru.yandex.market.logistics.lom.model.enums.OrderStatus> orderStatuses
    ) {
        return ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter.builder()
            .marketIdFrom(MARKET_ID)
            .statuses(shipmentApplicationStatuses)
            .orderStatuses(orderStatuses)
            .partnerType(partnerType)
            .build();
    }

    static Stream<Arguments> senderWaitCases() {
        return Stream.of(
            Arguments.of(
                "проверка отсутствия трансляции если статусы SENDER_WAIT_* не переданы",
                createInputFilter(Set.of(DaasOrderStatus.CANCELLED, DaasOrderStatus.FINISHED), Set.of()),
                createExpectedFilter(
                    null,
                    Set.of(),
                    Set.of(
                        ru.yandex.market.logistics.lom.model.enums.OrderStatus.CANCELLED,
                        ru.yandex.market.logistics.lom.model.enums.OrderStatus.FINISHED
                    )
                )
            ),
            Arguments.of(
                "трансляция статуса SENDER_WAIT_DELIVERY в REGISTRY_SENT + partnerType = DELIVERY",
                createInputFilter(Set.of(DaasOrderStatus.SENDER_WAIT_DELIVERY), Set.of()),
                createExpectedFilter(
                    PartnerType.DELIVERY,
                    Set.of(ShipmentApplicationStatus.REGISTRY_SENT),
                    null
                )
            ),
            Arguments.of(
                "SENDER_WAIT_DELIVERY мерж с остальными sas-статусами",
                createInputFilter(
                    Set.of(DaasOrderStatus.SENDER_WAIT_DELIVERY),
                    Set.of(ru.yandex.market.logistics.nesu.api.model.enums.ShipmentApplicationStatus.CREATED)
                ),
                createExpectedFilter(
                    PartnerType.DELIVERY,
                    Set.of(ShipmentApplicationStatus.REGISTRY_SENT, ShipmentApplicationStatus.CREATED),
                    null
                )
            ),
            Arguments.of(
                "трансляция статуса SENDER_WAIT_FULFILMENT в REGISTRY_SENT + partnerType = SORTING_CENTER",
                createInputFilter(
                    Set.of(DaasOrderStatus.SENDER_WAIT_FULFILMENT),
                    Set.of()
                ),
                createExpectedFilter(
                    PartnerType.SORTING_CENTER,
                    Set.of(ShipmentApplicationStatus.REGISTRY_SENT),
                    null
                )
            ),
            Arguments.of(
                "SENDER_WAIT_FULFILMENT мерж с остальными sas-статусами",
                createInputFilter(
                    Set.of(DaasOrderStatus.SENDER_WAIT_FULFILMENT),
                    Set.of(ru.yandex.market.logistics.nesu.api.model.enums.ShipmentApplicationStatus.NEW)
                ),
                createExpectedFilter(
                    PartnerType.SORTING_CENTER,
                    Set.of(ShipmentApplicationStatus.REGISTRY_SENT, ShipmentApplicationStatus.NEW),
                    null
                )
            ),
            Arguments.of(
                "если переданы SENDER_WAIT_DELIVERY,SENDER_WAIT_FULFILMENT (В отгрузке - Все) " +
                    "транслируем в REGISTRY_SENT",
                createInputFilter(
                    Set.of(DaasOrderStatus.SENDER_WAIT_FULFILMENT, DaasOrderStatus.SENDER_WAIT_DELIVERY),
                    Set.of()
                ),
                createExpectedFilter(
                    null,
                    Set.of(ShipmentApplicationStatus.REGISTRY_SENT),
                    null
                )
            ),
            Arguments.of(
                "мерж SENDER_WAIT_DELIVERY,SENDER_WAIT_FULFILMENT с остальными sas-статусами",
                createInputFilter(
                    Set.of(DaasOrderStatus.SENDER_WAIT_FULFILMENT, DaasOrderStatus.SENDER_WAIT_DELIVERY),
                    Set.of(ru.yandex.market.logistics.nesu.api.model.enums.ShipmentApplicationStatus.NEW)
                ),
                createExpectedFilter(
                    null,
                    Set.of(ShipmentApplicationStatus.REGISTRY_SENT, ShipmentApplicationStatus.NEW),
                    null
                )
            )
        );
    }
}
