package ru.yandex.market.logistics.lom.service;

import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;

public class AbstractExternalServiceTest extends AbstractContextualTest {

    protected static final ClientRequestMeta EXPECTED_CLIENT_REQUEST_META = new ClientRequestMeta("123");
}
