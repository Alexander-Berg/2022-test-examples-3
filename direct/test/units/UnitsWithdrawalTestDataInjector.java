package ru.yandex.autotests.directapi.test.units;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Iterators;

/**
 * Класс для проставления входных данных в тестовые классы (пока что только production pack-а).
 * <p>
 * Вынесен для того, чтобы не дублировать код и защититься от изменения порядка
 * или количества элементов в data set-е во время компиляции.
 */
public class UnitsWithdrawalTestDataInjector {

    private static final Iterable<Object[]> DATA = UnitsWithdrawalTestData.provideData().collect(Collectors.toList());

    public static UnitsWithdrawalTestDataInjector newInstance() {
        return new UnitsWithdrawalTestDataInjector();
    }

    /**
     * Проставить в тест данные одного — простейшего — тест кейса.
     *
     * @param testClass тестовый класс, потребитель тестовых данных.
     */
    @SuppressWarnings("unchecked")
    public void injectTheSimplestTestCaseData(UnitsWithdrawalTestDataConsumer testClass) {
        Object[] dataSet = DATA.iterator().next();
        Iterator<Object> inputItems = Iterators.forArray(dataSet);

        injectSingleDataSet(testClass, inputItems);
    }

    private void injectSingleDataSet(UnitsWithdrawalTestDataConsumer testClass, Iterator<Object> inputItems) {
        set(inputItems, String.class, testClass::setDescription);
        set(inputItems, String.class, testClass::setOperatorLogin);
        set(inputItems, String.class, testClass::setClientLogin);
        set(inputItems, String.class, testClass::setUseOperatorUnits);
        set(inputItems, Integer.class, testClass::setUnitsDelta);

        set(inputItems, Collection.class, testClass::setExpectedUnitsWithdrawLogins);
        set(inputItems, Collection.class, testClass::setExpectedUnitsKeepLogins);
    }

    private <T> void set(Iterator<Object> items, Class<T> type, Consumer<? super T> setter) {
        setter.accept(type.cast(items.next()));
    }

}
