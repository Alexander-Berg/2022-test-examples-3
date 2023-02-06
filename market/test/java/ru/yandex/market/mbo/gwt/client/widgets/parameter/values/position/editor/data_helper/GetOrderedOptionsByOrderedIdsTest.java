package ru.yandex.market.mbo.gwt.client.widgets.parameter.values.position.editor.data_helper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ru.yandex.market.mbo.gwt.client.widgets.parameter.values.position.editor.DataHelper;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
@SuppressWarnings("checkstyle:magicNumber")
public class GetOrderedOptionsByOrderedIdsTest {

    List<Long> orderedIds;
    List<Option> possibleValues;
    List<Long> expectedIds;

    public GetOrderedOptionsByOrderedIdsTest(List<Long> orderedIds, List<Long> possibleValuesIds,
                                             List<Long> expectedIds) {
        possibleValues = new ArrayList<>();
        possibleValuesIds.forEach(id -> {
            Option option = new OptionImpl();
            option.setId(id);
            possibleValues.add(option);
        });

        this.orderedIds = orderedIds;
        this.expectedIds = expectedIds;
    }

    @Test
    public void test() {
        List<Option> valuesForTest = DataHelper.getOrderedOptionsByOrderedIds(orderedIds, possibleValues);
        valuesForTest.forEach(value -> {
            Long expected = expectedIds.get(valuesForTest.indexOf(value));
            assertEquals(expected.longValue(), value.getId());
        });
    }

    @Parameters
    public static Collection<Object[]> generatedDataPoints() {
        Object[][] data = new Object[][] {
            {Arrays.asList(), Arrays.asList(), Arrays.asList()},
            {Arrays.asList(1L), Arrays.asList(1L), Arrays.asList(1L)},
            {Arrays.asList(1L, 2L), Arrays.asList(1L, 2L), Arrays.asList(1L, 2L)},
            {Arrays.asList(2L, 1L), Arrays.asList(1L, 2L), Arrays.asList(2L, 1L)},
            {Arrays.asList(1L, 5L), Arrays.asList(1L, 2L, 3L, 4L, 5L), Arrays.asList(1L, 5L)},
            {Arrays.asList(5L, 1L), Arrays.asList(1L, 2L, 3L, 4L, 5L), Arrays.asList(5L, 1L)},
            {Arrays.asList(1L, 3L, 5L), Arrays.asList(1L, 2L, 3L, 4L, 5L), Arrays.asList(1L, 3L, 5L)},
            {Arrays.asList(3L, 1L, 5L), Arrays.asList(1L, 2L, 3L, 4L, 5L), Arrays.asList(3L, 1L, 5L)},
            {Arrays.asList(3L, 5L, 1L), Arrays.asList(1L, 2L, 3L, 4L, 5L), Arrays.asList(3L, 5L, 1L)},
            {Arrays.asList(3L), Arrays.asList(1L, 2L, 3L, 4L, 5L), Arrays.asList(3L)},
            {Arrays.asList(), Arrays.asList(1L, 2L, 3L, 4L, 5L), Arrays.asList()},
        };
        return Arrays.asList(data);
    }
}
