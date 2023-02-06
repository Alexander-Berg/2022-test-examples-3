package ru.yandex.market.logistics.yard.base;

import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.yard.config.BlackBoxProperties;
import ru.yandex.market.logistics.yard.service.auth.BlackBoxAuthenticationFilter;
import ru.yandex.market.logistics.yard.service.auth.BlackboxRequestManager;

public abstract class AbstractSecurityMockedContextualTest extends AbstractContextualTest {

    @MockBean
    public BlackBoxProperties blackBoxProperties;

    @MockBean
    public BlackboxRequestManager blackboxRequestManager;

    @MockBean
    public BlackBoxAuthenticationFilter blackBoxAuthenticationFilter;
}
