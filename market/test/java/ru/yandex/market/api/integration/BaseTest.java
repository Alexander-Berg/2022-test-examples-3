package ru.yandex.market.api.integration;

import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.util.httpclient.spi.HttpExpectations;

import javax.inject.Inject;

public abstract class BaseTest extends ContainerTestBase {

    protected Context context;
    protected GenericParams genericParams;

    @Inject
    protected HttpExpectations httpExpectations;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        genericParams = GenericParams.DEFAULT;
        context = BaseTestContext.newContext();
        httpExpectations.reset();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        context = null;
        genericParams = null;
        ContextHolder.reset();
        httpExpectations.verify();
    }
}
