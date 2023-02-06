package ru.yandex.chemodan.app.psbilling.core.mocks;

import java.nio.charset.StandardCharsets;

import lombok.Getter;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.chemodan.app.psbilling.core.config.FeaturesSynchronize;
import ru.yandex.chemodan.app.psbilling.core.config.PsBillingCoreMocksConfig;

@Configuration
public class FeaturesSynchronizeRestTemplateMockConfiguration {
    @Autowired
    private PsBillingCoreMocksConfig mocksConfig;

    @Getter
    private RestTemplate featuresSynchronizeRestTemplate;

    @Primary
    @FeaturesSynchronize
    @Bean(name = {"featuresSynchronizeRestTemplate"})
    public RestTemplate featuresSynchronizeRestTemplateMock() {
        return this.featuresSynchronizeRestTemplate = mocksConfig.addMock(RestTemplate.class);
    }


    public void mockPostOk(String url) {
        mockPostOk(url, "");
    }

    public void mockPostOk(String url, String response) {
        HttpStatus statusCode = HttpStatus.OK;
        mockMethod(HttpMethod.POST, url, response, statusCode);
    }

    public void mockGetOk(String url, String response) {
        HttpStatus statusCode = HttpStatus.OK;
        mockMethod(HttpMethod.GET, url, response, statusCode);
    }

    public void mockMethod(HttpMethod httpMethod, String url, String response, HttpStatus statusCode) {
        Mockito.when(featuresSynchronizeRestTemplate.exchange(
                url, Mockito.eq(httpMethod),
                Mockito.any(), Mockito.eq(String.class)
        )).then(invocation -> {
            if (statusCode.is5xxServerError()) {
                throw new HttpServerErrorException(statusCode, "", response.getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8);
            }
            if (statusCode.is4xxClientError()) {
                throw new HttpClientErrorException(statusCode, "", response.getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8);
            }
            return new ResponseEntity<>(response, statusCode);
        });
    }
}
