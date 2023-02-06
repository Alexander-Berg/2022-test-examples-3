package ru.yandex.market.mbo.db.modelstorage.validation;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.collections.Cu;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author danfertev
 * @since 28.02.2018
 */
@SuppressWarnings("checkstyle:magicNumber")
public class UniqueVendorCodeValidatorTest extends BaseUniqueStringParameterValidatorTestClass {

    @Before
    public void setup() {
        baseSetup(new UniqueVendorCodeValidator());
        when(context.findModelsWithVendorCodes(anyLong(), anyLong(), any(), any())).then(invocation -> {
            long vendorId = invocation.getArgument(1);
            List<Word> values = invocation.getArgument(2);
            List<CommonModel.Source> types = invocation.getArgument(3);

            return storage.getAllModels().stream()
                .filter(model -> types.contains(model.getCurrentType()))
                .filter(model -> vendorId == model.getVendorId())
                .filter(model -> {
                    ParameterValues paramValues = model.getParameterValues(validator.getParamName());
                    List<Word> otherValues = paramValues != null
                        ? paramValues.getValues().stream()
                            .flatMap(v -> v.getStringValue().stream())
                            .collect(Collectors.toList())
                        : Collections.emptyList();

                    return Cu.intersects(values, otherValues);
                })
                .collect(Collectors.toList());
        });
    }

    @Test
    public void testDuplicateSameVendor() {
        CommonModel model1 = createGuruWithVendor(2, 1, "a");
        CommonModel model2 = createGuruWithVendor(1, 1, "a");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model1),
            Arrays.asList(model1, model2));
        ModelValidationError error = error("a", 2, 1, true);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
    }

    @Test
    public void testDuplicateDifferentVendor() {
        CommonModel model1 = createGuruWithVendor(0, 1, "a");
        CommonModel model2 = createGuruWithVendor(1, 2, "a");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model1),
            Arrays.asList(model1, model2));
        Assertions.assertThat(errors).isEmpty();
    }

    private CommonModel createGuruWithVendor(long id, long vendorId, String... values) {
        return createModel(id, CommonModel.Source.SKU, b ->
            b.startParameterValue()
                    .paramId(1)
                    .xslName(validator.getParamName())
                    .words(values)
                .endParameterValue()
                .startParameterValue()
                    .paramId(1000)
                    .xslName(XslNames.VENDOR)
                    .optionId(vendorId)
                .endParameterValue()
        );
    }
}
