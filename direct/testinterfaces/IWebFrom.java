package ru.yandex.autotests.direct.web.util.testinterfaces;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public interface IWebFrom<T> {
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
    void checkParameters(T expectedBean);

    /**
     * Метод возвращает бин, заполненный с полей формы, заполняя только те поля,
     * которые не null в expectedBean
     *
     * @param expectedBean ожидаемый бин
     * @return возвращает бин, заполненный с формы
     */
    T getFormFieldsAccording(T expectedBean);
}
