package ru.yandex.market.mbo.gwt.client.widgets.parameter.values.position.editor.validator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ru.yandex.market.mbo.gwt.client.widgets.parameter.values.position.editor.Validator;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(value = Parameterized.class)
@SuppressWarnings("checkstyle:magicNumber")
public class ValidateShortEnumValuesTest {

    CategoryParam param;
    List<Option> optionList;
    boolean expectedTrue;

    public ValidateShortEnumValuesTest(Integer shortEnumCount, Integer optionListLength, Boolean expectedTrue) {
        if (shortEnumCount != null) {
            param = new Parameter();
            param.setShortEnumCount(shortEnumCount);
        }

        if (optionListLength != null) {
            optionList = new ArrayList<>();
            IntStream.range(0, optionListLength).forEach(index -> {
                optionList.add(new OptionImpl());
            });
        }
        this.expectedTrue = expectedTrue;
    }

    @Test
    public void test() {
        if (expectedTrue) {
            assertTrue(Validator.validateSortingValuesMaxSize(param, optionList));
        } else {
            assertFalse(Validator.validateSortingValuesMaxSize(param, optionList));
        }
    }

    @Parameters
    public static Collection<Object[]> generatedDataPoints() {
        Object[][] data = new Object[][] {
                {null, null, true},
                {0, null, true},
                {null, 0, false},
                {10, null, true},
                {null, 10, false},
                {0, 0, true},
                {10, 10, true},
                {10, 0, true},
                {0, 10, false},
                {10, 5, true},
                {5, 10, false}
        };
        return Arrays.asList(data);
    }
}
