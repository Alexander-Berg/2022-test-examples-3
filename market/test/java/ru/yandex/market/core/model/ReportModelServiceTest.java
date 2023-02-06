package ru.yandex.market.core.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.common.report.MarketReportService;

/**
 * @author zoom
 */
public class ReportModelServiceTest extends Assert {

    private static final TestModel MODEL_1 = new TestModel(1);
    private static final TestModel MODEL_2 = new TestModel(2);
    private static final TestModel MODEL_3 = new TestModel(3);

    @Test(expected = ParserNotFoundError.class)
    public void shouldThrowExceptionWhenNoParsers() {
        MarketReportService report = (searchRequest, parser) -> parser.parse(null);
        ReportModelService service = new ReportModelService(report, Collections.emptyMap());
        service.getModels(Collections.singletonList(1L), TestModel.class, model -> {
        });
        fail();
    }

    @Test(expected = ParserNotFoundError.class)
    public void shouldThrowExceptionWhenParserForClassNotFound() {
        MarketReportService report = (searchRequest, parser) -> {
        };
        Map<Class<?>, Function<InputStream, List<?>>> parsers = new HashMap<>();
        parsers.put(String.class, in -> null);
        ReportModelService service = new ReportModelService(report, parsers);
        service.getModels(Collections.singletonList(1L), TestModel.class, model -> {
        });
        fail();
    }

    @Test
    public void shouldReturnZeroModelWhenParserReturnsZeroModels() {
        MarketReportService report = (searchRequest, parser) -> parser.parse(null);
        Map<Class<?>, Function<InputStream, List<?>>> parsers = new HashMap<>();
        parsers.put(TestModel.class, in -> Collections.emptyList());
        ReportModelService service = new ReportModelService(report, parsers);
        AtomicInteger counter = new AtomicInteger();
        service.getModels(Collections.singletonList(1L), TestModel.class, model -> counter.incrementAndGet());
        assertEquals(0, counter.get());
    }

    @Test
    public void shouldReturnOneModel() {
        MarketReportService report = (searchRequest, parser) -> parser.parse(null);
        Map<Class<?>, Function<InputStream, List<?>>> parsers = new HashMap<>();
        parsers.put(TestModel.class, in -> Collections.singletonList(new TestModel(1)));
        ReportModelService service = new ReportModelService(report, parsers);
        AtomicInteger counter = new AtomicInteger();
        service.getModels(Collections.singletonList(1L), TestModel.class, model -> {
            if (model.getId() == 1) {
                counter.incrementAndGet();
            }
        });
        assertEquals(1, counter.get());
    }

    @Test
    public void shouldReturnTreeModel() {
        MarketReportService report = (searchRequest, parser) -> parser.parse(null);
        Map<Class<?>, Function<InputStream, List<?>>> parsers = new HashMap<>();
        parsers.put(TestModel.class, in -> Arrays.asList(MODEL_1, MODEL_2, MODEL_3));
        ReportModelService service = new ReportModelService(report, parsers);
        List<TestModel> actual = new ArrayList<>();
        service.getModels(Collections.singletonList(1L), TestModel.class, actual::add);
        assertEquals(Arrays.asList(MODEL_1, MODEL_2, MODEL_3), actual);
    }

    @Test(expected = ModelServiceIOException.class)
    public void shouldThrowExceptionOnIoProblem() {
        MarketReportService report = (searchRequest, parser) -> {
            throw new IOException();
        };
        Map<Class<?>, Function<InputStream, List<?>>> parsers = new HashMap<>();
        parsers.put(TestModel.class, in -> Collections.emptyList());
        ReportModelService service = new ReportModelService(report, parsers);
        service.getModels(Collections.singletonList(1L), TestModel.class, model -> {
        });
        fail();
    }

    public static class TestModel {

        private final long id;

        public TestModel(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }
}