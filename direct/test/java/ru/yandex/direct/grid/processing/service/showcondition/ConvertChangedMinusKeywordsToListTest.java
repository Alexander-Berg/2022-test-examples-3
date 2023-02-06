package ru.yandex.direct.grid.processing.service.showcondition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.processing.service.showcondition.converter.FindAndReplaceDataConverter;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class ConvertChangedMinusKeywordsToListTest {

    @Parameterized.Parameter
    public String changedMinusKeywords;

    @Parameterized.Parameter(1)
    public List<String> expectedList;

    @Parameterized.Parameters(name = "changedMinusKeywords={0}, expectedList={1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {null, null},
                {"", Collections.emptyList()},
                {"one", Collections.singletonList("one")},
                {"     one     ", Collections.singletonList("one")},
                {"one two", Arrays.asList("one", "two")},
                {"    one        two   three    ", Arrays.asList("one", "two", "three")},
        });
    }


    @Test
    public void checkConvert() {
        assertThat(FindAndReplaceDataConverter.convertChangedMinusKeywordsToList(changedMinusKeywords))
                .isEqualTo(expectedList);
    }
}
