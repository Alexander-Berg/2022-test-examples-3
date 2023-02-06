package ru.yandex.market.mbo.gwt.models.modelstorage.assertions;

import com.google.common.primitives.Longs;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.internal.Iterables;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public abstract class BaseParameterValuesAssertion<SELF extends BaseParameterValuesAssertion<SELF>>
    extends AbstractObjectAssert<SELF, ParameterValues> {
    protected Iterables iterables = Iterables.instance();

    public BaseParameterValuesAssertion(ParameterValues actual, Class<?> selfType) {
        super(actual, selfType);
    }

    @Nonnull
    protected abstract String createSubFailMessage();

    public SELF exists() {
        if (actual == null) {
            failWithMessage("Expected parameterValues exist for " + createSubFailMessage());
        }

        return myself;
    }

    public SELF notExists() {
        if (actual != null) {
            failWithMessage("Expected parameterValues not exist for " + createSubFailMessage()
                + " Actual: " + actual);
        }
        return myself;
    }

    public SELF isEmpty() {
        exists();

        if (!actual.isEmpty()) {
            failWithMessage("Expected parameterValues to be empty. Actual: " + actual);
        }

        return myself;
    }

    public SELF notEmpty() {
        exists();

        if (actual.isEmpty()) {
            failWithMessage("Expected parameterValues is be not empty. Actual: " + actual);
        }

        return myself;
    }

    public SELF type(Param.Type type) {
        exists();

        if (actual.getType() != type) {
            failWithMessage("Expecting parameterValues to be '" + type + "' type.\n" +
                "Expecting:\n   " + type + "\n" +
                "Actual:\n   " + actual.getType() + "\n" +
                "Parameter values:\n   " + actual);
        }
        return myself;
    }

    public SELF types(Param.Type... types) {
        exists();

        if (!Arrays.asList(types).contains(actual.getType())) {
            failWithMessage("Expecting parameterValues to be one of '" + types + "' types.\n" +
                "Expecting:\n   " + Arrays.toString(types) + "\n" +
                "Actual:\n   " + actual.getType() + "\n" +
                "Parameter values:\n   " + actual);
        }
        return myself;
    }

    public SELF isSingle() {
        exists();

        List<ParameterValue> valuesList = actual.getValues();
        if (valuesList.size() != 1) {
            failWithMessage("Expected single value for " + actual);
        }

        return myself;
    }

    public ParameterValueAssertion getSingle() {
        isSingle();

        List<ParameterValue> valuesList = actual.getValues();

        return new ParameterValueAssertion(valuesList.get(0));
    }

    public SELF valuesWithTypeRecognition(Object... objects) {
        if (objects.length == 0) {
            return myself;
        }
        if (objects[0] instanceof Boolean) {
            return values(true, Arrays.stream(objects).toArray(Boolean[]::new));
        }
        if (objects[0] instanceof Long) {
            return values(true, Arrays.stream(objects).toArray(Long[]::new));
        }
        if (objects[0] instanceof Option) {
            return values(Arrays.stream(objects).toArray(Option[]::new));
        }
        if (objects[0] instanceof String) {
            return values(true, Arrays.stream(objects).toArray(String[]::new));
        }
        if (objects[0] instanceof Double) {
            return values(true, Arrays.stream(objects).toArray(Double[]::new));
        }
        if (objects[0] instanceof Integer) {
            int[] numbers = new int[objects.length];
            for (int i = 0; i < objects.length; i++) {
                numbers[i] = (int) objects[i];
            }
            return values(true, numbers);
        }
        throw new RuntimeException("Объект типа " + objects[0].getClass() + " не может быть обработан.");
    }

    public SELF values(Boolean... bool) {
        return values(true, bool);
    }

    public SELF valuesInAnyOrder(Boolean... bool) {
        return values(false, bool);
    }

    public SELF values(Long... optionIds) {
        return values(true, optionIds);
    }

    public SELF valuesInAnyOrder(Long... optionIds) {
        return values(false, optionIds);
    }

    public SELF values(Option... options) {
        return values(Arrays.stream(options).map(Option::getId).toArray(Long[]::new));
    }

    public SELF valuesInAnyOrder(Option... options) {
        return valuesInAnyOrder(Arrays.stream(options).map(Option::getId).toArray(Long[]::new));
    }

    public SELF values(String... strings) {
        return values(true, strings);
    }

    public SELF valuesInAnyOrder(String... strings) {
        return values(false, strings);
    }

    public SELF values(int... numbers) {
        return values(true, numbers);
    }

    public SELF valuesInAnyOrder(int... numbers) {
        return values(false, numbers);
    }

    public SELF values(Double... numbers) {
        return values(true, numbers);
    }

    public SELF values(BigDecimal... numbers) {
        Double[] values = Arrays.stream(numbers).map(BigDecimal::doubleValue).toArray(Double[]::new);
        return values(true, values);
    }

    public SELF valuesInAnyOrder(Double... numbers) {
        return values(false, numbers);
    }

    private SELF values(boolean ordered, Boolean... bool) {
        type(Param.Type.BOOLEAN);

        if (ordered) {
            iterables.assertContainsExactly(info, actual.getBooleanValues(), bool);
        } else {
            iterables.assertContainsExactlyInAnyOrder(info, actual.getBooleanValues(), bool);
        }
        if (actual.getOptionIds().stream().noneMatch(id -> id != null && id > 0)) {
            failWithMessage("Boolean values must also contain option ids. Actual: " + actual);
        }
        if (actual.getNumericValues().stream().anyMatch(Objects::nonNull)) {
            failWithMessage("Boolean values must NOT contain number values. Actual: " + actual);
        }
        if (actual.getStringValues().stream().anyMatch(Objects::nonNull)) {
            failWithMessage("Boolean values must NOT contain string values. Actual: " + actual);
        }
        return myself;
    }

    private SELF values(boolean ordered, Long... optionIds) {
        types(Param.Type.NUMERIC_ENUM, Param.Type.ENUM, Param.Type.BOOLEAN);

        if (ordered) {
            iterables.assertContainsExactly(info, actual.getOptionIds(), optionIds);
        } else {
            iterables.assertContainsExactlyInAnyOrder(info, actual.getOptionIds(), optionIds);
        }
        if (actual.getNumericValues().stream().anyMatch(Objects::nonNull)) {
            failWithMessage("OptionId values must NOT contain number values. Actual: " + actual);
        }
        if (actual.getStringValues().stream().anyMatch(Objects::nonNull)) {
            failWithMessage("OptionId values must NOT contain string values. Actual: " + actual);
        }
        return myself;
    }

    private SELF values(boolean ordered, String... strings) {
        type(Param.Type.STRING);

        List<String> words = actual.getStringValues().stream().map(Word::getWord).collect(Collectors.toList());
        if (ordered) {
            iterables.assertContainsExactly(info, words, strings);
        } else {
            iterables.assertContainsExactlyInAnyOrder(info, words, strings);
        }

        if (actual.getOptionIds().stream().anyMatch(Objects::nonNull)) {
            failWithMessage("String values must NOT contain option ids. Actual: " + actual);
        }
        if (actual.getNumericValues().stream().anyMatch(Objects::nonNull)) {
            failWithMessage("String values must NOT contain number values. Actual: " + actual);
        }
        if (actual.getBooleanValues().stream().anyMatch(Objects::nonNull)) {
            failWithMessage("String values must NOT contain boolean values. Actual: " + actual);
        }
        return myself;
    }

    private SELF values(boolean ordered, Double... numbers) {
        exists();
        type(Param.Type.NUMERIC);

        List<Double> doubles = actual.getNumericValues().stream()
            .map(BigDecimal::doubleValue)
            .collect(Collectors.toList());
        if (ordered) {
            iterables.assertContainsExactly(info, doubles, numbers);
        } else {
            iterables.assertContainsExactlyInAnyOrder(info, doubles, numbers);
        }

        if (actual.getOptionIds().stream().anyMatch(Objects::nonNull)) {
            failWithMessage("Number values must NOT contain option ids. Actual: " + actual);
        }
        if (actual.getBooleanValues().stream().anyMatch(Objects::nonNull)) {
            failWithMessage("Number values must NOT contain boolean values. Actual: " + actual);
        }
        if (actual.getStringValues().stream().anyMatch(Objects::nonNull)) {
            failWithMessage("Number values must NOT contain string values. Actual: " + actual);
        }
        return myself;
    }

    private SELF values(boolean ordered, int... numbers) {
        Double[] values = new Double[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            values[i] = (double) numbers[i];
        }
        return values(ordered, values);
    }

    public SELF modificationSource(ModificationSource... sources) {
        exists();

        List<ModificationSource> actualSources = actual.getValues().stream()
            .map(ParameterValue::getModificationSource)
            .collect(Collectors.toList());

        iterables.assertContainsExactly(info, actualSources, sources);
        return myself;
    }

    public SELF modificationUserId(long... changeUids) {
        exists();

        List<Long> modificationUids = actual.getValues().stream()
            .map(ParameterValue::getLastModificationUid)
            .collect(Collectors.toList());

        iterables.assertContainsExactly(info, modificationUids, Longs.asList(changeUids).toArray(new Long[0]));
        return myself;
    }

    public SELF modificationDate(Date... inputDates) {
        exists();

        List<Date> modificationDates = actual.getValues().stream()
            .map(ParameterValue::getLastModificationDate)
            .collect(Collectors.toList());

        iterables.assertContainsExactly(info, modificationDates, inputDates);

        return myself;
    }
}
