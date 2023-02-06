package ru.yandex.market.sc.core.domain.outbound;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ExtendWith(DefaultScUserWarehouseExtension.class)
class PartnerMappingTest {

    private final TestFactory testFactory;
    private final OutboundQueryService outboundQueryService;

    @MockBean
    Clock clock;

    @BeforeEach
    void init() {
        TestFactory.setupMockClock(clock);
    }

    /*
      Направление маршрута:     СЦ1 -> СЦ102,
      Кросс-док:                СЦ1 -> СЦ101 -> СЦ102
      Создан аутбаунд:          СЦ1 -> СЦ101
    */
    @Test
    void bindLotsToWrongOutbound() {
        var sortingCenter = testFactory.storedSortingCenter(1L);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, true);

        var firstHopPartnerId = 101L;
        var firstHopSc = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(firstHopPartnerId)
                        .yandexId("y" + firstHopPartnerId)
                        .build()
        );

        var destinationPartnerId = 102L;
        testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(destinationPartnerId)
                        .yandexId("y" + destinationPartnerId)
                        .build()
        );

        testFactory.storedCrossDockMapping(sortingCenter.getId(), firstHopPartnerId, destinationPartnerId);

        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(firstHopSc.getYandexId())
                .build()
        );
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(destinationPartnerId));

        var order = testFactory.createForToday(order(sortingCenter, "o1")
                .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow();
        var outboundList = outboundQueryService.findMagistralRouteOutbounds(
                testFactory.getRoutable(route));
        assertThat(outboundList).hasSize(1);
        assertThat(outboundList).contains(outbound);
    }

    /*
      Направление маршрута:     СЦ1 -> СЦ102,
      Кросс-док:                СЦ1 -> СЦ101 -> СЦ102
      Создан аутбаунд:          СЦ1 -> СЦ102
    */
    @Test
    void bindLotsToWrongOutbound2() {
        var sortingCenter = testFactory.storedSortingCenter(1L);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, true);

        var firstHopPartnerId = 101L;
        var firstHopSc = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(firstHopPartnerId)
                        .yandexId("y" + firstHopPartnerId)
                        .build()
        );

        var destinationPartnerId = 102L;
        var destinationSc = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(destinationPartnerId)
                        .yandexId("y" + destinationPartnerId)
                        .build()
        );

        testFactory.storedCrossDockMapping(sortingCenter.getId(), firstHopPartnerId, destinationPartnerId);

        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(destinationSc.getYandexId())
                .build()
        );
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(destinationPartnerId));

        var order = testFactory.createForToday(order(sortingCenter, "o1")
                .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var outboundList = outboundQueryService.findMagistralRouteOutbounds(
                testFactory.getRoutable(route));
        assertThat(outboundList).hasSize(1);
        assertThat(outboundList).contains(outbound);
    }

    /*
      Направление маршрута:     СЦ1 -> СЦ102,
      Кросс-док:                СЦ1 -> СЦ101 -> СЦ102
      Группа алиасов партнеров: СЦ101, СЦ1102, Проверяем, что наличие маппинга тут ни на что не влияет
      Создан аутбаунд:          СЦ1 -> СЦ103
    */
    @Test
    void bindLotsToWrongOutbound3() {
        var sortingCenter = testFactory.storedSortingCenter(1L);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, true);

        var firstHopPartnerId = 101L;
        var firstHopSc = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(firstHopPartnerId)
                        .yandexId("y" + firstHopPartnerId)
                        .build()
        );

        var destinationPartnerId = 102L;
        var destinationSc = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(destinationPartnerId)
                        .yandexId("y" + destinationPartnerId)
                        .build()
        );

        testFactory.storedCrossDockMapping(sortingCenter.getId(), firstHopPartnerId, destinationPartnerId);

        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(destinationSc.getYandexId())
                .build()
        );
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(destinationPartnerId + 1000));
        testFactory.storedPartnerMappingGroup(destinationPartnerId, destinationPartnerId + 1000);

        var order = testFactory.createForToday(order(sortingCenter, "o1")
                .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var outboundList = outboundQueryService.findMagistralRouteOutbounds(
                testFactory.getRoutable(route));
        assertThat(outboundList).hasSize(1);
        assertThat(outboundList).contains(outbound);
    }

    /*
      Направление маршрута:     СЦ1 -> СЦ102,
      Кросс-док:                СЦ1 -> СЦ101 -> СЦ102
      Группа алиасов партнеров: СЦ101, СЦ103
      Создан аутбаунд:          СЦ1 -> СЦ103
     */
    @Test
    void bindLotsToWrongOutbound4() {
        var sortingCenter = testFactory.storedSortingCenter(1L);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, true);

        var firstHopScId = 101L;
        var firstHopSc = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(firstHopScId)
                        .yandexId("y" + firstHopScId)
                        .build()
        );

        var destinationScId = 102L;
        var destinationSc = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(destinationScId)
                        .yandexId("y" + destinationScId)
                        .build()
        );
        var firstHopScAliasId = 103L;
        var mappedFirstHopSc = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(firstHopScAliasId)
                        .yandexId("y" + firstHopScAliasId)
                        .build()
        );

        testFactory.storedCrossDockMapping(sortingCenter.getId(), firstHopScId, destinationScId);
        testFactory.storedPartnerMappingGroup(firstHopScId, firstHopScAliasId);

        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(mappedFirstHopSc.getYandexId())
                .build()
        );
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(destinationScId));

        var order = testFactory.createForToday(order(sortingCenter, "o1")
                .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var outboundList = outboundQueryService.findMagistralRouteOutbounds(
                testFactory.getRoutable(route));
        assertThat(outboundList).hasSize(1);
        assertThat(outboundList).contains(outbound);
    }

    /*
      Направление маршрута:     СЦ1 -> СЦ104,
      Кросс-док:                СЦ1 -> СЦ101 -> СЦ102
      Группа алиасов партнеров: (СЦ101, СЦ103), (СЦ102, СЦ104)
      Создан аутбаунд:          СЦ1 -> СЦ103
     */
    @Test
    void bindLotsToWrongOutbound5() {
        var sortingCenter = testFactory.storedSortingCenter(1L);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, true);

        var firstHopScId = 101L;
        var firstHopSc = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(firstHopScId)
                        .yandexId("y" + firstHopScId)
                        .build()
        );

        var destinationScId = 102L;
        var destinationSc = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(destinationScId)
                        .yandexId("y" + destinationScId)
                        .build()
        );
        var firstHopScAliasId = 103L;
        var mappedFirstHopSc = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(firstHopScAliasId)
                        .yandexId("y" + firstHopScAliasId)
                        .build()
        );
        var destinationScIdAlias = 104L;

        testFactory.storedCrossDockMapping(sortingCenter.getId(), firstHopScId, destinationScId);
        testFactory.storedPartnerMappingGroup(firstHopScId, firstHopScAliasId);
        testFactory.storedPartnerMappingGroup(destinationScId, destinationScIdAlias);

        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(mappedFirstHopSc.getYandexId())
                .build()
        );
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(destinationScIdAlias));

        var order = testFactory.createForToday(order(sortingCenter, "o1")
                .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var outboundList = outboundQueryService.findMagistralRouteOutbounds(
                testFactory.getRoutable(route));
        assertThat(outboundList).hasSize(1);
        assertThat(outboundList).contains(outbound);
    }

}
