package ru.yandex.market.pers.qa.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.Pair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author varvara
 * 11.12.2018
 */
public class UrlCheckerTest {

    private List<Pair<String, List<String>>> createTestDataForGetUrls() {
        List<Pair<String, List<String>>> list = new ArrayList<>();
        addNewTestCase(list, "some_text http://test.site.ru,some_text", "test.site.ru");
        addNewTestCase(list, "some_text ftp://test.site.ru some_text", "test.site.ru");
        addNewTestCase(list, "some_text http://test.site.ru/query some_text", "test.site.ru");
        addNewTestCase(list, "some_text test.site.ru/query some_text", "test.site.ru");
        addNewTestCase(list, "some_text test.site.ru some_text", "test.site.ru");
        addNewTestCase(list, "some_text ya.ru/query?redirect=some-site.com some_text", "ya.ru", "some-site.com");
        addNewTestCase(list, "some_text some_text http://market.yandex.ru some_text", "market.yandex.ru");
        addNewTestCase(list, "some_text market.yandex.ru.fishing.com some_text", "market.yandex.ru.fishing.com");
        addNewTestCase(list, "some_text nya.ru some_text", "nya.ru");
        addNewTestCase(list, "some_text node4.yandex.ru.service.net some_text", "node4.yandex.ru.service.net");
        addNewTestCase(list, "some_text yandex.market.by some_text", "yandex.market.by");
        addNewTestCase(list, "some_text ya.com", "ya.com");
        addNewTestCase(list, "some_text site.com.ru", "site.com.ru");
        addNewTestCase(list, "some_text yandex.da.net.ru.by", "yandex.da.net.ru.by");
        addNewTestCase(list, "some_text nyandex.ru some text", "nyandex.ru");
        addNewTestCase(list, "some_text NYA.RU some_text", "NYA.RU");
        addNewTestCase(list, "some_text NYA.ru some_text", "NYA.ru");
        addNewTestCase(list, "some_text nya.RU some_text", "nya.RU");
        addNewTestCase(list, "yandex.ru some text", "yandex.ru");
        addNewTestCase(list, "test.club", "test.club");
        addNewTestCase(list, "some_text сайт.рф some text", "сайт.рф");
        addNewTestCase(list, "some_text САЙТ.рф some text", "САЙТ.рф");
        addNewTestCase(list, "some_text сайт.РФ some text", "сайт.РФ");
        addNewTestCase(list, "some_text САЙТ.РФ some text", "САЙТ.РФ");
        addNewTestCase(list, "some_text сайт.рф и подстава для теста сайт.ру some text", "сайт.рф");
        addNewTestCase(list, "some_text xn--e1ajnp.xn--90ae some text", "xn--e1ajnp.xn--90ae");
        return list;
    }

    private List<Pair<List<String>, Boolean>> createTestDataYandexUrls() {
        List<Pair<List<String>, Boolean>> list = new ArrayList<>();
        addNewTestCase(list, false, "site.ru");
        addNewTestCase(list, false, "site.com");
        addNewTestCase(list, false, "fishing.com");
        addNewTestCase(list, false, "nya.ru");
        addNewTestCase(list, false, "market.yandex.ru.fishing.com");
        addNewTestCase(list, false, "fake-yandex.ru");
        addNewTestCase(list, false, "nyandex.ru");

        addNewTestCase(list, false, "fake-yandex.ru", "yandex.by");
        addNewTestCase(list, false, "market-yandex.by", "yandex.ru");
        addNewTestCase(list, true, "market.yandex.by", "yandex.ru");
        addNewTestCase(list, true, "yandex.ru");
        addNewTestCase(list, true, "YANDEX.RU");
        addNewTestCase(list, true, "YANDEX.ru");
        addNewTestCase(list, true, "yandex.RU");
        addNewTestCase(list, false, "yandex.ru", "nyandex.ru");

        addNewTestCase(list, false, "ya.ru", "site.ru");
        addNewTestCase(list, false, "yandex.ru", "site.com");
        addNewTestCase(list, false, "site.com", "site.com", "yandex.ru");
        return list;
    }

