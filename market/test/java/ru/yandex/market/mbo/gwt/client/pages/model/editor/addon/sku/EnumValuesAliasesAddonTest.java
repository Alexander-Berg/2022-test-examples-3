package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.EnumValuesAliasesAddon;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.enumaliases.EnumValueAliasesChangedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.enumaliases.OpenEnumValueAliasesEditorEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableModel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableValues;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.SizeMeasureScaleInfo;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValuesWidgetStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EnumValueAliasesEditor;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ParamMeta;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValuesWidget;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.builders.EnumValueAliasBuilder;
import ru.yandex.market.mbo.gwt.models.gurulight.GLMeasure;
import ru.yandex.market.mbo.gwt.models.gurulight.SizeMeasureUnitOptionDto;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModelUtils;
import ru.yandex.market.mbo.gwt.models.modelstorage.EnumValueAlias;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParameterValueBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тестируем ${@link EnumValuesAliasesAddon}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class EnumValuesAliasesAddonTest extends BaseSkuAddonTest {

    private ParameterValue value2;
    private ParameterValue value4;
    private CommonModel sku;
    private CommonModel baseModel;

    @Override
    public void model() {
        // model id: 1 - основная модель
        // model id: 2, 3 - SKU
        CommonModel guru = CommonModelBuilder.newBuilder()
            .id(1).category(666).currentType(CommonModel.Source.GURU)
            .enumAlias(3).optionId(31).aliasOptionId(32).end()
            .enumAlias(3).optionId(31).aliasOptionId(33).end()
            .enumAlias(4).optionId(41).aliasOptionId(42).end()
            .enumAlias(4).optionId(41).aliasOptionId(43).end()
            .enumAlias(4).optionId(44).aliasOptionId(45).end()
            .startModelRelation()
            .id(3).categoryId(666).type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .endModel();

        data.startModel()
            .title("Test model")
            .id(2).category(666).vendorId(777).currentType(CommonModel.Source.SKU)
            .param("param2").setOption(21).modificationSource(ModificationSource.OPERATOR_FILLED)
            .param("param3").setOption(31).modificationSource(ModificationSource.OPERATOR_FILLED)
            .param("param4").setOption(44).modificationSource(ModificationSource.OPERATOR_FILLED)
            .param("size_param").setOption(53).modificationSource(ModificationSource.OPERATOR_FILLED)
            .param("size_param_2").setOption(62).modificationSource(ModificationSource.OPERATOR_FILLED)
            .endModel();
        data.sizeMeasureScaleInfos(getTestSizeMeasureScaleInfos());
        data.getModel().addRelation(CommonModelUtils.getRelationToModel(guru));
        guru.addRelation(CommonModelUtils.getRelationToModel(data.getModel()));
    }

    @Override
    public void parameters() {
        super.parameters();
        data.startParameters()
            .startParameter()
                // size parameter that is present in the scale infos
                .id(550).xsl("size_param").type(Param.Type.ENUM).name("SizeParam")
                .skuParameterMode(SkuParameterMode.SKU_DEFINING)
                .option(553, "1.75")
                .option(554, "1.80")
                .option(555, "170")
                .option(556, "184")
            .endParameter()
            .startParameter()
                // size parameter that is missing from scale infos
                .id(560).xsl("size_param_2").type(Param.Type.ENUM).name("SizeParam2")
                .skuParameterMode(SkuParameterMode.SKU_DEFINING)
                .option(561, "234")
                .option(562, "345")
            .endParameter()
            .endParameters();
    }

    @Override
    protected void onModelLoaded(EditableModel editableModel) {
        super.onModelLoaded(editableModel);

        sku = editableModel.getModel();
        baseModel = sku.getRelation(1).flatMap(r -> Optional.of(r.getModel())).orElse(null);
        value2 = sku.getSingleParameterValue(2);
        value4 = data.getModel().getSingleParameterValue(4);
    }

    @Test
    public void testAliasesButtonIsVisible() {
        List<String> visibleParams = Arrays.asList("param1", "param4", "size_param_2");

        List<ParamWidget<?>> paramWidgets = editableModel.getParamWidgets();
        Assert.assertFalse(paramWidgets.isEmpty());
        for (ParamWidget<?> paramWidget : paramWidgets) {
            ParamMeta meta = paramWidget.getParamMeta();
            String xslName = meta.getXslName();
            ValuesWidget<?> valuesWidget = paramWidget.getValuesWidget();
            ValueWidget<?> valueWidget = valuesWidget.getValueWidgets().get(0);
            boolean expectedVisible = visibleParams.contains(xslName);
            assertThat(valueWidget.isEnumValueAliasesButtonVisible())
                .as("Check '%s' aliases button's visibility", xslName)
                .isEqualTo(expectedVisible);
        }
    }

    @Test
    public void testOpeningAliasesEditorForValueWithoutAliases() {
        bus.fireEvent(new OpenEnumValueAliasesEditorEvent(
            ParameterValueBuilder.newBuilder()
                .paramId(2L)
                .optionId(21L)
                .build()));

        EnumValueAliasesEditor editor = (EnumValueAliasesEditor) view.getDialogWidget();
        List<String> availableOptions = editor.getAvailableOptions().stream()
            .map(Option::getName).collect(Collectors.toList());
        assertThat(editor.getParameterValue().getParamId()).isEqualTo(2);
        assertThat(editor.getCategoryParam().getId()).isEqualTo(2);
        assertThat(editor.getAliases()).isEmpty();
        assertThat(availableOptions).containsExactlyInAnyOrder("Option22", "Option23");
    }

    @Test
    public void testOpeningAliasesEditorForValueWithAliases() {
        bus.fireEvent(new OpenEnumValueAliasesEditorEvent(
            ParameterValueBuilder.newBuilder()
                .paramId(3L)
                .optionId(31L)
                .build()));

        EnumValueAliasesEditor editor = (EnumValueAliasesEditor) view.getDialogWidget();
        List<String> availableOptions = editor.getAvailableOptions().stream()
            .map(Option::getName).collect(Collectors.toList());
        List<Long> aliasesIds = editor.getAliases().stream()
            .map(EnumValueAlias::getAliasOptionId).collect(Collectors.toList());

        assertThat(editor.getParameterValue().getParamId()).isEqualTo(3);
        assertThat(editor.getCategoryParam().getId()).isEqualTo(3);
        assertThat(aliasesIds).containsExactlyInAnyOrder(32L, 33L);
        assertThat(availableOptions).containsExactlyInAnyOrder("Option32", "Option33", "Option34", "Option35");
    }

    @Test
    public void testOpeningAliasesEditorForValueWithoutAliasesForNewValue() {
        CategoryParam param3 = data.getModelData().getParam(3);
        EditableValues editableValues = editableModel.getEditableParameter(3).getEditableValues();
        ((ValuesWidgetStub<Object>) editableValues.getValuesWidget())
            .createNewValueWidget(param3.getOption(34), true, true);

        bus.fireEvent(new OpenEnumValueAliasesEditorEvent(
            ParameterValueBuilder.newBuilder()
                .paramId(3L)
                .optionId(34L)
                .build()));

        EnumValueAliasesEditor editor = (EnumValueAliasesEditor) view.getDialogWidget();
        List<String> availableOptions = editor.getAvailableOptions().stream()
            .map(Option::getName).collect(Collectors.toList());
        List<Long> aliasesIds = editor.getAliases().stream()
            .map(EnumValueAlias::getAliasOptionId).collect(Collectors.toList());

        assertThat(editor.getParameterValue().getParamId()).isEqualTo(3);
        assertThat(editor.getCategoryParam().getId()).isEqualTo(3);
        assertThat(aliasesIds).containsExactlyInAnyOrder();
        //Выбранные опции как знач параметра не должны быть доступны; алиасные же опции допускаются к выбору (MBO-15002)
        assertThat(availableOptions).containsExactlyInAnyOrder("Option32", "Option33", "Option35");
    }

    @Test
    public void testAvailableAliasesWillContainOnlyNotUsedOptions() {
        bus.fireEvent(new OpenEnumValueAliasesEditorEvent(
            ParameterValueBuilder.newBuilder()
                .paramId(4L)
                .optionId(44L)
                .build()));

        EnumValueAliasesEditor editor = (EnumValueAliasesEditor) view.getDialogWidget();
        List<String> availableOptions = editor.getAvailableOptions().stream()
            .map(Option::getName).collect(Collectors.toList());

        assertThat(availableOptions).containsExactlyInAnyOrder("Option41", "Option42",
            "Option43", "Option45", "Option46");
    }

    @Test
    public void testAliasCreated() {
        EnumValueAlias alias = EnumValueAliasBuilder.newBuilder().paramId(2).optionId(21).aliasOptionId(22).build();
        bus.fireEvent(new EnumValueAliasesChangedEvent(value4, Collections.singletonList(alias)));

        assertThat(baseModel.getEnumValueAliases(value2)).containsExactly(alias);
    }

    @Test
    public void testAliasChanging() {
        EnumValueAlias alias = EnumValueAliasBuilder.newBuilder().paramId(4).optionId(44).aliasOptionId(46).build();
        bus.fireEvent(new EnumValueAliasesChangedEvent(value4, Collections.singletonList(alias)));

        assertThat(baseModel.getEnumValueAliases(value4)).containsExactly(alias);
    }

    @Test
    public void testAliasRemoving() {
        bus.fireEvent(new EnumValueAliasesChangedEvent(value4, Collections.emptyList()));

        assertThat(baseModel.getEnumValueAliases(value4)).isEmpty();
    }

    private List<SizeMeasureScaleInfo> getTestSizeMeasureScaleInfos() {
        List<SizeMeasureScaleInfo> result = new ArrayList<>();
        GLMeasure heightMeasure = new GLMeasure();
        heightMeasure.setId(540L);
        heightMeasure.setName("Height");
        heightMeasure.setValueParamId(550L);

        SizeMeasureUnitOptionDto meterUnit = new SizeMeasureUnitOptionDto(new OptionImpl(551L, "Метр"), 0L);
        SizeMeasureUnitOptionDto centimeterUnit = new SizeMeasureUnitOptionDto(new OptionImpl(552L, "Сантиметр"), 0L);

        Option meterValue1 = new OptionImpl(553L, "1.75");
        Option meterValue2 = new OptionImpl(554L, "1.80");
        Option centimeterValue1 = new OptionImpl(555L, "170");
        Option centimeterValue2 = new OptionImpl(556L, "184");
        result.add(new SizeMeasureScaleInfo(heightMeasure, Arrays.asList(meterUnit, centimeterUnit),
            ImmutableMap.of(meterUnit.getOption().getId(), Arrays.asList(meterValue1, meterValue2),
                            centimeterUnit.getOption().getId(), Arrays.asList(centimeterValue1, centimeterValue2))));
        return result;
    }
}
