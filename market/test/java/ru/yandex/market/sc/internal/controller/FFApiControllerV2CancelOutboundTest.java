package ru.yandex.market.sc.internal.controller;

import java.time.Clock;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPartner;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.sc.internal.test.Template.fromFile;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FFApiControllerV2CancelOutboundTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final OutboundRepository outboundRepository;
    private final Clock clock;
    private SortingCenter sortingCenter;
    private String requestUID;
    private Warehouse warehouse;

    @BeforeEach
    void init() {
        SortingCenterPartner sortingCenterPartner =
            testFactory.storedSortingCenterPartner(1000, "sortingCenter-token");
        sortingCenter = testFactory.storedSortingCenter(
            TestFactory.SortingCenterParams.builder()
                .id(100L)
                .partnerName("Новый СЦ")
                .sortingCenterPartnerId(sortingCenterPartner.getId())
                .token(sortingCenterPartner.getToken())
                .yandexId("6667778881")
                .build());
        warehouse = testFactory.storedWarehouse();
        requestUID = UUID.randomUUID().toString().replace("-", "");
    }

    @Test
    @DisplayName("cancelOutbound")
    @SneakyThrows
    void cancelOutbound() {
        String yandexId = "TMU12345";
        Outbound outbound = createdOutbound(yandexId, OutboundType.XDOC, OutboundStatus.CREATED);
        String partnerId = outbound.getId().toString();

        var request = fromFile("ffapi/outbound/cancelOutboundRequestTemplate.xml")
            .setValue("token", sortingCenter.getToken())
            .setValue("uniq", requestUID)
            .setValue("outboundId.yandexId", yandexId)
            .setValue("outboundId.partnerId", partnerId)
            .resolve();

        var expectedResponse = fromFile("ffapi/outbound/cancelOutboundResponseTemplate.xml")
            .setValue("uniq", requestUID)
            .setValue("outboundId.yandexId", yandexId)
            .setValue("outboundId.partnerId", partnerId)
            .resolve();

        ScTestUtils.ffApiV2SuccessfulCall(mockMvc, request)
            .andExpect(content().xml(expectedResponse));

        var actualOutbound = testFactory.getOutbound(yandexId);
        assertThat(actualOutbound.getStatus()).isEqualTo(OutboundStatus.CANCELLED_BY_LOGISTICS);
    }

    @Test
    @DisplayName("cancelOutbound incorrect status")
    @SneakyThrows
    void cancelOutboundIncorrectStatus() {
        String yandexId = "TMU12345";
        Outbound outbound = createdOutbound(yandexId, OutboundType.XDOC, OutboundStatus.SHIPPED);
        String partnerId = outbound.getId().toString();

        var request = fromFile("ffapi/outbound/cancelOutboundRequestTemplate.xml")
            .setValue("token", sortingCenter.getToken())
            .setValue("uniq", requestUID)
            .setValue("outboundId.yandexId", yandexId)
            .setValue("outboundId.partnerId", partnerId)
            .resolve();

        ScTestUtils.ffApiV2ErrorCall(
            mockMvc,
            request,
            "Unexpected technical error. Please check logs for details."
        );

        var actualOutbound = testFactory.getOutbound(outbound.getExternalId());
        assertThat(actualOutbound.getStatus()).isEqualTo(OutboundStatus.SHIPPED);
    }

    private Outbound createdOutbound(String externalId, OutboundType outboundType, OutboundStatus outboundStatus) {
        var params = TestFactory.CreateOutboundParams.builder()
            .externalId(externalId)
            .type(outboundType)
            .fromTime(clock.instant())
            .toTime(clock.instant())
            .sortingCenter(sortingCenter)
            .partnerToExternalId(warehouse.getPartnerId())
            .logisticPointToExternalId(warehouse.getYandexId())
            .build();

        Outbound outbound = testFactory.createOutbound(params);

        outbound.setStatus(outboundStatus);
        outboundRepository.save(outbound);

        return outbound;
    }
}
