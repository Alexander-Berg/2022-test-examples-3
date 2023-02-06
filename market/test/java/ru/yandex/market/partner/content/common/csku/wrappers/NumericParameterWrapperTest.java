package ru.yandex.market.partner.content.common.csku.wrappers;

import Market.DataCamp.DataCampContentMarketParameterValue;
import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.judge.MarketParameterValueWrapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NumericParameterWrapperTest {

    private BaseParameterWrapper wrapper;
    private static final Long PARAM_ID_1 = 1L;
    private static final String VALUE_1 = "124";
    private static final String VALUE_2 = "125";
    private static final int SUPPLIER_ID = 123;

    @Test
    public void hasSkuAndOfferHaveTheSameValueFalseTest() {

        wrapper = new NumericParameterWrapper(buildOfferParam(), "param", SUPPLIER_ID,
                ModelStorage.ParameterValue.newBuilder()
                        .setNumericValue(VALUE_2)
                        .setParamId(PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.NUMERIC)
                        .build(), null);
        assertFalse(wrapper.isTheSameValueForOfferAndModel());
    }

    @Test
    public void hasSkuAndOfferHaveTheSameValueHypothesisTrueTest() {
        wrapper = new NumericParameterWrapper(buildOfferParam(), "param", SUPPLIER_ID,
                null,
                ModelStorage.ParameterValueHypothesis.newBuilder()
                        .addStrValue(MboParameters.Word.newBuilder().setName(VALUE_1))
                        .setParamId(PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.NUMERIC)
                        .build());
        assertTrue(wrapper.isTheSameValueForOfferAndModel());
    }


    @Test
    public void hasSkuAndOfferHaveTheSameValueTrueTest() {

        wrapper = new NumericParameterWrapper(buildOfferParam(), "param", SUPPLIER_ID,
                ModelStorage.ParameterValue.newBuilder()
                        .setNumericValue(VALUE_1)
                        .setParamId(PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.NUMERIC)
                        .build(),
                null);
        assertTrue(wrapper.isTheSameValueForOfferAndModel());
    }

    @Test
    public void whenOtherFormatThenSameValueTrueTest() {
        wrapper = new NumericParameterWrapper(buildOfferParam("20,0"), "param", SUPPLIER_ID,
                ModelStorage.ParameterValue.newBuilder()
                        .setNumericValue("20")
                        .setParamId(PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.NUMERIC)
                        .build(),
                null);
        assertTrue(wrapper.isTheSameValueForOfferAndModel());
    }

    @Test(expected = NumberFormatException.class)
    public void whenNonNumericThenThrowException() {
        wrapper = new NumericParameterWrapper(buildOfferParam("20,gg0"), "param", SUPPLIER_ID,
                ModelStorage.ParameterValue.newBuilder()
                        .setNumericValue("20")
                        .setParamId(PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.NUMERIC)
                        .build(),
                null);
        wrapper.isTheSameValueForOfferAndModel();
    }

    @Test(expected = RuntimeException.class)
    public void whenEmptyModelValueThenThrowException() {
        wrapper = new NumericParameterWrapper(buildOfferParam("20,0"), "param", SUPPLIER_ID,
                null,
                null);
        wrapper.isTheSameValueForOfferAndModel();
    }

    private MarketParameterValueWrapper buildOfferParam() {
        return new MarketParameterValueWrapper(DataCampContentMarketParameterValue
                .MarketParameterValue.newBuilder()
                .setParamId(PARAM_ID_1)
                .setValue(DataCampContentMarketParameterValue.MarketValue
                        .newBuilder().setNumericValue(VALUE_1).build())
                .build());
    }

    private MarketParameterValueWrapper buildOfferParam(String value) {
        return new MarketParameterValueWrapper(DataCampContentMarketParameterValue
                .MarketParameterValue.newBuilder()
                .setParamId(PARAM_ID_1)
                .setValue(DataCampContentMarketParameterValue.MarketValue
                        .newBuilder().setNumericValue(value).build())
                .build());
    }
}
