package ru.yandex.market.pricelabs;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import io.swagger.annotations.ApiOperation;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.misc.PricelabsRuntimeException;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.spring.exceptions.PricelabsExceptionHandler;
import ru.yandex.market.pricelabs.spring.interceptors.PricelabsInterceptors;
import ru.yandex.market.pricelabs.spring.security.TVM2SecurityFilter;
import ru.yandex.market.pricelabs.spring.security.TVM2ServiceSecurity;
import ru.yandex.market.pricelabs.spring.security.TVM2ServiceSecurity.ServiceTicket;
import ru.yandex.market.pricelabs.spring.security.TVM2UserSecurity;
import ru.yandex.market.pricelabs.spring.security.TVM2UserSecurity.UserTicket;
import ru.yandex.passport.tvmauth.TicketStatus;

/**
 * <p>
 * Класс выполняет проксирование к REST контроллеру через вызовы методов интерфейсов.
 * </p>
 * <ul>
 *     <li>Позволят проверять бины так, как они будут вызываться в реальности (с ValidationApi во всех входящих
 *     параметрах).</li>
 *     <li>Выполняет запросы с учетом контекста безопасности</li>
 *     <li>Позволяет проверять текст ошибки так, как он будет сформирован ручками.</li>
 *     <li>Выполняет запросы с разными возвращаемыми форматами (json, csv, excel)</li>
 * </ul>
 */
@Slf4j
public class MockMvcProxy {
    public static final long DEFAULT_UID = 669874;
    private static final int DEFAULT_CID = 1112336;

    private static final Map<Object, Object> IMPLEMENTATION_MAP = new ConcurrentHashMap<>();
    private static final Deque<Map<String, Object>> HEADER_STACK = new ArrayDeque<>();

    private MockMvcProxy() {
        //
    }

    public static <T> void registerProxy(MockWebServer mockWebServer, T implementation) {
        IMPLEMENTATION_MAP.computeIfAbsent(Pair.of(mockWebServer, implementation), pair ->
                new MockWebServerProxyBuilder<>(mockWebServer, implementation));
    }

    public static <T> T buildProxy(Class<T> classInterface, T implementation) {
        return buildProxy(classInterface, implementation, Utils.emptyConsumer());
    }

    // Создание прокси класса со специальными конвертерами (можно тестировать выгрузку в CSV и Excel)
    @SuppressWarnings("unchecked")
    public static <T> T buildProxy(Class<T> classInterface, T implementation,
                                   Supplier<List<HttpMessageConverter<?>>> converters) {
        // Эффективное кэширование настроек MockMvc, если список конвертеров не меняется
        // (т.е. создается в лямбде без контекста)
        Consumer<StandaloneMockMvcBuilder> cachedCfg =
                (Consumer<StandaloneMockMvcBuilder>) IMPLEMENTATION_MAP.computeIfAbsent(converters, conv ->
                        (Consumer<StandaloneMockMvcBuilder>) cfg -> {
                            List<HttpMessageConverter<?>> allConverters = new ArrayList<>();
                            new WebMvcConfigurationSupport() {
                                {
                                    addDefaultHttpMessageConverters(allConverters);
                                }
                            };
                            allConverters.addAll(((Supplier<List<HttpMessageConverter<?>>>) conv).get());
                            cfg.setMessageConverters(allConverters.toArray(HttpMessageConverter[]::new));
                        });

        return buildProxy(classInterface, implementation, cachedCfg);
    }

    @SuppressWarnings("unchecked")
    public static <T> T buildProxy(Class<T> classInterface, T implementation, Consumer<StandaloneMockMvcBuilder> cfg) {
        var key = new ProxyCfg<>(classInterface, implementation, cfg);
        return (T) IMPLEMENTATION_MAP.computeIfAbsent(key, pair ->
                new RestControllerProxyBuilder<>(key).buildProxy());
    }

    /**
     * Выполняет запрос с передачей отдельного MediaType, в котором будет отрендерен результат вызова ручки (не json)
     *
     * @param mediaType      формат данных (должен быть одним из указанных в {@link RequestMapping#produces()}
     * @param implementation метод, который нужно дернуть
     * @return результат выполнения метода в виде массива байтов
     */
    @SuppressWarnings("unchecked")
    public static ResponseEntity<byte[]> withMediaType(String mediaType, Supplier<ResponseEntity<?>> implementation) {
        HEADER_STACK.push(Map.of("Accept", mediaType));
        return (ResponseEntity<byte[]>) implementation.get();
    }

    private static class MockWebServerProxyBuilder<T> {

