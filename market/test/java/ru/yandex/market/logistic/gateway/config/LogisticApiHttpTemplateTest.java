package ru.yandex.market.logistic.gateway.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistic.api.client.LogisticApiClientFactory;
import ru.yandex.market.logistic.api.model.common.PartnerMethod;
import ru.yandex.market.logistic.api.model.common.request.AbstractRequest;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.request.Token;
import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.gateway.BaseTest;

import static org.mockito.Mockito.when;

public class LogisticApiHttpTemplateTest extends BaseTest {

    private static final String URL = "someUrl";
    private static final Token TOKEN = new Token("someToken");
    private static final RequestWrapper<AbstractRequest> REQUEST_WRAPPER = new RequestWrapper<>();
    private static final HttpHeaders HTTP_HEADERS = new HttpHeaders();
    private static HttpEntity<RequestWrapper<AbstractRequest>> httpEntity;
    private static final TypeReference<ResponseWrapper<AbstractResponse>> TYPE_REFERENCE = new TypeReference<>() { };
    private static final XmlMapper XML_MAPPER = LogisticApiClientFactory.createXmlMapper();
    private static String parseableResponseBody;
    private static final String ERROR_MESSAGE_FOR_PARSEABLE_RESPONSE_BODY = "Destination 'ZipCode' is not specified";
    private static final String UNPARSEABLE_RESPONSE_BODY = "someUnparseableResponse";
    private static final String ERROR_MESSAGE_FOR_UNPARSEABLE_RESPONSE_BODY = "Unparseable response body: " +
            UNPARSEABLE_RESPONSE_BODY;

    @Mock
    private RestTemplate restTemplate;

    private HttpTemplateImpl httpTemplate;

    @Before
    public void setUp() throws IOException {
        REQUEST_WRAPPER.setToken(TOKEN);
        REQUEST_WRAPPER.setRequest(new AbstractRequest(PartnerMethod.CREATE_ORDER_DS));

        HTTP_HEADERS.setContentType(MediaType.TEXT_XML);
        HTTP_HEADERS.setAccept(Arrays.asList(MediaType.TEXT_XML, MediaType.APPLICATION_XML));

        httpEntity = new HttpEntity<>(REQUEST_WRAPPER, HTTP_HEADERS);

        httpTemplate = HttpTemplateImpl.builder()
                .defaultRestTemplate(restTemplate)
                .contentType(MediaType.TEXT_XML)
                .acceptTypes(Arrays.asList(MediaType.TEXT_XML, MediaType.APPLICATION_XML))
                .xmlMapper(XML_MAPPER)
                .build();

        parseableResponseBody =
                Files.readString(Path.of(
                        getClass().getClassLoader().getResource("parseable_response_body.xml").getPath())
                );
    }

    @Test
    public void throw4xxWithParseableResponseBody() {
        when(restTemplate.exchange(
                URL,
                HttpMethod.POST,
                httpEntity,
                ParameterizedTypeReference.forType(TYPE_REFERENCE.getType())
            )
        ).thenThrow(new HttpClientErrorException(
                HttpStatus.BAD_REQUEST,
                HttpStatus.BAD_REQUEST.toString(),
                parseableResponseBody.getBytes(),
                Charset.defaultCharset()
            )
        );
        ResponseWrapper<AbstractResponse> response = httpTemplate.executePost(REQUEST_WRAPPER, TYPE_REFERENCE, URL);
        Assert.assertFalse(response.getRequestState().getErrorCodes().isEmpty());
        Assert.assertEquals(
                ERROR_MESSAGE_FOR_PARSEABLE_RESPONSE_BODY,
                response.getRequestState().getErrorCodes().get(0).getMessage()
        );
    }

    @Test
    public void throw4xxWithUnparseableResponseBody() {
        when(restTemplate.exchange(
                URL,
                HttpMethod.POST,
                httpEntity,
                ParameterizedTypeReference.forType(TYPE_REFERENCE.getType())
            )
        ).thenThrow(new HttpClientErrorException(
                HttpStatus.BAD_REQUEST,
                HttpStatus.BAD_REQUEST.toString(),
                UNPARSEABLE_RESPONSE_BODY.getBytes(),
                Charset.defaultCharset()
            )
        );
        ResponseWrapper<AbstractResponse> response = httpTemplate.executePost(REQUEST_WRAPPER, TYPE_REFERENCE, URL);
        Assert.assertFalse(response.getRequestState().getErrors().isEmpty());
        Assert.assertEquals(
                ERROR_MESSAGE_FOR_UNPARSEABLE_RESPONSE_BODY,
                response.getRequestState().getErrors().get(0)
        );
    }
}
