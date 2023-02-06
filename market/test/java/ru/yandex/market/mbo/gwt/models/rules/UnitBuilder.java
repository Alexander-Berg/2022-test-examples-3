package ru.yandex.market.mbo.gwt.models.rules;

import ru.yandex.market.mbo.gwt.models.params.Unit;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 19.04.2018
 */
public class UnitBuilder<C> {

    private final Function<Unit, C> endUnitCallback;

    private final Unit unit = new Unit();

    public UnitBuilder(Function<Unit, C> endUnitCallback) {
        this.endUnitCallback = endUnitCallback;
    }

    public static UnitBuilder<Unit> builder() {
        return new UnitBuilder<>(Function.identity());
    }

    public static <Callback> UnitBuilder<Callback> builder(
        Function<Unit, Callback> endUnitCallback) {

        return new UnitBuilder<>(endUnitCallback);
    }

    public UnitBuilder<C> name(String name) {
        unit.setName(name);
        return this;
    }

    public UnitBuilder<C> reportName(String reportName) {
        unit.setReportName(reportName);
        return this;
    }

    public UnitBuilder<C> scale(BigDecimal scale) {
        unit.setScale(scale);
        return this;
    }

    public UnitBuilder<C> scale(long scale) {
        unit.setScale(BigDecimal.valueOf(scale));
        return this;
    }

    public UnitBuilder<C> measureId(long measureId) {
        unit.setMeasureId(measureId);
        return this;
    }

    public UnitBuilder<C> id(long id) {
        unit.setId(id);
        return this;
    }

    public C endUnit() {
        return endUnitCallback.apply(unit);
    }
}
