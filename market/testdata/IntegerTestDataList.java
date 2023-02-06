package ru.yandex.autotests.market.billing.util.testdata;

import java.util.List;

/**
 * User: strangelet
 * Date: 28.01.13 : 15:37
 */
public class IntegerTestDataList {
    private List<Integer> testDataList;
    private String name;

    public IntegerTestDataList(List<Integer> subShopsList, String name) {
        this.testDataList = subShopsList;
        this.name = name;
    }

    @Override
    public String toString() {
        final int size = testDataList.size();
        final Integer first = testDataList.get(0);
        final Integer last = testDataList.get(size - 1);

        return String.format("Tested %ss: %d-%d/%d", name, first, last, size);
    }

    public List<Integer> getTestDataList() {
        return testDataList;
    }
}
