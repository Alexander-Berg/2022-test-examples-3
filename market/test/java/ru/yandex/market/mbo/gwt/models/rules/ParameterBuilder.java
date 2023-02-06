package ru.yandex.market.mbo.gwt.models.rules;

import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.EnumAlias;
import ru.yandex.market.mbo.gwt.models.params.GuruType;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.SubType;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Function;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 19.04.2018
 */
public class ParameterBuilder<T> {
    private final Function<CategoryParam, T> endCallback;
    private final Parameter param = new Parameter();
    private long id = 1L;

    ParameterBuilder(Function<CategoryParam, T> endCallback) {
        this.endCallback = endCallback;
    }

    public static <T> ParameterBuilder<T> builder(Function<CategoryParam, T> endCallback) {
        return new ParameterBuilder<>(endCallback);
    }

    public static ParameterBuilder<CategoryParam> builder() {
        return new ParameterBuilder<>(Function.identity());
    }

    public ParameterBuilder<T> id(long id) {
        param.setId(id);
        return this;
    }

    public ParameterBuilder<T> xsl(String xslName) {
        param.setXslName(xslName);
        return this;
    }

    public ParameterBuilder<T> xslAndName(String xslName) {
        this.xsl(xslName);
        return this.name(xslName);
    }

    public ParameterBuilder<T> name(String name) {
        param.addName(new Word(Word.DEFAULT_LANG_ID, name));
        return this;
    }

    public ParameterBuilder<T> type(Param.Type paramType) {
        param.setType(paramType);
        return this;
    }

    public ParameterBuilder<T> subType(SubType subType) {
        param.setSubtype(subType);
        return this;
    }

    public ParameterBuilder<T> level(CategoryParam.Level level) {
        param.setLevel(level);
        return this;
    }

    public ParameterBuilder<T> guruType(GuruType guruType) {
        param.setGuruType(guruType);
        return this;
    }

    public ParameterBuilder<T> hidden(boolean hidden) {
        param.setHidden(hidden);
        return this;
    }

    public ParameterBuilder<T> creation(boolean creation) {
        param.setCreationParam(creation);
        return this;
    }

    public ParameterBuilder<T> forImages(boolean forImages) {
        param.setUseForImages(forImages);
        return this;
    }

    public ParameterBuilder<T> option(long optionId, String optionName) {
        param.addOption(new OptionImpl(optionId, optionName));
        return this;
    }

    public ParameterBuilder<T> option(long optionId, String optionName, Collection<String> aliases) {
        OptionImpl option = new OptionImpl(optionId, optionName);
        for (String alias : aliases) {
            option.addAlias(new EnumAlias(-1, EnumAlias.DEFAULT_LANG_ID, alias));
        }
        param.addOption(option);
        return this;
    }

    public ParameterBuilder<T> option(long optionId, long numericValue) {
        Option option = new OptionImpl(optionId);
        option.setNumericValue(BigDecimal.valueOf(numericValue));
        param.addOption(option);
        return this;
    }

    public ParameterBuilder<T> localVendor(long globalVendorId, long localVendorId, String optionName) {
        OptionImpl global = new OptionImpl(globalVendorId, optionName, Option.OptionType.VENDOR);
        OptionImpl local = new OptionImpl(global, Option.OptionType.VENDOR);
        local.setId(localVendorId);
        param.addOption(local);
        return this;
    }

    public ParameterBuilder<T> maxValue(int maxValue) {
        return maxValue(new BigDecimal(maxValue));
    }

    public ParameterBuilder<T> maxValue(BigDecimal maxValue) {
        param.setMaxValue(maxValue);
        return this;
    }

    public ParameterBuilder<T> precision(int precision) {
        param.setPrecision(precision);
        return this;
    }

    public ParameterBuilder<T> mandatory(boolean mandatory) {
        param.setMandatory(mandatory);
        return this;
    }

    public ParameterBuilder<T> skuParameterMode(SkuParameterMode mode) {
        param.setSkuParameterMode(mode);
        return this;
    }

    public ParameterBuilder<T> extractInSkubd(boolean extract) {
        param.setExtractInSkubd(extract);
        return this;
    }

    public ParameterBuilder<T> skutchingType(CategoryParam.SkutchingType skutchingType) {
        param.setSkutchingType(skutchingType);
        return this;
    }

    public ParameterBuilder<T> cleanIfSkutchingFailed(boolean cleanIfSkutchingFailed) {
        param.setCleanIfSkutchingFailed(cleanIfSkutchingFailed);
        return this;
    }

    public ParameterBuilder<T> copyFirstSkuPictureToPicker(boolean copyFirstSkuPictureToPicker) {
        param.setCopyFirstSkuPictureToPicker(copyFirstSkuPictureToPicker);
        return this;
    }

    public ParameterBuilder<T> multifield(boolean multifield) {
        param.setMultifield(multifield);
        return this;
    }

    public UnitBuilder<ParameterBuilder<T>> startUnit() {
        return new UnitBuilder<>(unit -> {
            this.param.setUnit(unit);
            return this;
        });
    }

    ParameterBuilder<T> useForGuru(boolean useForGuru) {
        param.setUseForGuru(useForGuru);
        return this;
    }

    public ParameterBuilder<T> showOnSkuTab(boolean show) {
        this.param.setShowOnSkuTab(show);
        return this;
    }

    public ParameterBuilder<T> service(boolean service) {
        this.param.setService(service);
        return this;
    }

    public ParameterBuilder<T> mdmParameter(boolean mdm) {
        this.param.setMdmParameter(mdm);
        return this;
    }

    public T endParameter() {
        return endCallback.apply(param);
    }
}
