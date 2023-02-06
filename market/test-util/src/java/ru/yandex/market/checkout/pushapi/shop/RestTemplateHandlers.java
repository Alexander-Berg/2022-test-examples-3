package ru.yandex.market.checkout.pushapi.shop;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.log4j.Logger;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.client.*;
import ru.yandex.market.checkout.pushapi.client.entity.shop.AuthType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestTemplateHandlers {

    private static final Logger log = Logger.getLogger(RestTemplateHandlers.class);
    public static final String AUTHORIZATION_HEADER = "Authorization";

    public <T> RequestCallback requestCallback(
        final RestTemplate restTemplate, final T object, final Class<T> clazz, final HttpBodies httpBodies,
        final Settings settings
    ) {
        return new RequestCallback() {
            @Override
            public void doWithRequest(final ClientHttpRequest request) throws IOException {
                if(settings.getAuthType() == AuthType.HEADER) {
                    request.getHeaders().add(AUTHORIZATION_HEADER, settings.getAuthToken());
                }
                final ByteArrayOutputStream bodyOutputStream = new ByteArrayOutputStream();
                for(HttpMessageConverter messageConverter : restTemplate.getMessageConverters()) {
                    if(messageConverter.canWrite(clazz, MediaType.ALL)) {
                        final HttpMessageConverter<T> typedMessageConverter = messageConverter;
                        final MediaType mediaType = typedMessageConverter.getSupportedMediaTypes().get(0);
                        typedMessageConverter.write(object, mediaType, new ClientHttpRequest() {
                            @Override
                            public ClientHttpResponse execute() throws IOException {
                                return request.execute();
                            }

                            @Override
                            public OutputStream getBody() throws IOException {
                                return new TeeOutputStream(request.getBody(), bodyOutputStream);
                            }

                            @Override
                            public HttpMethod getMethod() {
                                return request.getMethod();
                            }

                            @Override
                            public URI getURI() {
                                return request.getURI();
                            }

                            @Override
                            public HttpHeaders getHeaders() {
                                return request.getHeaders();
                            }
                        });
                    }
                }

                final OutputStreamWriter headersWriter = new OutputStreamWriter(httpBodies.getRequestHeaders());
                headersWriter.write(
                    "" + request.getMethod().toString()
                        + " " + request.getURI().getPath()
                        + " HTTP/1.1\n"
                );
                for(Map.Entry<String, List<String>> stringListEntry : request.getHeaders().entrySet()) {
                    final String headerName = stringListEntry.getKey();
                    for(String headerValue : stringListEntry.getValue()) {
                        headersWriter.write(headerName + ": " + headerValue + "\n");
                    }
                }
                headersWriter.flush();

                final OutputStreamWriter bodyWriter = new OutputStreamWriter(httpBodies.getRequestBody());
                bodyWriter.write(new String(bodyOutputStream.toByteArray()));
                bodyWriter.flush();
            }
        };
    }

    public <T> ResponseExtractor<T> responseExtractor(
        final RestTemplate restTemplate, final Class<T> clazz, final HttpBodies httpBodies
    ) {
        return new ResponseExtractor<T>() {
            @Override
            public T extractData(final ClientHttpResponse response) throws IOException {
                final ByteArrayOutputStream body = new ByteArrayOutputStream();
                if(response.getBody() != null) {
                    IOUtils.copy(response.getBody(), body);
                }
                final MediaType mediaType = extractMediaType(response.getHeaders());

                final OutputStreamWriter headersWriter = new OutputStreamWriter(httpBodies.getResponseHeaders());
                headersWriter.write("HTTP/1.1 " + response.getStatusCode() + " " + response.getStatusText() + "\n");
                for(Map.Entry<String, List<String>> stringListEntry : response.getHeaders().entrySet()) {
                    final String headerName = stringListEntry.getKey();
                    for(String headerValue : stringListEntry.getValue()) {
                        headersWriter.write(headerName + ": " + headerValue + "\n");
                    }
                }
                headersWriter.flush();

                final OutputStreamWriter bodyWriter = new OutputStreamWriter(httpBodies.getResponseBody());
                bodyWriter.write(new String(body.toByteArray()));
                bodyWriter.flush();

                final HttpStatus statusCode = response.getStatusCode();
                final HttpStatus.Series series = statusCode.series();
                if(series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.REDIRECTION) {
                    throw new HttpClientErrorException(statusCode, response.getStatusText());
                } else if(series == HttpStatus.Series.SERVER_ERROR) {
                    throw new HttpServerErrorException(statusCode, response.getStatusText());
                } else {
                    if(clazz == Void.class) {
                        return null;
                    } else {
                        return readMessage(response, body, mediaType);
                    }
                }
            }

            private T readMessage(final ClientHttpResponse response, final ByteArrayOutputStream body, MediaType mediaType) throws IOException {
                final List<MediaType> supportedMediaTypes = new ArrayList<>();
                for(HttpMessageConverter messageConverter : restTemplate.getMessageConverters()) {
                    supportedMediaTypes.addAll(messageConverter.getSupportedMediaTypes());
                    if(messageConverter.canRead(clazz, mediaType)) {
                        final HttpMessageConverter<T> cartResponseMessageConverter = messageConverter;
                        if(body.toByteArray().length > 0) {
                            return cartResponseMessageConverter.read(clazz, new HttpInputMessage() {
                                @Override
                                public InputStream getBody() throws IOException {
                                    return new ByteArrayInputStream(body.toByteArray());
                                }

                                @Override
                                public HttpHeaders getHeaders() {
                                    return response.getHeaders();
                                }
                            });
                        } else {
                            return null;
                        }
                    }
                }

                log.error("unsupported media-type " + mediaType + " for class " + clazz);
                throw new HttpMessageConversionException(
                    "unsupported media-type",
                    new HttpMediaTypeNotSupportedException(mediaType, supportedMediaTypes)
                );
            }
        };
    }

    private MediaType extractMediaType(HttpHeaders httpHeaders) {
        for(String headerName : httpHeaders.keySet()) {
            if(headerName.equalsIgnoreCase("content-type")) {
                final List<String> values = httpHeaders.get(headerName);
                if(values.size() > 0) {
                    final String firstValue = values.get(0);
                    return MediaType.valueOf(firstValue);
                }

                break;
            }
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }

}
