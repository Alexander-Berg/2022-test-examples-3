package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import com.google.common.collect.ImmutableMap;
import ru.yandex.market.antifraud.filter.ClickGenerator;
import ru.yandex.market.antifraud.filter.FilterGenerator;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * Created by kateleb on 07.09.16.
 */
public class ForCpa3Filter implements FilterGenerator {
    public Map<String, List<TestClick>> generateForFilter3(DateTime timeOfClicks) {
        List<TestClick> clicks = ClickGenerator.generateUniqueClicks(timeOfClicks, 100);
        clicks.forEach(c -> c.setFilter(FilterConstants.FILTER_3));
        List<TestClick> cpaclicks = new ArrayList<>();
        Set<String> showuids = clicks.stream().filter(c -> c.getFilter().equals(FilterConstants.FILTER_3)).map(c -> c.get("show_uid", String.class)).collect(toSet());
        cpaclicks.addAll(generateCpasForShowUid(timeOfClicks, showuids, "forClicksForFilter3"));
        return ImmutableMap.of("clicks", clicks, "cpa_clicks", cpaclicks);
    }

    private List<TestClick> generateCpasForShowUid(DateTime timeOfClicks, Set<String> showuids, String comment) {
        return showuids.stream().map(showuid -> {
            TestClick cp = ClickGenerator.generateUniqueCpaClicks(timeOfClicks, 1, true).get(0);
            cp.set("show_uid", showuid);
            cp.set("utm_term", comment);
            cp.setFilter(FilterConstants.FILTER_3);
            return cp;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TestClick> generate() {
        return null;
    }
}
