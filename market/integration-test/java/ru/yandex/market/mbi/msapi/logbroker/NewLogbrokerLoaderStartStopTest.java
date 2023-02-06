package ru.yandex.market.mbi.msapi.logbroker;

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
public class NewLogbrokerLoaderStartStopTest {

    private static final Logger log = LoggerFactory.getLogger(NewLogbrokerLoaderStartStopTest.class);

    @Autowired
    @Qualifier("newlbRawCpaClicksReader")
    private LbReceiveManager clicksReceiver;

    /**
     * BEFORE run:
     * 1) install tvm lib locally
     * https://wiki.yandex-team.ru/passport/tvm2/library/#vysokourovnevajachast
     * 2) ADD vm option before run
     * -Djava.library.path=/<path to arcadia>/bin
     * i.e. -Djava.library.path=/Users/kateleb/arcadia/arcadia/bin
     */
    @Test
    @Ignore
    public void loadData() throws Exception {
        log.info("========Start receive========");
        clicksReceiver.receive();
        log.info("========Receive stopped========");
        TimeUnit.SECONDS.sleep(10);
        log.info("========Start receive========");
        clicksReceiver.receive();
        log.info("========Receive stopped========");
        TimeUnit.SECONDS.sleep(2);
        log.info("========Start receive========");
        clicksReceiver.receive();
        log.info("========Receive stopped========");
        TimeUnit.SECONDS.sleep(2);
    }
}
