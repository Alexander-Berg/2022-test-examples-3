package ru.yandex.canvas.model.validation;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertTrue;
import static ru.yandex.canvas.model.validation.Checkers.checkDomain;

@RunWith(Parameterized.class)
public class CheckersCheckDomainNegativeTest {

    @Parameterized.Parameter()
    public String domain;

    @Parameterized.Parameters(name = "{0} is invalid}")
    public static Collection<String> domainChecks() {
        return Arrays.asList(
                "https://ya.ru",
                "http://ya.ru",
                "ya.ru/",
                ".рф",
                "asf .рф",
                "",
                "  ",
                "asdf_dd.com",
                "asdf_dd",
                "asdf.d"
        );
    }

    @Test
    public void checkDomainTest() {
        assertTrue(!checkDomain(domain));
    }
}
