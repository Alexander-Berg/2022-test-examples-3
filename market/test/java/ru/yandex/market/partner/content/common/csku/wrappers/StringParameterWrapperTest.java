package ru.yandex.market.partner.content.common.csku.wrappers;

import java.util.HashMap;
import java.util.Map;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;
import org.junit.Test;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.ModelFromOfferBuilder;
import ru.yandex.market.partner.content.common.csku.OffersGenerator;
import ru.yandex.market.partner.content.common.csku.SimplifiedOfferParameter;
import ru.yandex.market.partner.content.common.csku.judge.MarketParameterValueWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class StringParameterWrapperTest {

    private BaseParameterWrapper wrapper;
    private ModelFromOfferBuilder builder;
    private DataCampOffer.Offer offer;
    private CategoryData categoryData = mock(CategoryData.class);
    private static final Long PARAM_ID_1 = 1L;
    private static final String VALUE_1 = "Some value";
    private static final String VALUE_2 = "Not some value";
    private static final int SUPPLIER_ID = 123;

    @Test
    public void whenOfferContainsStringParameterThenReturnItsStringValue() {
        Map<Long, String> paramIdToValue = new HashMap<>();
        paramIdToValue.put(PARAM_ID_1, VALUE_1);
        MarketParameterValueWrapper parameterValue =
               new MarketParameterValueWrapper(DataCampContentMarketParameterValue
                        .MarketParameterValue.newBuilder()
                        .setParamId(PARAM_ID_1)
                        .setValue(DataCampContentMarketParameterValue.MarketValue
                                .newBuilder().setStrValue(VALUE_1).build())
                        .build());

        wrapper = new StringParameterWrapper(parameterValue, "param", SUPPLIER_ID,
                null, null);
        SimplifiedOfferParameter simplifiedOfferParameter = wrapper.extractSelfFromOffer();
        assertThat(simplifiedOfferParameter).isNotNull();
        assertThat(simplifiedOfferParameter.getParamId()).isEqualTo(PARAM_ID_1);
        assertThat(simplifiedOfferParameter.getStrValue()).isEqualTo(VALUE_1);
    }

    @Test(expected = NullPointerException.class)
    public void whenNoParamInOfferThenReturnNoValue() {
        offer = OffersGenerator.generateEmptyOffer();

        ModelStorage.Model model = ModelStorage.Model.newBuilder().build();
        builder = ModelFromOfferBuilder.builder(model, true, categoryData, SUPPLIER_ID);

        wrapper = new StringParameterWrapper(null, "param", SUPPLIER_ID, null, null);
    }

    @Test
    public void hasSkuAndOfferHaveTheSameValueFalseTest() {
        wrapper = new StringParameterWrapper(buildOfferParam(), "param", SUPPLIER_ID,
                ModelStorage.ParameterValue.newBuilder()
                        .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(VALUE_2))
                        .setParamId(PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.STRING)
                        .build(), null);
        assertFalse(wrapper.isTheSameValueForOfferAndModel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void noSkuParamTest() {
        wrapper = new StringParameterWrapper(buildOfferParam(), "param", SUPPLIER_ID,
                null, null);
        assertFalse(wrapper.isTheSameValueForOfferAndModel());
    }

    @Test
    public void hasSkuAndOfferHaveTheSameValueHypothesisTrueTest() {
        wrapper = new StringParameterWrapper(buildOfferParam(), "param", SUPPLIER_ID,
                null,
                ModelStorage.ParameterValueHypothesis.newBuilder()
                    .addStrValue(MboParameters.Word.newBuilder().setName(VALUE_1))
                    .setParamId(PARAM_ID_1)
                    .setValueType(MboParameters.ValueType.STRING)
                    .build());
        assertTrue(wrapper.isTheSameValueForOfferAndModel());
    }

    @Test
    public void hasSkuAndOfferHaveTheSameValueTrueTest() {
        wrapper = new StringParameterWrapper(buildOfferParam(), "param", SUPPLIER_ID,
                ModelStorage.ParameterValue.newBuilder()
                        .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue(VALUE_1))
                        .setParamId(PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.STRING)
                        .build(),
                null);
        assertTrue(wrapper.isTheSameValueForOfferAndModel());
    }

    private MarketParameterValueWrapper buildOfferParam() {
        return new MarketParameterValueWrapper(DataCampContentMarketParameterValue
                        .MarketParameterValue.newBuilder()
                        .setParamId(PARAM_ID_1)
                        .setValue(DataCampContentMarketParameterValue.MarketValue
                                .newBuilder().setStrValue(VALUE_1).build())
                        .build());
    }
}
