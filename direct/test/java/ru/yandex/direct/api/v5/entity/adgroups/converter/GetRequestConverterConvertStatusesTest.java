package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.adgroups.AdGroupStatusSelectionEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupStatus;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetRequestConverter.convertStatuses;


@RunWith(Parameterized.class)
public class GetRequestConverterConvertStatusesTest {

    @Parameterized.Parameter
    public List<AdGroupStatusSelectionEnum> statuses;

    @Parameterized.Parameter(1)
    public Set<AdGroupStatus> expectedStatuses;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {singletonList(AdGroupStatusSelectionEnum.ACCEPTED), EnumSet.of(AdGroupStatus.ACCEPTED)},
                {singletonList(AdGroupStatusSelectionEnum.DRAFT), EnumSet.of(AdGroupStatus.DRAFT)},
                {singletonList(AdGroupStatusSelectionEnum.MODERATION), EnumSet.of(AdGroupStatus.MODERATION)},
                {singletonList(AdGroupStatusSelectionEnum.PREACCEPTED), EnumSet.of(AdGroupStatus.PREACCEPTED)},
                {singletonList(AdGroupStatusSelectionEnum.REJECTED), EnumSet.of(AdGroupStatus.REJECTED)},
                {asList(AdGroupStatusSelectionEnum.ACCEPTED, AdGroupStatusSelectionEnum.DRAFT,
                        AdGroupStatusSelectionEnum.MODERATION, AdGroupStatusSelectionEnum.PREACCEPTED,
                        AdGroupStatusSelectionEnum.REJECTED),
                        EnumSet.of(AdGroupStatus.ACCEPTED, AdGroupStatus.DRAFT, AdGroupStatus.MODERATION,
                                AdGroupStatus.PREACCEPTED, AdGroupStatus.REJECTED)},
                {asList(AdGroupStatusSelectionEnum.ACCEPTED, AdGroupStatusSelectionEnum.DRAFT,
                        AdGroupStatusSelectionEnum.MODERATION, AdGroupStatusSelectionEnum.PREACCEPTED,
                        AdGroupStatusSelectionEnum.REJECTED, AdGroupStatusSelectionEnum.ACCEPTED,
                        AdGroupStatusSelectionEnum.DRAFT, AdGroupStatusSelectionEnum.MODERATION,
                        AdGroupStatusSelectionEnum.PREACCEPTED, AdGroupStatusSelectionEnum.REJECTED),
                        EnumSet.of(AdGroupStatus.ACCEPTED, AdGroupStatus.DRAFT, AdGroupStatus.MODERATION,
                                AdGroupStatus.PREACCEPTED, AdGroupStatus.REJECTED)},
        };
    }

    @Test
    public void test() {
        assertThat(convertStatuses(statuses)).containsExactlyInAnyOrder(expectedStatuses.toArray(new AdGroupStatus[0]));
    }
}
