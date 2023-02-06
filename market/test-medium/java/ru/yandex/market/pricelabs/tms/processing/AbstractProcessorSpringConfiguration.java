package ru.yandex.market.pricelabs.tms.processing;

import java.util.List;
import java.util.function.Consumer;

import ru.yandex.market.pricelabs.bindings.csv.CSVMapper;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.yt.binding.YTBinder;

public abstract class AbstractProcessorSpringConfiguration<T> extends AbstractTmsSpringConfiguration {

    private final YTBinder<T> targetBinder = YTBinder.getBinder(getTargetClass());

    private final CSVMapper<T> targetMapper = CSVMapper.mapper(targetBinder);

    public final List<T> readTargetList() {
        return readTargetList(getTargetCsv());
    }

    public final List<T> readTargetList(String resource) {
        return TmsTestUtils.update(readCsv(targetMapper, resource), getTargetUpdate());
    }

    protected final YTBinder<T> getTargetBinder() {
        return targetBinder;
    }

    protected final CSVMapper<T> getTargetMapper() {
        return targetMapper;
    }

    protected abstract String getTargetCsv();

    protected abstract Class<T> getTargetClass();


    protected Consumer<T> getTargetUpdate() {
        return Utils.emptyConsumer();
    }

}
