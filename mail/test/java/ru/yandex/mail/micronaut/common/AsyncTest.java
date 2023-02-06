package ru.yandex.mail.micronaut.common;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import java.util.List;

import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;

class AsyncTest {
    private static final int PAGE_SIZE = 3;

    @Test
    @DisplayName("Verify that fetchPagesRx returns expected sequence of the values")
    void fetchPagesRxTest() {
        val values = List.of(1L, 5L, 42L, 0L, -33L, 100500L, 43L, 56L);

        val fetcher = new TestPageFetcher(values);
        val result = Async.fetchPagesRx(PAGE_SIZE, fetcher)
            .log()
            .flatMapIterable(identity())
            .subscribeOn(Schedulers.newParallel("parallel", 10))
            .collectList()
            .block();

        assertThat(result)
            .containsExactlyElementsOf(values);
    }
}