    private List<Pair<Boolean, String>> createTestDataContainsUrls() {
        List<Pair<Boolean, String>> list = new ArrayList<>();
        addNewTestCase(list, true, "some_text http://test.site.ru,some_text");
        addNewTestCase(list, true, "some_text ftp://test.site.ru some_text");
        addNewTestCase(list, true, "some_text http://test.site.ru/query some_text");
        addNewTestCase(list, true, "some_text test.site.ru/query some_text");
        addNewTestCase(list, true, "some_text test.site.ru some_text");
        addNewTestCase(list, true, "some_text ya.ru/query?redirect=some-site.com some_text");
        addNewTestCase(list, true, "some_text some_text http://market.yandex.ru some_text");
        addNewTestCase(list, true, "some_text market.yandex.ru.fishing.com some_text");
        addNewTestCase(list, true, "some_text nya.ru some_text");
        addNewTestCase(list, true, "some_text ya.com");
        addNewTestCase(list, true, "some_text site.com.ru");
        addNewTestCase(list, true, "some_text yandex.da.net.ru.by");
        addNewTestCase(list, true, "some_text nyandex.ru some text");
        addNewTestCase(list, true, "yandex.ru some text");
        addNewTestCase(list, true, "test.club");
        addNewTestCase(list, true, "some_text сайт.рф some text");
        addNewTestCase(list, true, "sometext _ttp://very.suspicious site");
        addNewTestCase(list, true, "sometext http://слышь.сюда.иди понял, да?");
        addNewTestCase(list, true, "sometext http://192.168.1.1, да?");
        addNewTestCase(list, true, "sometext udp://192.168.1.1, тоже странно");
        addNewTestCase(list, true, "://bad.verybadsite, сфоткай типа самый умный");
        addNewTestCase(list, false, "Ссылки, которые не являются корректными, например озон.ру мы пропускаем");
        addNewTestCase(list, false, "ОЗОН.РУ ЛУЧШИЙ МАГАЗИН, озон.РУ, победитель системы, ОЗОН.ру!");
        addNewTestCase(list, false, "chelovek.rubenich, мы все еще не считаем твой никнейм доменом");
        addNewTestCase(list, false, "человек.русалка тоже спасен от наших регулярок!");
        return list;
    }

    private List<Pair<List<String>, Boolean>> createTestDataMarketUrls() {
        List<Pair<List<String>, Boolean>> list = new ArrayList<>();
        addNewTestCase(list, false, "site.ru", "market.yandex.ru");
        addNewTestCase(list, false, "site.com");
        addNewTestCase(list, false, "fishing.com");
        addNewTestCase(list, false, "nya.ru");
        addNewTestCase(list, false, "market.yandex.ru.fishing.com");
        addNewTestCase(list, false, "fake-yandex.ru");
        addNewTestCase(list, false, "nyandex.ru");

        addNewTestCase(list, false, "fake-yandex.ru", "yandex.by");
        addNewTestCase(list, false, "market-yandex.by", "yandex.ru");
        addNewTestCase(list, true, "market.yandex.by", "yandex.ru");
        addNewTestCase(list, true, "market.yandex.ru");
        addNewTestCase(list, true, "MARKET.YANDEX.RU");
        addNewTestCase(list, true, "MARKET.YANDEX.ru");
        addNewTestCase(list, true, "market.yandex.RU");
        addNewTestCase(list, false, "pogoda.yandex.ru", "nyandex.ru");

        addNewTestCase(list, false, "ya.ru", "site.ru");
        addNewTestCase(list, false, "yandex.ru", "site.com");
        addNewTestCase(list, false, "site.com", "site.com", "yandex.ru");
        return list;
    }

    private static void addNewTestCase(List<Pair<List<String>, Boolean>> list, Boolean result, String... urls) {
        list.add(Pair.of(Arrays.asList(urls), result));
    }

    private static void addNewTestCase(List<Pair<String, List<String>>> list, String text, String ... urls) {
        list.add(Pair.of(text, Arrays.asList(urls)));
    }

    private static void addNewTestCase(List<Pair<Boolean, String>> list, Boolean result, String text) {
        list.add(Pair.of(result, text));
    }

    @Test
    void testGetUrls() {
        List<Pair<String, List<String>>> cases = createTestDataForGetUrls();
        for (Pair<String, List<String>> testCase : cases) {
            final List<String> urlsFromChecker = UrlChecker.getUrls(testCase.first);
            assertTrue(CollectionUtils.isEqualCollection(testCase.second, urlsFromChecker),
                String.format("test case: \'%s\', expected: \'%s\', found: \'%s\'", testCase.first, testCase.second,
                              StringUtils.join(urlsFromChecker, ", ")));
        }
    }

    @Test
    void testContainsOnlyYandexUrls() {
        List<Pair<List<String>, Boolean>> testCases = createTestDataYandexUrls();
        for (Pair<List<String>, Boolean> testCase : testCases) {
            assertEquals(testCase.second, UrlChecker.containsOnlyYandexUrls(testCase.first),
                testCase.first + " must contains only yandex urls");
        }
    }

    @Test
    void testContainsUrls() {
        List<Pair<Boolean, String>> testCases = createTestDataContainsUrls();
        for (Pair<Boolean, String> testCase : testCases) {
            assertEquals(testCase.first, UrlChecker.containsUrl(testCase.second),
                String.format("Test \'%s\' containing urls", testCase.second));
        }
    }

    @Test
    void testContainsMarketUrls() {
        List<Pair<List<String>, Boolean>> testCases = createTestDataMarketUrls();
        for (Pair<List<String>, Boolean> testCase : testCases) {
            assertEquals(testCase.second, UrlChecker.containsOnlyYandexUrls(testCase.first),
                testCase.first + " must contains only yandex urls");
        }
    }
}
