package ru.yandex.market.api.integration;

import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.util.httpclient.spi.HttpExpectations;

import javax.inject.Inject;

/**
 * Created by vdorogin on 18.05.17.
 */
public abstract class ParameterizedTestBase extends ParameterizedContainerTestBase {

    protected GenericParams genericParams;

    @Inject
    protected HttpExpectations httpExpectations;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        genericParams = GenericParams.DEFAULT;
        httpExpectations.reset();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        genericParams = null;
        ContextHolder.reset();
        httpExpectations.verify();
    }
}
