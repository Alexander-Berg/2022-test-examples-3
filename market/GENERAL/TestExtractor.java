package ru.yandex.ir.stub;

import ru.yandex.ir.common.features.FeatureInfo;
import ru.yandex.ir.common.knowledge.FeaturesExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nkondratyeva
 */
public class TestExtractor implements FeaturesExtractor<TestObject> {
    private static final FeatureInfo[] FEATURE_INFO_LIST = {
        new FeatureInfo("one"),
        new FeatureInfo("two")
    };

    @Override
    public List<float[]> calculateFeatures(TestObject element) {
        ArrayList<float[]> result = new ArrayList<>();
        for (Object o : element.getDocuments()) {
            result.add(new float[]{1, 2});
        }
        return result;
    }

    @Override
    public FeatureInfo[] getFeatureInfoList() {
        return FEATURE_INFO_LIST;
    }
}
