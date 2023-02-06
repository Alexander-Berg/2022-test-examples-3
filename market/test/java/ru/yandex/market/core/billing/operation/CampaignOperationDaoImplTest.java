package ru.yandex.market.core.billing.operation;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.billing.model.OperationType.CORRECTION_ROLLBACK;

class CampaignOperationDaoImplTest extends FunctionalTest {
    @Autowired
    CampaignOperationDaoImpl dao;

    @Test
    void update() {
        // simple smoke test
        var now = new Date();
        var id = dao.create(CORRECTION_ROLLBACK, 1, 1L, now, now, now, BigDecimal.ZERO, false);
        assertThat(id).isNotZero();
    }
}
