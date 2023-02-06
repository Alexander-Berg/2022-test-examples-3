package ru.yandex.market.logshatter.parser.strm.schemes;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;

import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.Charsets;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StrmSchemesMatcherFactoryTest {
    private static final String TEST_CASES_PATH = "strm/tests/schemes_test.json";
    private StrmSchemesMatcher matcher;
    private List<StrmSchemesMatcherTestCase> testCases;

    @BeforeEach
    void setUp() {
        try {
            matcher = StrmSchemesMatcherFactory.getMatcher();
        } catch (JSONException e) {
            throw new RuntimeException("Could not get matcher: " + e.toString());
        }

        URL testCasesJsonUrl = Resources.getResource(TEST_CASES_PATH);
        String testCasesJson;

        try {
            testCasesJson = Resources.toString(testCasesJsonUrl, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not load test cases json", e);
        }

        Type collectionType = new TypeToken<List<StrmSchemesMatcherTestCase>>() {
        }.getType();

        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        testCases = gson.fromJson(testCasesJson, collectionType);
    }

    @Test
    void parse() {
        for (StrmSchemesMatcherTestCase testCase : testCases) {
            for (String url : testCase.getUrls()) {
                assertEquals(testCase.getContentId(), matcher.getContentId(url));
            }
        }
    }

    static class StrmSchemesMatcherTestCase {
        private final List<String> urls;
        private final String contentId;

        StrmSchemesMatcherTestCase(List<String> urls, String contentId) {
            this.urls = urls;
            this.contentId = contentId;
        }

        public List<String> getUrls() {
            return urls;
        }

        public String getContentId() {
            return contentId;
        }
    }
}
