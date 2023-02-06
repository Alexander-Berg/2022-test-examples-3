package ru.yandex.market.mbi.msapi.logbroker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.mbi.msapi.logbroker.config.InitConfig;

/**
 * @author aostrikov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {InitConfig.class})
@ActiveProfiles({"integration-tests"})
public class LogbrokerLoaderTest {

    @Autowired
    @Qualifier("lbRawClicksReader")
    private ReceiveManager clicksReceiver;

    @Autowired
    @Qualifier("lbRawCpaClicksReader")
    private ReceiveManager cpaClicksReceiver;

    @Autowired
    @Qualifier("lbRawClicksRollbacksReader")
    private ReceiveManager rollbacksReceiver;

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
        while (true) {
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.execute(() -> clicksReceiver.receive());
            executor.execute(() -> cpaClicksReceiver.receive());
            executor.execute(() -> rollbacksReceiver.receive());

            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.MINUTES);

            TimeUnit.SECONDS.sleep(3);
        }
    }
}
