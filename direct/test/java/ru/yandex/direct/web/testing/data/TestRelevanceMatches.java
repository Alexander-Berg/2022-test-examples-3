package ru.yandex.direct.web.testing.data;

import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.web.entity.relevancematch.model.WebRelevanceMatch;

public class TestRelevanceMatches {

    private TestRelevanceMatches() {
    }

    public static WebRelevanceMatch randomPriceWebRelevanceMatch(Long id) {
        return new WebRelevanceMatch()
                .withId(id)
                .withPrice(RandomUtils.nextDouble(3L, 10000L))
                .withAutoBudgetPriority(3)
                .withSuspended(false);
    }
}
