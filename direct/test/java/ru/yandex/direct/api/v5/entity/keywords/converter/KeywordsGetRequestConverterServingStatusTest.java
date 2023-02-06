package ru.yandex.direct.api.v5.entity.keywords.converter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.general.ServingStatusEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.keyword.model.ServingStatus;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class KeywordsGetRequestConverterServingStatusTest {
    private KeywordsGetRequestConverter requestConverter;

    @Parameterized.Parameter
    public List<ServingStatusEnum> statuses;

    @Parameterized.Parameter(1)
    public List<ServingStatus> expectedStatuses;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {emptyList(), emptyList()},
                {singletonList(ServingStatusEnum.ELIGIBLE), singletonList(ServingStatus.ELIGIBLE)},
                {singletonList(ServingStatusEnum.RARELY_SERVED), singletonList(ServingStatus.RARELY_SERVED)},
                {asList(ServingStatusEnum.values()), asList(ServingStatus.values())},
        };
    }

    @Before
    public void prepare() {
        requestConverter = new KeywordsGetRequestConverter(MOSCOW_TIMEZONE);
    }

    @Test
    public void test() {
        assertThat(requestConverter.convertServingStatuses(statuses))
                .containsExactlyInAnyOrder(expectedStatuses.toArray(new ServingStatus[0]));
    }

}

