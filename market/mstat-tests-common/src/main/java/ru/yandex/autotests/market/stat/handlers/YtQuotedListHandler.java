package ru.yandex.autotests.market.stat.handlers;

import org.beanio.types.TypeConversionException;
import org.beanio.types.TypeHandler;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by kateleb on 07.06.17.
 */
public class YtQuotedListHandler implements TypeHandler {


    public String parse(String text) throws TypeConversionException {
        if (text != null) {
            String targetText = text.trim();
            String regex = text.contains("\"") ? "\"[\\s,\n]{0,10},[\\s,\n]{0,10}\"" : ",";
            if (targetText.startsWith("[") && targetText.endsWith("]")) {
                targetText = targetText.substring(1, targetText.length() - 1);
                return Arrays.stream(targetText.split(regex)).map(this::doQuotes).collect(Collectors.joining(",", "[", "]"));
            }
        }
        return "";
    }

    private String doQuotes(String item) {
        item = item.trim();
        if (item.length() > 0) {
            item = item.startsWith("\"") ? item : "\"" + item;
            item = item.endsWith("\"") ? item : item + "\"";
        }
        return item;
    }

    @Override
    public String format(Object value) {
        if (value == null || !String.class.isInstance(value)) {
            return "";
        }
        String strValue = (String) value;
        if (strValue.contains(",") && strValue.startsWith("[") && strValue.endsWith("]")) {
            strValue = strValue.substring(1, strValue.length() - 1);
        }
        return Arrays.stream(strValue.split(",")).map(this::doQuotes).collect(Collectors.joining(","));
    }

    @Override
    public Class<?> getType() {
        return String.class;
    }
}
