package ru.yandex.market.sc.internal.controller;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.ff.providers.OrdersInboundRegistryProvider;
import ru.yandex.market.sc.internal.ff.providers.SortablesInboundRegistryProvider;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.sc.internal.test.Template.fromFile;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FFApiControllerGetInboundDelegateTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final Clock clock;
    private SortingCenter sortingCenter;
    private String requestUID;

    @SpyBean
    private OrdersInboundRegistryProvider ordersRegistryProvider;

    @SpyBean
    private SortablesInboundRegistryProvider sortablesRegistryProvider;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        requestUID = UUID.randomUUID().toString().replace("-", "");
    }

    @ParameterizedTest(name = "getInbound запрос с типом {0} перенаправляется на провайдер Order реестров")
    @MethodSource("provideXDocTypes")
    void getInboundForXDocTypesRedirectedToSortableRegistryProvider(InboundType inboundType) {
        var inbound = createdInbound("inbound-1", inboundType);

        var request = fromFile("ffapi/inbound/getInboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getExternalId())
                .resolve();
        ScTestUtils.ffApiSuccessfulCall(mockMvc, request);
        verify(sortablesRegistryProvider, times(1)).getInboundRegistries(any(Inbound.class), any(ResourceId.class));
    }

    @ParameterizedTest(name = "getInbound запрос с типом {0} перенаправляется на провайдер Sortable реестров")
    @MethodSource("provideNonXDocTypes")
    void getInboundForNonXDocTypesRedirectedToOrdersRegistryProvider(InboundType inboundType) {
        var inbound = createdInbound("inbound-1", inboundType);

        var request = fromFile("ffapi/inbound/getInboundRequestTemplate.xml")
                .setValue("token", sortingCenter.getToken())
                .setValue("uniq", requestUID)
                .setValue("inboundId.yandexId", inbound.getExternalId())
                .setValue("inboundId.partnerId", inbound.getExternalId())
                .resolve();
        ScTestUtils.ffApiSuccessfulCall(mockMvc, request);
        verify(ordersRegistryProvider, times(1)).getInboundRegistries(any(Inbound.class), any(ResourceId.class));
    }

    private static Set<InboundType> provideXDocTypes() {
        return InboundType.getXDocTypes();
    }
    private static Set<InboundType> provideNonXDocTypes() {
        return InboundType.getNonXDocTypes();
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
