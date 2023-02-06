package ru.yandex.market.mbo.gwt.client.widgets.parameter.values.position.editor.data_helper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ru.yandex.market.mbo.gwt.client.widgets.parameter.values.position.editor.DataHelper;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
@SuppressWarnings("checkstyle:magicNumber")
public class CutSortingListToNecessarySizeTest {

    List<Option> sortingList;
    CategoryParam param;
    int expected;

    public CutSortingListToNecessarySizeTest(Integer sortingListCount, Integer cuttingCount, Integer expected) {
        param = new Parameter();
        param.setShortEnumCount(cuttingCount);

        sortingList = new ArrayList<>();
        IntStream.range(0, sortingListCount).forEach(index -> {
            sortingList.add(new OptionImpl());
        });

        this.expected = expected;
    }

    @Test
    public void test() {
        List<Option> testingList = DataHelper.cutSortingListToNecessarySize(sortingList, param);
        assertEquals(expected, testingList.size());
    }

    @Parameters
    public static Collection<Object[]> generatedDataPoints() {
        Object[][] data = new Object[][] {
            {0, 0, 0},
            {10, 0, 0},
            {10, 10, 10},
            {10, 5, 5},
            {5, 10, 5},
        };
        return Arrays.asList(data);
    }
}
