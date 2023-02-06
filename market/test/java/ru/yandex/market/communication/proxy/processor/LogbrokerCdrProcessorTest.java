package ru.yandex.market.communication.proxy.processor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;
import ru.yandex.market.communication.proxy.logbroker.LogbrokerCdrProcessor;
import ru.yandex.market.communication.proxy.util.LogbrokerTestUtil;

/**
 * @author zilzilok
 */
class LogbrokerCdrProcessorTest extends AbstractCommunicationProxyTest {

    @Autowired
    private LogbrokerCdrProcessor logbrokerCdrProcessor;

    @Test
    @DbUnitDataSet(
            before = "cdr/LogbrokerCdrProcessorTest.before.csv",
            after = "cdr/LogbrokerCdrProcessorTest.after.csv"
    )
    public void testCdrEventProcessor() {
        logbrokerCdrProcessor.process(
                LogbrokerTestUtil.createMessageBatch(this.getClass(),
                        "json/new-cdr-event.json",
                        "json/new-cdr-event-without-dial.json"
                )
        );
    }

    @Test
    @DbUnitDataSet(
            before = "cdr/LogbrokerCdrProcessorTest.before.csv",
            after = "cdr/LogbrokerCdrProcessorTest.before.csv"
    )
    public void testInvalidJson() {
        logbrokerCdrProcessor.process(
                LogbrokerTestUtil.createMessageBatch(this.getClass(), "json/invalid-new-cdr-event.json")
        );
    }
}
