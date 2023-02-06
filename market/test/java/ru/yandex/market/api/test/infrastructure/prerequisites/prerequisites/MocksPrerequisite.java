package ru.yandex.market.api.test.infrastructure.prerequisites.prerequisites;

import static org.mockito.MockitoAnnotations.initMocks;

public class MocksPrerequisite implements TestPrerequisite {
    @Override
    public void setUp(Object instanceOfTestClass) {
        initMocks(instanceOfTestClass);
    }

    @Override
    public void tearDown() {

    }
}
