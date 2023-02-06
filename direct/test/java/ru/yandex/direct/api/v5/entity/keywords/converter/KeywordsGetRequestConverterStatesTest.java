package ru.yandex.direct.api.v5.entity.keywords.converter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.keywords.KeywordStateSelectionEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.bids.container.ShowConditionStateSelection;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class KeywordsGetRequestConverterStatesTest {

    private KeywordsGetRequestConverter requestConverter;

    @Parameterized.Parameter
    public List<KeywordStateSelectionEnum> states;

    @Parameterized.Parameter(1)
    public List<ShowConditionStateSelection> expectedStates;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {emptyList(), emptyList()},
                {singletonList(KeywordStateSelectionEnum.OFF), singletonList(ShowConditionStateSelection.OFF)},
                {singletonList(KeywordStateSelectionEnum.ON), singletonList(ShowConditionStateSelection.ON)},
                {singletonList(KeywordStateSelectionEnum.SUSPENDED), singletonList(ShowConditionStateSelection.SUSPENDED)},
                {asList(KeywordStateSelectionEnum.values()), asList(ShowConditionStateSelection.values())},
        };
    }

    @Before
    public void prepare() {
        requestConverter = new KeywordsGetRequestConverter(MOSCOW_TIMEZONE);
    }

    @Test
    public void test() {
        assertThat(requestConverter.convertStates(states))
                .containsExactlyInAnyOrder(expectedStates.toArray(new ShowConditionStateSelection[0]));
    }

}

