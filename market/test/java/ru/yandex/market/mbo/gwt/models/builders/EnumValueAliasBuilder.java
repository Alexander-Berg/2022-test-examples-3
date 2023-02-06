package ru.yandex.market.mbo.gwt.models.builders;

import ru.yandex.market.mbo.gwt.models.modelstorage.EnumValueAlias;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Date;

/**
 * @author s-ermakov
 */
public class EnumValueAliasBuilder<T> {

    private EnumValueAlias enumValueAlias = new EnumValueAlias();
    private CommonModelBuilder<T> modelBuilder;

    private EnumValueAliasBuilder() {
    }

    public static EnumValueAliasBuilder<EnumValueAliasBuilder> newBuilder() {
        return new EnumValueAliasBuilder<>();
    }

    public static <T> EnumValueAliasBuilder<T> newBuilder(CommonModelBuilder<T> modelBuilder) {
        EnumValueAliasBuilder<T> builder = new EnumValueAliasBuilder<>();
        builder.modelBuilder = modelBuilder;
        return builder;
    }

    public EnumValueAliasBuilder<T> paramId(long paramId) {
        enumValueAlias.setParamId(paramId);
        return this;
    }

    public EnumValueAliasBuilder<T> xslName(String xslName) {
        enumValueAlias.setXslName(xslName);
        return this;
    }

    public EnumValueAliasBuilder<T> optionId(long optionId) {
        enumValueAlias.setOptionId(optionId);
        return this;
    }

    public EnumValueAliasBuilder<T> aliasOptionId(long aliasOptionId) {
        enumValueAlias.setAliasOptionId(aliasOptionId);
        return this;
    }

    public EnumValueAliasBuilder<T> modificationSource(ModificationSource modificationSource) {
        enumValueAlias.setModificationSource(modificationSource);
        return this;
    }

    public EnumValueAliasBuilder<T> lastModificationUid(Long lastModificationUid) {
        enumValueAlias.setLastModificationUid(lastModificationUid);
        return this;
    }

    public EnumValueAliasBuilder<T> lastModificationDate(Date lastModificationDate) {
        enumValueAlias.setLastModificationDate(lastModificationDate);
        return this;
    }

    public EnumValueAlias build() {
        return enumValueAlias;
    }

    // dsl methods

    public CommonModelBuilder<T> end() {
        modelBuilder.getModel().addEnumValueAlias(build());
        return modelBuilder;
    }
}
