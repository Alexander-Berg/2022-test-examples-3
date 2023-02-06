package ru.yandex.market.partner.hidden_offer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.mustache.MustacheMissingKeyCallback;
import ru.yandex.market.core.mustache.MustacheService;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author Vadim Lyalin
 */
class MustacheServiceTest extends FunctionalTest {
    private static final String JSON_FILE_NAME = "data.json";

    private static final String EXPECTED_TEXT = "" +
            "Короткое время 14.07.2017, 05:40\n" +
            "Длинное время 14 июля в 05:40\n" +
            "Срок доставки на Маркете 2 дня\n" +
            "API передает 1 день\n" +
            "Срок доставки на Маркете - 2-5 дней\n" +
            // тест на незаполненные или частично заполненные поля в json
            "Пустое время \n" +
            "API не передает \n" +
            "Срок доставки по API - 1 день\n" +
            "поставщик";

    private static final String TEMPLATE = "" +
            "Короткое время {{#_.shortDate}}{{timeout}}{{/_.shortDate}}\n" +
            "Длинное время {{#_.longDate}}{{timeout}}{{/_.longDate}}\n" +
            "Срок доставки на Маркете {{#_.plural}}{{details.marketParam.dayFrom}}|день|дня|дней{{/_.plural}}\n" +
            "API передает {{#_.plural}}{{details.shopParam.dayFrom}}|день|дня|дней{{/_.plural}}\n" +
            "Срок доставки на Маркете - " +
            "{{#_.range}}{{details.marketParam.dayFrom}}|{{details.marketParam.dayTo}}|день|дня|дней{{/_.range}}\n" +
            // тест на незаполненные или частично заполненные поля в json
            "Пустое время {{#_.longDate}}{{fake_time}}{{/_.longDate}}\n" +
            "API не передает {{#_.plural}}{{details.shopParam.fakeDay}}|день|дня|дней{{/_.plural}}\n" +
            "Срок доставки по API - " +
            "{{#_.range}}{{details.shopParam.dayFrom}}|{{details.shopParam.dayTo}}|день|дня|дней{{/_.range}}\n" +
            "{{#_.if}} {{tagName}} = vendor|поставщик{{/_.if}}{{#_.if}} {{tagName}} = customer|покупатель{{/_.if}}";

    private static final String WHEN_TEMPLATE = "" +
            "{{#_.when}} tagName=vendor |Нет элемента <vendor>{{/_.when}} " +
            "{{#_.when}} tagName=shop-sku |Нет элемента <{{tagName}}>{{/_.when}} " +
            "{{#_.when}} tagName=market-sku |Нет элемента <market-sku>{{/_.when}}";

    private static final String IGNORE_TEMPLATE = "{{#_.md}}Текст внутри markdown.{{/_.md}} ";

    private static final String EASY_TEMPLATE = "" +
            "Нет элемента <{{tagName}}>";

    private static final String BUILD_URL_TEMPLATE = "" +
            "{{#_.buildUrl}}external:partner-help|{\"path\": \"/troubleshooting/offers.html#wrong-profile\"," +
            " \"tld\":\"ru\"}{{/_.buildUrl}}";
    private static final String BUILD_URL_TEMPLATE_WITH_LINK_PARAMS = "" +
            "{{#_.buildUrl}}market-partner:html:manager-agency-list:get" +
            "|{\"platformType\": {{platformType}}}{{/_.buildUrl}}";

    private static final String BUILD_URL_TEMPLATE_WITH_OPTIONAL_PARAMS = "" +
            "{{#_.buildUrl}}market-partner:html:documents:get|{}{{/_.buildUrl}}";

    @Autowired
    private MustacheService mustacheService;

    @Test
    @DbUnitDataSet(before = "MustacheServiceTest.before.csv")
    void testFormat() throws IOException {
        InputStream is = this.getClass().getResourceAsStream(JSON_FILE_NAME);
        String json = IOUtils.toString(is, StandardCharsets.UTF_8.displayName());

        String text = mustacheService.format(TEMPLATE, new ObjectMapper().readValue(json, Map.class));

        Assertions.assertEquals(EXPECTED_TEXT, text);
    }

    @Test
    void testWhenFunction() throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream("when.json")) {
            Map map = new ObjectMapper().readValue(is, Map.class);
            String text = mustacheService.format(WHEN_TEMPLATE, map);
            MatcherAssert.assertThat(
                    "Значение совпадает с ожидаемым",
                    text,
                    Matchers.is("Нет элемента <shop-sku>")
            );
        }
    }

    @Test
    void testEasyTemplate() throws IOException {
        MustacheMissingKeyCallback callback = Mockito.mock(MustacheMissingKeyCallback.class);
        try (InputStream is = this.getClass().getResourceAsStream("when.json")) {
            Map map = new ObjectMapper().readValue(is, Map.class);
            String text = mustacheService.format(EASY_TEMPLATE, map, Map.of(), callback);
            MatcherAssert.assertThat(
                    "Значение совпадает с ожидаемым",
                    text,
                    Matchers.is("Нет элемента <shop-sku>")
            );
            verifyNoInteractions(callback);
        }
    }

    @Test
    void testMissingKeyCallback() {
        MustacheMissingKeyCallback callback = Mockito.mock(MustacheMissingKeyCallback.class);
        mustacheService.format(EASY_TEMPLATE, Map.of(), Map.of(), callback);
        Mockito.verify(callback, times(1)).invoke("tagName");
    }


    @Test
    void testIgnoreFunction() throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream("when.json")) {
            Map map = new ObjectMapper().readValue(is, Map.class);
            String text = mustacheService.format(IGNORE_TEMPLATE, map);
            MatcherAssert.assertThat(
                    "Значение совпадает с ожидаемым",
                    text,
                    Matchers.is("Текст внутри markdown.")
            );
        }
    }

    @Test
    @DbUnitDataSet(before = "BuildUrlTest.before.csv")
    void testBuildUrlFunction() {
        String text = mustacheService.format(BUILD_URL_TEMPLATE, Collections.emptyMap());
        MatcherAssert.assertThat(
                "Значение совпадает с ожидаемым",
                text,
                Matchers.is("https://yandex.ru/support/partnermarket/troubleshooting/offers.html#wrong-profile")
        );
    }

    @Test
    @DbUnitDataSet(before = "BuildUrlTest.before.csv")
    void testBuildUrlFunctionWithLinkParams() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream(JSON_FILE_NAME)) {
            Map map = new ObjectMapper().readValue(is, Map.class);
            String text = mustacheService.format(BUILD_URL_TEMPLATE_WITH_LINK_PARAMS, map);
            MatcherAssert.assertThat(
                    "Значение совпадает с ожидаемым",
                    text,
                    Matchers.is("/platform/agency/list/index.xml")
            );
        }
    }

    @Test
    @DbUnitDataSet(before = "BuildUrlTest.before.csv")
    void testBuildUrlFunctionWithOptionalParams() {
        String text = mustacheService.format(BUILD_URL_TEMPLATE_WITH_OPTIONAL_PARAMS, Collections.emptyMap());
        MatcherAssert.assertThat(
                "Значение совпадает с ожидаемым",
                text,
                Matchers.is("/documents/index.xml")
        );
    }
}
