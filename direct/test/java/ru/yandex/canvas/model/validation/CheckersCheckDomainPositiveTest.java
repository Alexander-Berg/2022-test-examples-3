package ru.yandex.canvas.model.validation;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertTrue;
import static ru.yandex.canvas.model.validation.Checkers.checkDomain;

@RunWith(Parameterized.class)
public class CheckersCheckDomainPositiveTest {
    @Parameterized.Parameter()
    public String domain;

    @Parameterized.Parameters(name = "{0} is valid}")
    public static Collection<String> domainChecks() {
        return Arrays.asList(
                "кто.рф",
                "ya.ru",
                "www.ya.com",
                "asdf.ru",
                "f.ru"
        );
    }

    @Test
    public void checkDomainTest() {
        assertTrue(checkDomain(domain));
    }
}

