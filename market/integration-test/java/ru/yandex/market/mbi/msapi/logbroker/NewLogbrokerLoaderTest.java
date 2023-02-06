package ru.yandex.market.mbi.msapi.logbroker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.mbi.msapi.logbroker.config.InitConfig;
import ru.yandex.market.mbi.msapi.logbroker_new.LbReceiveManager;

/**
 * @author kateleb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {InitConfig.class})
@ActiveProfiles({"integration-tests"})
public class NewLogbrokerLoaderTest {

    private static final Logger log = LoggerFactory.getLogger(NewLogbrokerLoaderTest.class);

    @Autowired
    @Qualifier("newlbRawClicksReader")
    private LbReceiveManager clicksReceiver;
    @Autowired
    @Qualifier("newlbRawCpaClicksReader")
    private LbReceiveManager cpaClicksReceiver;
    @Autowired
    @Qualifier("newlbRawClicksRollbacksReader")
    private LbReceiveManager clicksRollbacksReceiver;

    /**
     * BEFORE run:
     * 1) install tvm lib locally
     * https://wiki.yandex-team.ru/passport/tvm2/library/#vysokourovnevajachast
     * 2) ADD vm option before run
     * -Djava.library.path=/<path to arcadia>/bin
     */
    @Test
    @Ignore
    public void loadData() throws Exception {

        ExecutorService executor = Executors.newCachedThreadPool();
        log.info("========Start receive========");
        executor.execute(() -> clicksReceiver.receive());
        executor.execute(() -> cpaClicksReceiver.receive());
        executor.execute(() -> clicksRollbacksReceiver.receive());
        executor.shutdown();
        executor.awaitTermination(7, TimeUnit.MINUTES);
        log.info("========Receive stopped========");
    }
}
