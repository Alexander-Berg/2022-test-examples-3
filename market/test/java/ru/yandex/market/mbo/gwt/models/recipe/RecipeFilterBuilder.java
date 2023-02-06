package ru.yandex.market.mbo.gwt.models.recipe;

import ru.yandex.market.mbo.gwt.models.params.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author s-ermakov
 */
public class RecipeFilterBuilder {
    private long recipeId;
    private long paramId;
    private Param.Type paramType;
    private Boolean booleanValue = false;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Set<Long> valueIds = new HashSet<>();

    public static RecipeFilterBuilder newBuilder() {
        return new RecipeFilterBuilder();
    }

    public RecipeFilterBuilder setRecipeId(long recipeId) {
        this.recipeId = recipeId;
        return this;
    }

    public RecipeFilterBuilder setParamId(long paramId, Param.Type paramType) {
        this.paramId = paramId;
        this.paramType = paramType;
        return this;
    }

    public RecipeFilterBuilder setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
        return this;
    }

    public RecipeFilterBuilder setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
        return this;
    }

    public RecipeFilterBuilder setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    public RecipeFilterBuilder setValueIds(Collection<Long> valueIds) {
        this.valueIds = new HashSet<>(valueIds);
        return this;
    }

    public RecipeFilter create() {
        RecipeFilter recipeFilter = new RecipeFilter();
        recipeFilter.setRecipeId(recipeId);
        recipeFilter.setParamId(paramId);
        recipeFilter.setParamType(paramType);
        recipeFilter.setBooleanValue(booleanValue);
        recipeFilter.setMinValue(minValue);
        recipeFilter.setMaxValue(maxValue);
        recipeFilter.setValueIds(valueIds);
        return recipeFilter;
    }
}
