package ru.yandex.market.logistics.nesu.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;
import one.util.streamex.EntryStream;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.ownDeliveryFilter;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartner;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartnerFilter;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartnerResponseBuilder;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
@ParametersAreNonnullByDefault
public class ShopAvailableDeliveriesUtils {
    public static final SearchPartnerFilter OWN_DELIVERY_FILTER = ownDeliveryFilter().build();

    public static final SearchPartnerFilter AVAILABLE_DIRECTLY_PARTNERS_FILTER = createPartnerFilter(
        Set.of(1L, 2L, 3L),
        null
    );

    public static final SearchPartnerFilter ALL_AVAILABLE_PARTNERS_FILTER = createPartnerFilter(
        Set.of(1L, 2L, 3L, 4L, 5L),
        null
    );

    public static final PartnerRelationFilter SORTING_CENTER_RELATION_FILTER = PartnerRelationFilter.newBuilder()
        .fromPartnersIds(Set.of(1L, 2L, 3L))
        .fromPartnerTypes(Set.of(PartnerType.SORTING_CENTER))
        .enabled(true)
        .build();

    public static void mockShopAvailableDeliveries(LMSClient lmsClient) {
        List<PartnerResponse> partners = List.of(
            createPartner(1L, 1L, PartnerType.SORTING_CENTER),
            createPartner(2L, 1L, PartnerType.SORTING_CENTER),
            createPartner(3L, 1L, PartnerType.DELIVERY),
            createPartnerResponseBuilder(4L, PartnerType.DELIVERY, 1)
                .status(PartnerStatus.ACTIVE)
                .params(List.of(
                    new PartnerExternalParam("ASSESSED_VALUE_TOTAL_CHECK", "Description", null)
                ))
                .businessId(41L)
                .readableName("Sample Readable Partner")
                .build(),
            createPartnerResponseBuilder(5L, PartnerType.DELIVERY, 1)
                .status(PartnerStatus.ACTIVE)
                .params(List.of(
                    new PartnerExternalParam("ASSESSED_VALUE_TOTAL_CHECK", "Description", "1"),
                    new PartnerExternalParam("POSTAL_CODE_NEEDED", "Description", "True")
                ))
                .businessId(41L)
                .readableName("Sample Readable Partner")
                .build()
        );

        mockShopAvailableDeliveries(
            lmsClient,
            1L,
            partners,
            Set.of(1L, 2L, 3L),
            Map.of(1L, Set.of(3L, 4L), 2L, Set.of(5L))
        );
    }

    /**
     * Мокирование данных для получения доступных вариантов подключения магазина.
     *
     * @param lmsClient                   клиент для вызова методов LMS
     * @param marketId                    маркетный идентификатор магазина
     * @param partners                    список всех доступных партнёров
     * @param availableDirectlyPartnerIds список идентификаторов партнёров доступных напрямую (в том числе СЦ)
     * @param sortingCenterRelations      мапа связок партнёров СЦ и партнёров СД
     */
    public static void mockShopAvailableDeliveries(
        LMSClient lmsClient,
        Long marketId,
        List<PartnerResponse> partners,
        Set<Long> availableDirectlyPartnerIds,
        Map<Long, Set<Long>> sortingCenterRelations
    ) {
        when(lmsClient.searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnersIds(availableDirectlyPartnerIds)
                .fromPartnerTypes(Set.of(PartnerType.SORTING_CENTER))
                .enabled(true)
                .build()
        ))
            .thenReturn(
                EntryStream.of(sortingCenterRelations)
                    .flatMapValues(Collection::stream)
                    .mapKeyValue((soringCenterId, deliveryServiceId) ->
                        PartnerRelationEntityDto.newBuilder()
                            .fromPartnerId(soringCenterId).toPartnerId(deliveryServiceId)
                            .build()
                    )
                    .collect(Collectors.toList())
            );

        when(lmsClient.searchPartners(any())).thenAnswer(
            invocation -> {
                Set<Long> partnerIds = ((SearchPartnerFilter) invocation.getArgument(0)).getIds();
                if (partnerIds == null) {
                    return List.of();
                }
                return partners.stream()
                    .filter(partner -> partnerIds.contains(partner.getId()))
                    .collect(Collectors.toList());
            }
        );

        doReturn(List.of(TestOwnDeliveryUtils.partnerBuilder().build()))
            .when(lmsClient).searchPartners(ownDeliveryFilter().setMarketIds(Set.of(marketId)).build());
    }
}
