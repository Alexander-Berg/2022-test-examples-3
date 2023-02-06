package ru.yandex.autotests.direct.web.util.testinterfaces;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;

/**
 * Created by ssdmitriev on 25.11.15.
 */
public interface Checkable<T> {
    /**
     * Заполняет поля формы значениями из бина
     *
     * @param bean
     */
    void fillParameters(T bean);

    /**
     * Сверяет не null поля из expectedBean с соответствующими полями на форме
     *
     * @param expectedBean
     */
    void checkParameters(BeanDifferMatcher<T> expectedBean);

    /**
     * Метод возвращает бин, заполненный с полей формы, заполняя только те поля,
     * которые не null в expectedBean
     *
     * @return возвращает бин, заполненный с формы
     */
    T getBean(T expectedBean);
}
