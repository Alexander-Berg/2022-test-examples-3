package ru.yandex.market.partner.content.common.csku.wrappers;

import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.OffersGenerator;

import static org.assertj.core.api.Assertions.assertThat;

public class HypothesisParameterWrapperTest {

    private static final int SUPPLIER_ID = 123;

    @Test
    public void whenOptionInModelHypothesisInOfferThenGetOptionId() {
        HypothesisParameterWrapper wrapper = new HypothesisParameterWrapper(OffersGenerator.buildOfferParam(),
                "param", SUPPLIER_ID,
                ModelStorage.ParameterValue.newBuilder()
                        .setOptionId(1)
                        .setParamId(OffersGenerator.PARAM_ID_1)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .build(),
                null);
        assertThat(wrapper.getSkuValue()).isEqualTo("1");
    }
}
