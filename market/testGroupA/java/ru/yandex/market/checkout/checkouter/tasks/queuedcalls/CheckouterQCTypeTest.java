package ru.yandex.market.checkout.checkouter.tasks.queuedcalls;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class CheckouterQCTypeTest {

    @Test
    public void queuedCallDescriptionShouldNotBeEmpty() {
        List<CheckouterQCType> emptyDescriptionQueuedCallTypes = Stream.of(CheckouterQCType.values())
                .filter(cqt -> StringUtils.isBlank(cqt.getDescription()))
                .collect(Collectors.toList());

        assertThat(emptyDescriptionQueuedCallTypes, Matchers.empty());
    }
}
