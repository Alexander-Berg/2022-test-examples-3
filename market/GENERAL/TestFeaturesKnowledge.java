package ru.yandex.ir.stub;

import ru.yandex.ir.common.knowledge.FeaturesKnowledge;
import ru.yandex.ir.common.knowledge.NoConfigFeaturesFactory;

/**
 * @author nkondratyeva
 */
public class TestFeaturesKnowledge extends FeaturesKnowledge<TestObject> {
    private NoConfigFeaturesFactory<TestObject> testFeaturesFactory;

    public TestFeaturesKnowledge() {
        testFeaturesFactory = new NoConfigFeaturesFactory<>(TestExtractor.class, "test_extractor");
        addFactory(testFeaturesFactory);
    }

    public NoConfigFeaturesFactory<TestObject> getTestFeaturesFactory() {
        return testFeaturesFactory;
    }
}
