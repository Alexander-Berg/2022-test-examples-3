package ru.yandex.market.checkout.checkouter.order;

import ru.yandex.market.checkout.pushapi.client.entity.BaseBuilder;

import java.util.List;

/**
 * @author Mikhail Usachev <mailto:musachev@yandex-team.ru>
 *         Date: 18/05/2017.
 */
public class ItemParameterBuilder extends BaseBuilder<ItemParameter, ItemParameterBuilder> {
    public ItemParameterBuilder() {
        super(new ItemParameter());

        object.setType("number");
        object.setName("Длина");
        object.setValue("20");
        object.setUnit("м");
    }

    public ItemParameterBuilder withType(String val) {
        return withField("type", val);
    }

    public ItemParameterBuilder withSubType(String val) {
        return withField("subType", val);
    }

    public ItemParameterBuilder withName(String val) {
        return withField("name", val);
    }

    public ItemParameterBuilder withValue(String val) {
        return withField("value", val);
    }

    public ItemParameterBuilder withUnit(String val) {
        return withField("unit", val);
    }

    public ItemParameterBuilder withCode(String val) {
        return withField("code", val);
    }

    public ItemParameterBuilder withUnits(List<UnitValue> val) {
        return withField("units", val);
    }

}
