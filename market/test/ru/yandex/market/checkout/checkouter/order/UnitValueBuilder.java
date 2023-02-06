package ru.yandex.market.checkout.checkouter.order;

import ru.yandex.market.checkout.pushapi.client.entity.BaseBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author Mikhail Usachev <mailto:musachev@yandex-team.ru>
 *         Date: 31/05/2017.
 */
public class UnitValueBuilder extends BaseBuilder<UnitValue, UnitValueBuilder> {

    public UnitValueBuilder() {
        super(new UnitValue());
        object.setUnitId("RU");
        object.setDefaultUnit(true);
        object.setValues(Arrays.asList("1", "2", "3"));
        object.setShopValues(Arrays.asList("1", "2"));
    }

    public UnitValueBuilder withUnitId(String value) {
        return withField("unitId", value);
    }

    public UnitValueBuilder withDefaultUnit(boolean value) {
        return withField("defaultUnit", value);
    }

    public UnitValueBuilder withValues(List<String> values) {
        return withField("values", values);
    }

    public UnitValueBuilder withShopValues(List<String> values) {
        return withField("shopValues", values);
    }

}
