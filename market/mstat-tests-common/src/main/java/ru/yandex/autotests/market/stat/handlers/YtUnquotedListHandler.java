package ru.yandex.autotests.market.stat.handlers;

import org.beanio.types.TypeConversionException;
import org.beanio.types.TypeHandler;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by kateleb on 07.06.17.
 */
public class YtUnquotedListHandler implements TypeHandler {
    @Override
    public String parse(String text) throws TypeConversionException {
        if (text != null ) {
            String targetText = text.trim();
            if (targetText.startsWith("[") && targetText.endsWith("]")) {
                targetText = targetText.substring(0, targetText.length() - 1).replaceFirst("\\[", "");
                return Arrays.stream(targetText.split(",")).map(this::undoQuotes).collect(Collectors.joining(",", "[", "]"));
            }
        }
        return "";
    }

    private String undoQuotes(String item) {
        item = item.trim();
        if (!item.contains("\"") && item.length()>0) {
            item = item.replace("\"", "");
        }
        return item;
    }

    @Override
    public String format(Object value) {
        return value == null ? null : value.toString();
    }

    @Override
    public Class<?> getType() {
        return String.class;
    }
}
