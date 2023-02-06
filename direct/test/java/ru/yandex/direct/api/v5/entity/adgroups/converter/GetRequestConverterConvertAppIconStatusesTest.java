package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.adgroups.AdGroupAppIconStatusSelectionEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupAppIconStatus;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetRequestConverter.convertAppIconStatuses;

@RunWith(Parameterized.class)
public class GetRequestConverterConvertAppIconStatusesTest {

    @Parameterized.Parameter
    public List<AdGroupAppIconStatusSelectionEnum> statuses;

    @Parameterized.Parameter(1)
    public Set<AdGroupAppIconStatus> expectedSatuses;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {singletonList(AdGroupAppIconStatusSelectionEnum.ACCEPTED), EnumSet.of(AdGroupAppIconStatus.ACCEPTED)},
                {singletonList(AdGroupAppIconStatusSelectionEnum.MODERATION),
                        EnumSet.of(AdGroupAppIconStatus.MODERATION)},
                {singletonList(AdGroupAppIconStatusSelectionEnum.REJECTED), EnumSet.of(AdGroupAppIconStatus.REJECTED)},
                {asList(AdGroupAppIconStatusSelectionEnum.ACCEPTED, AdGroupAppIconStatusSelectionEnum.MODERATION,
                        AdGroupAppIconStatusSelectionEnum.REJECTED),
                        EnumSet.of(AdGroupAppIconStatus.ACCEPTED, AdGroupAppIconStatus.MODERATION,
                                AdGroupAppIconStatus.REJECTED)},
                {asList(AdGroupAppIconStatusSelectionEnum.ACCEPTED, AdGroupAppIconStatusSelectionEnum.MODERATION,
                        AdGroupAppIconStatusSelectionEnum.REJECTED, AdGroupAppIconStatusSelectionEnum.ACCEPTED,
                        AdGroupAppIconStatusSelectionEnum.MODERATION, AdGroupAppIconStatusSelectionEnum.REJECTED),
                        EnumSet.of(AdGroupAppIconStatus.ACCEPTED, AdGroupAppIconStatus.MODERATION,
                                AdGroupAppIconStatus.REJECTED)},
        };
    }

    @Test
    public void test() {
        assertThat(convertAppIconStatuses(statuses))
                .containsExactlyInAnyOrder(expectedSatuses.toArray(new AdGroupAppIconStatus[0]));
    }
}
