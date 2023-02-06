package ru.yandex.market.partner.content.common.csku.wrappers.special;

import Market.DataCamp.DataCampContentMarketParameterValue;
import org.junit.Test;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.ModelFromOfferBuilder;
import ru.yandex.market.partner.content.common.csku.judge.MarketParameterValueWrapper;
import ru.yandex.market.partner.content.common.csku.util.OfferSpecialParameterCreator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.IS_CSKU;

public class IsCskuParameterWrapperTest {
    private static final Long OWNER_ID = 123L;

    @Test
    public void verifyIsCskuSetToTrue() {
        MarketParameterValueWrapper parameterValue =
               new MarketParameterValueWrapper(DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                        .setParamId(IS_CSKU.getId())
                        .setParamName(IS_CSKU.getXslName())
                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.BOOLEAN)
                                .setBoolValue(true)
                        )
                        .build());

        IsCskuParameterWrapper wrapper = new IsCskuParameterWrapper(parameterValue, OWNER_ID, null);
        ModelFromOfferBuilder modelBuilder = ModelFromOfferBuilder.builder(ModelStorage.Model.newBuilder().build(),
                true, mock(CategoryData.class), Math.toIntExact(OWNER_ID));
        wrapper.putValuesInSkuAndModel(modelBuilder);
        ModelStorage.Model model = modelBuilder.build();
        ModelStorage.ParameterValue isCskuParamValue = model.getParameterValuesList().stream()
                .filter(parameterValue1 -> parameterValue1.getParamId() == IS_CSKU.getId())
                .findFirst().get();
        assertThat(isCskuParamValue.getBoolValue()).isTrue();
        assertThat(isCskuParamValue.getOptionId()).isEqualTo(OfferSpecialParameterCreator.IS_CSKU_TRUE_OPTION_ID);
    }
}
