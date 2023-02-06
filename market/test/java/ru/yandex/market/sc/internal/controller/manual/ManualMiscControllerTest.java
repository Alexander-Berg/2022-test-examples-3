package ru.yandex.market.sc.internal.controller.manual;

import java.util.List;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.courier.repository.CourierRepository;
import ru.yandex.market.sc.core.domain.inbound.model.TmInboundDto;
import ru.yandex.market.sc.core.domain.inbound.model.TmInboundStatus;
import ru.yandex.market.sc.core.domain.sorting_center.model.DistributionCenterStateDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseRepository;
import ru.yandex.market.sc.core.external.tm.TmClient;
import ru.yandex.market.sc.core.external.tm.model.PutScStateRequest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScIntControllerTest
public class ManualMiscControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestFactory testFactory;

    @Autowired
    WarehouseRepository warehouseRepository;

    @Autowired
    CourierRepository courierRepository;

    @Autowired
    SortingCenterRepository sortingCenterRepository;

    @Autowired
    ScIntControllerCaller caller;

    @Autowired
    XDocFlow flow;

    @MockBean
    TmClient tmClient;

    private Warehouse warehouse;

    private Courier courier;

    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        warehouse = testFactory.storedWarehouse();
        courier = testFactory.storedCourier();
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    @SneakyThrows
    void updateIncorporationTest() {
        String newName = "new name";
        mockMvc.perform(
                MockMvcRequestBuilders.put(String.format("/manual/warehouses/%s?name=%s",
                        warehouse.getYandexId(), newName))
        ).andExpect(status().is2xxSuccessful());

        Warehouse updatedWarehouse = warehouseRepository.findByYandexIdOrThrow(warehouse.getYandexId());

        assertThat(updatedWarehouse.getIncorporation()).isEqualTo(newName);
    }

    @Test
    @SneakyThrows
    void updateCourierNameByIdTest() {
        String newName = "new courier name";
        mockMvc.perform(
                MockMvcRequestBuilders.put(String.format("/manual/couriers/%d?name=%s",
                        courier.getId(), newName))
        ).andExpect(status().is2xxSuccessful());

        var updatedCourier = courierRepository.findByIdOrThrow(courier.getId());

        assertThat(updatedCourier.getName()).isEqualTo(newName);
    }

    @Test
    void updateSortingCenter() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.put("/manual/sortingCenters/" + sortingCenter.getId())
                .content("{\"address\": \"Партизанская 181\","
                        + "\"partnerId\": \"32\","
                        + "\"logisticPointId\": \"429\","
                        + "\"partnerName\": \"ООО рамашка\""
                        + "}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().is2xxSuccessful());

        var updated = sortingCenterRepository.findByIdOrThrow(sortingCenter.getId());
        assertThat(updated.getAddress()).isEqualTo("Партизанская 181");
        assertThat(updated.getPartnerId()).isEqualTo("32");
        assertThat(updated.getYandexId()).isEqualTo("429");
        assertThat(updated.getPartnerName()).isEqualTo("ООО рамашка");
    }

    @Test
    void getDiffBetweenTmAndSc() {
        testFactory.storedUser(flow.getSortingCenter(), TestFactory.USER_UID_LONG);
        TmInboundDto first = new TmInboundDto()
                .setBoxNumber(0)
                .setPalletNumber(1)
                .setStatus(TmInboundStatus.ARRIVED_ON_DC)
                .setInformationListCode("Зп-111");

        TmInboundDto second = new TmInboundDto()
                .setBoxNumber(0)
                .setPalletNumber(0)
                .setStatus(TmInboundStatus.CONFIRMED)
                .setInformationListCode("Зп-112");

        when(tmClient.getDcState(flow.getSortingCenter().getId()))
                .thenReturn(new DistributionCenterStateDto(List.of(first, second)));

        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-111")
                .build()
                .linkPallets("XDOC-1")
                .fixInbound()
                .inboundBuilder("in-2")
                .informationListBarcode("Зп-112")
                .build();

        caller.getDiffBetweenTmAndSc(flow.getSortingCenter().getId())
                .andExpect(status().isOk());
    }

    @Test
    void pushXDocSortingCenterState() {
        testFactory.setSortingCenterProperty(sortingCenter.getId(), SortingCenterPropertiesKey.XDOC_ENABLED, true);
        caller.pushXDocSortingCenterState(sortingCenter.getId()).andExpect(status().isOk());

        verify(tmClient, only()).putScState(eq(sortingCenter), any(PutScStateRequest.class));
    }

}
