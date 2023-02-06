package ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.sync;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.val;
import one.util.streamex.StreamEx;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffDto;
import ru.yandex.mail.micronaut.common.JsonMapper;
import ru.yandex.mail.cerberus.yt.staff.client.StaffResult;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

@AllArgsConstructor
class StaffResultAnswer<T extends StaffDto> implements Answer<CompletableFuture<StaffResult>> {
    private final Collection<T> items;
    private final JsonMapper jsonMapper;
    private final AtomicInteger offset = new AtomicInteger(0);

    private List<JsonNode> getChunk(int limit) {
        val pos = offset.getAndAdd(limit);
        return StreamEx.of(items)
            .sortedBy(StaffDto::getUniqueId)
            .skip(pos)
            .limit(limit)
            .map(jsonMapper::toJsonNode)
            .toImmutableList();
    }

    @Override
    public CompletableFuture<StaffResult> answer(InvocationOnMock invocation) {
        val limit = (int) invocation.getArgument(2);

        if (limit < 1) {
            return failedFuture(new IllegalArgumentException("limit needs to be > 0"));
        }

        val chunk = getChunk(limit);
        return completedFuture(new StaffResult(1, limit, chunk));
    }
}
