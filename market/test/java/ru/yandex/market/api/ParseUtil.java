package ru.yandex.market.api;

import ru.yandex.market.common.Parser;
import ru.yandex.market.api.util.ResourceHelpers;

public final class ParseUtil {

    public static <T> T parse(Parser<T> parser, String resource) {
        return parser.parse(ResourceHelpers.getResource(resource));
    }
}
