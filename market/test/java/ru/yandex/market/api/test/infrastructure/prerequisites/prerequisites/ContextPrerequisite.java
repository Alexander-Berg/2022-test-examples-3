package ru.yandex.market.api.test.infrastructure.prerequisites.prerequisites;

import ru.yandex.market.api.ContextHolderTestHelper;

public class ContextPrerequisite implements TestPrerequisite {
    @Override
    public void setUp(Object instanceOfTestClass) {
        ContextHolderTestHelper.destroyContext();
        ContextHolderTestHelper.initContext();
    }

    @Override
    public void tearDown() {
        ContextHolderTestHelper.destroyContext();
    }
}
