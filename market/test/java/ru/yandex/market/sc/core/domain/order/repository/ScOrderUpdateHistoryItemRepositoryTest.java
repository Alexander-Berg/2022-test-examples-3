package ru.yandex.market.sc.core.domain.order.repository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.order.model.ScOrderUpdateEvent;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

@EmbeddedDbTest
class ScOrderUpdateHistoryItemRepositoryTest {

    @Autowired
    ScOrderUpdateHistoryRepository scOrderUpdateHistoryRepository;
    @Autowired
    TestFactory testFactory;
    @Autowired
    Clock clock;
    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    void save() {
        var scOrder = testFactory.scOrder();
        var courier = testFactory.storedCourier();
        var user = testFactory.storedUser(scOrder.getSortingCenter(), 123L);

        ScOrderUpdateHistoryItem history = new ScOrderUpdateHistoryItem(scOrder, ScOrderUpdateEvent.UPDATE_COURIER,
                Instant.now(clock), courier, LocalDate.ofInstant(Instant.now(clock), DateTimeUtil.DEFAULT_ZONE_ID), user);

        Assertions.assertThatCode(
                () -> transactionTemplate.execute(ts ->scOrderUpdateHistoryRepository.save(history)))
                .doesNotThrowAnyException();
    }
}
