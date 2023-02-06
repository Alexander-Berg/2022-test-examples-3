package ru.yandex.autotests.direct.httpclient.util.mappers.campaignInfoApiToCmd.converters;

import org.dozer.DozerConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 06.04.15
 */
public class MinusKeywordsConverter extends DozerConverter<String[], List> {

    public MinusKeywordsConverter() {
        super(String[].class, List.class);
    }

    @Override
    public List convertTo(String[] source, List destination) {
        return new ArrayList<>(Arrays.asList(source));
    }

    @Override
    public String[] convertFrom(List source, String[] destination) {
        List<String> minusWords = (List<String>) source;
        return minusWords.stream().toArray(String[]::new);
    }
}
