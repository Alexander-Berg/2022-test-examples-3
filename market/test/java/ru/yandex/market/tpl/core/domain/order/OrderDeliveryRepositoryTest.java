package ru.yandex.market.tpl.core.domain.order;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.BDDAssertions.then;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderDeliveryRepositoryTest extends TplAbstractTest {

    private final OrderDeliveryRepository orderDeliveryRepository;
    private final TestDataFactory testDataFactory;

    @Test
    void findDeliveryServiceIdAndCityNamePairsBetweenDeliveryDates() {
        //given
        LocalDate now = LocalDate.now();
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Крыжополь")
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Мариуполь")
                        .build())
                .deliveryDate(now)
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Ставрополь")
                        .build())
                .deliveryDate(now.plusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Крыжополь")
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Севастополь")
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Севастополь")
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        //when
        Map<Long, Set<String>> cities = orderDeliveryRepository
                .mapServiceToCityNamesBetweenDeliveryDatesAndWithStatuses(
                        now.minusDays(2), now, OrderFlowStatus.PASSED_ADDRESS_VALIDATION_STATUSES);
        //then
        then(cities.get(198L)).containsAll(Set.of("Крыжополь", "Севастополь"));
        then(cities.get(239L)).containsAll(Set.of("Крыжополь"));
    }

    @Test
    void findDeliveryServiceIdAndCityNamePairsBetweenDeliveryDatesWithInvalidStatuses() {
        //given
        LocalDate now = LocalDate.now();
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Крыжополь")
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.CANCELLED)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Мариуполь")
                        .build())
                .deliveryDate(now)
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.CREATED)
                .build());
        //when
        Map<Long, Set<String>> cities = orderDeliveryRepository
                .mapServiceToCityNamesBetweenDeliveryDatesAndWithStatuses(
                        now.minusDays(2), now.plusDays(1), OrderFlowStatus.PASSED_ADDRESS_VALIDATION_STATUSES);
        //then
        then(cities).isEmpty();
    }

    @Test
    void mapServiceToCityNamesBetweenDeliveryDates() {
        //given
        LocalDate now = LocalDate.now();
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Крыжополь")
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Крыжополь")
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Севастополь")
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city("Севастополь")
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        //when
        Map<Long, Set<String>> deliveryServiceToCitiesMapping = orderDeliveryRepository
                .mapServiceToCityNamesBetweenDeliveryDatesAndWithStatuses(
                        now.minusDays(2), now, OrderFlowStatus.PASSED_ADDRESS_VALIDATION_STATUSES);
        //then
        then(deliveryServiceToCitiesMapping).containsKeys(198L, 239L);
        then(deliveryServiceToCitiesMapping.get(198L)).contains("Крыжополь", "Севастополь");
        then(deliveryServiceToCitiesMapping.get(239L)).contains("Крыжополь");
    }

    @Test
    void findRegionIdsBetweenDeliveryDates() {
        //given
        LocalDate now = LocalDate.now();
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(1)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(2)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(1)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(2)
                        .build())
                .deliveryDate(now.plusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        //when
        Map<Long, Set<Integer>> regions = orderDeliveryRepository
                .mapServiceToRegionIdsBetweenDeliveryDatesWithStatuses(
                        now.minusDays(1), now, OrderFlowStatus.PASSED_ADDRESS_VALIDATION_STATUSES);
        //then
        then(regions.get(198L)).contains(1);
        then(regions.get(239L)).contains(2);
    }

    @Test
    void findRegionIdsBetweenDeliveryDatesWithInvalidRegion() {
        //given
        LocalDate now = LocalDate.now();
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(0)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(-1)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(-2)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(null)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        //when
        Map<Long, Set<Integer>> regions = orderDeliveryRepository
                .mapServiceToRegionIdsBetweenDeliveryDatesWithStatuses(
                        now.minusDays(1), now, OrderFlowStatus.PASSED_ADDRESS_VALIDATION_STATUSES);
        //then
        then(regions).isEmpty();
    }

    @Test
    void findRegionIdsBetweenDeliveryDatesWithInvalidStatuses() {
        //given
        LocalDate now = LocalDate.now();
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(0)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.CREATED)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(-1)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.CANCELLED)
                .build());
        //when
        Map<Long, Set<Integer>> regions = orderDeliveryRepository
                .mapServiceToRegionIdsBetweenDeliveryDatesWithStatuses(
                        now.minusDays(1), now, OrderFlowStatus.PASSED_ADDRESS_VALIDATION_STATUSES);
        //then
        then(regions).isEmpty();
    }

    @Test
    void mapServiceToRegionIdsBetweenDeliveryDates() {
        //given
        LocalDate now = LocalDate.now();
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(1)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(2)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(239L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(1)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(3)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .regionId(2)
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .build());
        //when
        Map<Long, Set<Integer>> regions = orderDeliveryRepository
                .mapServiceToRegionIdsBetweenDeliveryDatesWithStatuses(
                        now.minusDays(1), now, OrderFlowStatus.PASSED_ADDRESS_VALIDATION_STATUSES);
        //then
        then(regions).containsKeys(198L, 239L);
        then(regions.get(198L)).contains(1, 2, 3);
        then(regions.get(239L)).contains(2);
    }

    @Test
    void findPersonalClarificationByBuyerUid() {
        // given
        Long BUYER_YANDEX_UID = 999L;
        LocalDate now = LocalDate.now();
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .clarified(true)
                        .addressPersonalId("123456")
                        .gpsPersonalId("qwerty")
                        .build())
                .deliveryDate(now.minusDays(1))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .buyerYandexUid(BUYER_YANDEX_UID)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .clarified(true)
                        .addressPersonalId("654321")
                        .gpsPersonalId("asdf")
                        .build())
                .deliveryDate(now.minusDays(30))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .buyerYandexUid(BUYER_YANDEX_UID)
                .build());
        testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .clarified(true)
                        .addressPersonalId("098765")
                        .gpsPersonalId("zxvbnm")
                        .build())
                .deliveryDate(now.minusDays(35))
                .deliveryServiceId(198L)
                .flowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT)
                .buyerYandexUid(BUYER_YANDEX_UID)
                .build());

        //when
        List<AddressClarification> result =
                orderDeliveryRepository.findPersonalClarificationByBuyerUid(BUYER_YANDEX_UID);

        //then
        then(result).hasSize(3);
        then(result.stream().map(AddressClarification::getPersonalAddressId))
                .hasSameElementsAs(List.of("123456", "654321", "098765"));
        then(result.stream().map(AddressClarification::getPersonalGpsId))
                .hasSameElementsAs(List.of("qwerty", "asdf", "zxvbnm"));
        then(result.stream().map(AddressClarification::getUpdatedAt))
                .doesNotContainNull();
    }
}
