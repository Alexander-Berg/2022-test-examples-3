package ru.yandex.market.ff.service.implementation.drivers;

import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.ff.model.dto.DaasServiceDTO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DaasServiceTest {

    private static final String HTML_PATH = "marketplace/driver-booklet-templates/kotelniki-shipment.html";
    private static final String META_AND_CONTENT_COMPONENTS = "html_heads,selector(query=.conbody)";

    private final RestTemplate mockRestTemplate = mock(RestTemplate.class);

    private final DaasServiceImpl daasService;

    public DaasServiceTest() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new NeverRetryPolicy());

         daasService = new DaasServiceImpl(mockRestTemplate, retryTemplate);
    }

    @Test
    public void getHtmlDocumentTest() {
        DaasServiceDTO dto = new DaasServiceDTO();
        dto.setDoccenterResponse("html with placeholders");
        dto.setProjectLink(new Object());
        when(mockRestTemplate.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<DaasServiceDTO>>any()))
                .thenReturn(new ResponseEntity<>(dto, HttpStatus.OK));

        String htmlDocument = daasService.getHtmlDocumentComponent(HTML_PATH, META_AND_CONTENT_COMPONENTS);

        assertNotNull(htmlDocument);
        assertEquals("html with placeholders", htmlDocument);
    }

    @Test
    public void getHtmlDocumentWithNoBodyTest() {
        when(mockRestTemplate.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<DaasServiceDTO>>any()))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        String htmlDocument = daasService.getHtmlDocumentComponent(HTML_PATH, META_AND_CONTENT_COMPONENTS);

        assertNotNull(htmlDocument);
        assertEquals("", htmlDocument);
    }

    @Test
    public void getHtmlDocumentWithRestTemplateExceptionTest() {
        when(mockRestTemplate.exchange(anyString(), any(), any(), ArgumentMatchers.<Class<DaasServiceDTO>>any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        String htmlDocument = daasService.getHtmlDocumentComponent(HTML_PATH, META_AND_CONTENT_COMPONENTS);
        assertEquals("", htmlDocument);
    }
}
