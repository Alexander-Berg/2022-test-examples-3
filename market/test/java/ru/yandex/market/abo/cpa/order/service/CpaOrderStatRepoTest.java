package ru.yandex.market.abo.cpa.order.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.order.delivery.OrderDelivery;
import ru.yandex.market.abo.cpa.order.delivery.OrderDeliveryRepo;
import ru.yandex.market.abo.cpa.order.model.CpaOrderStat;
import ru.yandex.market.abo.gen.HypothesisRepo;
import ru.yandex.market.abo.gen.model.Hypothesis;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author agavrikov
 * @since 07.04.18
 */
public class CpaOrderStatRepoTest extends EmptyTest {
    @Autowired
    HypothesisRepo hypothesisRepo;
    @Autowired
    OrderDeliveryRepo orderDeliveryRepo;
    @Autowired
    CpaOrderStatRepo cpaOrderStatRepo;

    @SuppressWarnings("deprecation")
    @Test
    public void testRepo() {
        CpaOrderStat cpaOrderStat = TestHelper.generateCpaOrderStat(
                1, TestHelper.generateShopId(), true, LocalDateTime.now(), Color.RED
        );
        cpaOrderStatRepo.save(cpaOrderStat);
        CpaOrderStat dbCpaOrderStat = cpaOrderStatRepo.findByIdOrNull(cpaOrderStat.getOrderId());

        assertEquals(cpaOrderStat.getOrderId(), dbCpaOrderStat.getOrderId());
        cpaOrderStat.setCancelledSubstatus(OrderSubstatus.PENDING_CANCELLED);
        cpaOrderStat.setCancelledRole(ClientRole.SHOP);
        cpaOrderStat.setDeliveryType(DeliveryType.DIGITAL);
        cpaOrderStatRepo.save(cpaOrderStat);
        dbCpaOrderStat = cpaOrderStatRepo.findByIdOrNull(cpaOrderStat.getOrderId());

        assertEquals(OrderSubstatus.PENDING_CANCELLED, dbCpaOrderStat.getCancelledSubstatus());
        assertEquals(ClientRole.SHOP, dbCpaOrderStat.getCancelledRole());
        assertEquals(DeliveryType.DIGITAL, dbCpaOrderStat.getDeliveryType());
    }

    @Test
    public void testFindCancelledOrderIdsWithoutHypothesis() {
        long shopId = TestHelper.generateShopId();
        Instant createdFrom = Instant.now().minus(7, ChronoUnit.DAYS);
        int generatorId1 = 1;
        int generatorId2 = 2;

        List<CpaOrderStat> cpaOrderStats = TestHelper.generateCpaOrderStatList(shopId).stream()
                .peek(s -> s.setCancelledSubstatus(OrderSubstatus.SHOP_PENDING_CANCELLED))
                .collect(Collectors.toUnmodifiableList());

        CpaOrderStat processedOrder = cpaOrderStats.get(0);
        CpaOrderStat marketOrder = cpaOrderStats.get(1);
        CpaOrderStat oldOrder = cpaOrderStats.get(2);
        oldOrder.setCreationDate(Date.from(createdFrom.minus(1, ChronoUnit.SECONDS)));

        List<OrderDelivery> orderDeliveries = cpaOrderStats.stream()
                .map(s -> new OrderDelivery(
                        s.getOrderId(), null, null,
                        s.getOrderId() == marketOrder.getOrderId()
                                ? DeliveryPartnerType.YANDEX_MARKET
                                : DeliveryPartnerType.SHOP,
                        null)
                )
                .collect(Collectors.toUnmodifiableList());

        Hypothesis hypothesis = Hypothesis.builder(shopId, generatorId1)
                .withSourceId(processedOrder.getOrderId())
                .build();

        cpaOrderStatRepo.saveAll(cpaOrderStats);
        orderDeliveryRepo.saveAll(orderDeliveries);
        hypothesisRepo.save(hypothesis);

        Set<Long> allShopOrderIds = cpaOrderStats.stream()
                .filter(s -> s.getOrderId() != marketOrder.getOrderId())
                .map(CpaOrderStat::getOrderId)
                .collect(Collectors.toSet());

        Set<Long> newShopOrderIds = cpaOrderStats.stream()
                .filter(s -> s.getOrderId() != marketOrder.getOrderId()
                        && s.getOrderId() != oldOrder.getOrderId()
                )
                .map(CpaOrderStat::getOrderId)
                .collect(Collectors.toSet());

        Set<Long> notProcessedByGen1Ids = cpaOrderStats.stream()
                .filter(s ->
                        s.getOrderId() != marketOrder.getOrderId()
                                && s.getOrderId() != oldOrder.getOrderId()
                                && s.getOrderId() != processedOrder.getOrderId()
                )
                .map(CpaOrderStat::getOrderId)
                .collect(Collectors.toSet());

        List<Long> foundGen1 = cpaOrderStatRepo.findCancelledDSBSOrderIdsWithoutHypothesis(
                generatorId1, Set.of(OrderSubstatus.SHOP_PENDING_CANCELLED.getId()), Date.from(createdFrom)
        );
        assertEquals(notProcessedByGen1Ids, new HashSet<>(foundGen1));
        assertEquals(notProcessedByGen1Ids.size(), foundGen1.size());

        List<Long> foundGen2 = cpaOrderStatRepo.findCancelledDSBSOrderIdsWithoutHypothesis(
                generatorId2, Set.of(OrderSubstatus.SHOP_PENDING_CANCELLED.getId()), Date.from(createdFrom)
        );
        assertEquals(newShopOrderIds, new HashSet<>(foundGen2));
        assertEquals(newShopOrderIds.size(), foundGen2.size());

        List<Long> foundGen2All = cpaOrderStatRepo.findCancelledDSBSOrderIdsWithoutHypothesis(
                generatorId2, Set.of(OrderSubstatus.SHOP_PENDING_CANCELLED.getId()),
                Date.from(createdFrom.minus(1, ChronoUnit.DAYS))
        );
        assertEquals(allShopOrderIds, new HashSet<>(foundGen2All));
        assertEquals(allShopOrderIds.size(), foundGen2All.size());
    }
}
