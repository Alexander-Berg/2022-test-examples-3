package ru.yandex.market.checkout.checkouter.checkout.idempotency;

import java.sql.Date;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.order.reservation.OrderPlacingService;
import ru.yandex.market.checkout.backbone.order.reservation.OrderPlacingServiceImpl;
import ru.yandex.market.checkout.checkouter.order.idempotency.IdempotentOperationDao;
import ru.yandex.market.checkout.checkouter.order.idempotency.IdempotentOperationStatusType;

import static ru.yandex.market.checkout.checkouter.order.idempotency.IdempotentOperationStatusType.FAILED_UNPREDICTABLE;
import static ru.yandex.market.checkout.checkouter.order.idempotency.IdempotentOperationStatusType.SUCCESS;

public class CheckoutIdempotencyTest extends AbstractWebTestBase {

    @Autowired
    IdempotentOperationDao idempotentOperationDao;
    @Autowired
    private OrderPlacingService orderPlacingService;
    @Autowired
    private Clock clock;

    @BeforeEach
    public void beforeEach() {
        ((OrderPlacingServiceImpl) orderPlacingService).setOrderPlacingTimeoutMs(10000);
    }

    @AfterEach
    public void afterEach() {
        clearFixed();
    }

    @Test
    public void idempotentOperationDao() {
        var idempotencyKey = UUID.randomUUID().toString();
        idempotentOperationDao.save(idempotencyKey);
        idempotentOperationDao.setOrderIds(idempotencyKey, SUCCESS, 123L, "123321");
        var idempotentOperation = idempotentOperationDao.find(idempotencyKey);

        Assertions.assertNotNull(idempotentOperation);
        Assertions.assertEquals(idempotentOperation.getStatus(), SUCCESS);
        Assertions.assertEquals(idempotentOperation.getOrderId(), 123L);
        Assertions.assertEquals(idempotentOperation.getMultiOrderId(), "123321");
    }

    @Test
    public void insertReturnIdempotentOperation() {
        var idempotencyKey = "123*20*12*2*0*323";
        idempotentOperationDao.save(idempotencyKey);
        idempotentOperationDao.setReturnId(idempotencyKey, SUCCESS, 123L);
        var idempotentOperation = idempotentOperationDao.find(idempotencyKey);

        Assertions.assertNotNull(idempotentOperation);
        Assertions.assertEquals(idempotentOperation.getStatus(), SUCCESS);
        Assertions.assertEquals(idempotentOperation.getReturnId(), 123L);
    }

    @Test
    public void removeIdempotentOperation() {
        var successIdempotencyKey = insertIdempotentOperation(SUCCESS);
        var failedIdempotencyKey = insertIdempotentOperation(FAILED_UNPREDICTABLE);

        Assertions.assertNotNull(idempotentOperationDao.find(successIdempotencyKey));
        Assertions.assertNotNull(idempotentOperationDao.find(failedIdempotencyKey));

        setFixedTime(Date.from(clock.instant().plus(6, ChronoUnit.DAYS)).toInstant());
        var failedIdempotencyKey2 = insertIdempotentOperation(FAILED_UNPREDICTABLE);
        tmsTaskHelper.runRemoveIdempotentOperationTaskV2();

        Assertions.assertNull(idempotentOperationDao.find(successIdempotencyKey));
        Assertions.assertNull(idempotentOperationDao.find(failedIdempotencyKey));
        Assertions.assertNotNull(idempotentOperationDao.find(failedIdempotencyKey2));
    }

    private String insertIdempotentOperation(IdempotentOperationStatusType idempotentOperationStatusType) {
        var idempotencyKey = UUID.randomUUID().toString();
        idempotentOperationDao.save(idempotencyKey);
        idempotentOperationDao.setStatus(idempotencyKey, idempotentOperationStatusType);
        return idempotencyKey;
    }
}
