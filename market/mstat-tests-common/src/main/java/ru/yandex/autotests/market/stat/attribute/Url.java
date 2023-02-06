package ru.yandex.autotests.market.stat.attribute;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

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

    private static String generateRandomDomain(String prefix) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < RandomUtils.nextInt(2) + 1; index++) {
            String word = (prefix != null && prefix.trim().length() > 0 && index == 0) ? prefix : generateWord();
            result.append(word);
            result.append(Constants.DOT);
        }
        result.append(DOMAIN_NAMES.get(RandomUtils.nextInt(DOMAIN_NAMES.size())));
        return result.toString().toLowerCase();
    }

    public static String generateRandomDomain() {
        return generateRandomDomain(null);
    }

    private static String generateRandomPath() {
        StringBuilder result = new StringBuilder();
        result.append(generateRandomPathWithoutDocument());
        if (RandomUtils.nextBoolean()) {
            result.append(generateWord()).append(RandomUtils.nextBoolean() ? ".html" : ".htm");
        }
        return result.toString().toLowerCase();
    }

    private static String generateRandomPathWithoutDocument() {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < RandomUtils.nextInt(4); index++) {
            result.append(generateWord());
            result.append(Constants.SLASH);
        }
        return result.toString().toLowerCase();
    }

    private static String generateWord() {
        return RandomStringUtils.random(RandomUtils.nextInt(7) + 2, Constants.ENGLISH_ALPHABET);
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
