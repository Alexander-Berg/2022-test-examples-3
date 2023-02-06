package ru.yandex.market.wms.common.spring.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class TrailerServiceTest extends IntegrationTest {

    @Autowired
    private TrailerService trailerService;
    @Autowired
    private SecurityDataProvider securityDataProvider;

    @Test
    @DatabaseSetup("/db/service/trailer/update-on-reopen-receipt/before.xml")
    @ExpectedDatabase(value = "/db/service/trailer/update-on-reopen-receipt/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateTrailerOnReopenReceipt() {
        trailerService.updateTrailerOnReopenReceipt("0000000006", securityDataProvider.getUser());
    }
}
