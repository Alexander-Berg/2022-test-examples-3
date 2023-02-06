package ru.yandex.chemodan.app.psbilling.worker;

import ru.yandex.chemodan.boot.ChemodanMainSupport;
import ru.yandex.chemodan.util.test.ContextTestSupport;

/**
 * @author tolmalev
 */
public class PsBillingWorkerContextTest extends ContextTestSupport {

    @Override
    public ChemodanMainSupport createMain() {
        return new PsBillingWorkerMain();
    }
}
