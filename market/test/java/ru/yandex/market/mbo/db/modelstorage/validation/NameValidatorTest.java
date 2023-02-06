package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NameValidatorTest {
    private NameValidator nameValidator;
    private List<Word> testNames;
    private List<Word> correctNames;

    @Before
    public void setUp() {
        nameValidator = new NameValidator();
        testNames = Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "test\nname\t"));
        correctNames = Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "I am       normal"));
    }

    @Test
    public void nameWithTabOrEOLValidate() {
        // arrange
        CommonModel after = new CommonModel();
        after.addParameterValue(new ParameterValue(0, "name", Param.Type.STRING, null,
            null, null, testNames, null));
        ModelChanges modelChanges = new ModelChanges(null, after);

        // act
        List<ModelValidationError> errors = nameValidator.validate(null, modelChanges, null);

        // assert
        List<ModelValidationError> expected = Arrays.asList(
            new ModelValidationError(0L, ModelValidationError.ErrorType.NAME_HAS_EOL)
                .addLocalizedMessagePattern("Имя '%{OPTION_NAME}' содержит символ конца строки."),
            new ModelValidationError(0L, ModelValidationError.ErrorType.NAME_HAS_TAB)
                .addLocalizedMessagePattern("Имя '%{OPTION_NAME}' содержит символ табуляции."));
        expected = expected.stream()
            .map(x -> x.addParam(ModelStorage.ErrorParamName.OPTION_NAME, testNames.get(0).getWord()))
            .collect(Collectors.toList());

        Assert.assertEquals(expected, errors);
    }

    @Test
    public void incorrectNameWarnWhenUnchanged() {
        // arrange

        CommonModel after = new CommonModel();
        after.addParameterValue(new ParameterValue(0, "name", Param.Type.STRING, null, null, null, testNames,
            null));
        ModelChanges modelChanges = new ModelChanges(after, after);

        // act
        List<ModelValidationError> errors = nameValidator.validate(null, modelChanges, null);

        // assert
        List<ModelValidationError> expected = Arrays.asList(
            new ModelValidationError(0L, ModelValidationError.ErrorType.NAME_HAS_EOL, null, false) // <-- warn
                .addLocalizedMessagePattern("Имя '%{OPTION_NAME}' содержит символ конца строки."),
            new ModelValidationError(0L, ModelValidationError.ErrorType.NAME_HAS_TAB, null, false) // <-- warn
                .addLocalizedMessagePattern("Имя '%{OPTION_NAME}' содержит символ табуляции."));
        expected = expected.stream()
            .map(x -> x.addParam(ModelStorage.ErrorParamName.OPTION_NAME, testNames.get(0).getWord()))
            .collect(Collectors.toList());

        Assert.assertEquals(expected, errors);
    }

    @Test
    public void correctNameValidate() {
        // arrange
        CommonModel after = new CommonModel();
        after.addParameterValue(new ParameterValue(0, "name", Param.Type.STRING, null, null, null, correctNames, null));
        ModelChanges modelChanges = new ModelChanges(null, after);

        // act
        List<ModelValidationError> errors = nameValidator.validate(null, modelChanges, null);

        // assert
        List<ModelValidationError> expected = Collections.emptyList();

        Assert.assertEquals(expected, errors);
    }
}
