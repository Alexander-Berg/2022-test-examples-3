package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author danfertev
 * @since 17.05.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SupplierValidatorTest {
    private SupplierValidator supplierValidator;
    private ModelValidationContext context;

    @Before
    public void setUp() throws Exception {
        supplierValidator = new SupplierValidator();
        context = mock(ModelValidationContext.class);

        when(context.isForcedSupplierId()).thenReturn(false);
    }

    @Test
    public void missingSupplierId() {
        CommonModel model = CommonModelBuilder.newBuilder()
            .id(1L)
            .currentType(CommonModel.Source.PARTNER)
            .getModel();

        List<ModelValidationError> errors = supplierValidator.validate(context, new ModelChanges(model),
            Collections.singletonList(model));

        assertThat(errors).containsExactlyInAnyOrder(SupplierValidator.createMissingError(model));
    }

    @Test
    public void hasNewSupplierId() {
        CommonModel model = CommonModelBuilder.newBuilder()
            .id(1L)
            .currentType(CommonModel.Source.PARTNER)
            .supplierId(2L)
            .getModel();

        List<ModelValidationError> errors = supplierValidator.validate(context, new ModelChanges(model),
            Collections.singletonList(model));

        assertThat(errors).isEmpty();
    }

    @Test
    public void hasNewSupplierIdFastSku() {
        CommonModel model = CommonModelBuilder.newBuilder()
                .id(1L)
                .currentType(CommonModel.Source.FAST_SKU)
                .supplierId(2L)
                .getModel();

        List<ModelValidationError> errors = supplierValidator.validate(context, new ModelChanges(model),
                Collections.singletonList(model));

        assertThat(errors).isEmpty();
    }

    @Test
    public void noSupplierIdFastSku() {
        CommonModel model = CommonModelBuilder.newBuilder()
                .id(1L)
                .currentType(CommonModel.Source.FAST_SKU)
                .getModel();

        List<ModelValidationError> errors = supplierValidator.validate(context, new ModelChanges(model),
                Collections.singletonList(model));

        assertThat(errors).isNotEmpty();
    }

    @Test
    public void updatedChangedCritical() {
        CommonModel before = CommonModelBuilder.newBuilder()
            .id(1L)
            .currentType(CommonModel.Source.PARTNER)
            .supplierId(2L)
            .getModel();

        CommonModel after = new CommonModel(before);
        after.setSupplierId(3L);

        List<ModelValidationError> errors = supplierValidator.validate(context, new ModelChanges(before, after),
            Collections.singletonList(after));

        assertThat(errors).containsExactlyInAnyOrder(SupplierValidator.createChangedError(after, true, 2L, 3L));
    }

    @Test
    public void updatedChangedForced() {
        CommonModel before = CommonModelBuilder.newBuilder()
            .id(1L)
            .currentType(CommonModel.Source.PARTNER)
            .supplierId(2L)
            .getModel();

        CommonModel after = new CommonModel(before);
        after.setSupplierId(3L);

        when(context.isForcedSupplierId()).thenReturn(true);

        List<ModelValidationError> errors = supplierValidator.validate(context, new ModelChanges(before, after),
            Collections.singletonList(after));

        assertThat(errors).containsExactlyInAnyOrder(SupplierValidator.createChangedError(after, false, 2L, 3L));
    }

    @Test
    public void notUpdatedSupplierId() {
        CommonModel before = CommonModelBuilder.newBuilder()
            .id(1L)
            .currentType(CommonModel.Source.PARTNER)
            .supplierId(2L)
            .getModel();

        CommonModel after = new CommonModel(before);

        List<ModelValidationError> errors = supplierValidator.validate(context, new ModelChanges(before, after),
            Collections.singletonList(after));

        assertThat(errors).isEmpty();
    }

    @Test
    public void setNewSupplierId() {
        CommonModel before = CommonModelBuilder.newBuilder()
            .id(1L)
            .currentType(CommonModel.Source.PARTNER)
            .getModel();

        CommonModel after = new CommonModel(before);
        after.setSupplierId(2L);

        List<ModelValidationError> errors = supplierValidator.validate(context, new ModelChanges(before, after),
            Collections.singletonList(after));

        assertThat(errors).isEmpty();
    }
}
