package ru.yandex.market.deliverycalculator.searchengine.util;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.deliverycalculator.searchengine.JsonProtoHelper;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

public class ProtoResultMatcher implements ResultMatcher {

    private final Class<?> clazz;
    private final String expectedFilePath;

    private ProtoResultMatcher(Class<?> clazz, String expectedFilePath) {
        this.clazz = clazz;
        this.expectedFilePath = expectedFilePath;
    }

    @Override
    public void match(MvcResult result) {
        String stringResponse = JsonProtoHelper.printShopOffersResp(result.getResponse().getContentAsByteArray());
        String expected = StringTestUtil.getString(clazz, expectedFilePath);

        assertJsonEquals(expected, stringResponse);
    }

    public static ProtoResultMatcher assertProtoAsJson(Class<?> clazz, String expectedFilePath) {
        return new ProtoResultMatcher(clazz, expectedFilePath);
    }
}
