package ru.yandex.market.checkout.pushapi.shop;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.checkout.common.common.ClassMapping;
import ru.yandex.market.checkout.common.json.JsonDeserializer;
import ru.yandex.market.checkout.common.json.JsonSerializer;
import ru.yandex.market.checkout.common.json.jackson.JacksonMessageConverter;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.client.entity.shop.SettingsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class RestTemplateHandlersTest {

    private RestTemplateHandlers restTemplateHandlers = new RestTemplateHandlers();
    private HttpMessageConverter messageConverter;
    private MediaType mediaType = MediaType.APPLICATION_XML;
    private RestTemplate restTemplate;
    private MyClass myClass;

    @Before
    public void setUp() throws Exception {
        messageConverter = mock(HttpMessageConverter.class);
        when(messageConverter.canRead(MyClass.class, mediaType)).thenReturn(true);
        when(messageConverter.canRead(MyOtherClass.class, mediaType)).thenReturn(false);
        when(messageConverter.canWrite(MyClass.class, mediaType)).thenReturn(true);
        when(messageConverter.canWrite(MyOtherClass.class, mediaType)).thenReturn(false);

        myClass = new MyClass();
        when(messageConverter.read(eq(MyClass.class), any(HttpInputMessage.class))).thenReturn(myClass);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final HttpOutputMessage message = (HttpOutputMessage) invocationOnMock.getArguments()[2];
                final OutputStream body = message.getBody();
                body.write("myClass".getBytes());
                body.flush();

                return null;
            }
        }).when(messageConverter).write(any(), any(MediaType.class), any(HttpOutputMessage.class));

        restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(Arrays.<HttpMessageConverter<?>>asList(messageConverter));
    }

    @Test
    public void json() throws Exception {
        final RestTemplate restTemplate = new RestTemplate();
        final JacksonMessageConverter jacksonMessageConverter = new JacksonMessageConverter(new ObjectMapper());
        final ClassMapping<JsonDeserializer> deserializers = new ClassMapping<>();
        deserializers.setMapping(new HashMap<Class, JsonDeserializer>() {{
            put(MyClass.class, mock(JsonDeserializer.class));
        }});
        jacksonMessageConverter.setDeserializers(deserializers);
        final ClassMapping<JsonSerializer> serializers = new ClassMapping<>();
        serializers.setMapping(new HashMap<Class, JsonSerializer>() {{
            put(MyClass.class, mock(JsonSerializer.class));
        }});
        jacksonMessageConverter.setSerializers(serializers);

        restTemplate.setMessageConverters(
            Arrays.<HttpMessageConverter<?>>asList(
                jacksonMessageConverter
            )
        );

        final HttpBodies httpBodies = new HttpBodies();
        final Settings settings = new SettingsBuilder().build();
        final MyClass value = new MyClass();
        final RequestCallback requestCallback = restTemplateHandlers.requestCallback(
            restTemplate, value, MyClass.class, httpBodies, settings
        );

        final URI uri = URI.create("http://localhost/blah");
        final HttpMethod method = HttpMethod.POST;
        final MockClientHttpRequest request = new MockClientHttpRequest(method, uri);

        requestCallback.doWithRequest(request);

        final MediaType actual = request.getHeaders().getContentType();
        final MediaType expected = MediaType.valueOf("application/json; charset=utf-8");
        assertEquals(actual, expected);
    }

    @Test
    public void doesntTryToSerializeVoidClasses() throws Exception {
        final RestTemplate restTemplate = new RestTemplate();

        final ResponseExtractor<Void> responseExtractor = restTemplateHandlers.responseExtractor(
            restTemplate, Void.class, new HttpBodies()
        );
        final MockClientHttpResponse httpResponse = new MockClientHttpResponse("blah blah".getBytes(), HttpStatus.OK);
        httpResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        responseExtractor.extractData(httpResponse);
    }

    @Test
    public void capturesBodyIfExceptionWasThrownDuringMessageDeserializing() throws Exception {
        final RestTemplate restTemplate = new RestTemplate();
        final HttpMessageConverter<?> messageConverter = mock(HttpMessageConverter.class);
        restTemplate.setMessageConverters(Collections.<HttpMessageConverter<?>>singletonList(messageConverter));
        when(messageConverter.canRead(any(Class.class), any(MediaType.class))).thenReturn(true);
        when(messageConverter.read(any(Class.class), any(HttpInputMessage.class))).thenThrow(
            new HttpMessageConversionException("can't deserialize")
        );

        final HttpBodies httpBodies = new HttpBodies();

        final ResponseExtractor<MyClass> responseExtractor = restTemplateHandlers.responseExtractor(
            restTemplate, MyClass.class, httpBodies
        );

        final String responseBody = "body";
        final MockClientHttpResponse httpResponse = new MockClientHttpResponse(responseBody.getBytes(), HttpStatus.OK);

        try {
            responseExtractor.extractData(httpResponse);
            fail();
        } catch(HttpMessageConversionException e) {
            assertEquals(responseBody, httpBodies.getResponseBody().toString());
            assertEquals("HTTP/1.1 200 OK", httpBodies.getResponseHeaders().toString().trim());
        }
    }

    @Test(expected = HttpMessageConversionException.class)
    public void throwsExceptionIfThereIsNoSuitableConverter() throws Exception {
        final RestTemplate restTemplate = new RestTemplate();
        final HttpMessageConverter<?> messageConverter = mock(HttpMessageConverter.class);
        restTemplate.setMessageConverters(Collections.<HttpMessageConverter<?>>singletonList(messageConverter));
        when(messageConverter.canRead(any(Class.class), any(MediaType.class))).thenReturn(false);

        final HttpBodies httpBodies = new HttpBodies();

        final ResponseExtractor<MyClass> responseExtractor = restTemplateHandlers.responseExtractor(
            restTemplate, MyClass.class, httpBodies
        );

        final MockClientHttpResponse httpResponse = new MockClientHttpResponse("body".getBytes(), HttpStatus.OK);
        responseExtractor.extractData(httpResponse);
    }

    @Test
     public void testHandleRequestWithoutHeaderAuth() throws Exception {
        fail();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final MockClientHttpRequest mockClientHttpRequest = new MockClientHttpRequest(
            HttpMethod.POST, URI.create("http://localhost/blah")
        );
        mockClientHttpRequest.getHeaders().setContentType(MediaType.APPLICATION_XML);

        final HttpBodies httpBodies = new HttpBodies();
        final RequestCallback requestCallback = restTemplateHandlers.requestCallback(
            restTemplate,
            new MyClass(),
            MyClass.class,
            httpBodies,
            new SettingsBuilder().build()
        );
        requestCallback.doWithRequest(mockClientHttpRequest);

        final String request = new String(outputStream.toByteArray());
        assertEquals(
            "POST /blah HTTP/1.1\n" +
                "Content-Type: application/xml\n" +
                "\n" +
                "myClass",
            request
        );
    }

    @Test(expected = HttpClientErrorException.class)
    public void httpStatus3xx() throws Exception {
        final RestTemplate restTemplate = new RestTemplate();
        final HttpBodies httpBodies = new HttpBodies();
        final ResponseExtractor<MyClass> responseExtractor = restTemplateHandlers.responseExtractor(
            restTemplate, MyClass.class, httpBodies
        );

        responseExtractor.extractData(new MockClientHttpResponse(new byte[] {}, HttpStatus.MOVED_PERMANENTLY));
    }

    @Test
    public void testHandleRequestWithHeaderAuth() throws Exception {
        fail();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final MockClientHttpRequest mockClientHttpRequest = new MockClientHttpRequest(
            HttpMethod.POST, URI.create("http://localhost/blah")
        );
        mockClientHttpRequest.getHeaders().setContentType(MediaType.APPLICATION_XML);

        final RequestCallback requestCallback = restTemplateHandlers.requestCallback(
            restTemplate,
            new MyClass(),
            MyClass.class,
            new HttpBodies(),
            new SettingsBuilder().build()
        );
        requestCallback.doWithRequest(mockClientHttpRequest);

        final String request = new String(outputStream.toByteArray());
        assertEquals(
            "POST /blah HTTP/1.1\n" +
                "Content-Type: application/xml\n" +
                "Authorization: auth-token\n" +
                "\n" +
                "myClass",
            request
        );
    }

    @Test
     public void testHandleRegularResponse() throws Exception {
        fail();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final ResponseExtractor<MyClass> responseExtractor = restTemplateHandlers.responseExtractor(
            restTemplate, MyClass.class, new HttpBodies()
        );
        final MockClientHttpResponse mockResponse = new MockClientHttpResponse("myClass".getBytes(), HttpStatus.OK);
        mockResponse.getHeaders().setContentType(MediaType.APPLICATION_XML);
        assertEquals(myClass, responseExtractor.extractData(mockResponse));

        final String response = new String(outputStream.toByteArray());
        assertEquals(
            "HTTP/1.1 200 OK\n" +
                "Content-Type: application/xml\n" +
                "\n" +
                "myClass",
            response
        );
    }

    @Test
    public void testHandleErrorResponse() throws Exception {
        fail();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final ResponseExtractor<MyClass> responseExtractor = restTemplateHandlers.responseExtractor(
            restTemplate, MyClass.class, new HttpBodies()
        );
        final MockClientHttpResponse mockResponse = new MockClientHttpResponse(
            "error".getBytes(), HttpStatus.NOT_FOUND
        );
        mockResponse.getHeaders().setContentType(MediaType.TEXT_HTML);
            responseExtractor.extractData(mockResponse);
            final String response = new String(outputStream.toByteArray());
            assertEquals(
                "HTTP/1.1 404 Not Found\n" +
                    "Content-Type: text/html\n" +
                    "\n" +
                    "error",
                response
            );
    }

    static class MyClass {}
    static class MyOtherClass {}
}
