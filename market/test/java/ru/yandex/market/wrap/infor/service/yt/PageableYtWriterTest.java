package ru.yandex.market.wrap.infor.service.yt;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.google.common.collect.Iterators;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.market.wrap.infor.entity.util.Pagination;
import ru.yandex.market.wrap.infor.repository.yt.YtWritingRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wrap.infor.service.yt.PageableYtWriter.YT_TRANSACTION_TIMEOUT_MINS;

@ExtendWith(MockitoExtension.class)
class PageableYtWriterTest {

    private static final int DB_PAGE_SIZE = 50;
    private static final int YT_WRITING_CHUNK_ROWS = 5;

    @Mock
    private YtWritingRepository<Long> mockRepository;
    @Mock
    private YtTransactions mockTransactions;

    private PageableYtWriter<Integer, Long> writer;

    @Captor
    private ArgumentCaptor<Collection<Long>> captor;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        writer = new PageableYtWriter<>(
            mockRepository, mockTransactions, DB_PAGE_SIZE, YT_WRITING_CHUNK_ROWS
        );
    }

    @Test
    void runWithTwoPages() {

        when(mockTransactions.startAndGet(Duration.ofMinutes(YT_TRANSACTION_TIMEOUT_MINS)))
            .thenReturn(getMockTransaction());
        int requiredPages = 2;

        writer.write(
            pagination -> getMockPage(pagination, requiredPages),
            this::mapToLongCollection
        );

        verify(mockRepository, times(DB_PAGE_SIZE / YT_WRITING_CHUNK_ROWS * requiredPages))
            .writeOrAppend(captor.capture(), any(GUID.class));
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(captor.getAllValues().size()).isEqualTo(
                DB_PAGE_SIZE / YT_WRITING_CHUNK_ROWS * requiredPages
            );
            Iterator<Long> expected = LongStream
                .range(0, 100)
                .boxed()
                .iterator();
            Iterator<Long> actual = captor.getAllValues()
                .stream()
                .flatMap(Collection::stream)
                .iterator();
            assertions.assertThat(Iterators.elementsEqual(expected, actual)).isTrue();
        });
    }

    private Collection<Integer> getMockPage(Pagination pagination, int amountOFDesiredPages) {
        if (pagination.getOffset() >= amountOFDesiredPages * DB_PAGE_SIZE) {
            return Collections.emptyList();
        } else {
            return IntStream.range(
                (int) pagination.getOffset(),
                (int) pagination.getOffset() + DB_PAGE_SIZE)
                .boxed()
                .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private Collection<Long> mapToLongCollection(Collection<Integer> input) {
        return input.stream().map(i -> (long) i).collect(Collectors.toList());
    }

    private Transaction getMockTransaction() {
        return new Transaction() {
            @Override
            public GUID getId() {
                return new GUID(1, 2, 2, 1);
            }

            @Override
            public Optional<Transaction> getParent() {
                return null;
            }

            @Override
            public Instant getStartTime() {
                return null;
            }

            @Override
            public Duration getTimeout() {
                return null;
            }

            @Override
            public Transaction start(boolean pingAncestorTransactions, Duration timeout) {
                return null;
            }

            @Override
            public void ping(boolean pingAncestorTransactions) {

            }

            @Override
            public void commit(boolean pingAncestorTransactions) {

            }

            @Override
            public void abort(boolean pingAncestorTransactions) {

            }
        };
    }
}
