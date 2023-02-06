package ru.yandex.market.sc.internal.controller;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPartner;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.inbound.repository.RegistryType.PLANNED;
import static ru.yandex.market.sc.internal.test.Template.fromFile;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FFApiControllerV2GetInboundStatusAndStatusHistoryTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private String requestUID;
    private SortingCenter sortingCenter;
    private SortingCenterPartner sortingCenterPartner;

    @MockBean
    private Clock clock;

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
        requestUID = UUID.randomUUID().toString().replace("-", "");
        testFactory.setupMockClock(clock);
    }

    @Test
    @DisplayName("getInboundStatus. Проверка времени выставления статуса.")
    @SneakyThrows
    void getInboundStatusUpdatedAt() {
        var inbound = createdInbound("inbound-2", InboundType.DEFAULT);
        var registry = testFactory.bindRegistry(inbound, "registryId-2", PLANNED);
        testFactory.bindInboundOrder(inbound, registry, "placeId-2", "palletId-2");

        var request = fromFile("ffapi/inbound/getInboundStatusRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getExternalId())
                .resolve();

        String historyRequest = fromFile("ffapi/inbound/getInboundStatusHistoryRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getExternalId())
                .resolve();

        String getStatusResponse = ScTestUtils.ffApiV2SuccessfulCall(mockMvc, request)
                .andReturn().getResponse().getContentAsString();
        String getStatusHistoryResponse = ScTestUtils.ffApiV2SuccessfulCall(mockMvc, historyRequest)
                .andReturn().getResponse().getContentAsString();
        String setDateStatus = StringUtils
                .substringBetween(getStatusResponse, "setDate", "</setDate>");
        String setDateStatusHistory = StringUtils
                .substringBetween(getStatusHistoryResponse, "setDate", "</setDate>");
        assertThat(setDateStatus).isEqualTo(setDateStatusHistory);
    }

    private Inbound createdInbound(String externalId, InboundType inboundType) {
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(externalId)
                .inboundType(inboundType)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build();
        return testFactory.createInbound(params);
    }
}
