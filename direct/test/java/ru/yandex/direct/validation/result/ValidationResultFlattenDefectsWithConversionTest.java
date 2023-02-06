package ru.yandex.direct.validation.result;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.validation.builder.ItemValidationBuilder;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.validation.result.PathHelper.concat;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@SuppressWarnings("unchecked")
public class ValidationResultFlattenDefectsWithConversionTest {

    private static final MappingPathNodeConverter REPLACING_HUMAN_CONVERTER =
            MappingPathNodeConverter.builder("")
                    .replace(Human.BRAIN, Human.CONVERTED_BRAIN)
                    .build();

    private static final MappingPathNodeConverter REPLACING_BRAIN_CONVERTER =
            MappingPathNodeConverter.builder("")
                    .replace(Brain.NEURONS, Brain.CONVERTED_NEURONS)
                    .build();

    private static final MappingPathNodeConverter REPLACING_NEURON_CONVERTER =
            MappingPathNodeConverter.builder("")
                    .replace(Neuron.THROUGHPUT, Neuron.CONVERTED_THROUGHPUT)
                    .build();

    @Test
    public void noFailOnNullParentWhenTopLevelErrorPresents() {
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? REPLACING_BRAIN_CONVERTER : null;

        Object humanError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, humanError, null, null, null, null, null, null);

