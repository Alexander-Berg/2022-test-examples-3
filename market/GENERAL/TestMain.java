package ru.yandex.ir.stub;

import org.apache.commons.csv.CSVRecord;
import ru.yandex.ir.common.CommonContext;
import ru.yandex.ir.common.knowledge.FeaturesExtractor;
import ru.yandex.ir.common.knowledge.FeaturesExtractorCreateException;
import ru.yandex.ir.common.serialize.FeatureExtractorManager;
import ru.yandex.ir.common.serialize.TsvHelper;
import ru.yandex.ir.common.snapshot.SnapshotHolder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nkondratyeva
 */
public class TestMain {
    public static void main(String[] args) throws IOException, FeaturesExtractorCreateException {
        String pathToConfig = args[0];
        String pathToElements = args[1];
        String pathToResult = args[2];

        Path configPath = Paths.get(pathToConfig);

        TestFeaturesKnowledge knowledge = new TestFeaturesKnowledge();
        FeatureExtractorManager<TestObject> manager = new FeatureExtractorManager<>(new CommonContext());

        FeaturesExtractor<TestObject> featuresExtractor = manager.load(configPath, knowledge, new SnapshotHolder());

        List<TestObject> elements = TsvHelper.readElements(pathToElements, TestMain::mapToObject, "ID");

        List<float[]> elementFeatures = new ArrayList<>(elements.size());

        for (TestObject element : elements) {
            List<float[]> elementResult = featuresExtractor.calculateFeatures(element);
            if (elementResult.size() != 1) {
                throw new RuntimeException();
            }
            elementFeatures.add(elementResult.get(0));
        }

        TsvHelper.writeFeatures(pathToResult, elementFeatures, featuresExtractor.getFeatureNames());
    }

    private static TestObject mapToObject(CSVRecord record) {
        long id = Long.parseLong(record.get("ID"));
        return new TestObject(id);
    }
}
