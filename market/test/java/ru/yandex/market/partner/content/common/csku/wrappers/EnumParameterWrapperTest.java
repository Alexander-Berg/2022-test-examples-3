package ru.yandex.market.partner.content.common.csku.wrappers;

import Market.DataCamp.DataCampContentMarketParameterValue;
import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.judge.MarketParameterValueWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EnumParameterWrapperTest {

    private BaseParameterWrapper wrapper;
    private static final Long PARAM_ID_1 = 1L;
    private static final Integer VALUE_1 = 124;
    private static final Integer VALUE_2 = 125;
    private static final int SUPPLIER_ID = 123;

    @Test
    public void hasSkuAndOfferHaveTheSameValueFalseTest() {

        wrapper = new EnumParameterWrapper(buildOfferParam(), "param",
                VALUE_1.longValue(),
                SUPPLIER_ID,
                ModelStorage.ParameterValue.newBuilder()
                        .setOptionId(VALUE_2)
                        .setParamId(PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .build(), null);
        assertFalse(wrapper.isTheSameValueForOfferAndModel());
    }


    @Test
    public void hasSkuAndOfferHaveTheSameValueTrueTest() {

        wrapper = new EnumParameterWrapper(buildOfferParam(), "param",
                VALUE_1.longValue(),
                SUPPLIER_ID,
                ModelStorage.ParameterValue.newBuilder()
                        .setOptionId(VALUE_1)
                        .setParamId(PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .build(),
                null);
        assertTrue(wrapper.isTheSameValueForOfferAndModel());
        assertThat(wrapper.extractSelfFromOffer().getOptionId()).isEqualTo(VALUE_1.longValue());
    }

    private MarketParameterValueWrapper buildOfferParam() {
        return new MarketParameterValueWrapper(DataCampContentMarketParameterValue
                .MarketParameterValue.newBuilder()
                .setParamId(PARAM_ID_1)
                .setValue(DataCampContentMarketParameterValue.MarketValue
                        .newBuilder().setOptionId(VALUE_1.longValue()).build())
                .build());
    }
}
