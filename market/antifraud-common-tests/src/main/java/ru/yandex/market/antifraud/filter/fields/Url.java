package ru.yandex.market.antifraud.filter.fields;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import ru.yandex.market.antifraud.filter.RndUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by entarrion on 22.01.15.
 */
public class Url {
    private static String SCHEME_HTTP = "http";
    private static String DOMAIN_LOCALHOST = "localhost";
    public static String TEST_URL_MARKER = "marketstat-test";
    private static ImmutableList<String> DOMAIN_NAMES =
            ImmutableList.of("ru", "com", "org", "net", "edu", "gov");

    public static String generateRandomUrl() {
        return buildUrl(generateRandomDomain(), generateRandomPath());
    }

    public static String generateUrlWithDomain(String domain) {
        return buildUrl(domain, generateRandomPath());
    }

    public static List<String> generateTestUrls(int count) {
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            urls.add(generateRandomTestUrl());
        }
        return urls;
    }

    private static String generateRandomTestUrl() {
        return buildUrl(generateRandomTestDomain(), generateRandomPath());
    }

    public static String generateAccessorInterfaceUrl() {
        return generateUrlWithRandomPath("market-preview.yandex.ru");
    }

    public static String generateUrlWithRandomPath(String domain) {
        return buildUrl(domain, generateRandomPath());
    }


    public static String generateUrlWithParam(String key, String value) {
        String path = String.format("%ssearch.xml?%s=%s", generateRandomPathWithoutDocument(), key, value);
        return buildUrl(generateRandomDomain(), path);
    }

    public static String generateUrlWithParams(Map<String, String> params) {
        List<String> pairs = params.keySet().stream().map(el -> el + "=" + params.get(el)).collect(Collectors.toList());
        String path = generateRandomPathWithoutDocument() + "search.xml?" + pairs.stream().collect(Collectors.joining("&"));
        return buildUrl(generateRandomDomain(), path);
    }

    public static String generateUrlWithParamWithRandValue(String key) {
        return generateUrlWithParam(key, generateWord());
    }

    public static String generateUrlWithWordInPath(String word) {
        return buildUrl(generateRandomDomain(), word + "/" + generateRandomPath());
    }

    private static String generateRandomDomain(String prefix) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < RandomUtils.nextInt(0, 2) + 1; index++) {
            String word = (prefix != null && prefix.trim().length() > 0 && index == 0) ? prefix : generateWord();
            result.append(word);
            result.append(".");
        }
        result.append(DOMAIN_NAMES.get(RandomUtils.nextInt(0, DOMAIN_NAMES.size())));
        return result.toString().toLowerCase();
    }

    public static String generateRandomDomain() {
        return generateRandomDomain(null);
    }

    private static String generateRandomTestDomain() {
        return generateRandomDomain(TEST_URL_MARKER);
    }

    private static String generateRandomPath() {
        StringBuilder result = new StringBuilder();
        result.append(generateRandomPathWithoutDocument());
        if (RandomUtils.nextBoolean()) {
            result.append(generateWord() + (RandomUtils.nextBoolean() ? ".html" : ".htm"));
        }
        return result.toString().toLowerCase();
    }

    private static String generateRandomPathWithoutDocument() {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < RandomUtils.nextInt(0, 4); index++) {
            result.append(generateWord());
            result.append("/");
        }
        return result.toString().toLowerCase();
    }

    private static String generateWord() {
        return RandomStringUtils.randomAlphabetic(RndUtil.nextInt(7) + 2);
    }

    private static String buildUrl(String scheme, String domain, String port, String path) {
        //scheme://domain:port/path
        StringBuilder result = new StringBuilder();
        result.append(StringUtils.isEmpty(scheme) ? SCHEME_HTTP : scheme);
        result.append("://");
        result.append(StringUtils.isEmpty(domain) ? DOMAIN_LOCALHOST : domain);
        if (StringUtils.isNotEmpty(port)) {
            result.append(":");
            result.append(port);
        }
        result.append("/");
        result.append(path);
        return result.toString().toLowerCase();
    }

    private static String buildUrl(String domain, String path) {
        return buildUrl(SCHEME_HTTP, domain, null, path);
    }
}
