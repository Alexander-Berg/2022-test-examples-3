package ru.yandex.market.abo.cpa.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.order.model.CpaExpressOrderEstimatedTime;
import ru.yandex.market.abo.cpa.order.model.CpaItemsUpdatedByPartnerFault;
import ru.yandex.market.abo.cpa.order.model.CpaOrderReturn;
import ru.yandex.market.abo.cpa.order.model.OrderParamType;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 * @date 09/07/19.
 */
class CpaOrderParamReposTest extends EmptyTest {
    private static final long ORDER_ID = 31312;
    private static final ReturnReasonType REASON_TYPE = ReturnReasonType.BAD_QUALITY;
    private static final ReturnStatus RETURN_STATUS = ReturnStatus.STARTED_BY_USER;

    @Autowired
    private CpaOrderReturnRepo cpaOrderReturnRepo;
    @Autowired
    private CpaExpressOrderEstimatedTimeRepo cpaExpressOrderEstimatedTimeRepo;
    @Autowired
    private DropshipItemsUpdateRepo dropshipItemsUpdateRepo;

    @Test
    void testInheritance() {
        Stream.of(cpaOrderReturnRepo, cpaExpressOrderEstimatedTimeRepo, dropshipItemsUpdateRepo)
                .forEach(repo -> assertEquals(0, repo.findAll().size()));

        cpaOrderReturnRepo.save(new CpaOrderReturn(TestHelper.generateReturn(ORDER_ID, RETURN_STATUS, REASON_TYPE)));

        var estimatedTime = LocalDateTime.now();
        cpaExpressOrderEstimatedTimeRepo.save(new CpaExpressOrderEstimatedTime(ORDER_ID, estimatedTime));

        var itemsUpdateTime = LocalDateTime.now();
        dropshipItemsUpdateRepo.save(new CpaItemsUpdatedByPartnerFault(
                ORDER_ID, itemsUpdateTime, HistoryEventReason.ITEMS_NOT_SUPPLIED.getId()
        ));

        flushAndClear();

        List<CpaOrderReturn> savedReturns = cpaOrderReturnRepo.findAll();
        assertEquals(1, savedReturns.size());
        CpaOrderReturn savedReturn = savedReturns.get(0);
        assertEquals(OrderParamType.ORDER_RETURN, savedReturn.getOrderParamType());
        assertEquals(ORDER_ID, savedReturn.getOrderId());
        assertEquals(REASON_TYPE.getId(), savedReturn.getReturnItemReasons()[0]);
        assertEquals(RETURN_STATUS.getId(), savedReturn.getReturnStatus());

        var savedEstimatedTimes = cpaExpressOrderEstimatedTimeRepo.findAll();
        assertEquals(1, savedEstimatedTimes.size());
        var savedEstimatedTime = savedEstimatedTimes.get(0);
        assertEquals(OrderParamType.EXPRESS_ORDER_ESTIMATED_TIME, savedEstimatedTime.getOrderParamType());
        assertEquals(ORDER_ID, savedEstimatedTime.getOrderId());
        assertEquals(estimatedTime, savedEstimatedTime.getExpressEstimatedTime());

        var savedItemsUpdatedFlags = dropshipItemsUpdateRepo.findAll();
        assertEquals(1, savedItemsUpdatedFlags.size());
        var savedItemsUpdatedFlag = savedItemsUpdatedFlags.get(0);
        assertEquals(OrderParamType.ITEMS_UPDATED_BY_PARTNER_FAULT, savedItemsUpdatedFlag.getOrderParamType());
        assertEquals(ORDER_ID, savedItemsUpdatedFlag.getOrderId());
        assertEquals(itemsUpdateTime, savedItemsUpdatedFlag.getItemsUpdateTime());
        assertEquals(HistoryEventReason.ITEMS_NOT_SUPPLIED, savedItemsUpdatedFlag.getItemsUpdateReason());

        cpaOrderReturnRepo.deleteAll();
        assertEquals(0, cpaOrderReturnRepo.findAll().size());

        cpaExpressOrderEstimatedTimeRepo.deleteAll();
        assertEquals(0, cpaExpressOrderEstimatedTimeRepo.findAll().size());

        dropshipItemsUpdateRepo.deleteAll();
        assertEquals(0, dropshipItemsUpdateRepo.findAll().size());
    }
}
