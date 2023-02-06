package ru.yandex.market.logistics.utilizer.service.events.ss;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;

class SkuStocksEventConsumerTest extends DbqueueContextualTest {
    @Autowired
    SkuStocksEventConsumer skuStocksEventConsumer;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/events/ss/1/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/events/ss/1/db-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
    )
    void consumerCreatesDbqueueEntry() {
        skuStocksEventConsumer.accept(List.of("aboba", "kek"));
    }
}
