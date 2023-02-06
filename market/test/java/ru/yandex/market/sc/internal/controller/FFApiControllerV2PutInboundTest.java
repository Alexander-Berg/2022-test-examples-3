package ru.yandex.market.sc.internal.controller;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.courier.repository.CourierRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundInfoRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPartner;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.internal.test.ScTestUtils.fileContent;

@ScIntControllerTest
public class FFApiControllerV2PutInboundTest {
    private static final String INBOUND_WITH_COURIER_TEMPLATE =
            "put_inbound_dropoff/ff_putInbound_addCourier_sample.xml";

    private static final String INBOUND_WITHOUT_COURIER_TEMPLATE =
            "put_inbound_dropoff/ff_putInbound_createInbound_withoutCourierNode_sample.xml";

    private static final String EXTERNAL_ID = "my_ext_id_123";
    private static final String COURIER_YANDEX_ID = "curYaId";
    private static final Long COURIER_UID = null;


    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;

    @Autowired
    CourierRepository courierRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    InboundInfoRepository inboundInfoRepository;

    private SortingCenter sortingCenter;
    private SortingCenterPartner sortingCenterPartner;

    @BeforeEach
    void init() {
        sortingCenterPartner = testFactory.storedSortingCenterPartner(1000, "sortingCenter-token");
        sortingCenter = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(100L)
                        .partnerName("Новый СЦ")
                        .sortingCenterPartnerId(sortingCenterPartner.getId())
                        .token(sortingCenterPartner.getToken())
                        .yandexId("6667778881")
                        .build());
    }

    @Test
    void ffApiV2Request() {
        String body = String.format(fileContent("ff_putInbound.xml"), sortingCenter.getToken(), EXTERNAL_ID, sortingCenter.getId());
        ScTestUtils.ffApiV2SuccessfulCall(mockMvc, body);
        var inbound = testFactory.getInbound(EXTERNAL_ID);
        assertThat(inbound.getExternalId()).isEqualTo(EXTERNAL_ID);
        assertThat(inbound.getWarehouseFromId()).isEqualTo(null);
        assertThat(inbound.getFromDate()).isEqualTo(OffsetDateTime.of(2020, 3, 20, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getToDate()).isEqualTo(OffsetDateTime.of(2020, 4, 21, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getMovementCourier().getExternalId()).isEqualTo("106");
        assertThat(inbound.getComment()).isEqualTo("comment about inbound");
        assertThat(inbound.getSortingCenter()).isEqualTo(sortingCenter);
        assertThat(inbound.getTransportationId()).isEqualTo("TM-123123");

        //update inbound
        body = String.format(fileContent("ff_putInbound_2.xml"), sortingCenter.getToken(), EXTERNAL_ID, sortingCenter.getId());
        ScTestUtils.ffApiV2SuccessfulCall(mockMvc, body);
        inbound = testFactory.getInbound(EXTERNAL_ID);
        assertThat(inbound.getExternalId()).isEqualTo(EXTERNAL_ID);
        assertThat(inbound.getWarehouseFromId()).isEqualTo(null);
        assertThat(inbound.getFromDate()).isEqualTo(OffsetDateTime.of(2020, 1, 20, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getToDate()).isEqualTo(OffsetDateTime.of(2020, 1, 21, 12, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID));
        assertThat(inbound.getMovementCourier().getExternalId()).isEqualTo("777");
        assertThat(inbound.getComment()).isEqualTo("another comment");
        assertThat(inbound.getSortingCenter()).isEqualTo(sortingCenter);
        assertThat(inbound.isConfirmed()).isEqualTo(true);
        assertThat(inbound.getTransportationId()).isEqualTo("TM-123123");
    }
}
