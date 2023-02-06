package ru.yandex.direct.api.v5.entity.keywords.converter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.keywords.KeywordStatusSelectionEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.bids.container.ShowConditionStatusSelection;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class KeywordsGetRequestConverterStatusesTest {

    private KeywordsGetRequestConverter requestConverter;

    @Parameterized.Parameter
    public List<KeywordStatusSelectionEnum> statuses;

    @Parameterized.Parameter(1)
    public List<ShowConditionStatusSelection> expectedStatuses;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {emptyList(), emptyList()},
                {singletonList(KeywordStatusSelectionEnum.DRAFT), singletonList(ShowConditionStatusSelection.DRAFT)},
                {singletonList(KeywordStatusSelectionEnum.ACCEPTED), singletonList(ShowConditionStatusSelection.ACCEPTED)},
                {singletonList(KeywordStatusSelectionEnum.REJECTED), singletonList(ShowConditionStatusSelection.REJECTED)},
                {asList(KeywordStatusSelectionEnum.values()), asList(ShowConditionStatusSelection.values())},
        };
    }

    @Before
    public void prepare() {
        requestConverter = new KeywordsGetRequestConverter(MOSCOW_TIMEZONE);
    }

    @Test
    public void test() {
        assertThat(requestConverter.convertStatuses(statuses))
                .containsExactlyInAnyOrder(expectedStatuses.toArray(new ShowConditionStatusSelection[0]));
    }

}

