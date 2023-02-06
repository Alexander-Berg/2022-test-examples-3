package ru.yandex.market.logistics.management.service.export.dynamic;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.support.TransactionCallback;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.domain.entity.DynamicFault;
import ru.yandex.market.logistics.management.domain.entity.PlatformClient;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.DeliveryDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.WarehouseDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;

class PartnerStatusBuilderTest extends AbstractDynamicBuilderTest {

    private static final PlatformClient BERU = new PlatformClient()
        .setId(1L)
        .setName("Беру");
    private static final PlatformClient BRINGLY = new PlatformClient()
        .setId(2L)
        .setName("Брингли");

    private static final List<DeliveryDto> DELIVERIES = createDeliveries();
    private static final List<WarehouseDto> FULFILLMENTS = createFulfillments();

    @BeforeEach
    public void setUp() {
        capacityPrepareService =
            new CapacityPrepareService(new CapacityTreeProcessorService(new RegionHelper(regionService)));
        factory = new DeliveryCapacityBuilderFactory(regionService, capacityPrepareService, capacityMergeService);
        initBuilder();

        Mockito.doAnswer((Answer<Pair<List<PartnerRelationDto>, List<DynamicFault>>>) invocation ->
            Pair.of(invocation.getArgument(0), Collections.emptyList())).when(validationService)
            .validate(Mockito.anyList());

        Stream.of(DELIVERIES, FULFILLMENTS)
            .flatMap(Collection::stream)
            .forEach(d -> d.setStatus(PartnerStatus.ACTIVE));

        Mockito.doReturn(ImmutableSet.of(BERU, BRINGLY))
            .when(platformClientService).findAllForDynamic();

        Mockito.doReturn(createPlatformClientPartners(DELIVERIES, FULFILLMENTS))
            .when(partnerPlatformClientRepository).getStatusByPartnerIdMap(BRINGLY);

        Mockito.doReturn(createPartnerRelations(DELIVERIES, FULFILLMENTS))
            .when(partnerRelationRepository).findAllForDynamic(any(), anySet(), anySet(), any());

        Mockito.when(transactionTemplate.execute(any())).thenAnswer((Answer) invocation -> {
            Object[] args = invocation.getArguments();
            return ((TransactionCallback) args[0]).doInTransaction(null);
        });
    }

    @ParameterizedTest(name = "{index} : {2}")
    @MethodSource("platformStatusArguments")
    void platformStatus(List<PartnerDto> partners, PartnerStatus status, @SuppressWarnings("unused") String caseName) {
        Mockito.doReturn(inactivePartnersOnBeru(partners, status))
            .when(partnerPlatformClientRepository).getStatusByPartnerIdMap(BERU);

        checkStatus(false, BERU);
        checkStatus(true, BRINGLY);
    }

    @ParameterizedTest(name = "{index} : {2}")
    @MethodSource("platformStatusArguments")
    void globallyStatus(List<PartnerDto> partners, PartnerStatus status, @SuppressWarnings("unused") String caseName) {
        Mockito.doReturn(createPlatformClientPartners(DELIVERIES, FULFILLMENTS))
            .when(partnerPlatformClientRepository).getStatusByPartnerIdMap(BERU);

        partners.forEach(p -> p.setStatus(status));

        checkStatus(false, BERU);
        checkStatus(false, BRINGLY);
    }

    static Stream<? extends Arguments> platformStatusArguments() {
        return Stream.of(
            Arguments.arguments(DELIVERIES, PartnerStatus.INACTIVE, "inactive deliveries"),
            Arguments.arguments(FULFILLMENTS, PartnerStatus.INACTIVE, "inactive fu lfillments")
        );
    }

    private Map<Long, PartnerStatus> inactivePartnersOnBeru(List<PartnerDto> partners, PartnerStatus status) {
        Map<Long, PartnerStatus> platformClientPartners =
            createPlatformClientPartners(DELIVERIES, FULFILLMENTS);
        Set<Long> ids = partners
            .stream()
            .map(PartnerDto::getId)
            .collect(Collectors.toSet());

        return platformClientPartners.entrySet().stream()
            .filter(e -> ids.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> status));
    }

    private void checkStatus(boolean active, PlatformClient platform) {
        Logistics.MetaInfo metaInfo = buildReport(platform);

        metaInfo.getWarehousesAndDeliveryServicesList()
            .forEach(v -> softly.assertThat(v.getIsActive()).isEqualTo(active));
    }
}
