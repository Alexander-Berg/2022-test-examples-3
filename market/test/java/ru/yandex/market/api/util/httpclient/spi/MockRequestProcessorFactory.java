package ru.yandex.market.api.util.httpclient.spi;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ru.yandex.market.http.CacheMode;
import ru.yandex.market.http.RequestProcessor;
import ru.yandex.market.http.RequestProcessorFactory;
import ru.yandex.market.http.listeners.RequestProcessorEventListener;

/**
 * Created by tesseract on 27.09.16.
 */
public class MockRequestProcessorFactory implements RequestProcessorFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MockRequestProcessorFactory.class);

    private final Environment propertySource;
    private final HttpExpectations httpExpectations;
    private final ApplicationContext appContext;

    public MockRequestProcessorFactory(
            Environment propertySource,
            HttpExpectations httpExpectations,
            ApplicationContext appContext
    ) {
        this.propertySource = propertySource;
        this.httpExpectations = httpExpectations;
        this.appContext = appContext;
    }

    @Override
    public RequestProcessor create(String name, int timeout, int connectTimeout,
                                   int maxPacketSize, RequestProcessorEventListener listener) {
        return new MockRequestProcessor(httpExpectations, build(name), appContext);
    }

    private MockExternal build(String configuration) {
        MockExternal result = new MockExternal();

        for (String parameterName : Splitter.on(',').split(property(null, configuration, "parameters.ignored", ""))) {
            result.getIgnoredParams().add(parameterName);
        }

        LOG.debug("#moc_request_processor_factory, {}, IGNORED_PARAMETERS: {}", configuration,
                result.getIgnoredParams());
        List<String> testNames = Lists.newArrayList("");
        testNames.addAll(Lists.newArrayList(Splitter.on(',').split(propertySource.getProperty("mock.test.names", ""))));

        for (String mockName : Lists.newArrayList("", propertySource.getProperty("mock.name"), null)) {
            if (Objects.isNull(mockName)) {
                continue;
            }
            for (String testName : testNames) {
                String prefix = Strings.isNullOrEmpty(testName) ? "" : (testName + ".");
                String index;
                for (int i = 0; hasProperty(mockName, configuration, index = prefix + String.valueOf(i), "url"); ++i) {
                    List<Integer> invocationNumbers = Lists.newArrayList((Integer) null);
                    String invocations = property(mockName, configuration, index, null, "invocations", "");
                    if (Objects.nonNull(invocations)) {
                        Iterable<String> split = Iterables.filter(Splitter.on(',').split(invocations),
                                v -> !Strings.isNullOrEmpty(v));
                        invocationNumbers.addAll(Lists.newArrayList(Iterables.transform(split,
                                (String v) -> Integer.parseInt(v))));
                    }

                    for (Integer invocationNumber : invocationNumbers) {
                        process(result, configuration, mockName, testName, index, invocationNumber);
                    }
                }
            }
        }

        for (String key : result.getIndex().keySet()) {
            LOG.debug("#moc_request_processor_factory, {}, REGISTERED: {}", configuration, key);
        }

        return result;
    }


    private String configurationPrefix(String testName, String configuration) {
        if (Strings.isNullOrEmpty(testName)) {
            return "mock.http." + configuration;
        } else {
            return "mock." + testName + ".http." + configuration;
        }
    }

    private boolean hasProperty(String testName, String configuration, String position, String name) {
        LOG.trace("#moc_request_processor_factory, CHECK: {}", propertyName(testName, configuration, position, null,
                name));
        return Objects.nonNull(propertySource.getProperty(propertyName(testName, configuration, position, null, name)));
    }

    private void process(MockExternal result, String configuration, String mockName, String testName, String index,
                         Integer invocationNumber) {
        MockResource mock = new MockResource();

        mock.setMethod(property(mockName, configuration, index, null, "method", "GET"));
        mock.setUrl(property(mockName, configuration, index, null, "url", ""));
        mock.setContentMd5(property(mockName, configuration, index, null, "contentMd5", ""));
        mock.setStatus(Integer.parseInt(property(mockName, configuration, index, invocationNumber, "status", "200")));
        mock.setTimeout(Integer.parseInt(property(mockName, configuration, index, invocationNumber, "timeout", "1")));
        mock.setError(property(mockName, configuration, index, invocationNumber, "error", ""));

        String defaultResource = "/mock/" +
                (Strings.isNullOrEmpty(mockName) ? "" : mockName + "/") +
                "http/" + configuration + "/" + index +
                (Objects.nonNull(invocationNumber) ? "." + invocationNumber : "");
        mock.setValue(property(mockName, configuration, index, invocationNumber, "resource", defaultResource));

        String key = MockHelper.key(mock.getMethod(),
                mock.getContentMd5(),
                testName,
                mock.getUrl(),
                result.getIgnoredParams(),
                invocationNumber);
        result.addResources(key, mock);

        LOG.debug("#moc_request_processor_factory, LOADED: {}={}", propertyName(mockName, configuration, index,
                invocationNumber, "url"), mock.getUrl());
    }

    private String property(String testName, String configuration, String name, String defaultValue) {
        return propertySource.getProperty(propertyName(testName, configuration, name), defaultValue);
    }

    private String property(String testName,
                            String configuration,
                            String position,
                            Integer invocationNumber,
                            String name,
                            String defaultValue) {
        List<String> values = Lists.newArrayList();
        values.add(propertySource.getProperty(propertyName(testName, configuration, position, invocationNumber, name)));
        values.add(propertySource.getProperty(propertyName(testName, configuration, position, null, name)));
        values.add(propertySource.getProperty(propertyName(testName, configuration, name)));
        values.add(propertySource.getProperty(propertyName(null, configuration, name)));
        values.add(defaultValue);
        return Iterables.find(values, Predicates.notNull());
    }

    private String propertyName(String testName, String configuration, String name) {
        return configurationPrefix(testName, configuration) + "." + name;
    }

    @NotNull
    private String invocationNumberPart(Integer invocationNumber) {
        return Objects.nonNull(invocationNumber) ? "[" + invocationNumber + "]" : "";
    }

    private String propertyName(String testName,
                                String configuration,
                                String position,
                                Integer invocationNumber,
                                String name) {
        return configurationPrefix(testName, configuration) + "[" + position + "]" +
                invocationNumberPart(invocationNumber) + "." + name;
    }
}