        DefectInfo<Object> expectedDefectInfo = new DefectInfo<>(path(), human, humanError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    // flattenErrors with one-to-one conversion

    @Test
    public void replaceOneToOneAndErrorOnConvertedPathNode() {
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? REPLACING_BRAIN_CONVERTER : null;

        Object neuronsError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, neuronsError, null, null, null, null);

        Path path = path(field(Human.BRAIN), field(Brain.CONVERTED_NEURONS));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain.neurons, neuronsError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void replaceOneToOneAndErrorAfterConvertedPathNode() {
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? REPLACING_BRAIN_CONVERTER : null;

        Object neuron1Error = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, null, neuron1Error, null, null, null);

        Path path = path(field(Human.BRAIN), field(Brain.CONVERTED_NEURONS), index(0));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain.neurons.get(0), neuron1Error);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void severalOneToOneReplacementsAndSeveralErrors() {
        PathNodeConverterProvider converterProvider = DefaultPathNodeConverterProvider.builder()
                .register(Brain.class, REPLACING_BRAIN_CONVERTER)
                .register(Neuron.class, REPLACING_NEURON_CONVERTER)
                .build();

        Object neuron1Error = new Object();
        Object neuron1FieldError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult = createHumanValidationResult(human,
                null, null, null, neuron1Error, null, neuron1FieldError, null);

        Path path1 = path(field(Human.BRAIN), field(Brain.CONVERTED_NEURONS), index(0));
        DefectInfo<Object> expectedDefectInfo1 =
                new DefectInfo<>(path1, human.brain.neurons.get(0), neuron1Error);

        Path path2 = concat(path1, field(Neuron.CONVERTED_THROUGHPUT));
        DefectInfo<Object> expectedDefectInfo2 =
                new DefectInfo<>(path2, human.brain.neurons.get(0).throughput, neuron1Error);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo1), beanDiffer(expectedDefectInfo2)));
    }

    // flattenErrors with one-to-many conversion

    @Test
    public void replaceOneToManyAndErrorOnConvertedPathNode() {
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .replace(Brain.NEURONS, asList(Brain.CONVERTED_NEURONS, "internalConvertedNeurons"))
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? brainConverter : null;

        Object neuronsError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, neuronsError, null, null, null, null);

        Path path = path(field(Human.BRAIN), field(Brain.CONVERTED_NEURONS), field("internalConvertedNeurons"));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain.neurons, neuronsError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void replaceOneToManyAndErrorAfterConvertedPathNode() {
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .replace(Brain.NEURONS, asList(Brain.CONVERTED_NEURONS, "internalConvertedNeurons"))
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? brainConverter : null;

        Object neuron2Error = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, null, null, neuron2Error, null, null);

        Path path = path(field(Human.BRAIN), field(Brain.CONVERTED_NEURONS),
                field("internalConvertedNeurons"), index(1));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain.neurons.get(1), neuron2Error);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    // conversion with skipping part of path

    @Test
    public void skippingInMiddleAndErrorStraightBeforeConvertedPathNode() {
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .skip(Brain.NEURONS)
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? brainConverter : null;

        Object brainError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, brainError, null, null, null, null, null);

        Path path = path(field(Human.BRAIN));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain, brainError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void skippingInMiddleAndErrorOnSkippedPathNode() {
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .skip(Brain.NEURONS)
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? brainConverter : null;

        Object neuronsError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, neuronsError, null, null, null, null);

        Path path = path(field(Human.BRAIN));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain.neurons, neuronsError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void skippingInMiddleAndErrorStraightAfterSkippedPathNode() {
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .skip(Brain.NEURONS)
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? brainConverter : null;

        Object neuron1Error = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, null, neuron1Error, null, null, null);

        Path path = path(field(Human.BRAIN), index(0));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain.neurons.get(0), neuron1Error);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void skippingInBeginningAndErrorStraightBeforeSkippedPathNode() {
        MappingPathNodeConverter humanConverter = MappingPathNodeConverter.builder("")
                .skip(Human.BRAIN)
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Human.class ? humanConverter : null;

        Object humanError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, humanError, null, null, null, null, null, null);

        Path path = path();
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human, humanError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void skippingInBeginningAndErrorOnSkippedPathNode() {
        MappingPathNodeConverter humanConverter = MappingPathNodeConverter.builder("")
                .skip(Human.BRAIN)
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Human.class ? humanConverter : null;

        Object brainError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, brainError, null, null, null, null, null);

        Path path = path();
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain, brainError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void skippingInBeginningAndErrorStraightAfterSkippedPathNode() {
        MappingPathNodeConverter humanConverter = MappingPathNodeConverter.builder("")
                .skip(Human.BRAIN)
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Human.class ? humanConverter : null;

        Object neuronsError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, neuronsError, null, null, null, null);

        Path path = path(field(Brain.NEURONS));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain.neurons, neuronsError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void severalSkippingsAndSeveralErrors() {
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .skip(Brain.NEURONS)
                .build();
        MappingPathNodeConverter neuronConverter = MappingPathNodeConverter.builder("")
                .skip(Neuron.THROUGHPUT)
                .build();
        PathNodeConverterProvider converterProvider = DefaultPathNodeConverterProvider.builder()
                .register(Brain.class, brainConverter)
                .register(Neuron.class, neuronConverter)
                .build();

        Object neuron1Error = new Object();
        Object neuron2ThroughputError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, null,
                        neuron1Error, null, null, neuron2ThroughputError);

        Path path1 = path(field(Human.BRAIN), index(0));
        DefectInfo<Object> expectedDefectInfo1 =
                new DefectInfo<>(path1, human.brain, neuron1Error);

        Path path2 = path(field(Human.BRAIN), index(1));
        DefectInfo<Object> expectedDefectInfo2 =
                new DefectInfo<>(path2, human.brain.neurons.get(1).throughput, neuron2ThroughputError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo1), beanDiffer(expectedDefectInfo2)));
    }

    // "appending list item by one item" conversion

    @Test
    public void appendingByOneItemAndErrorOnListPathNode() {
        String midItem = "midItem";
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .appendListItems(Brain.NEURONS, midItem)
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? brainConverter : null;

        Object neuronsError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, neuronsError, null, null, null, null);

        Path path = path(field(Human.BRAIN), field(Brain.NEURONS));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain.neurons, neuronsError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void appendingByOneItemAndErrorOnListItemPathNode() {
        String midItem = "midItem";
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .appendListItems(Brain.NEURONS, midItem)
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? brainConverter : null;

        Object neuron1Error = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, null, neuron1Error, null, null, null);

        Path path = path(field(Human.BRAIN), field(Brain.NEURONS), index(0), field(midItem));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain.neurons.get(0), neuron1Error);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void appendingByOneItemAndErrorAfterListItemPathNode() {
        String midItem = "midItem";
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .appendListItems(Brain.NEURONS, midItem)
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? brainConverter : null;

        Object neuron2ThroughputError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, null, null, null, null, neuron2ThroughputError);

        Path path = path(field(Human.BRAIN), field(Brain.NEURONS), index(1),
                field(midItem), field(Neuron.THROUGHPUT));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain.neurons.get(1).throughput, neuron2ThroughputError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void severalappendingsByOneItemAndSeveralErrors() {
        String midItem = "midItem";
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .appendListItems(Brain.NEURONS, midItem)
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? brainConverter : null;

        Object neuron2Error = new Object();
        Object neuron1ThroughputError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, null, null, neuron2Error, neuron1ThroughputError, null);

        Path path1 = path(field(Human.BRAIN), field(Brain.NEURONS), index(0),
                field(midItem), field(Neuron.THROUGHPUT));
        DefectInfo<Object> expectedDefectInfo1 =
                new DefectInfo<>(path1, human.brain.neurons.get(0).throughput, neuron1ThroughputError);

        Path path2 = path(field(Human.BRAIN), field(Brain.NEURONS), index(1), field(midItem));
        DefectInfo<Object> expectedDefectInfo2 =
                new DefectInfo<>(path2, human.brain.neurons.get(1), neuron2Error);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo1), beanDiffer(expectedDefectInfo2)));
    }

    // "appending list item by many items" conversion

    @Test
    public void appendingByManyItemsAndErrorOnListItemPathNode() {
        String midItem1 = "midItem";
        String midItem2 = "data";
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .appendListItems(Brain.NEURONS, asList(midItem1, midItem2))
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? brainConverter : null;

        Object neuron1Error = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, null, neuron1Error, null, null, null);

        Path path = path(field(Human.BRAIN), field(Brain.NEURONS), index(0), field(midItem1), field(midItem2));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain.neurons.get(0), neuron1Error);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    @Test
    public void appendingByManyItemsAndErrorAfterListItemPathNode() {
        String midItem1 = "midItem";
        String midItem2 = "data";
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .appendListItems(Brain.NEURONS, asList(midItem1, midItem2))
                .build();
        PathNodeConverterProvider converterProvider =
                clazz -> clazz == Brain.class ? brainConverter : null;

        Object neuron2ThroughputError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, null, null, null, null, neuron2ThroughputError);

        Path path = path(field(Human.BRAIN), field(Brain.NEURONS), index(1),
                field(midItem1), field(midItem2), field(Neuron.THROUGHPUT));
        DefectInfo<Object> expectedDefectInfo =
                new DefectInfo<>(path, human.brain.neurons.get(1).throughput, neuron2ThroughputError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo)));
    }

    // mixed conversions

    @Test
    public void replcaeOneToOneAndOneToManyAndSeveralErrors() {
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .replace(Brain.NEURONS, asList(Brain.CONVERTED_NEURONS, "internalConvertedNeurons"))
                .build();
        PathNodeConverterProvider converterProvider = DefaultPathNodeConverterProvider.builder()
                .register(Human.class, REPLACING_HUMAN_CONVERTER)
                .register(Brain.class, brainConverter)
                .build();

        Object brainError = new Object();
        Object neuron2Error = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, brainError, null, null, neuron2Error, null, null);

        Path path1 = path(field(Human.CONVERTED_BRAIN));
        DefectInfo<Object> expectedDefectInfo1 = new DefectInfo<>(path1, human.brain, brainError);
        Path path2 = path(field(Human.CONVERTED_BRAIN), field(Brain.CONVERTED_NEURONS),
                field("internalConvertedNeurons"), index(1));
        DefectInfo<Object> expectedDefectInfo2 = new DefectInfo<>(path2, human.brain.neurons.get(1), neuron2Error);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo1), beanDiffer(expectedDefectInfo2)));
    }

    @Test
    public void skippingAndOneToManyReplacementAndSeveralErrors() {
        MappingPathNodeConverter humanConverter = MappingPathNodeConverter.builder("")
                .skip(Human.BRAIN)
                .build();
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .replace(Brain.NEURONS, asList(Brain.CONVERTED_NEURONS, "internalConvertedNeurons"))
                .build();
        PathNodeConverterProvider converterProvider = DefaultPathNodeConverterProvider.builder()
                .register(Human.class, humanConverter)
                .register(Brain.class, brainConverter)
                .build();

        Object brainError = new Object();
        Object neuron2Error = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, brainError, null, null, neuron2Error, null, null);

        Path path1 = path();
        DefectInfo<Object> expectedDefectInfo1 = new DefectInfo<>(path1, human.brain, brainError);
        Path path2 = path(field(Brain.CONVERTED_NEURONS), field("internalConvertedNeurons"), index(1));
        DefectInfo<Object> expectedDefectInfo2 = new DefectInfo<>(path2, human.brain.neurons.get(1), neuron2Error);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo1), beanDiffer(expectedDefectInfo2)));
    }

    @Test
    public void appendingListItemAndOneToOneReplacementOnTheSameField() {
        String midItem = "midItem";
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .replace(Brain.NEURONS, Brain.CONVERTED_NEURONS)
                .appendListItems(Brain.NEURONS, midItem)
                .build();
        PathNodeConverterProvider converterProvider = DefaultPathNodeConverterProvider.builder()
                .register(Brain.class, brainConverter)
                .build();

        Object neuronsError = new Object();
        Object neuron2Error = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, neuronsError, null, neuron2Error, null, null);

        Path path1 = path(field(Human.BRAIN), field(Brain.CONVERTED_NEURONS));
        DefectInfo<Object> expectedDefectInfo1 = new DefectInfo<>(path1, human.brain.neurons, neuronsError);

        Path path2 = path(field(Human.BRAIN), field(Brain.CONVERTED_NEURONS), index(1), field(midItem));
        DefectInfo<Object> expectedDefectInfo2 = new DefectInfo<>(path2, human.brain.neurons.get(1), neuron2Error);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo1), beanDiffer(expectedDefectInfo2)));
    }

    @Test
    public void appendingListItemAndOneToManyReplacementOnTheSameField() {
        String midItem1 = "midItem";
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .replace(Brain.NEURONS, asList(Brain.CONVERTED_NEURONS, "data"))
                .appendListItems(Brain.NEURONS, midItem1)
                .build();
        PathNodeConverterProvider converterProvider = DefaultPathNodeConverterProvider.builder()
                .register(Brain.class, brainConverter)
                .build();

        Object neuronsError = new Object();
        Object neuron2Error = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, neuronsError, null, neuron2Error, null, null);

        Path path1 = path(field(Human.BRAIN), field(Brain.CONVERTED_NEURONS), field("data"));
        DefectInfo<Object> expectedDefectInfo1 = new DefectInfo<>(path1, human.brain.neurons, neuronsError);

        Path path2 = path(field(Human.BRAIN), field(Brain.CONVERTED_NEURONS), field("data"), index(1), field(midItem1));
        DefectInfo<Object> expectedDefectInfo2 = new DefectInfo<>(path2, human.brain.neurons.get(1), neuron2Error);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo1), beanDiffer(expectedDefectInfo2)));
    }

    @Test
    public void appendingListItemAndSkippingReplacementOnTheSameField() {
        String midItem = "midItem";
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .skip(Brain.NEURONS)
                .appendListItems(Brain.NEURONS, midItem)
                .build();
        PathNodeConverterProvider converterProvider = DefaultPathNodeConverterProvider.builder()
                .register(Brain.class, brainConverter)
                .build();

        Object neuronsError = new Object();
        Object neuron1Error = new Object();
        Object neuron2ThroughputError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult = createHumanValidationResult(human, null, null,
                neuronsError, neuron1Error, null, null, neuron2ThroughputError);

        Path path1 = path(field(Human.BRAIN));
        DefectInfo<Object> expectedDefectInfo1 = new DefectInfo<>(path1, human.brain.neurons, neuronsError);

        Path path2 = path(field(Human.BRAIN), index(0), field(midItem));
        DefectInfo<Object> expectedDefectInfo2 = new DefectInfo<>(path2, human.brain.neurons.get(0), neuron1Error);

        Path path3 = path(field(Human.BRAIN), index(1), field(midItem), field(Neuron.THROUGHPUT));
        DefectInfo<Object> expectedDefectInfo3 =
                new DefectInfo<>(path3, human.brain.neurons.get(1).throughput, neuron2ThroughputError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(
                beanDiffer(expectedDefectInfo1),
                beanDiffer(expectedDefectInfo2),
                beanDiffer(expectedDefectInfo3)));
    }

    @Test
    public void appendingListItemAndSeveralOneToOneReplacementOnDifferentFields() {
        String midItem = "midItem";
        MappingPathNodeConverter humanConverter = MappingPathNodeConverter.builder("")
                .replace(Human.BRAIN, Human.CONVERTED_BRAIN)
                .build();
        MappingPathNodeConverter brainConverter = MappingPathNodeConverter.builder("")
                .appendListItems(Brain.NEURONS, midItem)
                .build();
        MappingPathNodeConverter neuronConverter = MappingPathNodeConverter.builder("")
                .replace(Neuron.THROUGHPUT, Neuron.CONVERTED_THROUGHPUT)
                .build();
        PathNodeConverterProvider converterProvider = DefaultPathNodeConverterProvider.builder()
                .register(Human.class, humanConverter)
                .register(Brain.class, brainConverter)
                .register(Neuron.class, neuronConverter)
                .build();

        Object neuron1Error = new Object();
        Object neuron2ThroughputError = new Object();
        Human human = createHuman();
        ValidationResult<Human, Object> validationResult =
                createHumanValidationResult(human, null, null, null, neuron1Error, null, null, neuron2ThroughputError);

        Path path1 = path(field(Human.CONVERTED_BRAIN), field(Brain.NEURONS), index(0), field(midItem));
        DefectInfo<Object> expectedDefectInfo1 = new DefectInfo<>(path1, human.brain.neurons.get(0), neuron1Error);

        Path path2 = path(field(Human.CONVERTED_BRAIN), field(Brain.NEURONS), index(1),
                field(midItem), field(Neuron.CONVERTED_THROUGHPUT));
        DefectInfo<Object> expectedDefectInfo2 =
                new DefectInfo<>(path2, human.brain.neurons.get(1).throughput, neuron2ThroughputError);

        List<DefectInfo<Object>> defectInfoList = validationResult.flattenErrors(converterProvider);
        assertThat(defectInfoList, contains(beanDiffer(expectedDefectInfo1), beanDiffer(expectedDefectInfo2)));
    }

    private ValidationResult<Human, Object> createHumanValidationResult(Human human,
                                                                        Object humanError, Object brainError, Object neuronsError,
                                                                        Object firstNeuronError, Object secondNeuronError,
                                                                        Object firstNeuronFieldError, Object secondNeuronFieldError) {
        ItemValidationBuilder<Human, Object> humanIvb = ItemValidationBuilder.of(human);
        humanIvb.check(h -> humanError);
        humanIvb.item(human.brain, Human.BRAIN)
                .check(b -> brainError)
                .checkBy(brain -> {
                    ItemValidationBuilder<Brain, Object> brainIvb = ItemValidationBuilder.of(brain);
                    brainIvb.list(brain.neurons, Brain.NEURONS)
                            .check(nList -> neuronsError)
                            .checkEachBy((i, neuron) -> {
                                ItemValidationBuilder<Neuron, Object> neuronIvb =
                                        ItemValidationBuilder.of(neuron);
                                neuronIvb.check(n -> i == 0 ? firstNeuronError : secondNeuronError);
                                neuronIvb.item(neuron.throughput, Neuron.THROUGHPUT)
                                        .check(nf -> i == 0 ? firstNeuronFieldError : secondNeuronFieldError);
                                return neuronIvb.getResult();
                            });
                    return brainIvb.getResult();
                });
        return humanIvb.getResult();
    }

    private Human createHuman() {
        return new Human()
                .withBrain(new Brain()
                        .withNeurons(
                                asList(
                                        new Neuron().withThroughput(1),
                                        new Neuron().withThroughput(2))));
    }

    private static class Human {
        public static final String BRAIN = "brain";
        public static final String CONVERTED_BRAIN = "convertedBrain";
        private Brain brain;

        public Human withBrain(Brain brain) {
            this.brain = brain;
            return this;
        }
    }

    private static class Brain {
        public static final String NEURONS = "neurons";
        public static final String CONVERTED_NEURONS = "convertedNeurons";
        private List<Neuron> neurons;

        public Brain withNeurons(List<Neuron> neurons) {
            this.neurons = neurons;
            return this;
        }
    }

    private static class Neuron {
        public static final String THROUGHPUT = "throughput";
        public static final String CONVERTED_THROUGHPUT = "convertedThroughput";
        private int throughput;

        public Neuron withThroughput(int throughput) {
            this.throughput = throughput;
            return this;
        }
    }
}
