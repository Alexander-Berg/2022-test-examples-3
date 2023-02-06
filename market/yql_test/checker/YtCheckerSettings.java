package ru.yandex.market.yql_test.checker;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class YtCheckerSettings {

    private final Map<String, String> schemas;
    private final String initialCsvContent;
    private final String expectedCsvContent;

    public YtCheckerSettings(Map<String, String> schemas,
                             String initialCsvContent,
                             String expectedCsvContent) {
        this.schemas = schemas;
        this.initialCsvContent = initialCsvContent;
        this.expectedCsvContent = expectedCsvContent;
    }

    public Map<String, String> getSchemas() {
        return schemas;
    }

    public String getInitialCsvContent() {
        return initialCsvContent;
    }

    public String getExpectedCsvContent() {
        return expectedCsvContent;
    }

    public static YtCheckerSettingsBuilder ytCheckerSettingsBuilder() {
        return new YtCheckerSettingsBuilder();
    }

    public static class YtCheckerSettingsBuilder {

        private Map<String, String> schemas;
        private String initialCsvContent;
        private String expectedCsvContent;

        public YtCheckerSettingsBuilder withSchemas(Map<String, String> schemas) {
            this.schemas = schemas;
            return this;
        }

        public YtCheckerSettingsBuilder withInitialCsvContent(String initialCsvContent) {
            this.initialCsvContent = initialCsvContent;
            return this;
        }

        public YtCheckerSettingsBuilder withExpectedCsvContent(String expectedCsvContent) {
            this.expectedCsvContent = expectedCsvContent;
            return this;
        }

        public YtCheckerSettings build() {
            return new YtCheckerSettings(
                    ImmutableMap.copyOf(schemas),
                    initialCsvContent,
                    expectedCsvContent);
        }
    }
}
