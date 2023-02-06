package ru.yandex.autotests.market.billing.util.testdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: strangelet
 * Date: 08.02.13 : 15:36
 */
public class IntegerTestedDataSplitter {

    private static final String DEFAULT_DATA_NAME = "data id";

    /**
     * Получить списки id  сущностей для параметризированного запуска теста.
     *
     * @param testNumber количество запусков тестов. Если количество запусков тестов будет больше чем величина списка,
     *                   то это количество уменьшися до размера  исходного списка сущностей.
     * @param testedID   полный список айди тестируемых сущностей
     * @param name       название тестируемой сущности ,  если <i>name</i> задан как <i>null</i>,
     *                   то присвоится дефолтное имя
     * @return коллекция IntegerTestDataList Количество кусков будет <i>testNumber</i> + 1 (остаток от деления)
     */
    public static Collection<Object[]> split(int testNumber, List<Integer> testedID, String name) {

        if (testedID == null || testedID.isEmpty()) {
            throw new Error("List of tested data is empty or null ( testedID = " + testedID + ")");
        }
        if (name == null || name.equals("")) {
            name = DEFAULT_DATA_NAME;
        }

        List<Object[]> list = new ArrayList<Object[]>();
        final int size = testedID.size();

        if (testNumber <= 0) {
            throw new Error("No valid number of tests (testNumber = " + testNumber + ")");
        } else if (testNumber == 1) {
            list.add(new Object[]{new IntegerTestDataList(testedID, name)});
        } else {
            if (size < testNumber) {
                testNumber = size;
            }
            final int step = size / testNumber;
            final boolean remainder = hasRemainder(testNumber, size);
            for (int i = 0; i < testNumber + (remainder ? 1 : 0); i++) {
                list.add(new Object[]{
                        new IntegerTestDataList(takeSubListForTest(testedID, i, step), name)});
            }

        }
        return list;
    }

    private static boolean hasRemainder(int testNumber, int size) {
        return size % testNumber > 0;
    }

    /**
     * Получить список id сущностей для параметризированного запуска теста
     *
     * @param testedId полный список сущностей
     * @param testNum  номер параметризированного запуска теста
     * @param step     количество айди сущностей в данном запуске
     * @return
     */

    private static List<Integer> takeSubListForTest(List<Integer> testedId, int testNum, int step) {
        final int testedIdSize = testedId.size();
        int fromIndex = Math.min(testedIdSize - 1, testNum * step);
        int toIndex = Math.min(testedIdSize, fromIndex + step - 1);
        return testedId.subList(fromIndex, toIndex);
    }


}
