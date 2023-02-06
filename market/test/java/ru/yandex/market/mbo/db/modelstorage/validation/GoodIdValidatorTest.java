package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.GoodIdSaveService;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param.Type;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class GoodIdValidatorTest {

    @Mock
    private GoodIdSaveService goodIdService;
    private GoodIdValidator validator;

    @Before
    public void setup() {
        when(goodIdService.alreadyExists(anyString())).thenReturn(Collections.emptyList());
        validator = new GoodIdValidator(goodIdService);
    }

    @Test
    public void testWrongPatternGivesError() {
        CommonModel model = model(0, "damn good id");
        List<ModelValidationError> errors = validator.validate(
            null,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(model.getId(), ErrorType.INVALID_FORMAT_GOOD_ID, null, true)
            .addLocalizedMessagePattern("Идентификатор товара %{GOOD_ID} имеет неправильный формат.")
            .addParam(ModelStorage.ErrorParamName.GOOD_ID, "damn good id")
        );
    }

    @Test
    public void testSameIdsOnModelError() {
        CommonModel model = model(0,
            "deadbeef00000000deadbeef00000000",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "deadbeef00000000deadbeef00000000");
        List<ModelValidationError> errors = validator.validate(
            null,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(
                model.getId(), ErrorType.DUPLICATE_GOOD_ID, null, true)
                .addLocalizedMessagePattern("Среди добавленных good_id есть дубликаты.")
        );
    }

    @Test
    public void testChangesDetection() {
        // A. New model always generates crits.
        assertThat(validator.validate(
            null,
            new ModelChanges(null, model(42, "1234")),
            null
        )).containsExactlyInAnyOrder(
            new ModelValidationError(42L, ErrorType.INVALID_FORMAT_GOOD_ID, null, true) // <-- crit
                .addLocalizedMessagePattern("Идентификатор товара %{GOOD_ID} имеет неправильный формат.")
                .addParam(ModelStorage.ErrorParamName.GOOD_ID, "1234")
        );

        // B. Update of same model in same category gives warning on unchanged goodId
        CommonModel unchangedModelBefore = model(42, "1234");
        CommonModel unchangedModelAfter = model(42, "1234"); // purposely bad, but same good-ids
        assertThat(validator.validate(
            null,
            new ModelChanges(unchangedModelBefore, unchangedModelAfter),
            null
        )).containsExactlyInAnyOrder(
            new ModelValidationError(42L, ErrorType.INVALID_FORMAT_GOOD_ID, null, false) // <-- warn
                .addLocalizedMessagePattern("Идентификатор товара %{GOOD_ID} имеет неправильный формат.")
                .addParam(ModelStorage.ErrorParamName.GOOD_ID, "1234")
        );

        // C. Update of the same model in same category gives crit on goodId change
        CommonModel changedModelBefore = model(42, "говорят, на хелген напал графон");
        CommonModel changedModelAfter = model(42, "овладевание"); // purposely bad and changed good-ids
        assertThat(validator.validate(
            null,
            new ModelChanges(changedModelBefore, changedModelAfter),
            null
        )).containsExactlyInAnyOrder(
            new ModelValidationError(42L, ErrorType.INVALID_FORMAT_GOOD_ID, null, true) // <-- crit
                .addLocalizedMessagePattern("Идентификатор товара %{GOOD_ID} имеет неправильный формат.")
                .addParam(ModelStorage.ErrorParamName.GOOD_ID, "овладевание")
        );
    }

    private CommonModel model(long id, String... goodIds) {
        CommonModel model = new CommonModel();
        model.setId(id);
        model.setCategoryId(100500);
        for (String goodId : goodIds) {
            ParameterValue value = new ParameterValue();
            value.setXslName(XslNames.GOOD_ID);
            value.setStringValue(WordUtil.defaultWords(goodId));
            value.setType(Type.STRING);
            model.addParameterValue(value);
        }
        return model;
    }
}
