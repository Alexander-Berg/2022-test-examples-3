package ru.yandex.market.tpl.billing.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Тесты для {@link PageableCollectorUtil}
 */
public class PageableCollectorUtilTest {

    @Test
    public void testGetAllByPage() {
        int batchSize = 10;
        class TestInterface implements PageableCollectorUtil.GetDataPerPageFunction<List<Integer>> {
            @Override
            public List<Integer> getData(int page, int batchSize) {
                if (page == 3) {
                    return List.of(1, 2, 3);
                }
                return IntStream.range(0, 10).boxed().toList();
            }
        }

        //без тестового класса нельзя, потому что mokito пишет, что лямбда - это final class
        PageableCollectorUtil.GetDataPerPageFunction<List<Integer>> function = spy(new TestInterface());

        List<Integer> totalList = new ArrayList<>();
        Consumer<List<Integer>> consumer = totalList::addAll;

        PageableCollectorUtil.getAll(
                batchSize,
                function,
                Function.identity(),
                consumer
        );

        assertThat(totalList, hasSize(33));
        verify(function, times(4)).getData(anyInt(), anyInt());
    }
}
