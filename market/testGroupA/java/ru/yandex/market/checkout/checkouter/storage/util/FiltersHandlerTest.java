package ru.yandex.market.checkout.checkouter.storage.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.ItemParameter;
import ru.yandex.market.checkout.checkouter.order.UnitValue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * @author Nicolai Iusiumbeli <mailto:armor@yandex-team.ru>
 * date: 13/04/2017
 */
public class FiltersHandlerTest {

    @Test
    public void serializeParameters() throws Exception {
        ItemParameter parameter = new ItemParameter();
        parameter.setCode("#FFFFFF");
        parameter.setName("asdasd");
        parameter.setType("type");
        parameter.setSubType("subtype");
        parameter.setUnit("рост");
        parameter.setValue("черный");
        String s = FiltersHandler.serializeParameters(Collections.singletonList(parameter));
        List<ItemParameter> itemParameters = FiltersHandler.deserializeParameters(s);
        assertThat(itemParameters, hasSize(1));
        ItemParameter p = itemParameters.get(0);
        assertThat(p.getCode(), is(parameter.getCode()));
        assertThat(p.getName(), is(parameter.getName()));
        assertThat(p.getType(), is(parameter.getType()));
        assertThat(p.getSubType(), is(parameter.getSubType()));
        assertThat(p.getUnit(), is(parameter.getUnit()));
        assertThat(p.getValue(), is(parameter.getValue()));
    }

    @Test
    public void testEmpty() throws Exception {
        List<ItemParameter> itemParameters = FiltersHandler.deserializeParameters("[]");
        assertThat(itemParameters, hasSize(0));
    }

    @Test
    public void serializeUnitParameters() throws Exception {
        ItemParameter parameter = new ItemParameter();
        parameter.setName("Размеры :)");
        parameter.setType("enum");
        parameter.setSubType("size");
        UnitValue unitValueDefault = new UnitValue();
        unitValueDefault.setValues(Arrays.asList("30", "40", "50"));
        unitValueDefault.setDefaultUnit(true);
        UnitValue unitValue = new UnitValue();
        unitValue.setValues(Arrays.asList("100", "101"));
        parameter.setUnits(Arrays.asList(unitValueDefault, unitValue));

        String s = FiltersHandler.serializeParameters(Collections.singletonList(parameter));
        List<ItemParameter> itemParameters = FiltersHandler.deserializeParameters(s);
        assertThat(itemParameters, hasSize(1));
        ItemParameter p = itemParameters.get(0);
        assertThat(p.getName(), is(parameter.getName()));
        assertThat(p.getType(), is(parameter.getType()));
        assertThat(p.getSubType(), is(parameter.getSubType()));
        assertThat(p.getUnits(), hasSize(2));
        assertThat(p.getUnits().get(0).getValues(), hasSize(parameter.getUnits().get(0).getValues().size()));
        assertThat(p.getUnits().get(0).getValues().get(0), is(parameter.getUnits().get(0).getValues().get(0)));
        assertThat(p.getUnits().get(0).isDefaultUnit(), is(parameter.getUnits().get(0).isDefaultUnit()));
        assertThat(p.getUnits().get(1).getValues(), hasSize(parameter.getUnits().get(1).getValues().size()));
        assertThat(p.getUnits().get(1).isDefaultUnit(), is(parameter.getUnits().get(1).isDefaultUnit()));
        assertThat(p.getUnits().get(1).getValues().get(1), is(parameter.getUnits().get(1).getValues().get(1)));
    }
}
