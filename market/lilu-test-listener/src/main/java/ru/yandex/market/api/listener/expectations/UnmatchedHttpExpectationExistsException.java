package ru.yandex.market.api.listener.expectations;

import java.util.List;
import java.util.stream.Collectors;

public class UnmatchedHttpExpectationExistsException extends RuntimeException {
    public UnmatchedHttpExpectationExistsException(List<PredefinedHttpResponse> configuration) {
        super(formatMessage(configuration));
    }

    private static String formatMessage(List<PredefinedHttpResponse> configuration) {
        String expectations = configuration.stream()
            .map(x -> x.getHttpRequestDescription().toString())
            .collect(Collectors.joining("\n"));
        return String.format("unmatched expectation exists:%n%s", expectations);
    }
}
