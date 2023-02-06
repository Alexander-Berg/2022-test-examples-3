package ru.yandex.autotests.direct.web.util.testinterfaces;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public interface ITest {
    void generateTestData();

    void comeToEntryPoint();

    void clearTestData();
}