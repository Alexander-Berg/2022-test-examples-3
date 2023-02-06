package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.tovartree.NameToAliasesSettings;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.class)
public class ModelCreateWithAliasTest extends BaseGroupStorageUpdatesTest {

    private static final String MODEL_TITLE = "Лунная призма";
    private static final String MODEL_ALIAS1 = "призма возмездие луна";
    private static final String MODEL_ALIAS2 = "купить призма лунный мощь сила две SIM карты для";

    private CategoryParam param;
    private CommonModel model;

    @Before
    public void setup() {
        param = CategoryParamBuilder.newBuilder()
            .setXslName(XslNames.ALIASES)
            .setId(1L)
            .setType(Param.Type.STRING)
            .setCategoryHid(123L)
            .setUseForGuru(true)
            .build();

        guruService.addCategoryParam(param);

        model = CommonModelBuilder.newBuilder()
            .id(0L)
            .category(123L)
            .currentType(CommonModel.Source.GURU)
            .title(MODEL_TITLE)
            .getModel();
        guruService.addNameToAliasesSettingByCategory(123, NameToAliasesSettings.COPY_NAME_TO_ALIAS);

        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.setHid(124);
        categoryEntities.addParameter(
            CategoryParamBuilder.newBuilder()
                .setId(1)
                .setXslName(XslNames.ALIASES)
                .setType(Param.Type.STRING)
                .setUseForGuru(true)
                .build());
        guruService.addCategoryEntities(categoryEntities);
    }

    @Test
    public void testNameCopiedToAliasIfNoneProvided() {
        GroupOperationStatus status = storage.saveModel(model, context);

        CommonModel savedModel = checkOkAndGetModel(status);
        assertThat(aliases(savedModel)).containsExactlyInAnyOrder(MODEL_TITLE);
        assertThat(modifSource(savedModel)).containsExactlyInAnyOrder(ModificationSource.BACKEND_RULE);
    }

    @Test
    public void testNotCopiedIfAliasExists() {
        addAlias(model, MODEL_ALIAS1, ModificationSource.OPERATOR_FILLED);
        GroupOperationStatus status = storage.saveModel(model, context);

        CommonModel savedModel = checkOkAndGetModel(status);
        assertThat(aliases(savedModel)).containsExactlyInAnyOrder(MODEL_ALIAS1);
        assertThat(modifSource(savedModel)).containsExactlyInAnyOrder(ModificationSource.OPERATOR_FILLED);
    }

    @Test
    public void testNotCopiedIfSeveralAliasesExist() {
        addAlias(model, MODEL_ALIAS1, ModificationSource.OPERATOR_FILLED);
        addAlias(model, MODEL_ALIAS2, ModificationSource.OPERATOR_COPIED);
        GroupOperationStatus status = storage.saveModel(model, context);

        CommonModel savedModel = checkOkAndGetModel(status);
        assertThat(aliases(savedModel)).containsExactlyInAnyOrder(MODEL_ALIAS1, MODEL_ALIAS2);
        //т.к. алиасы - это words в single value, modification source у них один, последний применённый
        assertThat(modifSource(savedModel)).containsExactlyInAnyOrder(ModificationSource.OPERATOR_COPIED);
    }

    @Test
    public void whenModelCopyNameToAliasAbsentShouldCopyNameToAlias() {
        model = CommonModelBuilder.newBuilder()
            .id(0L)
            .category(124L)
            .currentType(CommonModel.Source.GURU)
            .title(MODEL_TITLE)
            .getModel();

        guruService.addNameToAliasesSettingByCategory(124, null);

        GroupOperationStatus status = storage.saveModel(model, context);

        CommonModel savedModel = checkOkAndGetModel(status);
        assertThat(aliases(savedModel)).containsExactlyInAnyOrder(MODEL_TITLE);
        assertThat(modifSource(savedModel)).containsExactlyInAnyOrder(ModificationSource.BACKEND_RULE);
    }

    @Test
    public void whenCopyNameAndSourceAbsentShouldCopyNameToAlias() {
        model = CommonModelBuilder.newBuilder()
            .id(0L)
            .category(124L)
            .currentType(CommonModel.Source.GURU)
            .title(MODEL_TITLE)
            .getModel();

        guruService.addNameToAliasesSettingByCategory(124, NameToAliasesSettings.COPY_NAME_TO_ALIAS);

        GroupOperationStatus status = storage.saveModel(model, context);

        CommonModel savedModel = checkOkAndGetModel(status);
        assertThat(aliases(savedModel)).containsExactlyInAnyOrder(MODEL_TITLE);
        assertThat(modifSource(savedModel)).containsExactlyInAnyOrder(ModificationSource.BACKEND_RULE);
    }

