package ru.yandex.direct.core.testing.steps.campaign.model0.context;

import java.util.function.BiConsumer;
import java.util.function.Function;

import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelProperty;

/**
 * Настройки показа в рекламных сетях
 * <p>
 * Примечание: те настройки, которые устанавливаеются относительно показов в поиске,
 * такие как ограничение расхода относительно расхода на поиске или ограничение
 * цены клика относительно цены на поиске доступны только в том случае,
 * если выключено раздельное управление показами на поиске и в рекламных сетях.
 */
public class ContextSettings implements Model {

    public static final ModelProperty<ContextSettings, ContextLimitType> LIMIT_TYPE =
            prop("limitType", ContextSettings::getLimitType, ContextSettings::setLimitType);
    public static final ModelProperty<ContextSettings, Integer> LIMIT =
            prop("limit", ContextSettings::getLimit, ContextSettings::setLimit);
    public static final ModelProperty<ContextSettings, Integer> PRICE_COEFF =
            prop("priceCoeff", ContextSettings::getPriceCoeff, ContextSettings::setPriceCoeff);
    public static final ModelProperty<ContextSettings, Boolean> ENABLE_CPC_HOLD =
            prop("enableCpcHold", ContextSettings::getEnableCpcHold, ContextSettings::setEnableCpcHold);

    private static <V> ModelProperty<ContextSettings, V> prop(String name,
                                                              Function<ContextSettings, V> getter, BiConsumer<ContextSettings, V> setter) {
        return ModelProperty.create(ContextSettings.class, name, getter, setter);
    }

    /**
     * Тип ограничения бюджета в рекламных сетях
     * (ручной (зависит от параметра limit), авто, неограничен)
     */
    private ContextLimitType limitType;

    /**
     * Ограничение расходов в рекламных сетях (в процентах от общего расхода кампании от 1 до 100)
     */
    private Integer limit;

    /**
     * Ограничение цены клика в рекламных сетях (в проценте от поиска от 10 до 100)
     */
    private Integer priceCoeff;

    /**
     * Ограничение средненедельной цены клика в рекламных сетях ниже цены на поиске
     * campaigns.opts.enable_cpc_hold
     */
    private Boolean enableCpcHold;

    public ContextLimitType getLimitType() {
        return limitType;
    }

    public void setLimitType(ContextLimitType limitType) {
        this.limitType = limitType;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getPriceCoeff() {
        return priceCoeff;
    }

    public void setPriceCoeff(Integer priceCoeff) {
        this.priceCoeff = priceCoeff;
    }

    public Boolean getEnableCpcHold() {
        return enableCpcHold;
    }

    public void setEnableCpcHold(Boolean enableCpcHold) {
        this.enableCpcHold = enableCpcHold;
    }

    public ContextSettings withLimitType(ContextLimitType limitType) {
        this.limitType = limitType;
        return this;
    }

    public ContextSettings withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public ContextSettings withPriceCoeff(Integer priceCoeff) {
        this.priceCoeff = priceCoeff;
        return this;
    }

    public ContextSettings withEnableCpcHold(Boolean enableCpcHold) {
        this.enableCpcHold = enableCpcHold;
        return this;
    }
}
