package ru.yandex.market.sc.internal.controller;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPartner;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.sc.internal.test.Template.fromFile;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FFApiControllerV2CancelInboundTest {
    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final InboundRepository inboundRepository;
    private final Clock clock;
    private SortingCenter sortingCenter;
    private String requestUID;

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
        requestUID = UUID.randomUUID().toString().replace("-", "");
    }

    @Test
    @DisplayName("cancelInbound")
    @SneakyThrows
    void cancelInbound() {
        String yandexId = "TMU12345";
        Inbound inbound = createdInbound(yandexId, InboundType.XDOC_TRANSIT, InboundStatus.CREATED);
        String partnerId = inbound.getInboundInfo().getId().toString();

        var request = fromFile("ffapi/inbound/cancelInboundRequestTemplate.xml")
            .setValue("token", sortingCenter.getToken())
            .setValue("uniq", requestUID)
            .setValue("inboundId.yandexId", yandexId)
            .setValue("inboundId.partnerId", partnerId)
            .resolve();

        var expectedResponse = fromFile("ffapi/inbound/cancelInboundResponseTemplate.xml")
            .setValue("uniq", requestUID)
            .setValue("inboundId.yandexId", yandexId)
            .setValue("inboundId.partnerId", partnerId)
            .resolve();

        ScTestUtils.ffApiV2SuccessfulCall(mockMvc, request)
            .andExpect(content().xml(expectedResponse));

        var actualInbound = testFactory.getInbound(inbound.getId());
        assertThat(actualInbound.getInboundStatus()).isEqualTo(InboundStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelInbound incorrect status")
    @SneakyThrows
    void cancelInboundIncorrectStatus() {
        String yandexId = "TMU12345";
        Inbound inbound = createdInbound(yandexId, InboundType.XDOC_TRANSIT, InboundStatus.IN_PROGRESS);
        String partnerId = inbound.getInboundInfo().getId().toString();

        var request = fromFile("ffapi/inbound/cancelInboundRequestTemplate.xml")
            .setValue("token", sortingCenter.getToken())
            .setValue("uniq", requestUID)
            .setValue("inboundId.yandexId", yandexId)
            .setValue("inboundId.partnerId", partnerId)
            .resolve();

        ScTestUtils.ffApiV2ErrorCall(
            mockMvc,
            request,
            "Unexpected technical error. Please check logs for details."
        );

        var actualInbound = testFactory.getInbound(inbound.getId());
        assertThat(actualInbound.getInboundStatus()).isEqualTo(InboundStatus.IN_PROGRESS);
    }

    private Inbound createdInbound(String externalId, InboundType inboundType, InboundStatus inboundStatus) {
        var params = TestFactory.CreateInboundParams.builder()
            .inboundExternalId(externalId)
            .inboundType(inboundType)
            .fromDate(OffsetDateTime.now(clock))
            .warehouseFromExternalId("warehouse-from-id")
            .toDate(OffsetDateTime.now(clock))
            .sortingCenter(sortingCenter)
            .registryMap(Map.of())
            .build();

        Inbound inbound = testFactory.createInbound(params);

        inbound.setInboundStatus(inboundStatus);
        inboundRepository.save(inbound);

        return inbound;
    }

}
