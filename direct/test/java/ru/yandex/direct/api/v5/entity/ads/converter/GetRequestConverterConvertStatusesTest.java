package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdStatusSelectionEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.Status;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.GetRequestConverter.convertStatuses;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetRequestConverterConvertStatusesTest {

    @Parameterized.Parameter
    public List<AdStatusSelectionEnum> statuses;

    @Parameterized.Parameter(1)
    public List<Status> expectedStatuses;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {emptyList(), emptyList()},
                {singletonList(AdStatusSelectionEnum.ACCEPTED), singletonList(Status.ACCEPTED)},
                {singletonList(AdStatusSelectionEnum.DRAFT), singletonList(Status.DRAFT)},
                {singletonList(AdStatusSelectionEnum.MODERATION), singletonList(Status.MODERATION)},
                {singletonList(AdStatusSelectionEnum.PREACCEPTED), singletonList(Status.PREACCEPTED)},
                {singletonList(AdStatusSelectionEnum.REJECTED), singletonList(Status.REJECTED)},
                {asList(AdStatusSelectionEnum.values()), asList(Status.values())},
        };
    }

    @Test
    public void test() {
        assertThat(convertStatuses(statuses)).containsExactlyInAnyOrder(expectedStatuses.toArray(new Status[0]));
    }

}
