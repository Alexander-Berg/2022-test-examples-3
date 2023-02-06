package ru.yandex.travel.orders.services;

import java.util.concurrent.CompletableFuture;

import org.asynchttpclient.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.orders.services.MailSenderConfigurationProperties.DebugProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MailSenderServiceImplTest {
    private MailSenderServiceImpl service;
    private AsyncHttpClientWrapper ahcWrapper;
    private Response okResponse;

    @Before
    public void init() {
        MailSenderConfigurationProperties properties = MailSenderConfigurationProperties.builder()
                .mailerUrlBase("https://sender/api/")
                .authenticationKey("test_auth_key")
                .debug(new DebugProperties())
                .build();
        ahcWrapper = Mockito.mock(AsyncHttpClientWrapper.class);
        service = new MailSenderServiceImpl(properties, ahcWrapper);
        okResponse = Mockito.mock(Response.class);
        when(okResponse.getStatusCode()).thenReturn(HttpStatus.OK.value());
    }

    @Test
    public void sendEmailSync_campaignIdFix() {
        when(ahcWrapper.executeRequest(any())).thenReturn(CompletableFuture.completedFuture(okResponse));

        // new proper id
        service.sendEmailSync("CAMPAIGN_1_ID", "foo@test.ru", null, null, null, null, null);
        verify(ahcWrapper).executeRequest(argThat(rb ->
                rb.build().getUrl().startsWith("https://sender/api/CAMPAIGN_1_ID/send?")));

        // old id
        service.sendEmailSync("CAMPAIGN_2_ID/send", "foo@test.ru", null, null, null, null, null);
        verify(ahcWrapper).executeRequest(argThat(rb ->
                rb.build().getUrl().startsWith("https://sender/api/CAMPAIGN_2_ID/send?")));
    }
}
