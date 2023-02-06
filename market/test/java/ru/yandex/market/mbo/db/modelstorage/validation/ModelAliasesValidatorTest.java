package ru.yandex.market.mbo.db.modelstorage.validation;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.ir.parser.matcher.tokenizers.StringValuesTokenizer;
import ru.yandex.market.mbo.db.modelstorage.StatsModelStorageService;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;

public class ModelAliasesValidatorTest {
    private BatchModelValidator aliasesValidator;
    private List<Word> testAliases;
    private ModelValidationContextStub context;

    private StatsModelStorageService statsModelStorageService = Mockito.mock(StatsModelStorageService.class);

    @Before
    public void setUp() {
        aliasesValidator = new ModelAliasesValidator();
        Word testAlias1 = new Word(Word.DEFAULT_LANG_ID, "test\nalias\t");
        Word testAlias2 = new Word(Word.DEFAULT_LANG_ID, "I am normal");
        testAliases = Arrays.asList(testAlias1, testAlias2);

        context = new ModelValidationContextStub(null);
        context.setStatsModelStorageService(statsModelStorageService);
    }

    private String normalize(String alias) {
        return alias.replace('\n', ' ').replace('\t', ' ');
    }

    @Test
    public void aliasesWithTabOrEOLValidate() {
        CommonModel after = new CommonModel();
        after.setSource(CommonModel.Source.GURU);
        after.setCurrentType(CommonModel.Source.GURU);
        after.addParameterValue(new ParameterValue(0, "aliases", Param.Type.STRING,
            null, null, null, testAliases, null));
        after.addParameterValue(new ParameterValue(0, "search_aliases", Param.Type.STRING,
            null, null, null, testAliases, null));


        ModelChanges modelChanges = new ModelChanges(null, after);

        Map<CommonModel, List<ModelValidationError>> errors = aliasesValidator.validate(context,
            singletonList(modelChanges), emptyList());

        List<ModelValidationError> expected = Arrays.asList(
            new ModelValidationError(0L, ModelValidationError.ErrorType.ALIAS_HAS_EOL)
                .addLocalizedMessagePattern("Алиас '%{ALIAS}' содержит символ конца строки."),
            new ModelValidationError(0L, ModelValidationError.ErrorType.ALIAS_HAS_TAB)
                .addLocalizedMessagePattern("Алиас '%{ALIAS}' содержит символ табуляции."));
        expected = expected.stream()
            .map(x -> x.addParam(ModelStorage.ErrorParamName.ALIAS, normalize(testAliases.get(0).getWord())))
            .collect(toList());

        Assert.assertEquals(expected, errors.values().stream().flatMap(List::stream).collect(toList()));
    }

    @Test
    public void aliasesDuplicatesWithOthers() {
        final long currentId = 13453L;
        final long foundId1 = 7778L;
        final long foundId2 = 8889L;

        CommonModel before = newGuruModelWithAliases(currentId, "old alias1", "old alias 1");
        CommonModel after = newGuruModelWithAliases(currentId, "75мг/3мл", "sleep 2 tfk 80х195",
            "100% dup", "good alias");

        List<CommonModel> indexFoundModels = Arrays.asList(
            newGuruModelWithAliases(foundId1, "75мг 3мл", "other ok alias"),
            newGuruModelWithAliases(foundId2, "sleep 2 tfk) /80х195", "100% dup")
        );

        Mockito.when(statsModelStorageService.getModelsByAliases(any(), anySet(), anySet(), anySet(), any()))
            .thenReturn(indexFoundModels);

        ModelChanges modelChanges = new ModelChanges(before, after);

        Collection<ModelValidationError> expectedErrors = new ArrayList<>();

        expectedErrors.addAll(
            createErrors(singletonList("75мг/3мл"), currentId, foundId1)
        );

        expectedErrors.addAll(
            createErrors(Arrays.asList("sleep 2 tfk 80х195", "100% dup"), currentId, foundId2)
        );

        Map<CommonModel, List<ModelValidationError>> errors = aliasesValidator.validate(context,
            singletonList(modelChanges), emptyList());
        Assertions.assertThat(errors.values().stream()
            .flatMap(List::stream)
            .collect(toList()))
            .containsExactlyInAnyOrderElementsOf(expectedErrors);
    }

