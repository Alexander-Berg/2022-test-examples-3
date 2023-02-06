package ru.yandex.market.partner.content.common.csku.wrapperGroups;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.ModelFromOfferBuilder;
import ru.yandex.market.partner.content.common.csku.OfferParameterType;
import ru.yandex.market.partner.content.common.csku.OffersGenerator;
import ru.yandex.market.partner.content.common.csku.SimplifiedOfferParameter;
import ru.yandex.market.partner.content.common.csku.judge.ModelData;
import ru.yandex.market.partner.content.common.csku.wrapperGroups.holders.WrapperGroupsHolder;
import ru.yandex.market.partner.content.common.csku.wrappers.BaseParameterWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.MODEL_QUALITY;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.USE_NAME_AS_TITLE;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.VENDOR;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.VENDOR_LINE;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.VOLUME;

public class WrapperGroupsFactoryTest {
    private CategoryData categoryData;
    private DataCampOffer.Offer offer;
    private static final Long NEW_PARAM_ID = 22L;
    private static final Long OLD_PARAM_ID = 11L;
    private static final Long PARAM_ID = 33L;
    private static final int SUPPLIER_ID = 123;
    private static final String SHOP_SKU = "Shop sku";
    private static final String SOME_LINE = "Some line";
    private static final Integer GROUP_ID = 14567;

    @Before
    public void init() {
        categoryData = mock(CategoryData.class);
    }

    @Test
    public void whenMigratedAndOldParamThenProcessOnlyMigrated() {
        when(categoryData.getMigratedParamId(OLD_PARAM_ID)).thenReturn(NEW_PARAM_ID);
        when(categoryData.containsParam(NEW_PARAM_ID)).thenReturn(true);
        when(categoryData.getParamById(NEW_PARAM_ID)).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(NEW_PARAM_ID)
                .setValueType(MboParameters.ValueType.STRING)
                .setXslName("new")
                .build());

