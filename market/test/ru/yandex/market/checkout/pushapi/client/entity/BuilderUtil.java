package ru.yandex.market.checkout.pushapi.client.entity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author msavelyev
 */
public class BuilderUtil {

    public static <T, B extends Builder<T>> List<T> buildList(List<B> builders) {
        final ArrayList<T> list = new ArrayList<>(builders.size());

        for (B builder : builders) {
            list.add(builder.build());
        }

        return list;
    }

    public static <T, B extends Builder<T>> List<T> buildList(B[] builders) {
        return buildList(Arrays.asList(builders));
    }

    public static <K, T, B extends Builder<T>> Map<K, T> buildMap(Map<K, B> builders) {
        final HashMap<K, T> result = new HashMap<>();

        for (K key : builders.keySet()) {
            result.put(key, builders.get(key).build());
        }

        return result;
    }

    public static Date createDate(String dateStr) {
        final SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return fullDateFormat.parse(dateStr);
        } catch (ParseException e) {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                return simpleDateFormat.parse(dateStr);
            } catch (ParseException e1) {
                throw new RuntimeException("can't parse date: " + dateStr, e);
            }
        }
    }

    public static void waitForCondition(Condition condition) throws Exception {
        while (true) {
            Thread.sleep(5l);
            if (condition.satisfies()) {
                break;
            }
        }
    }


}
