package ru.yandex.direct.core.testing.data;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import ru.yandex.direct.core.entity.domain.model.Domain;

public class TestDomain {

    private TestDomain() {
    }

    public static Domain testDomain(String prefix) {
        return testDomain(prefix, "");
    }

    public static Domain testDomain(String prefix, String postfix) {
        return testDomain(prefix, postfix, false);
    }

    public static Domain testDomain(String prefix, String postfix, boolean lower) {
        String domain = prefix + RandomStringUtils.randomAlphanumeric(6) + postfix;
        if (lower) {
            domain = domain.toLowerCase();
        }
        return new Domain()
                .withDomain(domain)
                .withReverseDomain(StringUtils.reverse(domain));
    }

    public static Domain testDomain() {
        return testDomain("test-");
    }

    public static Domain testRussianDomain() {
        return testDomain("тест-");
    }

    public static String randomDomain() {
        return RandomStringUtils.randomAlphanumeric(10).toLowerCase() + ".com";
    }
}
