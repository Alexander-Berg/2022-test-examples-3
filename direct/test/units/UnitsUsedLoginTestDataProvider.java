package ru.yandex.autotests.directapi.test.units;

import java.util.Arrays;

import static java.util.stream.Collectors.toList;

public class UnitsUsedLoginTestDataProvider {

    public static Iterable<Object[]> buildDataSets(Object serviceCall) {
        return UnitsWithdrawalTestData.provideData()
                .map(ds -> Arrays.copyOf(ds, ds.length + 1))
                .peek(ds -> ds[ds.length - 1] = serviceCall)
                .collect(toList());
    }

}
