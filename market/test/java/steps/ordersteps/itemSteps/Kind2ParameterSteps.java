package steps.orderSteps.itemSteps;

import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.checkout.checkouter.order.ItemParameter;
import ru.yandex.market.checkout.checkouter.order.UnitValue;

class Kind2ParameterSteps {
    private static final String TYPE = "type";
    private static final String SUB_TYPE = "subType";
    private static final String NAME = "name";
    private static final String VALUE = "value";
    private static final String UNIT = "unit";
    private static final String CODE = "code";

    private Kind2ParameterSteps() {
    }

    static List<ItemParameter> getKind2ParametersList() {
        ItemParameter itemParameter = new ItemParameter();

        itemParameter.setType(TYPE);
        itemParameter.setSubType(SUB_TYPE);
        itemParameter.setName(NAME);
        itemParameter.setValue(VALUE);
        itemParameter.setUnit(UNIT);
        itemParameter.setCode(CODE);
        itemParameter.setUnits(UnitValueSteps.getUnitValueList());

        return Collections.singletonList(itemParameter);
    }

    private static class UnitValueSteps {
        private static final List<String> VALUES = Collections.singletonList("value");
        private static final List<String> SHOP_VALUES = Collections.singletonList("shopValues");
        private static final String UNIT_ID = "123";
        private static final boolean DEFAULT_UNIT = true;

        static List<UnitValue> getUnitValueList() {
            UnitValue unitValue = new UnitValue();

            unitValue.setValues(VALUES);
            unitValue.setShopValues(SHOP_VALUES);
            unitValue.setUnitId(UNIT_ID);
            unitValue.setDefaultUnit(DEFAULT_UNIT);

            return Collections.singletonList(unitValue);
        }

        static JSONArray getUnitValueJsonArray() throws JSONException {
            JSONArray unitValueJsonArray = new JSONArray();
            JSONObject unitValueJson = new JSONObject();

            unitValueJson.put("defaultUnit", DEFAULT_UNIT);
            unitValueJson.put("values", VALUES);
            unitValueJson.put("shopValues", SHOP_VALUES);
            unitValueJson.put("unitId", UNIT_ID);

            unitValueJsonArray.put(unitValueJson);
            return unitValueJsonArray;
        }
    }
}