        private MockWebServerProxyBuilder(@NonNull MockWebServer mockWebServer, @NonNull T implementation) {
            log.info("Configuring new MockWebServer based on {}", implementation);

            var serviceSec = new TVM2ServiceSecurity(
                    ticket -> new ServiceTicket(TicketStatus.OK, DEFAULT_CID, ApiConst.UNKNOWN_UID),
                    new int[]{DEFAULT_CID},
                    DEFAULT_CID,
                    true);
            var userSec = new TVM2UserSecurity(
                    ticket -> {
                        throw new PricelabsRuntimeException("Ticket is trusted, does not expect any check");
                    },
                    new int[]{DEFAULT_CID},
                    DEFAULT_CID);

            var mockMvc = MockMvcBuilders.standaloneSetup(implementation)
                    .addInterceptors(PricelabsInterceptors.getInterceptors())
                    .addFilter(new TVM2SecurityFilter(serviceSec, userSec))
                    .setHandlerExceptionResolvers(new PricelabsExceptionHandler(null))
                    .build();

            mockWebServer.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    try {
                        var mvcRequest = MockMvcRequestBuilders
                                .request(request.getMethod(), request.getRequestUrl().uri())
                                .characterEncoding(StandardCharsets.UTF_8.name());

                        for (var headerEntry : request.getHeaders().toMultimap().entrySet()) {
                            mvcRequest.header(headerEntry.getKey(), headerEntry.getValue().toArray());
                        }

                        var contentType = request.getHeader("Content-Type");
                        if (contentType != null) {
                            mvcRequest.contentType(contentType);
                        }
                        if (request.getBodySize() > 0) {
                            byte[] body = request.getBody().readByteArray();
                            mvcRequest.content(body);
                        }
                        var mvcResponse = mockMvc.perform(mvcRequest).andReturn().getResponse();
                        var mockResponse = new MockResponse()
                                .setResponseCode(mvcResponse.getStatus())
                                .setBody(mvcResponse.getContentAsString(StandardCharsets.UTF_8));
                        if (Utils.isNonEmpty(mvcResponse.getErrorMessage())) {
                            mockResponse.setBody(mvcResponse.getErrorMessage());
                        }

                        for (String headerName : mvcResponse.getHeaderNames()) {
                            mockResponse.addHeader(headerName, mvcResponse.getHeaderValue(headerName));
                        }
                        return mockResponse;
                    } catch (Exception e) {
                        log.error("Unexpected exception when processing TMS request", e);
                        return new MockResponse()
                                .setResponseCode(500)
                                .setBody(e.getMessage());
                    }
                }
            });
        }
    }

    private static class RestControllerProxyBuilder<T> {

        private final Class<T> classInterface;
        private final MockMvc mockMvc;
        private final Map<Method, Function<Object[], Object>> invocationMap = new ConcurrentHashMap<>();
        private final String requestPrefix;

        private RestControllerProxyBuilder(@NonNull ProxyCfg<T> cfg) {
            log.info("Building new RestControllerProxy based on {}", cfg);

            if (!cfg.classInterface.isInterface()) {
                throw new IllegalStateException("Expect only interfaces here");
            }

            this.requestPrefix = cfg.implementation.getClass().getAnnotation(RequestMapping.class).value()[0];
            this.classInterface = cfg.classInterface;

            var serviceSec = new TVM2ServiceSecurity(
                    ticket -> new ServiceTicket(TicketStatus.OK, DEFAULT_CID, ApiConst.UNKNOWN_UID),
                    new int[]{DEFAULT_CID},
                    -1,
                    true);
            var userSec = new TVM2UserSecurity(
                    ticket -> new UserTicket(TicketStatus.OK, DEFAULT_UID),
                    new int[]{DEFAULT_CID},
                    -1);

            var mockMvcBuilder = MockMvcBuilders.standaloneSetup(cfg.implementation)
                    .addInterceptors(PricelabsInterceptors.getInterceptors())
                    .addFilter(new TVM2SecurityFilter(serviceSec, userSec))
                    .setHandlerExceptionResolvers(new PricelabsExceptionHandler(null));
            cfg.cfg.accept(mockMvcBuilder);

            this.mockMvc = mockMvcBuilder.build();
        }

        @SuppressWarnings("unchecked")
        T buildProxy() {
            return (T) Proxy.newProxyInstance(classInterface.getClassLoader(), new Class<?>[]{classInterface},
                    (proxy, method, args) -> invocationMap.computeIfAbsent(method,
                            RestControllerProxyBuilder.this::buildInvocation).apply(args));
        }

        @SuppressWarnings("unchecked")
        private Function<Object[], Object> buildInvocation(Method method) {
            var apiOperation = method.getAnnotation(ApiOperation.class);
            var responseClass = apiOperation.response();
            var responseContainer = apiOperation.responseContainer();

            Function<String, Object> responseConverter;
            if (String.class.equals(responseClass)) {
                responseConverter = response -> response;
            } else if (Void.class.equals(responseClass)) {
                responseConverter = response -> null;
            } else if ("List".equals(responseContainer)) {
                responseConverter = response -> Utils.fromJsonStringList(response, responseClass);
            } else {
                responseConverter = response -> Utils.fromJsonString(response, responseClass);
            }

            var requestMapping = method.getAnnotation(RequestMapping.class);

            var requestMethod = requestMapping.method()[0].name();
            var requestPath = requestMapping.value()[0];
            var consumes = requestMapping.consumes();
            var produces = Set.of(requestMapping.produces());

            var params = method.getParameters();
            var paramAnnotations = method.getParameterAnnotations();

            BiConsumer<MockHttpServletRequestBuilder, Object>[] builders = new BiConsumer[params.length];
            for (int i = 0; i < params.length; i++) {
                var param = params[i];
                var annotations = paramAnnotations[i];

                var requestParamOpt = Stream.of(annotations)
                        .filter(ann -> ann instanceof RequestParam)
                        .findFirst();
                var requestBodyOpt = Stream.of(annotations)
                        .filter(ann -> ann instanceof RequestBody)
                        .findFirst();

                if (requestParamOpt.isPresent()) {
                    RequestParam requestParam = (RequestParam) requestParamOpt.get();
                    if (Collection.class.isAssignableFrom(param.getType())) {
                        builders[i] = (req, arg) -> {
                            if (arg != null) {
                                req.queryParam(requestParam.value(), ((Collection<?>) arg).stream()
                                        .map(String::valueOf)
                                        .collect(Collectors.joining(",")));
                            }
                        };
                    } else {
                        builders[i] = (req, arg) -> {
                            if (arg != null) {
                                req.queryParam(requestParam.value(), String.valueOf(arg));
                            }
                        };
                    }
                } else if (requestBodyOpt.isPresent()) {
                    if (String.class.equals(param.getType())) {
                        builders[i] = (req, arg) -> {
                            req.contentType(consumes[0]);
                            req.content(arg == null ? "" : (String) arg);
                        };
                    } else {
                        builders[i] = (req, arg) -> {
                            req.contentType(consumes[0]);
                            req.content(arg == null ? "" : Utils.toJsonString(arg));
                        };
                    }

                } else {
                    throw new IllegalStateException("Unable to find param or body annotation for param " +
                            param + ": " + List.of(annotations));
                }
            }

            var uri = URI.create(requestPrefix + requestPath);
            log.info("Method [{}] points to [{}]", method.getName(), uri);
            return args -> {
                try {
                    var mvcRequest = MockMvcRequestBuilders.request(requestMethod, uri);

                    if (args != null) {
                        int len = args.length;
                        for (int i = 0; i < len; i++) {
                            builders[i].accept(mvcRequest, args[i]);
                        }
                    }

                    boolean withRawResponse = false;
                    @Nullable var headers = HEADER_STACK.pollLast();
                    if (Utils.isNonEmpty(headers)) {
                        for (var e : headers.entrySet()) {
                            var header = e.getKey();
                            var value = e.getValue();
                            mvcRequest.header(header, value);
                            if ("Accept".equals(header)) {
                                if (!produces.contains(String.valueOf(value))) {
                                    throw new IllegalStateException("Invalid Accept header, " + value +
                                            " must be one of " + produces);
                                }
                                withRawResponse = true;
                            }
                        }
                    }

                    if (!withRawResponse) {
                        mvcRequest.characterEncoding(StandardCharsets.UTF_8.name());
                    }

                    mvcRequest.header(ApiConst.TVM2_SERVICE_TICKET, "service");
                    mvcRequest.header(ApiConst.TVM2_USER_TICKET, "user");

                    var mvcResult = mockMvc.perform(mvcRequest).andReturn();
                    var mvcResponse = mvcResult.getResponse();

                    if (mvcResponse.getStatus() != 200) {
                        throw new MockMvcProxyHttpException(Utils.nvl(mvcResponse.getErrorMessage()));
                    }

                    if (withRawResponse) {
                        return ResponseEntity.ok(mvcResponse.getContentAsByteArray());
                    } else {
                        var responseContent = mvcResponse.getContentAsString(StandardCharsets.UTF_8);
                        return ResponseEntity.ok(responseConverter.apply(responseContent));
                    }
                } catch (MockMvcProxyHttpException e) {
                    throw e;
                } catch (Exception e) {
                    throw new MockMvcProxyHttpException(e.getMessage(), e);
                }
            };
        }
    }

    @Value
    private static class ProxyCfg<T> {
        @NonNull Class<T> classInterface;
        @NonNull T implementation;
        @NonNull Consumer<StandaloneMockMvcBuilder> cfg;
    }
}
