package ru.yandex.market.core.moderation;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.SingleFileCsvProducer;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.mbi.common.repository.jdbc.JdbcRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fbokovikov
 */
class ModerationStartedShopIdsQueryHandlerTest extends FunctionalTest {

    @Autowired
    private JdbcRepository jdbcRepository;

    @BeforeEach
    void init() {
        jdbcRepository.addHandler(
                ModerationStartedShopIdsQuery.class,
                new ModerationStartedShopIdsQueryHandler()
        );
    }

    @Test
    @DbUnitDataSet(before = "ModerationStartedShopIdsQueryHandlerTest.before.csv")
    void testModerationDelayQueryHandler() {
        var result = new ArrayList<TestingShop>();
        var sysdate = SingleFileCsvProducer.Functions.sysdate().toInstant();
        jdbcRepository.query(
                new ModerationStartedShopIdsQuery(TestingStatus.PENDING_CHECK_START, sysdate),
                cursor -> result.add(new TestingShop(cursor.getId(), cursor.getShopId()))
        );
        assertThat(result).containsExactlyInAnyOrder(
                new TestingShop(22, 11),
                new TestingShop(23, 11)
        );
    }
}
