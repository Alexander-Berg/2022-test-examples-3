package ru.yandex.market.mbo.gwt.models.modelstorage;

import com.google.common.base.Objects;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.export.modelstorage.utils.ModelStorageParameterValueUtils;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue.ValueBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.CollectionsTools;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.randomizers.ParameterValueRandomizer;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ParameterValueValueEqualsTest {
    private static final long RANDOM_SEED = 14121993;
    private static final int META_SIZE = 500;

    private EnhancedRandom random;
    private ParameterValueRandomizer randomizer;

    @Before
    @SuppressWarnings("checkstyle:magicNumber")
    public void setUp() throws Exception {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(RANDOM_SEED)
            .stringLengthRange(3, 10)
            .collectionSizeRange(1, 5)
            .build();

        randomizer = new ParameterValueRandomizer(random, META_SIZE, true, false);
    }

    @Test
    public void testValueEqualsAndValueHashCodeWorksCorrectly() {
        for (int i = 0; i < 100; i++) {
            ParameterValue parameterValue = randomizer.getRandomValue();
            ParameterValue copy = new ParameterValue(parameterValue);
            ParameterValue notEqualValue = createNotEqualValue(parameterValue);

            String str1 = parameterValue.toString();
            String str2 = copy.toString();
            String str3 = notEqualValue.toString();
            int valueHashCode1 = parameterValue.valueHashCode();
            int valueHashCode2 = copy.valueHashCode();
            int valueHashCode3 = notEqualValue.valueHashCode();

            String compareErrorStr = String.format("value1:\n%s\nvalueHashCode1: %d\n" +
                "value2:\n%s\nvalueHashCode2: %d", str1, valueHashCode1, str2, valueHashCode2);
            String compareError3Str = String.format("value1:\n%s\nvalueHashCode1: %d\n" +
                "value3:\n%s\nvalueHashCode3: %d", str1, valueHashCode1, str3, valueHashCode3);

            Assert.assertTrue("Values expected to be equal by value equals\n" + compareErrorStr,
                parameterValue.valueEquals(copy));
            Assert.assertTrue("Values expected to be have equal valueHashCode\n" + compareErrorStr,
                valueHashCode1 == valueHashCode2);
            Assert.assertFalse("Values DON'T expected to be equal by value equals\n" + compareError3Str,
                parameterValue.valueEquals(notEqualValue));
            Assert.assertFalse("Values DON'T expected to be have equal valueHashCode\n" + compareError3Str,
                valueHashCode1 == valueHashCode3);
        }
    }

    @Test
    public void testValueEqualsAndValueHashCodeWorksCorrectlyInProtoObjects() {
        for (int i = 0; i < 1000; i++) {
            ParameterValue pojo = randomizer.getRandomValue();
            ModelStorage.ParameterValue parameterValue = ModelProtoConverter.convert(pojo);
            ModelStorage.ParameterValue notEqualValue = ModelProtoConverter.convert(createNotEqualValue(pojo));
            ModelStorage.ParameterValue copy = ModelStorage.ParameterValue.newBuilder(parameterValue).build();

            String str1 = parameterValue.toString();
            String str2 = copy.toString();
            String str3 = notEqualValue.toString();
            int valueHashCode1 = ModelStorageParameterValueUtils.valueHashCode(parameterValue);
            int valueHashCode2 = ModelStorageParameterValueUtils.valueHashCode(copy);
            int valueHashCode3 = ModelStorageParameterValueUtils.valueHashCode(notEqualValue);

            String compareErrorStr = String.format("value1:\n%svalueHashCode1: %d\n\n" +
                "value2:\n%svalueHashCode2: %d", str1, valueHashCode1, str2, valueHashCode2);
            String compareError3Str = String.format("value1:\n%svalueHashCode1: %d\n\n" +
                "value3:\n%svalueHashCode3: %d", str1, valueHashCode1, str3, valueHashCode3);

            Assert.assertTrue("Values expected to be equal by value equals\n" + compareErrorStr,
                ModelStorageParameterValueUtils.valueEquals(parameterValue, copy));
            Assert.assertTrue("Values expected to be have equal valueHashCode\n" + compareErrorStr,
                valueHashCode1 == valueHashCode2);
            Assert.assertFalse("Values DON'T expected to be equal by value equals\n" + compareError3Str,
                ModelStorageParameterValueUtils.valueEquals(parameterValue, notEqualValue));
            Assert.assertFalse("Values DON'T expected to be have equal valueHashCode\n" + compareError3Str,
                valueHashCode1 == valueHashCode3);
        }
    }

    @Test
    public void valueEqualsConsidersWordsWithoutTakingWordIdIntoConsideration() {
        ParameterValue param1 =
            new ParameterValue(1, "xsl_name", Param.Type.STRING,
                               new ValueBuilder().setStringValue(new Word(0, Language.RUSSIAN.getId(), "Значение")));
        ParameterValue param2 =
            new ParameterValue(1, "xsl_name", Param.Type.STRING,
                               new ValueBuilder().setStringValue(new Word(1, Language.RUSSIAN.getId(), "Значение")));
        Assert.assertTrue(param1.valueEquals(param2));
    }

    @Test
    public void valueEqualsConsidersWordsWithTakingOrderIntoConsideration() {
        ParameterValue param1 =
            new ParameterValue(1, "xsl_name", Param.Type.STRING,
                               new ValueBuilder().setStringValue(
                                   new Word(0, Language.RUSSIAN.getId(), "Значение1"),
                                   new Word(1, Language.RUSSIAN.getId(), "Значение2")
                               )
            );
        ParameterValue param2 =
            new ParameterValue(1, "xsl_name", Param.Type.STRING,
                               new ValueBuilder().setStringValue(
                                   new Word(1, Language.RUSSIAN.getId(), "Значение2"),
                                   new Word(0, Language.RUSSIAN.getId(), "Значение1")
                               )
            );
        Assert.assertFalse(param1.valueEquals(param2));
    }

    private ParameterValue createNotEqualValue(ParameterValue from) {
        ParameterValue result = new ParameterValue(from);
        switch (result.getType()) {
            case BOOLEAN:
                final List<Boolean> boolValues = Arrays.asList(null, true, false);
                while (Objects.equal(result.getBooleanValue(), from.getBooleanValue())) {
                    result.setBooleanValue(boolValues.get(random.nextInt(boolValues.size())));
                }
                break;
            case ENUM:
            case NUMERIC_ENUM:
                final List<Long> optionIds = Arrays.asList(null, 1L, 2L, 3L, 4L, 5L);
                while (optionIdValueEquals(result.getOptionId(), from.getOptionId())) {
                    result.setOptionId(optionIds.get(random.nextInt(optionIds.size())));
                }
                break;
            case NUMERIC:
                final List<BigDecimal> numbers = Arrays.asList(null, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN);
                while (Objects.equal(result.getNumericValue(), from.getNumericValue())) {
                    result.setNumericValue(numbers.get(random.nextInt(numbers.size())));
                }
                break;
            case STRING:
                while (stringValueEquals(result.getStringValue(), from.getStringValue())) {
                    if (random.nextBoolean()) {
                        result.setStringValue(null);
                    } else {
                        result.setStringValue(IntStream.range(0, random.nextInt(5))
                            .mapToObj(i -> random.nextObject(String.class))
                            .map(WordUtil::defaultWord)
                            .collect(Collectors.toList()));
                    }
                }
                break;
            case HYPOTHESIS:
                while (stringValueEquals(result.getHypothesisValue(), from.getHypothesisValue())) {
                    if (random.nextBoolean()) {
                        result.setHypothesisValue(null);
                    } else {
                        result.setHypothesisValue(IntStream.range(0, random.nextInt(5))
                            .mapToObj(i -> random.nextObject(String.class))
                            .map(WordUtil::defaultWord)
                            .collect(Collectors.toList()));
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unsupported type: " + result.getType());
        }
        return result;
    }

    private boolean stringValueEquals(List<Word> words1, List<Word> words2) {
        return (CollectionsTools.isEmpty(words1) && CollectionsTools.isEmpty(words2)) ||
            Objects.equal(words1, words2);
    }

    private boolean optionIdValueEquals(Long optionId1, Long optionId2) {
        // для optionId 0 и null считаются равными, так как 0 является NullFakeOption.FAKE_OPTION_ID
        if (optionId1 == null) {
            optionId1 = 0L;
        }
        if (optionId2 == null) {
            optionId2 = 0L;
        }
        return Objects.equal(optionId1, optionId2);
    }
}
