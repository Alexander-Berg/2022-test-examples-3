package ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters;

import org.dozer.DozerConverter;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 03.04.15
 */
public class YesNoToOneZeroConverter extends DozerConverter<String, String> {

    public YesNoToOneZeroConverter() {
        super(String.class, String.class);
    }

    @Override
    public String convertTo(String source, String destination) {
        if (source.equals("1")) {
            return "Yes";
        }
        if (source.equals("0")) {
            return "No";
        }
        return source;
    }

    @Override
    public String convertFrom(String source, String destination) {

        if (source.equals("Yes")) {
            return "1";
        }
        if (source.equals("No")) {
            return "0";
        }
        return source;
    }
}
