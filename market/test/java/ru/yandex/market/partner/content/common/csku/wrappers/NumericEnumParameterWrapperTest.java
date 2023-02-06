package ru.yandex.market.partner.content.common.csku.wrappers;

import Market.DataCamp.DataCampContentMarketParameterValue;
import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.judge.MarketParameterValueWrapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NumericEnumParameterWrapperTest {

    private BaseParameterWrapper wrapper;
    private static final Long PARAM_ID_1 = 1L;
    private static final String VALUE_1 = "124";
    private static final String VALUE_2 = "125";
    private static final int SUPPLIER_ID = 123;

    @Test
    public void hasSkuAndOfferHaveTheSameValueFalseTest() {

        wrapper = new NumericEnumParameterWrapper(buildOfferParam(), "param",
                1,
                SUPPLIER_ID,
                ModelStorage.ParameterValue.newBuilder()
                        .setNumericValue(VALUE_2)
                        .setParamId(PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.NUMERIC_ENUM)
                        .build(), null);
        assertFalse(wrapper.isTheSameValueForOfferAndModel());
    }

    @Test
    public void hasSkuAndOfferHaveTheSameValueHypothesisTrueTest() {
        wrapper = new NumericEnumParameterWrapper(buildOfferParam(), "param",
                1,
                SUPPLIER_ID,
                null,
                ModelStorage.ParameterValueHypothesis.newBuilder()
                        .addStrValue(MboParameters.Word.newBuilder().setName(VALUE_1))
                        .setParamId(PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.NUMERIC_ENUM)
                        .build());
        assertTrue(wrapper.isTheSameValueForOfferAndModel());
    }

    @Test
    public void hasSkuAndOfferHaveTheSameValueTrueTest() {

        wrapper = new NumericEnumParameterWrapper(buildOfferParam(), "param",
                1,
                SUPPLIER_ID,
                ModelStorage.ParameterValue.newBuilder()
                        .setNumericValue(VALUE_1)
                        .setParamId(PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.NUMERIC_ENUM)
                        .build(),
                null);
        assertTrue(wrapper.isTheSameValueForOfferAndModel());
    }

    @Test
    public void noSkuTest() {

        wrapper = new NumericEnumParameterWrapper(buildOfferParam(), "param",
                1,
                SUPPLIER_ID,
                null,
                null);
        assertFalse(wrapper.isTheSameValueForOfferAndModel());
    }

    private MarketParameterValueWrapper buildOfferParam() {
        return new MarketParameterValueWrapper(DataCampContentMarketParameterValue
                .MarketParameterValue.newBuilder()
                .setParamId(PARAM_ID_1)
                .setValue(DataCampContentMarketParameterValue.MarketValue
                        .newBuilder().setNumericValue(VALUE_1).build())
                .build());
    }
}
