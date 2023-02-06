package ru.yandex.autotests.market.stat.handlers;

import org.beanio.types.TypeConversionException;
import org.codehaus.jackson.map.ObjectMapper;
import ru.yandex.autotests.market.common.attacher.Attacher;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by kateleb on 30.06.16
 */
public class QuotedMapHandler extends YtQuotedListHandler {

    @Override
    public String parse(String text) throws TypeConversionException {
        if (text != null) {
            try {
                HashMap params = new ObjectMapper().readValue(text, HashMap.class);
                return convertMapToSortedValuesString(params);
            } catch (IOException e) {
                Attacher.attach("Error occurred while converting to map", e.getMessage());
                return super.parse(text);
            }
        }
        return "";
    }

    public static String convertMapToSortedValuesString(Map<String, String> errors) {
        return errors.entrySet().stream().sorted(sortByKeysInc()).map(Map.Entry::getValue).collect(Collectors.joining(","));
    }

    private static Comparator<? super Map.Entry<String, String>> sortByKeysInc() {
        return (Map.Entry<String, String> o1, Map.Entry<String, String> o2)->
                tryInteger(o1.getKey()).compareTo(tryInteger(o2.getKey()));
    }

    private static Comparable tryInteger(String s) {
        try {
            return Integer.valueOf(s);
        } catch (RuntimeException e) {
            Attacher.attach("Error while converting", e.getMessage());
            return s;
        }
    }
    @Override
    public Class<?> getType() {
        return String.class;
    }
}
