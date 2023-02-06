package ru.yandex.autotests.directapi.test.units;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class UnitsWithdrawalTestDataProviderImpl implements UnitsWithdrawalTestDataProvider {

    /** @inheritDoc */
    @Override
    public Iterable<Object[]> buildDataSets(Runnable... serviceCalls) {
        return buildDataSets((Object[]) serviceCalls);
    }

    /** @inheritDoc */
    @Override
    public Iterable<Object[]> buildDataSets(Supplier<?>... serviceCalls) {
        return buildDataSets((Object[]) serviceCalls);
    }

    /** @inheritDoc */
    @Override
    public Iterable<Object[]> buildDataSets(Function<?, ?>... serviceCalls) {
        return buildDataSets((Object[]) serviceCalls);
    }

    /** @inheritDoc */
    @Override
    public Iterable<Object[]> buildDataSets(Stream<Object[]> data, Function<?, ?>... serviceCalls) {
        return Stream.of(serviceCalls)
                .flatMap(call -> addToDataSets(data, call))
                .collect(toList());
    }

    private Iterable<Object[]> buildDataSets(Object... serviceCalls) {
        return Stream.of(serviceCalls)
                .flatMap(UnitsWithdrawalTestDataProviderImpl::addToDataSets)
                .collect(toList());
    }

    private static Stream<Object[]> addToDataSets(Object... objects) {
        return addToDataSets(UnitsWithdrawalTestData.provideData(), objects);
    }

    private static Stream<Object[]> addToDataSets(Stream<Object[]> data, Object... objects) {
        return data
                .map(ds -> Arrays.copyOf(ds, ds.length + objects.length))
                .peek(ds -> addToDataSet(ds, objects));
    }

    private static void addToDataSet(Object[] dataSet, Object... objects) {
        for (int i = 0; i < objects.length; i++) {
            dataSet[dataSet.length - 1 - i] = objects[objects.length - 1 - i];
        }
    }

}
