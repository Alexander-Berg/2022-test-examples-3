package ru.yandex.autotests.market.stat.handlers;

import org.beanio.types.TypeConversionException;
import org.beanio.types.TypeHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by kateleb on 29.06.16.
 */
public class QuotedHandler implements TypeHandler {

    @Override
    public String parse(String text) throws TypeConversionException {
        if (text != null) {
            if (text.contains(",")) {
                ArrayList errors = new ArrayList(Arrays.asList(text.split(",")));
                return (String) errors.stream().map(i -> undoQuotes(i.toString())).collect(Collectors.joining(","));
            } else {
                return undoQuotes(text);
            }
        }
        return "";
    }

    @Override
    public String format(Object text) {
        if (text != null && text instanceof String) {
            if (((String) text).contains(",")) {
                ArrayList errors = new ArrayList(Arrays.asList(((String) text).split(",")));
                return (String) errors.stream().map(i -> doQuotes(i.toString())).collect(Collectors.joining(","));
            } else {
                return doQuotes(text);
            }
        }
        return "";
    }

    protected String doQuotes(Object text) {
        return "\"" + text + "\"";
    }

    protected String undoQuotes(String text) {
        return text.trim().replace("\"", "");
    }

    @Override
    public Class<?> getType() {
        return String.class;
    }
}
