package ru.yandex.market.abo.util.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import ru.yandex.common.framework.core.MockServResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 * @date 06.04.18.
 */
public class AsyncModelAndViewTest {
    private static final int ELEMENTS_NUM = 1_000_000;

    private final ExecutorService pool = Executors.newFixedThreadPool(10);

    @Test
    public void asyncAddObject() {
        ModelAndView modelAndView = new AsyncModelAndView();

        AsyncTaskUtil.runAsyncUnchecked(pool, IntStream.range(0, ELEMENTS_NUM).boxed().collect(Collectors.toList()),
                i -> modelAndView.addObject(String.valueOf(i), i));

        assertEquals(ELEMENTS_NUM, modelAndView.getModelMap().size());
    }

    @Test
    public void concurrentBuilder() {
        var factory = new ConcurrentModelAndViewBuilder.Factory(pool);
        var builder = factory.modelBuilder("");
        IntStream.range(0, ELEMENTS_NUM).forEach(i -> builder.addObject(String.valueOf(i), () -> i));

        assertEquals(ELEMENTS_NUM, builder.buildModelAndView().getModelMap().size());
    }

    @Test
    public void asyncServResponce() {
        MockServResponse data = new MockServResponse();
        AsyncServResponse resp = new AsyncServResponse(data);

        AsyncTaskUtil.runAsyncUnchecked(pool, IntStream.range(0, ELEMENTS_NUM).boxed().collect(Collectors.toList()),
                i -> resp.addData(String.valueOf(i)));

        assertEquals(ELEMENTS_NUM, data.getData().size());
    }
}