    @Test
    public void whenTurnOffCopyNameAndSourceAbsentShouldNotCopyNameToAlias() {
        model = CommonModelBuilder.newBuilder()
            .id(0L)
            .category(124L)
            .currentType(CommonModel.Source.GURU)
            .title(MODEL_TITLE)
            .getModel();

        guruService.addNameToAliasesSettingByCategory(124, NameToAliasesSettings.TURN_OFF_COPY_NAME_TO_ALIAS);

        GroupOperationStatus status = storage.saveModel(model, context);

        CommonModel savedModel = checkOkAndGetModel(status);
        assertThat(aliases(savedModel)).isEmpty();
    }

    @Test
    public void whenTurnOffCopyNameAndSourceVendorShouldNotCopyNameToAlias() {
        addAlias(model, MODEL_ALIAS1, ModificationSource.OPERATOR_FILLED);

        guruService.addNameToAliasesSettingByCategory(123,
            NameToAliasesSettings.TURN_OFF_COPY_NAME_TO_ALIAS_FOR_RECEIVING_DATA);
        context.setSource(AuditAction.Source.VENDOR);

        GroupOperationStatus status = storage.saveModel(model, context);

        CommonModel savedModel = checkOkAndGetModel(status);
        assertThat(aliases(savedModel)).containsExactlyInAnyOrder(MODEL_ALIAS1);
        assertThat(modifSource(savedModel)).containsExactly(ModificationSource.OPERATOR_FILLED);
    }

    @Test
    public void whenTurnOffCopyNameAndSourceAutogenerationShouldCopyNameToAlias() {
        model = CommonModelBuilder.newBuilder()
            .id(0L)
            .category(124L)
            .currentType(CommonModel.Source.GURU)
            .title(MODEL_ALIAS1)
            .getModel();

        guruService.addNameToAliasesSettingByCategory(124,
            NameToAliasesSettings.TURN_OFF_COPY_NAME_TO_ALIAS_FOR_RECEIVING_DATA);
        context.setSource(AuditAction.Source.AUTOGENERATION);

        GroupOperationStatus status = storage.saveModel(model, context);

        CommonModel savedModel = checkOkAndGetModel(status);
        assertThat(aliases(savedModel)).containsExactlyInAnyOrder(MODEL_ALIAS1);
        assertThat(modifSource(savedModel)).containsExactlyInAnyOrder(ModificationSource.BACKEND_RULE);
    }

    @Test
    public void whenAliasAlreadyExistShouldNotCopyNameToAlias() {
        model = CommonModelBuilder.newBuilder()
            .id(0L)
            .category(124L)
            .currentType(CommonModel.Source.GURU)
            .title(MODEL_TITLE)
            .parameterValues(1, XslNames.ALIASES, MODEL_ALIAS1)
            .getModel();

        guruService.addNameToAliasesSettingByCategory(124,
            NameToAliasesSettings.COPY_NAME_TO_ALIAS);
        context.setSource(AuditAction.Source.VENDOR);

        GroupOperationStatus status = storage.saveModel(model, context);

        CommonModel savedModel = checkOkAndGetModel(status);
        assertThat(aliases(savedModel)).containsExactlyInAnyOrder(MODEL_ALIAS1);
        assertThat(modifSource(savedModel)).containsExactlyInAnyOrder(ModificationSource.OPERATOR_FILLED);
    }

    private List<String> aliases(CommonModel model) {
        return model.getAliases().values().stream()
            .flatMap(Collection::stream)
            .map(Word::getWord).collect(Collectors.toList());
    }

    private List<ModificationSource> modifSource(CommonModel model) {
        ParameterValues pvalues = model.getParameterValues(XslNames.ALIASES);
        if (pvalues == null || pvalues.isEmpty()) {
            return Collections.emptyList();
        }
        return pvalues.stream().map(ParameterValue::getModificationSource).collect(Collectors.toList());
    }

    private void addAlias(CommonModel model, String alias, ModificationSource source) {
        ParameterValues pvalues = model.getParameterValues(XslNames.ALIASES);
        if (pvalues == null || pvalues.isEmpty()) {
            ParameterValue pvalue = new ParameterValue(param, WordUtil.defaultWord(alias));
            pvalue.setModificationSource(source);
            model.addParameterValue(pvalue);
        } else {
            ParameterValue pvalue = pvalues.getSingle();
            pvalue.setModificationSource(source);
            pvalue.getStringValue().add(WordUtil.defaultWord(alias));
        }
    }

    private CommonModel checkOkAndGetModel(GroupOperationStatus status) {
        assertEquals(OperationStatusType.OK, status.getStatus());
        Optional<CommonModel> result = status.getRequestedModelByIndex(0);
        assertThat(result).isPresent();
        return result.get();
    }
}
