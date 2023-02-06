package ru.yandex.market.logistics.lom.admin;

import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Скачивание списка отгрузок")
@DatabaseSetup("/controller/admin/shipment/before/shipments.xml")
class DownloadShipmentsTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(lmsClient);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Скачивание отгрузок")
    void downloadOrdersFile(
        @SuppressWarnings("unused") String displayName,
        AdminShipmentSearchFilter filter,
        String responsePath
    ) throws Exception {
        when(lmsClient.searchPartners(any(SearchPartnerFilter.class)))
            .thenAnswer(invocation -> invocation.<SearchPartnerFilter>getArgument(0)
                .getIds()
                .stream()
                .map(
                    id -> PartnerResponse.newBuilder()
                        .id(id)
                        .readableName("partner_" + id)
                        .build()
                )
                .collect(Collectors.toList())
            );
        when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class)))
            .thenAnswer(
                invocation -> invocation.<LogisticsPointFilter>getArgument(0)
                    .getIds()
                    .stream()
                    .map(id -> LmsFactory.createWarehouseResponseBuilder(id).build())
                    .collect(Collectors.toList())
            );

        downloadShipments(filter)
            .andExpect(status().isOk())
            .andExpect(content().string(extractFileContent(responsePath)));

        verify(lmsClient).searchPartners(any(SearchPartnerFilter.class));
        verify(lmsClient).getLogisticsPoints(any(LogisticsPointFilter.class));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminShipmentSearchFilter(),
                "controller/admin/shipment/download/shipments_all.csv"
            ),
            Arguments.of(
                "По идентификатору отгрузки",
                new AdminShipmentSearchFilter().setShipmentId(1L),
                "controller/admin/shipment/download/shipments_id_1.csv"
            ),
            Arguments.of(
                "По идентификатору заявки",
                new AdminShipmentSearchFilter().setShipmentApplicationId(1L),
                "controller/admin/shipment/download/shipments_id_1.csv"
            ),
            Arguments.of(
                "По идентификатору заявки в службе",
                new AdminShipmentSearchFilter().setExternalId("external-id-2"),
                "controller/admin/shipment/download/shipments_id_2.csv"
            ),
            Arguments.of(
                "По MarketId партнера отгрузки",
                new AdminShipmentSearchFilter().setMarketIdFrom(10L),
                "controller/admin/shipment/download/shipments_id_1.csv"
            ),
            Arguments.of(
                "По типу отгрузки",
                new AdminShipmentSearchFilter().setShipmentType(ShipmentType.WITHDRAW),
                "controller/admin/shipment/download/shipments_id_1_4.csv"
            ),
            Arguments.of(
                "По идентификатору склада отправки",
                new AdminShipmentSearchFilter().setWarehouseFromId(1L),
                "controller/admin/shipment/download/shipments_id_1.csv"
            ),
            Arguments.of(
                "По дате отгрузки",
                new AdminShipmentSearchFilter().setShipmentDate(LocalDate.parse("2019-08-03")),
                "controller/admin/shipment/download/shipments_id_1_2.csv"
            ),
            Arguments.of(
                "По статусу",
                new AdminShipmentSearchFilter().setStatus(ShipmentApplicationStatus.NEW),
                "controller/admin/shipment/download/shipments_id_1_4.csv"
            ),
            Arguments.of(
                "По дате создания",
                new AdminShipmentSearchFilter().setCreated(LocalDate.parse("2019-08-02")),
                "controller/admin/shipment/download/shipments_id_2_3.csv"
            ),
            Arguments.of(
                "По количеству заказов",
                new AdminShipmentSearchFilter().setOrdersCount(2L),
                "controller/admin/shipment/download/shipments_id_1.csv"
            ),
            Arguments.of(
                "По всем полям",
                new AdminShipmentSearchFilter()
                    .setShipmentId(2L)
                    .setShipmentApplicationId(2L)
                    .setExternalId("external-id-2")
                    .setMarketIdFrom(20L)
                    .setShipmentType(ShipmentType.IMPORT)
                    .setWarehouseFromId(3L)
                    .setShipmentDate(LocalDate.parse("2019-08-03"))
                    .setStatus(ShipmentApplicationStatus.REGISTRY_SENT)
                    .setCreated(LocalDate.parse("2019-08-02"))
                    .setOrdersCount(1L),
                "controller/admin/shipment/download/shipments_id_2.csv"
            )
        );
    }

    @Test
    @DisplayName("Скачивание по несуществующему идентификатору отгрузки")
    void notExistingShipment() throws Exception {
        downloadShipments(new AdminShipmentSearchFilter().setShipmentId(100L))
            .andExpect(status().isOk())
            .andExpect(content().string(extractFileContent("controller/admin/shipment/download/shipments_empty.csv")));
    }

    private ResultActions downloadShipments(AdminShipmentSearchFilter shipmentSearchFilter) throws Exception {
        return mockMvc.perform(
            get("/admin/shipments/download/shipments-file-csv")
                .params(toParams(shipmentSearchFilter))
        );
    }
}