        when(categoryData.containsParam(PARAM_ID)).thenReturn(true);
        when(categoryData.getParamById(PARAM_ID)).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(PARAM_ID)
                .setValueType(MboParameters.ValueType.STRING)
                .setXslName("param")
                .build());

        List<SimplifiedOfferParameter> simplifiedOfferParameters = Arrays.asList(
                SimplifiedOfferParameter.forOffer(OLD_PARAM_ID, "old_name", "Old",
                        OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(NEW_PARAM_ID, "new_name", "New",
                        OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(PARAM_ID, "piu_name", "Param-param-piu",
                        OfferParameterType.STRING));

        offer = OffersGenerator.generate(simplifiedOfferParameters);

        ModelData data = new ModelData(ModelStorage.Model.newBuilder().build(), false,
                SHOP_SKU);
        WrapperGroupsHolder offerWrapperGroup
                = WrapperGroupsFactory.getOfferWrapperGroup(offer, data, categoryData, new HashSet<>());
        List<BaseParameterWrapper> parameterWrappers = offerWrapperGroup.getParameterWrappers();
        //Проверяем, что для старого параметра нет обёртки
        assertThat(parameterWrappers).hasSize(5);
        assertThat(parameterWrappers.stream()
                .map(BaseParameterWrapper::extractSelfFromOffer)
                .map(SimplifiedOfferParameter::getParamId))
                .containsOnly(
                        NEW_PARAM_ID,
                        PARAM_ID,
                        VENDOR.getId(),
                        USE_NAME_AS_TITLE.getId(),
                        MODEL_QUALITY.getId()  // Добавляется для модели
                );
    }

    @Test
    public void whenVendorLineInOfferThenProcessSeparately() {
        when(categoryData.containsParam(VENDOR_LINE.getId())).thenReturn(true);
        when(categoryData.isSkuParameter(VENDOR_LINE.getId())).thenReturn(true);
        when(categoryData.getParamById(VENDOR_LINE.getId())).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(VENDOR_LINE.getId())
                .setValueType(MboParameters.ValueType.STRING)
                .setXslName(VENDOR_LINE.getXslName())
                .build());

        List<SimplifiedOfferParameter> simplifiedOfferParameters = Collections.singletonList(
                SimplifiedOfferParameter.forOffer(VENDOR_LINE.getId(), VENDOR_LINE.getXslName(), SOME_LINE,
                        OfferParameterType.STRING));

        DataCampOffer.Offer.Builder offerBuilder = OffersGenerator.generateOfferBuilder(simplifiedOfferParameters);
        offer = offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .setOfferId(SHOP_SKU)).build();

        ModelData data = new ModelData(ModelStorage.Model.newBuilder().build(), true,
                SHOP_SKU);
        WrapperGroupsHolder offerWrapperGroup
                = WrapperGroupsFactory.getOfferWrapperGroup(offer, data, categoryData, new HashSet<>());
        List<BaseParameterWrapper> parameterWrappers = offerWrapperGroup.getParameterWrappers();
        //Vendor, vendor_line, use_name_as_title, isCsku
        assertThat(parameterWrappers).hasSize(4);
        ModelFromOfferBuilder builder = ModelFromOfferBuilder
                .builder(ModelStorage.Model.newBuilder().build(), true, categoryData, SUPPLIER_ID);
        //Выбираем обёртку для VENDOR_LINE
        parameterWrappers.stream()
                .filter(parameterWrapper -> VENDOR_LINE.getId().equals(parameterWrapper.getParamId()))
                .findFirst().get().putValuesInSkuAndModel(builder);
        ModelStorage.Model model = builder.build();
        List<ModelStorage.ParameterValueHypothesis> parameterValueHypotheses =
                model.getParameterValueHypothesisList().stream()
                .filter(parameterValueHypothesis -> VENDOR_LINE.getId()
                        .equals(parameterValueHypothesis.getParamId())).collect(Collectors.toList());
        assertThat(parameterValueHypotheses).hasSize(1);
        assertThat(parameterValueHypotheses.get(0).getOwnerId()).isEqualTo(SUPPLIER_ID);
        assertThat(parameterValueHypotheses.get(0).getStrValue(0).getName()).isEqualTo(SOME_LINE);
        assertThat(model.getParameterValuesList()).isEmpty();
    }

    @Test
    public void whenSkippedParameterInOfferThenDoNotProcess() {
        when(categoryData.containsParam(VOLUME.getId())).thenReturn(true);
        when(categoryData.isSkuParameter(VOLUME.getId())).thenReturn(true);
        when(categoryData.getParamById(VOLUME.getId())).thenReturn(MboParameters.Parameter.newBuilder()
                .setId(VOLUME.getId())
                .setValueType(MboParameters.ValueType.NUMERIC)
                .setXslName(VOLUME.getXslName())
                .build());

        List<SimplifiedOfferParameter> simplifiedOfferParameters = List.of(
                SimplifiedOfferParameter.forOffer(VOLUME.getId(), VOLUME.getXslName(), "20.gg1",
                        OfferParameterType.STRING),
                SimplifiedOfferParameter.forOffer(VENDOR.getId(), VENDOR.getXslName(), "Some vendor",
                        OfferParameterType.STRING));

        DataCampOffer.Offer.Builder offerBuilder = OffersGenerator.generateOfferBuilder(simplifiedOfferParameters);
        offer = offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(SUPPLIER_ID)
                .setOfferId(SHOP_SKU)).build();

        ModelData data = new ModelData(ModelStorage.Model.newBuilder().build(), true,
                SHOP_SKU);
        WrapperGroupsHolder offerWrapperGroup
                = WrapperGroupsFactory.getOfferWrapperGroup(offer, data, categoryData, Set.of(VOLUME.getId()));
        List<BaseParameterWrapper> parameterWrappers = offerWrapperGroup.getParameterWrappers();
        //Vendor, use_name_as_title, isCsku
        assertThat(parameterWrappers).hasSize(3);
        ModelFromOfferBuilder builder = ModelFromOfferBuilder
                .builder(ModelStorage.Model.newBuilder().build(), true, categoryData, SUPPLIER_ID);
        //Выбираем обёртку для VENDOR_LINE
        long volumeWrappersCount = parameterWrappers.stream()
                .filter(parameterWrapper -> VOLUME.getId().equals(parameterWrapper.getParamId()))
                .count();
        assertThat(volumeWrappersCount).isEqualTo(0);
    }
}
