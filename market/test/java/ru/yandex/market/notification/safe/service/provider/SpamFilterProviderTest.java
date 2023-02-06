package ru.yandex.market.notification.safe.service.provider;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.partner.notification.AbstractFunctionalTest;

class SpamFilterProviderTest extends AbstractFunctionalTest {

    @Autowired
    private Collection<Long> spamFilterExclusions;

    @Test
    void testSpamFilterExclusions() {
        final Collection<Long> duplicates = spamFilterExclusions.stream()
                .collect(Collectors.groupingBy(Function.identity()))
                .entrySet()
                .stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Assertions.assertThat(duplicates).isEmpty();
    }

}
