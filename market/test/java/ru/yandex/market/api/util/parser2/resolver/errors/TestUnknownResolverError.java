package ru.yandex.market.api.util.parser2.resolver.errors;

public class TestUnknownResolverError implements ResolverError {
    private final String source;

    public TestUnknownResolverError(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String getMessage(String parserDescription) {
        return "Can not process value: " + source;
    }
}
