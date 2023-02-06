package ru.yandex.market.tpl.core.service.routing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.external.routing.api.MultiClientReturn;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestClientReturnFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.order.TplOrderGenerateConstants.DEFAULT_DS_ID;

@RequiredArgsConstructor
class MultiClientReturnPackagerServiceTest extends TplAbstractTest {
    private final TestClientReturnFactory clientReturnFactory;
    private final MultiClientReturnPackagerService multiClientReturnPackagerService;
    private final ClientReturnRepository clientReturnRepository;
    private final TransactionTemplate transactionTemplate;

    @Test
    void pack_withCapacityRestriction_whenCapacityIsEnoughForAll() {
        //given
        LocalDateTime arriveFrom = LocalDate.now().atStartOfDay();
        LocalDateTime arriveTo = LocalDate.now().atStartOfDay().plusHours(4);
        String phone = "+7999999999";
        BigDecimal lat = BigDecimal.valueOf(55.5);
        BigDecimal lon = BigDecimal.valueOf(33.5);
        long courierId = 1L;

        List<TestClientReturnFactory.ItemDimensions> itemDimensions1 = List.of(
                TestClientReturnFactory.ItemDimensions.of(BigDecimal.ONE, 1, 2, 3));
        List<TestClientReturnFactory.ItemDimensions> itemDimensions2 = List.of(
                TestClientReturnFactory.ItemDimensions.of(BigDecimal.ONE, 1, 1, 1));

        ClientReturn clientReturnAlreadyOnCourier = clientReturnRepository.findById(
                clientReturnFactory.buildAndSave(DEFAULT_DS_ID, arriveFrom,
                        arriveTo, phone, lat, lon, itemDimensions1).getId()).orElseThrow();

        ClientReturn clientReturnWithValueEnoughForAppend = clientReturnRepository.findById(
                clientReturnFactory.buildAndSave(DEFAULT_DS_ID, arriveFrom,
                        arriveTo, phone, lat, lon, itemDimensions2).getId()).orElseThrow();

        //when
        List<MultiClientReturn> multiClientReturns =
                transactionTemplate.execute(s -> {

                            var cr1 = clientReturnRepository.findById(clientReturnAlreadyOnCourier.getId())
                                    .orElseThrow();

                            var cr2 = clientReturnRepository.findById(clientReturnWithValueEnoughForAppend.getId())
                                    .orElseThrow();

                            return multiClientReturnPackagerService.pack(
                                    List.of(cr1, cr2),
                                    BigDecimal.TEN,
                                    Map.of(courierId, List.of(List.of(cr1.getId())))
                            );
                        }
                );

        //then
        assertThat(multiClientReturns).hasSize(1);
        assertThat(multiClientReturns.get(0).getCourierId()).isEqualTo(courierId);
        assertThat(multiClientReturns.get(0).isPartOfHugeMultiItem()).isEqualTo(true);
        assertThat(multiClientReturns.get(0).getItems()).containsExactly(clientReturnAlreadyOnCourier,
                clientReturnWithValueEnoughForAppend);
    }

    @Test
    void pack_withCapacityRestriction_whenCapacityIsNotEnough() {
        //given
        LocalDateTime arriveFrom = LocalDate.now().atStartOfDay();
        LocalDateTime arriveTo = LocalDate.now().atStartOfDay().plusHours(4);
        String phone = "+7999999999";
        BigDecimal lat = BigDecimal.valueOf(55.5);
        BigDecimal lon = BigDecimal.valueOf(33.5);
        Long courierId = 1L;

        List<TestClientReturnFactory.ItemDimensions> itemDimensions1 = List.of(
                TestClientReturnFactory.ItemDimensions.of(BigDecimal.ONE, 1, 2, 3));
        List<TestClientReturnFactory.ItemDimensions> itemDimensions2 = List.of(
                TestClientReturnFactory.ItemDimensions.of(BigDecimal.ONE, 3, 3, 3));

        ClientReturn clientReturnAlreadyOnCourier = clientReturnFactory.buildAndSave(DEFAULT_DS_ID, arriveFrom,
                arriveTo, phone, lat, lon, itemDimensions1);
        ClientReturn clientReturnNotAssigned = clientReturnFactory.buildAndSave(DEFAULT_DS_ID, arriveFrom,
                arriveTo, phone, lat, lon, itemDimensions2);

        //when
        List<MultiClientReturn> multiClientReturns = transactionTemplate.execute(s -> {

            var cr1 = clientReturnRepository.findById(clientReturnAlreadyOnCourier.getId())
                    .orElseThrow();

            var cr2 = clientReturnRepository.findById(clientReturnNotAssigned.getId())
                    .orElseThrow();

            return multiClientReturnPackagerService.pack(
                    List.of(cr1, cr2),
                    BigDecimal.TEN,
                    Map.of(courierId, List.of(List.of(cr1.getId()))));
        });

        //then
        assertThat(multiClientReturns).hasSize(2);

        var assignedMultiClientReturns = multiClientReturns
                .stream()
                .filter(mCr -> courierId.equals(mCr.getCourierId()))
                .collect(Collectors.toList());

        assertThat(assignedMultiClientReturns).hasSize(1);
        var assignedMultiClientReturn = assignedMultiClientReturns.iterator().next();
        assertThat(assignedMultiClientReturn.getItems()).isEqualTo(List.of(clientReturnAlreadyOnCourier));


        var notAssignedMultiClientReturns = multiClientReturns
                .stream()
                .filter(mCr -> mCr.getCourierId() == null)
                .collect(Collectors.toList());

        assertThat(notAssignedMultiClientReturns).hasSize(1);
        var notAssignedMultiClientReturn = notAssignedMultiClientReturns.iterator().next();
        assertThat(notAssignedMultiClientReturn.getItems()).isEqualTo(List.of(clientReturnNotAssigned));
    }

    @Test
    void testPack_withoutCapacityRestriction() {
        //given
        LocalDateTime arriveFrom = LocalDate.now().atStartOfDay();
        LocalDateTime arriveTo = LocalDate.now().atStartOfDay().plusHours(4);
        String phone = "+7999999999";
        String phone2 = "+7999999998";
        BigDecimal lat = BigDecimal.valueOf(55.5);
        BigDecimal lon = BigDecimal.valueOf(33.5);

        List<ClientReturn> clientReturnsOneMulti = IntStream.range(0, 10)
                .mapToObj(i -> clientReturnFactory.buildAndSave(DEFAULT_DS_ID, arriveFrom, arriveTo,
                        phone, lat, lon))
                .collect(Collectors.toList());

        List<ClientReturn> clientReturnsAnotherMulti = IntStream.range(0, 10)
                .mapToObj(i -> clientReturnFactory.buildAndSave(DEFAULT_DS_ID, arriveFrom, arriveTo,
                        phone2, lat, lon))
                .collect(Collectors.toList());


        var all = StreamEx.of(clientReturnsOneMulti)
                .append(clientReturnsAnotherMulti)
                .collect(Collectors.toList());

        //when
        List<MultiClientReturn> multiClientReturns = multiClientReturnPackagerService.pack(all);

        //then
        assertThat(multiClientReturns).hasSize(2);
        assertThat(multiClientReturns.stream().map(MultiClientReturn::getItems).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(clientReturnsOneMulti, clientReturnsAnotherMulti);

    }
}
