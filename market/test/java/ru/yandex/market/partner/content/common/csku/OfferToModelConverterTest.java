package ru.yandex.market.partner.content.common.csku;


import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import Market.DataCamp.DataCampOffer;
import com.googlecode.protobuf.format.JsonFormat;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.judge.Action;
import ru.yandex.market.partner.content.common.csku.judge.Decision;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.csku.judge.ModelData;
import ru.yandex.market.partner.content.common.csku.util.UniqueParameterValue;
import ru.yandex.market.partner.content.common.csku.util.UniqueParameterValueHypothesis;
import ru.yandex.market.partner.content.common.csku.wrappers.BaseParameterWrapper;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.export.MboParameters.Category;
import static ru.yandex.market.mbo.export.MboParameters.GetCategoryParametersResponse;
import static ru.yandex.market.partner.content.common.csku.util.DcpSpecialParameterCreator.DCP_SPECIAL_IDS;

public class OfferToModelConverterTest {
    // Подкраска для царапин и сколов Aston Martin 6156, A6156ABB, HG6156BM - Tourmalin Blue
    // (Базовая краска, Двухслойный металлик, Синий)
    private static final String COLOR_OFFER_FILE = "/tourmalin_blue_color.json";
    private static final String COLOR_MODEL_FILE = "/tourmalin_model.json";
    private static final String COLOR_SKU_FILE = "/tourmalin_sku.json";
    public static final String CATEGORY_PARAMETERS_FILE = "/category_parameters.json";  //categoryId=14369617
    private static final int SUPPLIER_ID = 123; //3698794


    @Test
    public void overallTestForEmptyModel() throws IOException {
        DataCampOffer.Offer offer = loadOffer();
        //Данные об sku, model - из тикета 324472327
        Judge judge = new Judge();
        CategoryData categoryData = loadCategoryData();
        assertParameters(offer, judge, categoryData, loadModel(COLOR_MODEL_FILE), false);
        assertParameters(offer, judge, categoryData, loadModel(COLOR_SKU_FILE), true);
    }

    private void assertParameters(DataCampOffer.Offer offer,
                                  Judge judge,
                                  CategoryData categoryData,
                                  ModelStorage.Model loadedModel,
                                  boolean isSku
    ) {
        String shopSku = offer.getIdentifiers().getOfferId();
        Integer groupId = offer.getContent().getPartner().getOriginal().getGroupId().getValue();

        ModelStorage.Model model = ModelStorage.Model.newBuilder().build();
        ModelData modelData = new ModelData(model, isSku, shopSku);

        Map<BaseParameterWrapper, Decision> exactParameterWrapperDecisionMap =
                judge.calculateAllowedModelChanges(offer, modelData, categoryData, new HashSet<>());

        ModelFromOfferBuilder builder = ModelFromOfferBuilder.builder(
                model, modelData.isSku(), categoryData, SUPPLIER_ID);
        exactParameterWrapperDecisionMap.forEach((wrapper, decision) -> {
            if (!Action.NONE.equals(decision.getAllowedAction())) {
                wrapper.putValuesInSkuAndModel(builder);
            }
        });
        ModelStorage.Model updatedModel = builder.build();
        Map<Boolean, Set<UniqueParameterValue>> uniqueUpdatedValues = updatedModel.getParameterValuesList().stream()
                .map(UniqueParameterValue::new)
                .collect(groupingBy(upv -> DCP_SPECIAL_IDS.contains(upv.getValue().getParamId()), HashMap::new,
                        toSet()));

        Map<Boolean, Set<UniqueParameterValue>> uniqueLoadedValues = loadedModel.getParameterValuesList().stream()
                .map(UniqueParameterValue::new)
                .collect(groupingBy(upv -> DCP_SPECIAL_IDS.contains(upv.getValue().getParamId()), HashMap::new,
                        toSet()));

        // DCP values
        Set<UniqueParameterValue> dcpUpdatedValues = uniqueUpdatedValues.get(true);
        Set<UniqueParameterValue> dcpLoadedValues = uniqueLoadedValues.get(true);
        assertThat(dcpLoadedValues).containsExactlyInAnyOrderElementsOf(dcpUpdatedValues);

        // Other values
        Set<UniqueParameterValue> updatedValues = uniqueUpdatedValues.get(false);
        Set<UniqueParameterValue> loadedValues = uniqueLoadedValues.get(false);
        if (!isSku) {
            // TODO: Remove after https://st.yandex-team.ru/MCR-3310
            loadedValues = loadedValues.stream()
                    .filter(upv -> KnownParameters.PSKU_SOURCE_CREATE.getId() != upv.getValue().getParamId())
                    .filter(upv -> KnownParameters.PSKU_SOURCE_EDIT.getId() != upv.getValue().getParamId())
                    .collect(toSet());
        }
        // SKU from prod additionally contains parameters from enriched_offer block
        assertThat(updatedValues).isSubsetOf(loadedValues);

        // Hypotheses
        Set<UniqueParameterValueHypothesis> updatedHypotheses = updatedModel.getParameterValueHypothesisList().stream()
                .map(UniqueParameterValueHypothesis::new)
                .collect(toSet());

        Set<UniqueParameterValueHypothesis> loadedHypotheses = loadedModel.getParameterValueHypothesisList().stream()
                .map(UniqueParameterValueHypothesis::new)
                .collect(toSet());

        assertThat(loadedHypotheses).containsExactlyInAnyOrderElementsOf(updatedHypotheses);
    }

    @NotNull
    private CategoryData loadCategoryData() throws IOException {
        InputStreamReader streamReader = new InputStreamReader(
                Objects.requireNonNull(OfferToModelConverterTest.class.getResourceAsStream(CATEGORY_PARAMETERS_FILE)));
        GetCategoryParametersResponse.Builder builder = GetCategoryParametersResponse.newBuilder();
        JsonFormat.merge(streamReader, builder);
        Category category = builder.build().getCategoryParameters();
        return CategoryData.build(category);
    }

    @NotNull
    private DataCampOffer.Offer loadOffer() throws IOException {
        InputStreamReader streamReader = new InputStreamReader(
                Objects.requireNonNull(OfferToModelConverterTest.class.getResourceAsStream(COLOR_OFFER_FILE)));
        DataCampOffer.Offer.Builder builder = DataCampOffer.Offer.newBuilder();
        JsonFormat.merge(streamReader, builder);
        return builder.build();
    }

    @NotNull
    private ModelStorage.Model loadModel(String file) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(
                Objects.requireNonNull(OfferToModelConverterTest.class.getResourceAsStream(file)));
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
        JsonFormat.merge(streamReader, builder);
        return builder.build();
    }

}
