package ru.yandex.market.pricelabs.tms.processing;

import java.util.List;
import java.util.function.Consumer;

import ru.yandex.market.pricelabs.bindings.csv.CSVMapper;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.yt.binding.YTBinder;

public abstract class AbstractSourceTargetProcessorConfiguration<S, T>
        extends AbstractProcessorSpringConfiguration<T> {

    private final YTBinder<S> sourceBinder = YTBinder.getStaticBinder(getSourceClass());
    private final CSVMapper<S> sourceMapper = CSVMapper.mapper(sourceBinder);

    public final List<S> readSourceList() {
        return readSourceList(getSourceCsv());
    }

    protected final List<S> readSourceList(String resource) {
        return TmsTestUtils.update(readCsv(sourceMapper, resource), getSourceUpdate());
    }

    protected final YTBinder<S> getSourceBinder() {
        return sourceBinder;
    }

    protected final CSVMapper<S> getSourceMapper() {
        return sourceMapper;
    }

    protected abstract String getSourceCsv();

    protected abstract Class<S> getSourceClass();

    protected Consumer<S> getSourceUpdate() {
        return Utils.emptyConsumer();
    }

}
