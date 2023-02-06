package ru.yandex.mail.micronaut.common;

import lombok.AllArgsConstructor;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;

@AllArgsConstructor
final class TestPageFetcher implements Async.PageFetcher<Integer, Long> {
    private final List<Long> elements;

    @Override
    public CompletableFuture<Page<Integer, Long>> fetch(Pageable<Integer> pageable) {
        val index = pageable.getPageId().orElse(0);
        if (index >= elements.size()) {
            return completedFuture(new Page<>(emptyList()));
        }

        val toIndex = Math.min(index + pageable.getPageSize(), elements.size());
        val nextPageId = (toIndex == elements.size())
            ? Optional.<Integer>empty()
            : Optional.of(toIndex);

        val result = elements.subList(index, toIndex);
        return completedFuture(new Page<>(result, nextPageId));
    }
}
