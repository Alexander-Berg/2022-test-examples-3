package ru.yandex.market.mbisfintegration.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.mbisfintegration.AbstractFunctionalTest;
import ru.yandex.market.mbisfintegration.generated.sf.model.Soap;
import ru.yandex.market.mbisfintegration.salesforce.SoapHolder;

import static org.mockito.Mockito.clearInvocations;

public class AbstractApiTest extends AbstractFunctionalTest {

    protected Soap soap;

    @Autowired
    private SoapHolder soapHolder;

    @BeforeEach
    void setUp() throws Exception {
        soap = soapHolder.getSoap();
    }

    @AfterEach
    void tearDown() {
        clearInvocations(soap);
    }

    protected <T> T call(ExecuteCall<T, ?> executeCall) {
        return executeCall.schedule().join();
    }
}
