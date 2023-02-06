package ru.yandex.market.mbo.randomizers;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author s-ermakov
 */
public class ParameterValueRandomizer implements Randomizer<ParameterValue> {
    private static final int META_MAP_SIZE = 100;
    private static final int MAX_STRING_VALUES_AMOUT = 5;
    private static final int MAX_HYPOTHESIS_VALUES_AMOUT = 5;
    private static final int SHORT_VALUES_BITS = 18;
    private static final int SHORT_VALUES_MAX = 1 << SHORT_VALUES_BITS;

    public static final Comparator<ParameterValues> PARAMETER_VALUES_COMPARATOR =
        (o1, o2) -> o1.unorderedValueEquals(o2) ? 0 : 1;

    private final EnhancedRandom enhancedRandom;
    private final boolean createNullValues;
    private final List<ParamValueMeta> paramValuesMetaList;
    private final boolean shortValues;

    public ParameterValueRandomizer(EnhancedRandom enhancedRandom) {
        this(enhancedRandom, META_MAP_SIZE, false, false);
    }

    public ParameterValueRandomizer(EnhancedRandom enhancedRandom, int metaSize, boolean createNullValues,
                                    boolean shortValues) {
        this.enhancedRandom = enhancedRandom;
        this.createNullValues = createNullValues;
        this.shortValues = shortValues;
        this.paramValuesMetaList = generateParamMetas(enhancedRandom, metaSize);
    }

    @Override
    public ParameterValue getRandomValue() {
        int index = enhancedRandom.nextInt(paramValuesMetaList.size());
        ParamValueMeta meta = paramValuesMetaList.get(index);
        return createRandomValue(meta);
    }

    public ParameterValue getRandomValue(Param.Type ofType) {
        List<ParamValueMeta> metas = paramValuesMetaList.stream()
            .filter(meta -> meta.type == ofType)
            .collect(Collectors.toList());
        int index = enhancedRandom.nextInt(metas.size());
        ParamValueMeta meta = metas.get(index);
        return createRandomValue(meta);
    }

    @SuppressWarnings("checkstyle:magicNumber")
    private ParameterValue createRandomValue(ParamValueMeta meta) {
        ParameterValue parameterValue = enhancedRandom.nextObject(ParameterValue.class,
            "paramId", "xslName", "type",
            "numericValue", "booleanValue", "optionId", "stringValue", "hypothesisValue");

        parameterValue.setParamId(meta.paramId);
        parameterValue.setXslName(meta.xslName);
        parameterValue.setType(meta.type);

        if (createNullValues && enhancedRandom.nextBoolean()) {
            return parameterValue;
        }

        switch (parameterValue.getType()) {
            case BOOLEAN:
                boolean boolValue = enhancedRandom.nextBoolean();
                parameterValue.setBooleanValue(boolValue);
                parameterValue.setOptionId(boolValue ? 1L : 0L);
                break;
            case NUMERIC_ENUM:
            case ENUM:
                long optionIdValue = enhancedRandom.nextInt(5);
                parameterValue.setOptionId(optionIdValue);
                break;
            case NUMERIC:
                int randomValue = enhancedRandom.nextInt(5);
                BigDecimal numberValue = new BigDecimal(String.valueOf(randomValue));
                parameterValue.setNumericValue(numberValue);
                break;
            case STRING:
                List<Word> wordValues = generateWords(MAX_STRING_VALUES_AMOUT);
                parameterValue.setStringValue(wordValues);
                break;
            case HYPOTHESIS:
                List<Word> hypothesisValues = generateWords(MAX_HYPOTHESIS_VALUES_AMOUT);
                parameterValue.setHypothesisValue(hypothesisValues);
                break;
            default:
                throw new IllegalArgumentException("Unsupported type: " + parameterValue.getType());
        }

        return parameterValue;
    }

    private List<Word> generateWords(int maxCount) {
        int count = enhancedRandom.nextInt(maxCount);
        return enhancedRandom.objects(String.class, count)
            .map(WordUtil::defaultWord)
            .collect(Collectors.toList());
    };

    private List<ParamValueMeta> generateParamMetas(EnhancedRandom enhancedRandom, int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> {
                long paramId = shortValues ? enhancedRandom.nextInt(SHORT_VALUES_MAX) : enhancedRandom.nextLong();
                String xslName = enhancedRandom.nextObject(String.class);
                Param.Type type = enhancedRandom.nextObject(Param.Type.class);
                return new ParamValueMeta(paramId, xslName, type);
            })
            .collect(Collectors.toList());
    }

    private static class ParamValueMeta {
        private long paramId;
        private String xslName;
        private Param.Type type;

        ParamValueMeta(long paramId, String xslName, Param.Type type) {
            this.paramId = paramId;
            this.xslName = xslName;
            this.type = type;
        }
    }
}
