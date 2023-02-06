package ru.yandex.market.api.test.infrastructure.prerequisites.prerequisites;

public interface TestPrerequisite {
    void setUp(Object instanceOfTestClass);

    void tearDown();
}
