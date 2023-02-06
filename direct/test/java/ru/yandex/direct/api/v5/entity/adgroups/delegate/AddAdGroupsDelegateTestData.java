package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class AddAdGroupsDelegateTestData {

    static List<AdGroup> createListOfTextAdGroups(long campaignId, int size) {
        return Stream.generate(() ->
                new TextAdGroup()
                        .withCampaignId(campaignId)
                        .withId(RandomUtils.nextLong(0, Long.MAX_VALUE)))
                .limit(size)
                .collect(toList());
    }

    static ApiMassResult<Long> successfulMassResult() {
        ApiMassResult<Long> result = mock(ApiMassResult.class);
        when(result.getErrorCount()).thenReturn(0);
        when(result.isSuccessful()).thenReturn(true);
        return result;
    }

    static ApiMassResult<Long> brokenMassResult() {
        ApiMassResult<Long> result = mock(ApiMassResult.class);
        when(result.getErrorCount()).thenReturn(1);
        when(result.isSuccessful()).thenReturn(false);
        return result;
    }
}
