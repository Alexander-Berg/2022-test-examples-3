package ru.yandex.direct.core.entity.campaign.converter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterPermission;
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterWithAdditionalInformation;
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect;
import ru.yandex.direct.metrika.client.model.response.UserCountersExtended;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class CampaignConverterMetrikaCountersTest {

    @Test
    public void testConvertOneOwner() {
        CounterInfoDirect counter = createCounter(11, MetrikaCounterPermission.VIEW);
        var userCounters = createUserCountersExtended(counter);
        Set<MetrikaCounterWithAdditionalInformation> result =
                CampaignConverter.toMetrikaCountersWithAdditionalInformation(List.of(userCounters));
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void testConvertTwoOwners_DifferentCounters() {
        var counter1 = createCounter(11, MetrikaCounterPermission.VIEW);
        var userCountersFirst = createUserCountersExtended(counter1);
        var counter2 = createCounter(22, MetrikaCounterPermission.OWN);
        var userCountersSecond = createUserCountersExtended(counter2);
        Set<MetrikaCounterWithAdditionalInformation> result =
                CampaignConverter.toMetrikaCountersWithAdditionalInformation(
                        List.of(userCountersFirst, userCountersSecond));
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void testConvertTwoOwners_SameCounterId() {
        int counterId1 = RandomNumberUtils.nextPositiveInteger();
        int counterId2 = RandomNumberUtils.nextPositiveInteger();
        var counter1 = createCounter(counterId1, MetrikaCounterPermission.VIEW);
        var counter2 = createCounter(counterId2, MetrikaCounterPermission.VIEW);
        var userCountersFirst = createUserCountersExtended(counter1, counter2);
        var counter3 = createCounter(counterId1, MetrikaCounterPermission.OWN);
        var userCountersSecond = createUserCountersExtended(counter3);

        Set<MetrikaCounterWithAdditionalInformation> result =
                CampaignConverter.toMetrikaCountersWithAdditionalInformation(
                        List.of(userCountersFirst, userCountersSecond));
        Map<Long, MetrikaCounterWithAdditionalInformation> countersById =
                StreamEx.of(result).toMap(MetrikaCounterWithAdditionalInformation::getId, Function.identity());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(countersById.size()).isEqualTo(2);
            softly.assertThat(countersById.get((long) counterId1).getPermissionsByUid())
                    .isEqualTo(Map.of(userCountersFirst.getOwner(), MetrikaCounterPermission.VIEW,
                            userCountersSecond.getOwner(), MetrikaCounterPermission.OWN));
            softly.assertThat(countersById.get((long) counterId2).getPermissionsByUid())
                    .isEqualTo(Map.of(userCountersFirst.getOwner(), MetrikaCounterPermission.VIEW));
        });
    }

    private UserCountersExtended createUserCountersExtended(CounterInfoDirect... counters) {
        return new UserCountersExtended()
                .withOwner(RandomNumberUtils.nextPositiveLong())
                .withCounters(List.of(counters));
    }

    private CounterInfoDirect createCounter(int counterId, MetrikaCounterPermission permission) {
        return new CounterInfoDirect()
                .withId(counterId)
                .withName("counter name")
                .withSitePath("domain")
                .withCounterSource("turbodirect")
                .withCounterPermission(permission.name().toLowerCase());
    }
}
