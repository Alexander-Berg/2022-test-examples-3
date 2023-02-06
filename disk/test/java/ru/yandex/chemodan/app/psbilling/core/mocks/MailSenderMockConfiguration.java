package ru.yandex.chemodan.app.psbilling.core.mocks;

import lombok.SneakyThrows;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.chemodan.app.psbilling.core.config.MailSendingConfiguration;
import ru.yandex.chemodan.app.psbilling.core.mail.configuration.GlobalMailConfiguration;
import ru.yandex.chemodan.util.http.DiskInstrumentedClosableHttpClient;
import ru.yandex.chemodan.util.http.HttpClientConfigurator;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;

@Configuration
@Import({MailSendingConfiguration.class})
public class MailSenderMockConfiguration {
    @Autowired
    private HttpClientConfigurator senderHttpClientConfigurator;

    private HttpClient httpClientMock;
    private GlobalMailConfiguration mailConfigurationMock;


    @Primary
    @Bean
    public GlobalMailConfiguration mailConfiguration() {
        mailConfigurationMock = Mockito.mock(GlobalMailConfiguration.class,
                Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
        return mailConfigurationMock;
    }

    @Bean
    @Primary
    public HttpClient senderHttpClient() {
        httpClientMock = Mockito.mock(DiskInstrumentedClosableHttpClient.class,
                Mockito.withSettings().spiedInstance(senderHttpClientConfigurator.configure()).defaultAnswer(CALLS_REAL_METHODS));
        return httpClientMock;
    }

    public void reset() {
        Mockito.reset(httpClientMock);
        Mockito.reset(mailConfigurationMock);
    }

    @SneakyThrows
    public void mockHttpClientResponse(int statusCode, String reasonPhrase) {
        HttpResponse response = new BasicHttpResponse(
                new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, reasonPhrase));
        doAnswer(invocation -> {
            @SuppressWarnings("rawtypes")
            ResponseHandler arg = invocation.getArgument(1);
            return arg.handleResponse(response);
        }).when(httpClientMock).execute(Mockito.any(HttpUriRequest.class), Mockito.any(ResponseHandler.class));
    }

    public void mockConfigToEnabled() {
        Mockito.when(mailConfigurationMock.isTaskEnabled(Mockito.notNull())).thenReturn(Boolean.TRUE);
    }

    @SneakyThrows
    public HttpPost verifyEmailSent() {
        ArgumentCaptor<HttpPost> requestArgumentCaptor = ArgumentCaptor.forClass(HttpPost.class);
        Mockito.verify(httpClientMock, Mockito.times(1))
                .execute(requestArgumentCaptor.capture(), Mockito.any(ResponseHandler.class));

        return requestArgumentCaptor.getValue();
    }

    @SneakyThrows
    public void verifyNoEmailSent() {
        Mockito.verify(httpClientMock, Mockito.never()).execute(Mockito.any(), Mockito.any(ResponseHandler.class));
    }
}
