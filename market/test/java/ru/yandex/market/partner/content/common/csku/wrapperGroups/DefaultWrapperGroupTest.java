package ru.yandex.market.partner.content.common.csku.wrapperGroups;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampContentMarketParameterValue;
import org.apache.bcel.generic.NEW;
import org.junit.Test;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.ModelFromOfferBuilder;
import ru.yandex.market.partner.content.common.csku.SimplifiedOfferParameter;
import ru.yandex.market.partner.content.common.csku.judge.MarketParameterValueWrapper;
import ru.yandex.market.partner.content.common.csku.wrappers.BaseParameterWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultWrapperGroupTest {

    private static final Long OLD_PARAM_ID = 7014698L;
    private static final Long NEW_PARAM_ID = 27141671L;
    private static final String XSL_NAME = "backpack_purpose_gl";

    private static final Long SUPPLIER_ID = 1L;
    private static final Long VENDOR_OPTION_ID = 2L;
    private static final Long OPTION_ID = 3L;
    private static final Long NEW_OPTION_ID = 33L;

    private CategoryData categoryData = mock(CategoryData.class);

    @Test
    public void testWhenMigratedParamThenUseNewIdAndName() {
        MboParameters.Parameter migratedParameter = MboParameters.Parameter.newBuilder()
                .setId(NEW_PARAM_ID)
                .setXslName(XSL_NAME)
                .addOption(MboParameters.Option.newBuilder().setId(NEW_OPTION_ID).build())
                .build();
        when(categoryData.getParamById(OLD_PARAM_ID)).thenReturn(migratedParameter);
        when(categoryData.getMigratedOptionId(OLD_PARAM_ID, OPTION_ID)).thenReturn(NEW_OPTION_ID);
        DataCampContentMarketParameterValue.MarketParameterValue offerOldParam =
                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                        .setParamId(OLD_PARAM_ID)
                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setOptionId(OPTION_ID)
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                .build())
                        .build();
        DefaultWrapperGroup defaultWrapperGroup = new DefaultWrapperGroup(Collections.singletonList(
            new MarketParameterValueWrapper(offerOldParam)),
                categoryData, SUPPLIER_ID,
                VENDOR_OPTION_ID,
                new HashMap<>(), new HashMap<>());
        BaseParameterWrapper parameterWrapper = defaultWrapperGroup.getParameterWrappers().get(0);
        assertThat(parameterWrapper.getParamId()).isEqualTo(NEW_PARAM_ID);

        SimplifiedOfferParameter offerParameter = parameterWrapper.extractSelfFromOffer();
        assertThat(offerParameter.getParamId()).isEqualTo(NEW_PARAM_ID);

        //Посмотрим, что в итоге попадает в модель
        ModelFromOfferBuilder modelBuilder = ModelFromOfferBuilder.builder(
                ModelStorage.Model.newBuilder().build(),
                true, categoryData, SUPPLIER_ID.intValue());
        parameterWrapper.putValuesInSkuAndModel(modelBuilder);
        ModelStorage.Model model = modelBuilder.build();
        ModelStorage.ParameterValue parameterValue = model.getParameterValuesList().get(0);
        assertThat(parameterValue.getParamId()).isEqualTo(NEW_PARAM_ID);
        assertThat(parameterValue.getXslName()).isEqualTo(XSL_NAME);

    }
}
