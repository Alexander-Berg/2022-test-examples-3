package ru.yandex.market.abo.cpa.order.service;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitService;
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimit;
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason;
import ru.yandex.market.abo.cpa.order.model.PartnerModel;
import ru.yandex.market.abo.util.entity.DeletableEntityService;
import ru.yandex.market.abo.util.entity.DeletableEntityServiceTest;

/**
 * @author artemmz
 * @date 26/02/2020.
 */
class CpaOrderLimitServiceTest extends DeletableEntityServiceTest<CpaOrderLimit, Long> {
    @Autowired
    private CpaOrderLimitService orderLimitService;

    @Override
    protected DeletableEntityService<CpaOrderLimit, Long> service() {
        return orderLimitService;
    }

    @Override
    protected Long extractId(CpaOrderLimit entity) {
        return entity.getId();
    }

    @Override
    protected CpaOrderLimit newEntity() {
        return new CpaOrderLimit(12131L, PartnerModel.DSBB, CpaOrderLimitReason.MANUAL, 10, null, 4234);
    }

    @Override
    protected CpaOrderLimit example() {
        return new CpaOrderLimit();
    }
}
