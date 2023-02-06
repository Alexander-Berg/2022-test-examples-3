package ru.yandex.market.sc.internal.controller;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.internal.test.ScTestUtils.fileContent;

@ScIntControllerTest
class FFApiControllerPutOutboundTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;

    private SortingCenter sortingCenter;
    private Warehouse warehouse;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        warehouse = testFactory.storedWarehouse();
    }

    @Test
    void ffApiRequest() {
        String externalId = "my_ext_id_123";
        String body = String.format(fileContent("ff_putOutbound.xml"), sortingCenter.getToken(), externalId, warehouse.getYandexId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        var outbound = testFactory.getOutbound(externalId);
        assertThat(outbound.getExternalId()).isEqualTo(externalId);
        assertThat(outbound.getFromTime()).isEqualTo(Instant.parse("2020-03-20T09:00:00Z"));
        assertThat(outbound.getToTime()).isEqualTo(Instant.parse("2020-04-21T09:00:00Z"));
        assertThat(outbound.getMovementCourier().getExternalId()).isEqualTo("106");
        assertThat(outbound.getPartnerToExternalId()).isEqualTo("172");
        assertThat(outbound.getLogisticPointToExternalId()).isEqualTo("10000123");
        assertThat(outbound.getComment()).isEqualTo("comment about outbound");
        assertThat(outbound.getSortingCenter()).isEqualTo(sortingCenter);
        assertThat(outbound.getTransportationId()).isEqualTo("TM-45645645");

        // Update outbound
        body = String.format(fileContent("ff_putOutbound_2.xml"), sortingCenter.getToken(), externalId, warehouse.getYandexId());
        ScTestUtils.ffApiSuccessfulCall(mockMvc, body);
        outbound = testFactory.getOutbound(externalId);
        assertThat(outbound.getExternalId()).isEqualTo(externalId);
        assertThat(outbound.getFromTime()).isEqualTo(Instant.parse("2020-01-20T09:00:00Z"));
        assertThat(outbound.getToTime()).isEqualTo(Instant.parse("2020-01-21T09:00:00Z"));
        assertThat(outbound.getMovementCourier().getExternalId()).isEqualTo("777");
        assertThat(outbound.getPartnerToExternalId()).isEqualTo("172");
        assertThat(outbound.getLogisticPointToExternalId()).isEqualTo("10000123");
        assertThat(outbound.getComment()).isEqualTo("another comment");
        assertThat(outbound.getSortingCenter()).isEqualTo(sortingCenter);
        assertThat(outbound.getTransportationId()).isEqualTo("TM-45645645");
    }
}
