package ru.yandex.market.tpl.core.domain.routing.movement;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementTestBuilder;
import ru.yandex.market.tpl.core.domain.movement.SameDayDropoffService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.RoutingGeoPointMapper;
import ru.yandex.market.tpl.core.domain.routing.tag.RequiredRoutingTag;
import ru.yandex.market.tpl.core.external.routing.api.DimensionsClass;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.routing.movement.MovementsRequestItemsCollector.DROPOFF_RETURN_REF_PREFIX;
import static ru.yandex.market.tpl.core.domain.routing.movement.MovementsRequestItemsCollector.DROPSHIPS_REF_PREFIX;
import static ru.yandex.market.tpl.core.domain.routing.movement.MovementsRequestItemsCollector.DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY;
import static ru.yandex.market.tpl.core.domain.routing.movement.MovementsRequestItemsCollector.DROPSHIP_FASHION_ORDERS_COUNT;

@ExtendWith(MockitoExtension.class)
class MovementsRequestItemsCollectorTest {

    @InjectMocks
    private MovementsRequestItemsCollector subject;
    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @Mock
    private SameDayDropoffService sameDayDropoffService;

    @Test
    void isDropshipExist() {
        assertThat(
                subject.isDropshipExist(List.of(
                        Movement.builder()
                                .tags(List.of())
                                .build(),
                        Movement.builder()
                                .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                                .build()
                ))
        ).isTrue();

        assertThat(
                subject.isDropshipExist(List.of(
                        Movement.builder()
                                .tags(null)
                                .build(),
                        Movement.builder()
                                .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                                .build()
                ))
        ).isTrue();

        assertThat(
                subject.isDropshipExist(List.of(
                        Movement.builder()
                                .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                                .build()
                ))
        ).isFalse();
    }

    @Test
    void buildCollectRequestItems_dropship() {
        var sc = getSortingCenter();

        var movement = MovementTestBuilder.builder()
                .id(133L)
                .build().get();

        var resultItems = subject.buildCollectRequestItems(List.of(movement), sc, false);
        assertThat(resultItems.size()).isEqualTo(1);

        RoutingRequestItem result = resultItems.get(0);
        assertThat(result.getType()).isEqualTo(RoutingRequestItemType.DROPSHIP);
        assertThat(result.getRef()).isEqualTo(DROPSHIPS_REF_PREFIX + movement.getExternalId());
        assertThat(result.getDepotId()).isEqualTo(sc.getId().toString());

        assertThat(result.getAddress().getAddressString()).isEqualTo(movement.getWarehouse().getAddress().getAddress());
        assertThat(result.getAddress().getHouse()).isEqualTo(movement.getWarehouse().getAddress().getHouse());
        assertThat(result.getAddress().getGeoPoint()).isEqualTo(RoutingGeoPointMapper.toRoutingGeoPoint(movement.getWarehouse().getAddress().getGeoPoint()));

        assertThat(result.getTags()).isEqualTo(Set.of(RequiredRoutingTag.DROPSHIP.getCode()));
        assetMovementCommonFields(movement, result);
    }

    @Test
    void buildCollectRequestItems_dropoffReturn() {
        var sc = getSortingCenter();

        var movement = MovementTestBuilder.builder()
                .id(145L)
                .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                .build().get();

        var resultItems = subject.buildCollectRequestItems(List.of(movement), sc, false);
        assertThat(resultItems.size()).isEqualTo(1);

        RoutingRequestItem result = resultItems.get(0);
        assertThat(result.getType()).isEqualTo(RoutingRequestItemType.DROPOFF_CARGO_RETURN);
        assertThat(result.getRef()).isEqualTo(DROPOFF_RETURN_REF_PREFIX + movement.getExternalId());
        assertThat(result.getDepotId()).isEqualTo(sc.getId().toString());

        assertThat(result.getAddress().getAddressString()).isEqualTo(movement.getWarehouseTo().getAddress().getAddress());
        assertThat(result.getAddress().getHouse()).isEqualTo(movement.getWarehouseTo().getAddress().getHouse());
        assertThat(result.getAddress().getGeoPoint()).isEqualTo(RoutingGeoPointMapper.toRoutingGeoPoint(movement.getWarehouse().getAddress().getGeoPoint()));

        assertThat(result.getTags()).isEqualTo(Set.of(RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode()));
        assetMovementCommonFields(movement, result);
    }

    private void assetMovementCommonFields(Movement movement, RoutingRequestItem result) {
        assertThat(result.getTaskId()).isEqualTo(movement.getId().toString());
        assertThat(result.isExcludedFromLocationGroups()).isEqualTo(false);
        assertThat(result.getDimensionsClass()).isEqualTo(DimensionsClass.REGULAR_CARGO);
        assertThat(result.getAdditionalTimeForSurvey()).isEqualTo(DROPSHIP_ADDITIONAL_TIME_FOR_SURVEY);
        assertThat(result.getFashionOrdersCount()).isEqualTo(DROPSHIP_FASHION_ORDERS_COUNT);
        assertThat(result.isUserShiftRoutingRequest()).isEqualTo(false);
    }

    private SortingCenter getSortingCenter() {
        SortingCenter sc = mock(SortingCenter.class);
        when(sc.getId()).thenReturn(1L);
        lenient().when(sc.getZoneOffset()).thenReturn(ZoneOffset.UTC);
        return sc;
    }

}
