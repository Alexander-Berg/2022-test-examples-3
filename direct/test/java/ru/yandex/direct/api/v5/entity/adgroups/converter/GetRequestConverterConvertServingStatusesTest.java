package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.general.ServingStatusEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.ServingStatus;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetRequestConverter.convertServingStatuses;

@RunWith(Parameterized.class)
public class GetRequestConverterConvertServingStatusesTest {

    @Parameterized.Parameter
    public List<ServingStatusEnum> statuses;

    @Parameterized.Parameter(1)
    public Set<ServingStatus> expectedStatuses;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {singletonList(ServingStatusEnum.ELIGIBLE), EnumSet.of(ServingStatus.ELIGIBLE)},
                {singletonList(ServingStatusEnum.RARELY_SERVED), EnumSet.of(ServingStatus.RARELY_SERVED)},
                {asList(ServingStatusEnum.ELIGIBLE, ServingStatusEnum.RARELY_SERVED),
                        EnumSet.of(ServingStatus.ELIGIBLE, ServingStatus.RARELY_SERVED)},
                {asList(ServingStatusEnum.ELIGIBLE, ServingStatusEnum.RARELY_SERVED, ServingStatusEnum.ELIGIBLE,
                        ServingStatusEnum.RARELY_SERVED),
                        EnumSet.of(ServingStatus.ELIGIBLE, ServingStatus.RARELY_SERVED)},
        };
    }

    @Test
    public void test() {
        assertThat(convertServingStatuses(statuses))
                .containsExactlyInAnyOrder(expectedStatuses.toArray(new ServingStatus[0]));
    }
}