    @Test
    public void aliasesWithSelfDuplicates() {
        final long currentId = 13453L;
        final long foundId1 = 7778L;
        final long foundId2 = 8889L;

        CommonModel before = newGuruModelWithAliases(currentId, "195/65r15 95t", "195/65 r15 95t",
            "195/65 r 15 95t"); // these all are old self-duplicates (when tokenized)

        CommonModel after = newGuruModelWithAliases(currentId, "195/65r15 95t", "195/65 r15 95t",
            "195/65 r 15 95t",
            // new tokenized-duplicates below:
            "195///65r15 95t", "195//65 r15 95t",
            // other model duplicates below:
            "sleep 2 tfk 80х195", "6x15 5/112 et35 d67,1 (bd)");

        List<CommonModel> indexFoundModels = Arrays.asList(
            newGuruModelWithAliases(foundId1, "6x15 5/112 et35 d67.1 bd"),
            newGuruModelWithAliases(foundId2, "sleep 2 tfk) /80х195")
        );

        Collection<ModelValidationError> expectedErrors = new ArrayList<>();

        expectedErrors.addAll(
            // all self-duplicates are shown as errors:
            createErrors(Arrays.asList("195///65r15 95t", "195//65 r15 95t",
                "195/65r15 95t", "195/65 r15 95t", "195/65 r 15 95t"), currentId, currentId)
        );

        expectedErrors.addAll(
            createErrors(singletonList("6x15 5/112 et35 d67,1 (bd)"), currentId, foundId1)
        );
        expectedErrors.addAll(
            createErrors(singletonList("sleep 2 tfk 80х195"), currentId, foundId2)
        );

        Mockito.when(statsModelStorageService.getModelsByAliases(any(), anySet(), anySet(), anySet(), any()))
            .thenReturn(indexFoundModels);

        ModelChanges modelChanges = new ModelChanges(before, after);

        Map<CommonModel, List<ModelValidationError>> errors =
            aliasesValidator.validate(context, singletonList(modelChanges), emptyList());
        Assertions.assertThat(errors.values().stream()
            .flatMap(List::stream)
            .collect(toList()))
            .containsExactlyInAnyOrderElementsOf(expectedErrors);

    }

    private CommonModel newGuruModelWithAliases(Long id, String... aliases) {
        CommonModel model = new CommonModel();
        model.setId(id);
        model.setSource(CommonModel.Source.GURU);
        model.setCurrentType(CommonModel.Source.GURU);
        List<Word> aliasesWords = new ArrayList<>();
        for (String alias : aliases) {
            aliasesWords.add(new Word(Word.DEFAULT_LANG_ID, alias));
        }
        model.addParameterValue(new ParameterValue(0, XslNames.ALIASES, Param.Type.STRING,
            null, null, null, aliasesWords, null));
        return model;
    }

    private Collection<ModelValidationError> createErrors(Collection<String> aliases, long modelId, long anotherId) {
        return aliases.stream()
            .map(alias -> alias + String.format(" (token: %s)",
                StringValuesTokenizer.tokenize(alias).toSearchValue()))
            .map(alias -> new ModelValidationError(modelId, ModelValidationError.ErrorType.DUPLICATE_NAME)
                .addLocalizedMessagePattern("Алиас '%{ALIAS}' дублируется в модели/модификации %{MODEL_ID}.")
                .addParam(ModelStorage.ErrorParamName.ALIAS, alias)
                .addParam(ModelStorage.ErrorParamName.MODEL_ID, anotherId)
            ).collect(toList());
    }
}
