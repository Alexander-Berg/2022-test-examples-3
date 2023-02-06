package ru.yandex.market.mbo.db.modelstorage.validation;

import io.qameta.allure.Issue;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.mbo_category.MboCategoryMappingsService;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.Mapping;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.OfferMappingDestination;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.SupplierOffer;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getSkuBuilder;

/**
 * Test of {@link SkuMappingValidator}.
 *
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(MockitoJUnitRunner.class)
public class SkuMappingValidatorTest extends BaseValidatorTestClass {

    private SkuMappingValidator validator;

    @Mock
    private ModelValidationContext context;

    @Mock
    private MboCategoryMappingsService mboCategoryMappingsService;

    private CommonModel model;
    private CommonModel modelSku;

    @Before
    public void setup() {
        validator = new SkuMappingValidator(mboCategoryMappingsService);

        when(context.isSkuMappingEditingAllowed()).thenReturn(true);

        when(mboCategoryMappingsService.searchMappingsByMarketSkuId(
            any(Collection.class), eq(false), eq(false))).thenAnswer(i -> {
            List<SupplierOffer> result = new ArrayList<>();
            Collection<Long> requestedIds = i.getArgument(0);
            if (requestedIds.contains(2L)) {
                result.add(createMapping(1, "ssku1", 2L, "skuTitle1"));
                result.add(createMapping(1, "ssku1", 2L, "skuTitle1"));
                result.add(createMapping(2, "ssku2", 2L, "skuTitle2"));
            }
            if (requestedIds.contains(3L)) {
                result.add(createMapping(2, "ssku3", 3L, "skuTitle3"));
            }
            return result;
        });

        CommonModel sku2 = getSkuBuilder(1)
            .id(2)
            .endModel();
        model = createModel(1, m -> {
            m.startModelRelation()
                .id(2)
                .categoryId(1)
                .type(ModelRelation.RelationType.SKU_MODEL)
                .endModelRelation();
        });
        modelSku = createModel(3, m -> {
            m.startParameterValue()
                .paramId(1L)
                .xslName(XslNames.IS_SKU)
                .type(Param.Type.BOOLEAN)
                .booleanValue(true, 2L)
                .endParameterValue();
        });
        storage.saveModels(ModelSaveGroup.fromModels(model, modelSku, sku2), saveContext);
    }

    @Test
    public void testNotDeleted() {
        CommonModel skuNotDeleted = getSkuBuilder(1)
            .id(2)
            .endModel();

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(model), Arrays.asList(model, skuNotDeleted));

        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testDeletedSku() {
        CommonModel skuDeleted = getSkuBuilder(1)
            .id(2)
            .deleted(true)
            .endModel();

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(model), Arrays.asList(model, skuDeleted));

        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            error(createMapping(1, "ssku1", 2L, "skuTitle1"), true, true),
            error(createMapping(2, "ssku2", 2L, "skuTitle2"), true, true)
        );
    }

    @Test
    public void testDeletedModelIsSku() {
        CommonModel modelDeleted = new CommonModel(modelSku);
        modelDeleted.setDeleted(true);

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(modelDeleted), Collections.singletonList(modelDeleted));

        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            error(createMapping(2, "ssku3", 3L, "skuTitle3"), true, true)
        );
    }

    @Test
    public void testModelIsSkuSetFalse() {
        CommonModel notSkuAnymore = createModel(3, m -> {
            m.startParameterValue()
                .paramId(1L)
                .xslName(XslNames.IS_SKU)
                .type(Param.Type.BOOLEAN)
                .booleanValue(false, 2L)
                .endParameterValue();
        });

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(notSkuAnymore), Collections.singletonList(notSkuAnymore));

        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            error(createMapping(2, "ssku3", 3L, "skuTitle3"), true, true)
        );
    }

    @Test
    public void testModelIsSkuSetNoData() {
        CommonModel notSkuAnymore = createModel(3);

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(notSkuAnymore), Collections.singletonList(notSkuAnymore));

        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            error(createMapping(2, "ssku3", 3L, "skuTitle3"), true, true)
        );
    }

    @Test
    public void testDeletedAndConfirmed() {
        when(context.isForcedRemoval()).thenReturn(true);
        CommonModel skuDeleted = getSkuBuilder(1)
            .id(2)
            .deleted(true)
            .endModel();

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(model), Arrays.asList(model, skuDeleted));

        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            error(createMapping(1, "ssku1", 2L, "skuTitle1"), false, true),
            error(createMapping(2, "ssku2", 2L, "skuTitle2"), false, true)
        );
    }

    @Test
    public void forceSomeIgnoreValidatorsOnRemove() {
        when(context.isForcedRemoval()).thenReturn(false);
        when(context.isForceSomeIgnoreValidatorsOnRemove()).thenReturn(true);
        CommonModel skuDeleted = getSkuBuilder(1)
            .id(2)
            .deleted(true)
            .endModel();

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(model), Arrays.asList(model, skuDeleted));

        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            error(createMapping(1, "ssku1", 2L, "skuTitle1"), false, true),
            error(createMapping(2, "ssku2", 2L, "skuTitle2"), false, true)
        );
    }

    @Test
    @Issue("MBO-18395")
    public void testDeletedIfSkuMappingEditingNotAllowed() {
        when(context.isSkuMappingEditingAllowed()).thenReturn(false);
        CommonModel skuDeleted = getSkuBuilder(1)
            .id(2)
            .deleted(true)
            .endModel();

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(model), Arrays.asList(model, skuDeleted));

        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            errorWithCustomMessage(createMapping(1, "ssku1", 2L, "skuTitle1"), true, false,
                "SKU привязан к товару %{SUPPLIER_SKU_ID} поставщика %{SUPPLIER_ID}: %{SKU_MAPPING_TITLE}. " +
                    "Принудительное сохранение доступно только для пользователей с ролью SKU mapping operator."),
            errorWithCustomMessage(createMapping(2, "ssku2", 2L, "skuTitle2"), true, false,
                "SKU привязан к товару %{SUPPLIER_SKU_ID} поставщика %{SUPPLIER_ID}: %{SKU_MAPPING_TITLE}. " +
                    "Принудительное сохранение доступно только для пользователей с ролью SKU mapping operator.")
        );
    }

    @Test
    @Issue("MBO-18395")
    public void testDeletedAndConfirmedIfSkuMappingEditingNotAllowed() {
        when(context.isForcedRemoval()).thenReturn(true);
        when(context.isSkuMappingEditingAllowed()).thenReturn(false);
        CommonModel skuDeleted = getSkuBuilder(1)
            .id(2)
            .deleted(true)
            .endModel();

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(model), Arrays.asList(model, skuDeleted));

        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            errorWithCustomMessage(createMapping(1, "ssku1", 2L, "skuTitle1"), true, false,
                "SKU привязан к товару %{SUPPLIER_SKU_ID} поставщика %{SUPPLIER_ID}: %{SKU_MAPPING_TITLE}. " +
                    "Принудительное сохранение доступно только для пользователей с ролью SKU mapping operator."),
            errorWithCustomMessage(createMapping(2, "ssku2", 2L, "skuTitle2"), true, false,
                "SKU привязан к товару %{SUPPLIER_SKU_ID} поставщика %{SUPPLIER_ID}: %{SKU_MAPPING_TITLE}. " +
                    "Принудительное сохранение доступно только для пользователей с ролью SKU mapping operator.")
        );
    }


    private static ModelValidationError error(SupplierOffer mapping, boolean isCritical, boolean allowForce) {
        return errorWithCustomMessage(mapping, isCritical, allowForce,
            "SKU привязан к товару %{SUPPLIER_SKU_ID} поставщика %{SUPPLIER_ID}: %{SKU_MAPPING_TITLE}. " +
                "Если сохранить принудительно - привязка станет недействительна.");
    }

    private static ModelValidationError errorWithCustomMessage(
        SupplierOffer mapping, boolean isCritical, boolean allowForce, String localizedMessagePattern) {

        return new ModelValidationError(
            mapping.getApprovedMapping().getSkuId(),
            ModelValidationError.ErrorType.SKU_HAS_DEPENDENT_DATA,
            ModelValidationError.ErrorSubtype.SKU_HAS_MAPPINGS,
            isCritical,
            allowForce)
            .addLocalizedMessagePattern(localizedMessagePattern)
            .addParam(ModelStorage.ErrorParamName.SUPPLIER_ID, mapping.getBusinessId())
            .addParam(ModelStorage.ErrorParamName.SUPPLIER_SKU_ID, mapping.getShopSkuId())
            .addParam(ModelStorage.ErrorParamName.SKU_MAPPING_TITLE, mapping.getTitle());
    }

    private SupplierOffer createMapping(int shopId, String shopSkuId, long mskuId, String title) {
        Mapping mapping = new Mapping();
        mapping.setSkuId(mskuId);
        SupplierOffer offer = new SupplierOffer();
        offer.setBusinessId(shopId);
        offer.setShopSkuId(shopSkuId);
        offer.setApprovedMapping(mapping);
        offer.setTitle(title);
        offer.setDestination(OfferMappingDestination.BLUE);
        return offer;
    }
}
