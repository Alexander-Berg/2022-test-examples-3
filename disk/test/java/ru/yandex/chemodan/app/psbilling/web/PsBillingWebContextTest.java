package ru.yandex.chemodan.app.psbilling.web;

import ru.yandex.chemodan.boot.ChemodanMainSupport;
import ru.yandex.chemodan.util.test.ContextTestSupport;

public class PsBillingWebContextTest extends ContextTestSupport {

    @Override
    public ChemodanMainSupport createMain() {
        return new PsBillingWebMain();
    }
}
