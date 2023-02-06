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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatusHistoryRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPartner;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.sc.core.domain.inbound.repository.RegistryType.PLANNED;
import static ru.yandex.market.sc.internal.test.Template.fromFile;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FFApiControllerV2GetInboundTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final XDocFlow flow;
    private final ScIntControllerCaller caller;
    private final InboundRepository inboundRepository;
    private final InboundStatusHistoryRepository inboundStatusHistoryRepository;
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
    @DisplayName("getInbound для ff-apiV2")
    @SneakyThrows
    void getInboundNonXDoc() {
        var inbound = createdInbound("inbound-1", InboundType.DEFAULT);
        var registry = testFactory.bindRegistry(inbound, "registryId-1", PLANNED);
        testFactory.bindInboundOrder(inbound, registry, "placeId-1", "palletId-1");

        var request = fromFile("ffapi/inbound/getInboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getExternalId())
                .resolve();

        var expectedResponse = fromFile("ffapi/inbound/getInboundResponseTemplate.xml")
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getId().toString())
                .setValue("registryId.yandexId", registry.getExternalId())
                .setValue("registryId.partnerId", registry.getExternalId())
                .setValue("placeId", "placeId-1")
                .setValue("palletId", "palletId-1")
                .resolve();

        ScTestUtils.ffApiV2SuccessfulCall(mockMvc, request)
                .andExpect(content().xml(expectedResponse));
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
