package ru.yandex.chemodan.app.psbilling.core.mail.sending;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;

public abstract class BaseEmailSendTest extends AbstractPsBillingCoreTest {
    protected final ArgumentCaptor<HttpUriRequest> requestArgumentCaptor =
            ArgumentCaptor.forClass(HttpUriRequest.class);

    @Autowired
    protected ApplicationContext applicationContext;

    @Before
    public void setup() {
        mailSenderMockConfig.mockHttpClientResponse(200, "OK");
    }
}
