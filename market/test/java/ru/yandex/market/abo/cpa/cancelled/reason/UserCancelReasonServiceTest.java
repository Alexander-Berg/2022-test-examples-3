package ru.yandex.market.abo.cpa.cancelled.reason;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.util.entity.DeletableEntityService;
import ru.yandex.market.abo.util.entity.DeletableEntityServiceTest;

/**
 * @author antipov93.
 * @date 30.09.18.
 */
public class UserCancelReasonServiceTest extends DeletableEntityServiceTest<UserCancelReason, Long> {

    @Autowired
    private UserCancelReasonService service;

    @Override
    protected DeletableEntityService<UserCancelReason, Long> service() {
        return service;
    }

    @Override
    protected Long extractId(UserCancelReason entity) {
        return entity.getId();
    }

    @Override
    protected UserCancelReason newEntity() {
        UserCancelReason reason = new UserCancelReason();
        reason.setName("причина отмены");
        reason.setDeleted(false);
        return reason;
    }

    @Override
    protected UserCancelReason example() {
        return new UserCancelReason();
    }
}
