package ru.yandex.market.logistics.utilizer.service.events.ss;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;
import ru.yandex.market.logistics.utilizer.util.FileContentUtils;

public class UnparsedSkuStocksFromLbHandlingServiceTest extends DbqueueContextualTest {
    @Autowired
    UnparsedSkuStocksFromLbHandlingService unparsedSkuStocksFromLbHandlingService;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/events/ss/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/events/ss/2/db-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection")
    void handlerCreatesDbqueueEntry() {
        String skuStocks = FileContentUtils.getFileContent("fixtures/service/events/ss/2/string.json");
        unparsedSkuStocksFromLbHandlingService.handle(skuStocks);
    }
}
