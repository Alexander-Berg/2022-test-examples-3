package ru.yandex.market.mbi.bpmn.mvc;

import java.io.IOException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.model.DatacampPartnerProperties;
import ru.yandex.market.mbi.bpmn.model.DatacampUpdatePartnerPropertiesRequest;
import ru.yandex.market.mbi.bpmn.model.FeedRegisterUrlRequest;
import ru.yandex.market.mbi.bpmn.model.RegisterFeedResponse;
import ru.yandex.market.mbi.bpmn.model.SiteRegisterUrlRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на {@link DatacampController}
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatacampControllerTest extends FunctionalTest {

    @Test
    void testDatacampFeedRegisterSite() {
        ru.yandex.market.mbi.open.api.client.model.RegisterFeedResponse mbiApiResponse =
                new ru.yandex.market.mbi.open.api.client.model.RegisterFeedResponse()
                        .businessId(1L)
                        .feedId(1L)
                        .partnerId(1L);
        when(mbiOpenApiClient.datacampFeedRegisterSite(any())).thenReturn(mbiApiResponse);

        RegisterFeedResponse response = doPost("/datacamp/feed/register/site",
                new SiteRegisterUrlRequest()
                        .url("http://example.com"),
                RegisterFeedResponse.class
        );
        Assertions.assertThat(response).usingRecursiveComparison().ignoringFields("processId")
                .isEqualTo(mbiApiResponse);
    }

    @Test
    void testDatacampFeedRegisterUrl() {
        ru.yandex.market.mbi.open.api.client.model.RegisterFeedResponse mbiApiResponse =
                new ru.yandex.market.mbi.open.api.client.model.RegisterFeedResponse()
                        .businessId(1L)
                        .feedId(1L)
                        .partnerId(1L);
        when(mbiOpenApiClient.datacampFeedRegisterUrl(any())).thenReturn(mbiApiResponse);

        RegisterFeedResponse response = doPost("/datacamp/feed/register/url",
                new FeedRegisterUrlRequest()
                        .url("http://example.com"),
                RegisterFeedResponse.class
        );
        Assertions.assertThat(response).usingRecursiveComparison().ignoringFields("processId")
                .isEqualTo(mbiApiResponse);
    }

    @Test
    void testDatacampFeedUpdateFeatures() throws IOException {
        HttpPost httpPost = doHttpPost("/datacamp/feed/update/features",
                new DatacampUpdatePartnerPropertiesRequest()
                        .partnerId(1L)
                        .datacampPartnerProperties(
                                new DatacampPartnerProperties()
                                        .verticalShare(true)
                        )
        );
        int code = HttpClientBuilder.create().build().execute(httpPost).getStatusLine().getStatusCode();
        Assertions.assertThat(code).isEqualTo(200);
    }

}
