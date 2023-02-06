package ru.yandex.market.mbo.gwt.models.params;

import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.util.ArrayList;
import java.util.List;

/**
 * @author s-ermakov
 */
public class OptionBuilder {

    private long id;
    private long paramId;

    private boolean published;
    private boolean isFilterValue;
    private boolean isFilterValueRed;
    private boolean isDefaultValue;
    private boolean isDefaultValueRed;
    private List<Word> names = new ArrayList<>();
    private List<EnumAlias> aliases = new ArrayList<>();
    private boolean dontUseAsAlias;

    public static OptionBuilder newBuilder() {
        return new OptionBuilder();
    }

    public static OptionBuilder newBuilder(long id) {
        return newBuilder()
                .setId(id);
    }

    public OptionBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public OptionBuilder setParamId(long paramId) {
        this.paramId = paramId;
        return this;
    }

    public OptionBuilder setPublished(boolean published) {
        this.published = published;
        return this;
    }

    public OptionBuilder setFilterValue(boolean isFilterValue) {
        this.isFilterValue = isFilterValue;
        return this;
    }

    public OptionBuilder setFilterValueRed(boolean filterValueRed) {
        isFilterValueRed = filterValueRed;
        return this;
    }

    public OptionBuilder setDefaultValue(boolean defaultValue) {
        isDefaultValue = defaultValue;
        return this;
    }

    public OptionBuilder setDefaultValueRed(boolean defaultValueRed) {
        isDefaultValueRed = defaultValueRed;
        return this;
    }

    public OptionBuilder addName(Word name) {
        names.add(name);
        return this;
    }

    public OptionBuilder addName(String name) {
        names.add(new Word(id, Word.DEFAULT_LANG_ID, name));
        return this;
    }

    public OptionBuilder addAlias(EnumAlias alias) {
        aliases.add(alias);
        return this;
    }

    public OptionBuilder setDontUseAsAlias(boolean dontUseAsAlias) {
        this.dontUseAsAlias = dontUseAsAlias;
        return this;
    }

    public Option build() {
        Option option = new OptionImpl();
        option.setId(id);
        option.setParamId(paramId);
        option.setPublished(published);
        option.setFilterValue(isFilterValue);
        option.setDefaultValue(isDefaultValue);
        names.forEach(option::addName);
        aliases.forEach(option::addAlias);
        option.setDontUseAsAlias(dontUseAsAlias);
        return option;
    }
}
