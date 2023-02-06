package ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd.converters;

import org.apache.commons.lang3.StringUtils;
import org.dozer.DozerConverter;

/**
 * Created by shmykov on 14.04.15.
 */
public class ApiToCmdMinusKeywordsConverter extends DozerConverter<String[], String> {

    public ApiToCmdMinusKeywordsConverter() {
        super(String[].class, String.class);
    }

    @Override
    public String convertTo(String[] source, String destination) {
        if (source == null || source.length == 0) {
            return null;
        }
        return "-" + StringUtils.join(source, " -");

    }

    @Override
    public String[] convertFrom(String source, String[] destination) {
        return source.split(" -");
    }
}