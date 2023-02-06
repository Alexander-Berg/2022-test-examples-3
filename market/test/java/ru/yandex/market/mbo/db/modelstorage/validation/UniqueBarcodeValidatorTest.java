package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import ru.yandex.common.util.collections.Cu;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test of {@link UniqueBarcodeValidator}.
 *
 * @author s-ermakov
 * @author danfertev
 */
public class UniqueBarcodeValidatorTest extends BaseUniqueStringParameterValidatorTestClass {
    @Before
    public void setup() {
        baseSetup(new UniqueBarcodeValidator());
        when(context.findModelsBarcodes(any(), any())).then(invocation -> {
            List<Word> barcodes = invocation.getArgument(0);
            List<CommonModel.Source> types = UniqueBarcodeValidator.SUPPORTED_MODEL_TYPES;

            return storage.getAllModels().stream()
                .filter(model -> types.contains(model.getCurrentType()))
                .filter(model -> {
                    ParameterValues barcodeValues = model.getParameterValues(XslNames.BAR_CODE);
                    List<Word> otherBarcodes = barcodeValues != null
                        ? barcodeValues.getValues().stream()
                            .flatMap(v -> v.getStringValue().stream())
                            .collect(Collectors.toList())
                        : Collections.emptyList();

                    return Cu.intersects(barcodes, otherBarcodes);
                })
                .collect(Collectors.toList());
        });
    }
}
