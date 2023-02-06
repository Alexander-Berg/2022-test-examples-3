package ru.yandex.market.pers.grade.core.util;

import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.apache.http.client.HttpClient;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withPath;

public class MbiApiClientTest {
    private static final Long EXIST_PARTNER_ID = 111L;

    private final HttpClient httpClient = mock(HttpClient.class);
    private final MbiApiClient mbiApiClient = new MbiApiClient(
            new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)),
            "http://target:90"
    );

    @Test
    public void testMethodGetSuperAdminUid() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/data/super_admin_response.json",
                withPath("/partners/" + EXIST_PARTNER_ID + "/superadmin"));
        Long superAdminUid = mbiApiClient.getSuperAdminUid(EXIST_PARTNER_ID);

        assertEquals(Long.valueOf(3249875680L), superAdminUid);
    }

    @Test
    public void testMethodGetSuperAdminUidWithNullAnswer() throws IOException {
        doThrow(new RestClientException("")).when(httpClient).execute(any(), any(HttpContext.class));
        Long superAdminUid = mbiApiClient.getSuperAdminUid(112L);

        assertNull(superAdminUid);
    }

}
