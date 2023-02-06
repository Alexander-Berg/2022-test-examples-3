package ru.yandex.market.stat.dicts.loaders;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class DomainsLoaderTest {

    @DataProvider
    public static Object[][] normalizeDataProvider() {
        return new Object[][]{
                new Object[]{"", ""},
                new Object[]{"yandex.ru", "yandex.ru"},
                new Object[]{"www.yandex.ru", "yandex.ru"},
                new Object[]{"http://yandex.ru", "yandex.ru"},
                new Object[]{"http://www.yandex.ru", "yandex.ru"},
                new Object[]{"www0.yandex.ru", "yandex.ru"},
                new Object[]{"www01.yandex.ru", "www01.yandex.ru"},
                new Object[]{"wwww.yandex.ru", "wwww.yandex.ru"},
                new Object[]{"www.yandex.ru/search", "yandex.ru"},
                new Object[]{"www.yandex.ru:8080/search", "yandex.ru"},
                new Object[]{"http://www.yandex.ru:8080/search", "yandex.ru"},
        };
    }

    @Test
    @UseDataProvider("normalizeDataProvider")
    public void normalize(String domain, String expected) throws Exception {
        // When
        String result = DomainsLoader.normalize(domain);

        // Then
        assertThat(result, equalTo(expected));
    }

    @DataProvider
    public static Object[][] extractDomainDataProvider() {
        return new Object[][]{
                new Object[]{"", ""},
                new Object[]{"yandex.ru", "yandex.ru"},
                new Object[]{"google.com", "google.com"},
                new Object[]{"abc.google.com", "abc.google.com"},
                new Object[]{"aaa.bbb.google.com", "bbb.google.com"},
                new Object[]{"aaa.bbb.ccc.google.com", "ccc.google.com"},
        };
    }

    @Test
    @UseDataProvider("extractDomainDataProvider")
    public void extractDomain(String domain, String expected) throws Exception {
        // When
        String result = DomainsLoader.extractDomain(domain, ImmutableMap.of("google.com", 3));

        // Then
        assertThat(result, equalTo(expected));
    }
}
