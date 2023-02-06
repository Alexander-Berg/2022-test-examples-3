package ru.yandex.market.b2bcrm.module.account;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.springframework.test.annotation.Commit;

import ru.yandex.market.b2bcrm.module.config.B2bAccountTests;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.db.test.DbTestTool;
import ru.yandex.market.jmf.tx.TxService;

@B2bAccountTests
@Commit
public abstract class AbstractDataImportTest {
    @Inject
    protected DbService dbService;
    @Inject
    protected BcpService bcpService;
    @Inject
    protected TxService txService;
    @Inject
    private DbTestTool dbTestTool;

    @AfterEach
    public void tearDown() {
        dbTestTool.clearDatabase();
    }

}
