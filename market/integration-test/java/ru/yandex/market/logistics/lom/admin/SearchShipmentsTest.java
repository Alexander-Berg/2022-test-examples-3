package ru.yandex.market.logistics.lom.admin;

import java.time.LocalDate;
import java.time.Month;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.admin.filter.AdminShipmentSearchFilter;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentApplicationStatus;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentType;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Получение отгрузок")
class SearchShipmentsTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(lmsClient);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск отгрузок")
    @DatabaseSetup("/controller/admin/shipment/before/shipments.xml")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminShipmentSearchFilter filter,
        String responsePath
    ) throws Exception {
        when(lmsClient.searchPartners(any(SearchPartnerFilter.class)))
            .thenAnswer(
                invocation -> invocation.<SearchPartnerFilter>getArgument(0)
                    .getIds()
                    .stream()
                    .map(
                        id -> PartnerResponse.newBuilder()
                            .id(id)
                            .readableName("ID партнёра = " + id)
                            .build()
                    )
                    .collect(Collectors.toList())
            );
        when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class)))
            .thenAnswer(
                invocation -> invocation.<LogisticsPointFilter>getArgument(0)
                    .getIds()
                    .stream()
                    .map(id -> LmsFactory.createWarehouseResponseBuilder(id).name("ID склада = " + id).build())
                    .collect(Collectors.toList())
            );

        mockMvc.perform(get("/admin/shipments").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));

        verify(lmsClient).searchPartners(any(SearchPartnerFilter.class));
        verify(lmsClient).getLogisticsPoints(any(LogisticsPointFilter.class));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Triple.of(
                "Поиск всех отгрузок",
                new AdminShipmentSearchFilter(),
                "controller/admin/shipment/response/all.json"
            ),
            Triple.of(
                "По идентификатору отгрузки",
                new AdminShipmentSearchFilter().setShipmentId(1L),
                "controller/admin/shipment/response/id_1.json"
            ),
            Triple.of(
                "По идентификатору заявки",
                new AdminShipmentSearchFilter().setShipmentApplicationId(1L),
                "controller/admin/shipment/response/id_1.json"
            ),
            Triple.of(
                "По внешнему идентификатору",
                new AdminShipmentSearchFilter().setExternalId("\t external-id-2 \t "),
                "controller/admin/shipment/response/id_2.json"
            ),
            Triple.of(
                "По дате создания",
                new AdminShipmentSearchFilter().setCreated(LocalDate.of(2019, Month.AUGUST, 2)),
                "controller/admin/shipment/response/id_2_and_3.json"
            ),
            Triple.of(
                "По типу отгрузки",
                new AdminShipmentSearchFilter().setShipmentType(ShipmentType.WITHDRAW),
                "controller/admin/shipment/response/id_1_and_4.json"
            ),
            Triple.of(
                "По дате отгрузки",
                new AdminShipmentSearchFilter().setShipmentDate(LocalDate.of(2019, Month.AUGUST, 3)),
                "controller/admin/shipment/response/id_1_and_2.json"
            ),
            Triple.of(
                "По статусу заявки",
                new AdminShipmentSearchFilter().setStatus(ShipmentApplicationStatus.NEW),
                "controller/admin/shipment/response/id_1_and_4.json"
            ),
            Triple.of(
                "По ID службы доставки",
                new AdminShipmentSearchFilter().setPartnerTo(2L),
                "controller/admin/shipment/response/id_2.json"
            ),
            Triple.of(
                "По MarketId партнера отгрузки",
                new AdminShipmentSearchFilter().setMarketIdFrom(20L),
                "controller/admin/shipment/response/id_2.json"
            ),
            Triple.of(
                "По ID склада отправки",
                new AdminShipmentSearchFilter().setWarehouseFromId(1L),
                "controller/admin/shipment/response/id_1.json"
            ),
            Triple.of(
                "По количеству заказов",
                new AdminShipmentSearchFilter().setOrdersCount(2L),
                "controller/admin/shipment/response/id_1.json"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }
}
