package ru.yandex.market.sc.internal.controller;

import java.time.Instant;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.outbound.OutboundCommandService;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatusHistoryLockRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPartner;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@ScIntControllerTest
public class FFApiControllerV2GetOutboundStatusTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    OutboundCommandService commandService;
    @Autowired
    OutboundStatusHistoryLockRepository statusHistoryLockRepository;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    JdbcTemplate jdbcTemplate;

    private SortingCenter sortingCenter;
    private Outbound createdOutbound;
    private Outbound cancelledOutbound;
    private SortingCenterPartner sortingCenterPartner;

    @BeforeEach
    void setup() {
        sortingCenterPartner = testFactory.storedSortingCenterPartner(1000, "sortingCenter-token");
        sortingCenter = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(100L)
                        .partnerName("Новый СЦ")
                        .sortingCenterPartnerId(sortingCenterPartner.getId())
                        .token(sortingCenterPartner.getToken())
                        .build());

        transactionTemplate.execute(ts -> {
            createdOutbound = testFactory.createOutbound(sortingCenter);
            cancelledOutbound = prepareCancelledOutbound();
            jdbcTemplate.update("UPDATE outbound " +
                    "SET updated_at = '2021-04-21T08:00:00Z' WHERE status = 'CREATED'");
            jdbcTemplate.update("UPDATE outbound_status_history " +
                    "SET created_at = '2021-04-21T08:00:00Z' WHERE status = 'CREATED'");
            jdbcTemplate.update("UPDATE outbound " +
                    "SET updated_at = '2021-04-21T09:00:00Z' WHERE status = 'CANCELLED_BY_SC'");
            jdbcTemplate.update("UPDATE outbound_status_history " +
                    "SET created_at = '2021-04-21T09:00:00Z' WHERE status = 'CANCELLED_BY_SC'");
            return null;
        });
    }

    @Test
    @SneakyThrows
    void getStatus() {
        ScTestUtils.ffApiV2SuccessfulCall(mockMvc,
                        sortingCenter.getToken(),
                        "getOutboundStatus",
                        outboundIdsRequestContent())
                .andExpect(content().xml(ScTestUtils.fileContent("ff_getOutboundStatus_response.xml")));
    }


    private Outbound prepareCancelledOutbound() {
        var outbound = testFactory.createOutbound(
                TestFactory.CreateOutboundParams.builder()
                        .externalId("222")
                        .fromTime(Instant.parse("2021-04-22T02:00:00Z"))
                        .toTime(Instant.parse("2021-04-22T12:00:00Z"))
                        .locationCreateRequest(TestFactory.locationCreateRequest())
                        .sortingCenter(sortingCenter)
                        .build()
        );
        commandService.cancelOutdated(Instant.parse("2021-04-22T06:00:00Z"));
        return outbound;
    }

    private String outboundIdsRequestContent() {
        String id1 = createdOutbound.getExternalId();
        String id2 = cancelledOutbound.getExternalId();
        return "<outboundIds>" +
                "<outboundId><yandexId>" + id1 + "</yandexId><partnerId>" + id1 + "</partnerId></outboundId>" +
                "<outboundId><yandexId>" + id2 + "</yandexId><partnerId>" + id2 + "</partnerId></outboundId>" +
                "</outboundIds>";
    }
}
