package ru.yandex.market.replenishment.autoorder.config;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.NStringEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class DeepmindHttpClientTestConfig {

    @Bean
    @Qualifier("deepmindHttpClient")
    public CloseableHttpClient deepmindHttpClient() {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getEntity()).thenReturn(new NStringEntity("result", ContentType.APPLICATION_JSON));
        when(response.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion(
                "https", 0, 0), HttpStatus.SC_OK, "fake reason"));
        var client = mock(CloseableHttpClient.class);
        try {
            when(client.execute(any())).thenReturn(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return client;
    }

}
