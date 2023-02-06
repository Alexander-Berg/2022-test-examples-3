package ru.yandex.market.mbo.db.utils;

import com.google.common.base.Preconditions;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @author yuramalinov
 * @created 10.01.19
 */
public class ParameterGenerator implements TestRule {
    private static final long DEFAULT_ID = 1001L;

    private final long defaultId;
    private final long defaultCategoryId;
    private long nextId;
    private Map<String, Option> options;

    public ParameterGenerator() {
        this(DEFAULT_ID, 0L);
    }

    private ParameterGenerator(long defaultCategoryId) {
        this(DEFAULT_ID, defaultCategoryId);
    }

    private ParameterGenerator(long defaultId, long defaultCategoryId) {
        this.defaultId = defaultId;
        this.defaultCategoryId = defaultCategoryId;
        reset(defaultId);
    }

    public long getOptionId(String name) {
        return getOption(name).getId();
    }

    public Option getOption(String name) {
        return Preconditions.checkNotNull(options.get(name), "Can't find option %s", name);
    }

    public long getTrueOptionId(Parameter parameter) {
        return parameter.getOptions().stream()
            .filter(o -> o.getName().equalsIgnoreCase("true"))
            .map(Option::getId)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Can't find TRUE option in parameter " + parameter));
    }

    public long getFalseOptionId(Parameter parameter) {
        return parameter.getOptions().stream()
            .filter(o -> o.getName().equalsIgnoreCase("true"))
            .map(Option::getId)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Can't find TRUE option in parameter " + parameter));
    }

    private void reset(long defaultId) {
        nextId = defaultId;
        options = new LinkedHashMap<>();
    }

    public ParameterBuilder param(String xslName, Param.Type type) {
        Parameter parameter = new Parameter();
        parameter.setId(nextId++);
        parameter.setXslName(xslName);
        parameter.setLevel(CategoryParam.Level.MODEL);
        parameter.setType(type);
        parameter.setCategoryHid(defaultCategoryId);
        return new ParameterBuilder(parameter);
    }

    public ParameterBuilder boolParam(String xslName) {
        return param(xslName, Param.Type.BOOLEAN)
            .option("TRUE")
            .option("FALSE");
    }

    @Override
    public Statement apply(Statement base, Description description) {
        reset(defaultId);
        return base;
    }

    public class ParameterBuilder {
        private Parameter parameter;

        ParameterBuilder(Parameter parameter) {
            this.parameter = parameter;
        }

        public ParameterBuilder multifield(boolean multifield) {
            parameter.setMultifield(multifield);
            return this;
        }

        public ParameterBuilder option(String name) {
            return option(name, true);
        }

        public ParameterBuilder option(String name, boolean published) {
            OptionImpl option = new OptionImpl(nextId++, name, published);
            options.put(name, option);
            parameter.addOption(option);
            return this;
        }

        public Parameter get() {
            return parameter;
        }
    }
}
