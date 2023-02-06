package ru.yandex.autotests.direct.cmd.steps.performancefilters;

import org.apache.commons.beanutils.BeanUtils;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.group.Condition;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedBusinessType;
import ru.yandex.autotests.direct.cmd.data.feeds.FilterConditions;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilterMap;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.cmd.util.FileUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by aleran on 16.11.2015.
 */
public class PerformanceFiltersHelper {

    private static final String FILTER_PATH = "filters/";
    private static final String FILTER_FILE_EXT = ".txt";
    private static final String RELATION_POTFIX = "relation";
    private static final PerformanceFilter DEFAULT_FILTER = BeanLoadHelper
            .loadCmdBean(CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_DEFAULT, PerformanceFilter.class)
            .withFromTab("condition");
    private static final String RANGE_RELATION = "<->";

    public static List<PerformanceFilter> convertPerformanceFilterMapToPerformanceFilter(PerformanceFilterMap filterMap) {
        List<PerformanceFilter> performanceFilters = new ArrayList<>();
        for (String key : filterMap.getPerformanceFilterMap().keySet()) {
            PerformanceFilter filter = filterMap.getPerformanceFilterMap().get(key);
            performanceFilters.add(filter);
        }
        return performanceFilters;
    }

    public static List<PerformanceFilter> sortConditions(List<PerformanceFilter> performanceFilter) {
        performanceFilter.forEach(t ->
                t.getConditions().sort(Comparator.<Condition>naturalOrder()));
        return performanceFilter;
    }


    public static List<Object[]> getFilters(FeedBusinessType feedType) {
        return getFiltersByFeedType(feedType).stream()
                .map(t -> new PerformanceFilter[]{t})
                .collect(Collectors.toList());
    }

    public static List<PerformanceFilter> getFiltersByFeedType(FeedBusinessType feedType) {
        List<PerformanceFilter> performanceFilters = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(getFilterConditionsPath(feedType)))) {
            String headerLine = scanner.hasNext() ? scanner.nextLine() : null;
            HashMap<String, Integer> headers = getHeaders(headerLine, feedType);
            while (scanner.hasNext()) {
                PerformanceFilter filter = (PerformanceFilter) BeanUtils.cloneBean(DEFAULT_FILTER);
                String[] filterConditions = scanner.nextLine().split("\t");
                filter.setConditions(getPerformanceFilterConditions(headers, filterConditions));
                performanceFilters.add(filter);
            }

        } catch (Exception e) {
            throw new DirectCmdStepsException("Ошибка при генерации значений фильтра", e);
        }
        return performanceFilters;
    }

    private static String getFilterConditionsPath(FeedBusinessType feedType) {
        return FileUtils.getFilePath(FILTER_PATH + feedType.getValue() + FILTER_FILE_EXT);
    }

    private static HashMap<String, Integer> getHeaders(String line, FeedBusinessType feedType) {
        assumeThat("строка не пуста", line, notNullValue());
        String[] headers = line.split("\t");
        return getPositions(headers, FilterConditions.getFilterConditionsByFeedBysinessType(feedType));
    }

    private static HashMap<String, Integer> getPositions(String[] headers, FilterConditions filterConditions) {
        HashMap<String, Integer> condPositions = new HashMap<>();
        for (String condition : filterConditions.getConditions()) {
            condPositions.put(condition, indexOfIgnoreCase(headers, condition));
            condPositions.put(condition + RELATION_POTFIX, indexOfIgnoreCase(headers, condition + RELATION_POTFIX));
        }
        return condPositions;
    }

    private static List<Condition> getPerformanceFilterConditions(HashMap<String, Integer> positions, String[] values) {
        List<Condition> performanceFilterConditions = new ArrayList<>();
        for (String conditionName : positions.keySet().stream()
                .filter(t -> !t.toLowerCase().contains(RELATION_POTFIX))
                .collect(Collectors.toList())) {
            Integer conditionRelationPos = positions.get(conditionName + RELATION_POTFIX);
            Integer conditionValuePos = positions.get(conditionName);
            if (conditionRelationPos == -1 && conditionValuePos == -1) continue;
            Condition filterCondition = new Condition().withField(conditionName);

            if (conditionRelationPos != -1) {
                filterCondition.setRelation(values[conditionRelationPos]);
            }
            if (conditionValuePos != -1) {
                setConditionValue(filterCondition, values[conditionValuePos]);
            }
            performanceFilterConditions.add(filterCondition);
        }
        return performanceFilterConditions;
    }

    private static void setConditionValue(Condition filterCondition, String value) {
        if (filterCondition.getRelation().equals(RANGE_RELATION)) {
            filterCondition.withValue(Arrays.asList(value.split(":")));

        } else {
            //TODO value may need Integer
            //if (StringUtils.isNumeric(value)) {
            //    filterCondition.setValue(Collections.singletonList(Integer.valueOf(value)));
            //} else {
                filterCondition.withValue(Collections.singletonList(value));
            //}

        }
    }

    private static int indexOfIgnoreCase(String[] array, String stringToFind) {
        for (int i = 0; i < array.length; i++) {
            if (stringToFind.equalsIgnoreCase(array[i])) {
                return i;
            }
        }
        return -1;
    }
}
