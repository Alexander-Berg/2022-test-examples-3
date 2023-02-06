package ru.yandex.market.mbo.gwt.models.rules;

import com.google.common.collect.Lists;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.PickerImage;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.math.BigDecimal;
import java.util.Date;
import java.util.function.Consumer;

/**
 * @author s-ermakov
 */
public class ParameterValueBuilder<T> {
    private CommonModelBuilder<T> modelBuilder;
    private Consumer<ParameterValue> endModelConsumer;
    private ParameterValue parameterValue = new ParameterValue();

    private ParameterValueBuilder() {
    }

    public static ParameterValueBuilder<ParameterValueBuilder<?>> newBuilder() {
        return new ParameterValueBuilder<>();
    }

    public static <T> ParameterValueBuilder<T> newBuilder(CommonModelBuilder<T> modelBuilder,
                                                          Consumer<ParameterValue> endModelConsumer) {
        ParameterValueBuilder<T> builder = new ParameterValueBuilder<>();
        builder.modelBuilder = modelBuilder;
        builder.endModelConsumer = endModelConsumer;
        return builder;
    }

    public ParameterValueBuilder<T> paramId(long id) {
        parameterValue.setParamId(id);
        return this;
    }

    public ParameterValueBuilder<T> xslName(String xslname) {
        parameterValue.setXslName(xslname);
        return this;
    }

    public ParameterValueBuilder<T> type(Param.Type type) {
        parameterValue.setType(type);
        return this;
    }

    public ParameterValueBuilder<T> optionId(long optionId) {
        parameterValue.setType(Param.Type.ENUM);
        parameterValue.setOptionId(optionId);
        return this;
    }

    public ParameterValueBuilder<T> numericOptionId(long optionId) {
        parameterValue.setType(Param.Type.NUMERIC_ENUM);
        parameterValue.setOptionId(optionId);
        return this;
    }

    public ParameterValueBuilder<T> booleanValue(Boolean value, long optionId) {
        parameterValue.setType(Param.Type.BOOLEAN);
        parameterValue.setBooleanValue(value);
        parameterValue.setOptionId(optionId);
        return this;
    }

    public ParameterValueBuilder<T> words(String... words) {
        parameterValue.setType(Param.Type.STRING);
        parameterValue.setStringValue(WordUtil.defaultWords(words));
        return this;
    }

    public ParameterValueBuilder<T> words(Word... words) {
        parameterValue.setType(Param.Type.STRING);
        parameterValue.setStringValue(Lists.newArrayList(words));
        return this;
    }

    public ParameterValueBuilder<T> num(long num) {
        parameterValue.setType(Param.Type.NUMERIC);
        parameterValue.setNumericValue(new BigDecimal(num));
        return this;
    }

    public ParameterValueBuilder<T> modificationSource(ModificationSource source) {
        parameterValue.setModificationSource(source);
        return this;
    }

    public ParameterValueBuilder<T> ruleModificationId(long id) {
        parameterValue.setRuleModificationId(id);
        return this;
    }

    public ParameterValueBuilder<T> lastModificationDate(Date date) {
        parameterValue.setLastModificationDate(date);
        return this;
    }

    public ParameterValueBuilder<T> lastModificationUid(long uid) {
        parameterValue.setLastModificationUid(uid);
        return this;
    }

    public ParameterValueBuilder<T> pickerImage(PickerImage pickerImage) {
        parameterValue.setPickerImage(pickerImage);
        return this;
    }

    public ParameterValueBuilder<T> pickerImage(String url) {
        PickerImage pickerImage = new PickerImage(url, "", "", "", "");
        parameterValue.setPickerImage(pickerImage);
        return this;
    }

    public ParameterValueBuilder<T> pickerImageSource(ModificationSource source) {
        parameterValue.setPickerModificationSource(source);
        return this;
    }

    public ParameterValueBuilder<T> hypothesis(String... hypothesis) {
        parameterValue.setHypothesisValue(WordUtil.defaultWords(hypothesis));
        return this;
    }

    public ParameterValueBuilder<T> hypothesis(Word... hypothesis) {
        parameterValue.setHypothesisValue(Lists.newArrayList(hypothesis));
        return this;
    }

    public ParameterValue build() {
        return parameterValue;
    }

    // dsl methods

    public CommonModelBuilder<T> endParameterValue() {
        endModelConsumer.accept(build());
        return modelBuilder;
    }
}
