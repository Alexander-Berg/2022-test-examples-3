package ru.yandex.market.core.order.returns;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.core.order.resupply.ResupplyOrderDao;
import ru.yandex.market.core.order.returns.complain_expiration_strategy.ReturnComplainExpirationStrategyProvider;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;

class ExtendedReturnStatusRecalculateServiceTest extends FunctionalTest {
    @Autowired
    private OrderReturnsService orderReturnsService;
    @Autowired
    private ResupplyOrderDao resupplyOrderDao;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PartnerPlacementProgramService partnerPlacementProgramService;
    @Autowired
    private ReturnComplainExpirationStrategyProvider returnComplainExpirationStrategyProvider;

    private ExtendedReturnStatusRecalculateService returnStatusRecalculateService;

    @BeforeEach
    void setUp() {
        OrderReturnComplainExpirationTimingsService expirationTimingsService =
                new OrderReturnComplainExpirationTimingsService(
                        partnerPlacementProgramService,
                        returnComplainExpirationStrategyProvider,
                        Clock.fixed(LocalDateTime.of(2021, 6, 27, 6, 0, 0)
                                .toInstant(OffsetDateTime.now().getOffset()), ZoneOffset.systemDefault())
                );
        returnStatusRecalculateService = new ExtendedReturnStatusRecalculateService(orderReturnsService,
                resupplyOrderDao, orderService, expirationTimingsService);
    }

    @Test
    @DbUnitDataSet(
            before = "ExtendedReturnStatusRecalculateServiceTest.before.csv",
            after = "ExtendedReturnStatusRecalculateServiceTest.after.csv"
    )
    void recalculateExtendedStatusForAllReturns() {
        returnStatusRecalculateService.recalculateOrdersReturnsExtendedStatuses();
    }
}
