package ru.yandex.market.logistics.nesu.utils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.model.LmsFactory;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.when;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
@ParametersAreNonnullByDefault
public class SenderAvailableDeliveriesUtils {
    /**
     * Мокирование данных для получения доступных вариантов подключения магазина.
     *
     * @param lmsClient        клиент для вызова методов LMS
     * @param sortingCenter    партнёр сортировочного центра
     * @param deliveryServices список служб доставки
     * @param warehouses       список складов для поиска
     */
    public static void mockGetSenderAvailableDeliveries(
        LMSClient lmsClient,
        PartnerResponse sortingCenter,
        List<PartnerResponse> deliveryServices,
        List<LogisticsPointResponse> warehouses
    ) {
        long sortingCenterId = sortingCenter.getId();

        when(lmsClient.getLogisticsPoints(refEq(LmsFactory.createLogisticsPointsFilter(
            warehouses.stream().map(LogisticsPointResponse::getId).collect(Collectors.toSet()),
            null,
            PointType.WAREHOUSE,
            true
        ))))
            .thenReturn(warehouses);

        when(lmsClient.searchPartners(
            refEq(LmsFactory.createPartnerFilter(
                Stream.concat(deliveryServices.stream(), Stream.of(sortingCenter))
                    .map(PartnerResponse::getId)
                    .collect(Collectors.toSet()),
                null
            ))
        ))
            .thenReturn(
                Stream.concat(deliveryServices.stream(), Stream.of(sortingCenter))
                    .collect(Collectors.toList())
            );

        when(lmsClient.searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnersIds(Set.of(sortingCenterId))
                .enabled(true)
                .build()
        ))
            .thenReturn(
                deliveryServices.stream()
                    .map(PartnerResponse::getId)
                    .map(
                        id -> PartnerRelationEntityDto.newBuilder()
                            .fromPartnerId(sortingCenterId)
                            .toPartnerId(id)
                            .build()
                    )
                    .collect(Collectors.toList())
            );
    }
}
