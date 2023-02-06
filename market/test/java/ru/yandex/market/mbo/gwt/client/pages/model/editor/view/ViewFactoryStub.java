package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import com.google.gwt.user.client.ui.Widget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.matched_offers.MatchedOffersWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.ModelData;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.ModelImages;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.AskWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.CompatibilityWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.CopyModelImagesPanel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.DateFormat;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditDependentOptionsWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EnumValueAliasesEditor;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ErrorsWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ForceSaveWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.FormatApi;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ReadOnlySkuRelationWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ImageParamValueLinksEditorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.JsSupport;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ModelEditorView;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.NewParamOptionDialogWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamInfoWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.PartnerSkuRelationWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.PickerImageEditorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.PicturesTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.CardApiModelWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.RenameWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.RuleInfoWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SelectModelSuccessorsWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SelectParameterValueWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SelectPicturesList;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SourcesWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SystemTimer;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ViewFactory;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.partner_sku.SupplierTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.MoveSkuEditorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.SelectSkuSuccessorsWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.SkuFullPicturesEditor;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.SkuMappingsWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.SkuRelationWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.widget.SkuTableFilter;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.sku.pictures.editors.subwidgets.ModelPictureInfoWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.sku.widgets.SkuTableFilterStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ParamMeta;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueField;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValuesWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.view.model_parameter_relation.EditDependentOptionsWidgetStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelInfosOfVendorResult;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelPictureInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.SupplierOffer;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author gilmulla
 */
public class ViewFactoryStub extends ViewFactory {

    private SelectSkuSuccessorsWidgetStub currentSelectSkuSuccessorsWidget;
    private SelectModelSuccessorsWidgetStub currentSelectModelSuccessorsWidget;
    private boolean yesNoState = true;

    @Override
    public ModelEditorView createEditorView() {
        return new ModelEditorViewStub();
    }

    @Override
    public BlockWidget createBlockWidget() {
        return new BlockWidgetStub();
    }

    @Override
    public SourcesWidget createSourcesWidget() {
        return new SourcesWidgetStub();
    }

    @Override
    protected <T> ValueField<T> doCreateValueField(ParamMeta paramMeta) {
        return new ValueFieldStub<>(paramMeta);
    }

    @Override
    public <T> ParamWidget<T> doCreateParamWidget(ValuesWidget<T> valueWidget,
                                                  ValuesWidget<List<String>> hypothesisWidget) {
        return new ParamWidgetStub<>(valueWidget);
    }

    @Override
    protected <T> ValuesWidget<T> doCreateValuesWidget(ParamMeta meta, Supplier<ValueWidget<T>> valueFieldGenerator) {
        return new ValuesWidgetStub<>(meta, valueFieldGenerator);
    }

    @Override
    protected ValueField<List<String>> doCreateReadonlyStringValueField(ParamMeta paramMeta) {
        return new ValueFieldStub<>(paramMeta);
    }

    @Override
    public <T> ValueWidget<T> doCreateValueWidget(ValueField<T> valueField) {
        return new ValueWidgetStub<>(valueField);
    }

    @Override
    public <T> ValueField<T> createValueField(CategoryParam param, CommonModel model, ModelData modelData) {
        ValueFieldStub<T> valueField = (ValueFieldStub<T>) super.createValueField(param, model, modelData);
        if (param.getType() == Param.Type.ENUM || param.getType() == Param.Type.NUMERIC_ENUM) {
            valueField.setOptions(param.getOptions());
        }
        return valueField;
    }

    @Override
    public ParamsTab createParamsTab() {
        return new ParamsTabStub();
    }

    @Override
    public PicturesTab createPicturesTab() {
        return new PicturesTabStub();
    }

    @Override
    public SupplierTab createSupplierTab() {
        return new SupplierTab()  {
            private Long supplierId;
            @Override
            public void setSupplierId(Long supplierId) {
                this.supplierId = supplierId;
            }

            @Override
            public Long getSupplierId() {
                return this.supplierId;
            }

            @Override
            public boolean isVisible() {
                return false;
            }

            @Override
            public void setVisible(boolean visible) {

            }

            @Override
            public Widget asWidget() {
                return null;
            }
        };
    }

    @Override
    public ParamInfoWidget createParamInfoPopup() {
        return new ParamInfoPopupStub();
    }

    @Override
    public FormatApi createFormatApi() {
        return new FormatApi() {

            @Override
            public DateFormat createDateFormat(String pattern) {
                SimpleDateFormat df = new SimpleDateFormat(pattern);
                return new DateFormat() {

                    @Override
                    public Date parse(String str) {
                        try {
                            return df.parse(str);
                        } catch (ParseException e) {
                            throw new IllegalArgumentException("Malformed input", e);
                        }
                    }
                };
            }

            @Override
            public String format(BigDecimal value, String pattern) {
                DecimalFormat df = new DecimalFormat(pattern);
                return df.format(value);
            }
        };
    }

    @Override
    public JsSupport createJavascriptSupport() {
        return new NashornJavascriptSupport();
    }

    @Override
    public ErrorsWidget createErrorsWidget() {
        return new ErrorsWidgetStub();
    }

    @Override
    public ForceSaveWidget createForceSaveWidget() {
        return new ForceSaveWidgetStub();
    }

    @Override
    public CompatibilityWidget createCompatibilityWidget() {
        return new CompatibilityWidgetStub();
    }

    @Override
    public MatchedOffersWidget createMatchedOffersWidget() {
        return new MatchedOffersWidgetStub();
    }

    @Override
    public SkuMappingsWidget createSkuMappingsWidget() {
        return new SkuMappingsWidgetStub();
    }

    @Override
    public EditDependentOptionsWidget createEditDependentOptionsWidget() {
        return new EditDependentOptionsWidgetStub();
    }

    @Override
    public NewParamOptionDialogWidget createNewParamOptionDialogWidget() {
        return NewParamOptionDialogWidgetStub.getInstance();
    }

    @Override
    public RuleInfoWidget createRuleInfoWidget() {
        return new RuleInfoDlgStub();
    }

    @Override
    public EditorWidget createLegendWidget() {
        return null;
    }

    @Override
    public EditorWidget createSkuLegendWidget() {
        return null;
    }

    @Override
    public EnumValueAliasesEditor createEnumValueAliasesEditor() {
        return new EnumValueAliasesEditorStub();
    }

    @Override
    public SelectParameterValueWidget createSelectParameterValueWidget() {
        return new SelectParameterValueWidgetStub();
    }

    @Override
    public SelectPicturesList createSelectPicturesWidget() {
        return new SelectPicturesListStub();
    }

    @Override
    public PickerImageEditorWidget createPickerImageEditorWidget() {
        return new PickerImageEditorWidgetStub();
    }

    @Override
    public SkuTableFilter createSkuTableFilter(CategoryParam param, boolean isParamHypothetical) {
        return new SkuTableFilterStub(param, editorEventBus);
    }

    @Override
    public CardApiModelWidget createPreformattedTextWidget() {
        return new CardApiModelWidgetStub();
    }

    @Override
    public SelectSkuSuccessorsWidget createSelectSkuSuccessorsWidget(
        ModelInfo removedSku, List<ModelInfo> allVendorSkus, List<SupplierOffer> removedSkuMappings) {

        currentSelectSkuSuccessorsWidget = new SelectSkuSuccessorsWidgetStub(removedSku);
        return currentSelectSkuSuccessorsWidget;
    }

    @Override
    public SelectModelSuccessorsWidget createSelectModelSuccessorsWidget(ModelInfo removedModel,
                                                                         ModelInfosOfVendorResult modelInfosResult) {
        currentSelectModelSuccessorsWidget = new SelectModelSuccessorsWidgetStub(removedModel);
        return currentSelectModelSuccessorsWidget;
    }

    @Override
    protected SkuRelationWidget doCreateSkuRelationWidget(ModelData modelData,
                                                          EditorEventBus bus,
                                                          List<CategoryParam> params) {
        return new SkuRelationWidgetStub();
    }

    @Override
    public ReadOnlySkuRelationWidget doCreateGSkuRelationWidget() {
        return new SkuRelationWidgetStub();
    }

    @Override
    protected PartnerSkuRelationWidget doCreatePartnerSkuRelationWidget() {
        return new SkuRelationWidgetStub();
    }

    @Override
    public SystemTimer createTimer(Runnable tick) {
        return null;
    }

    @Override
    protected MoveSkuEditorWidget doCreateMoveSkuEditorWidget(List<CategoryParam> params,
                                                              CommonModel fromModel,
                                                              Set<CommonModel> skus) {
        return new MoveSkuEditorWidgetStub(params, fromModel, skus);
    }

    @Override
    protected SkuFullPicturesEditor doCreateSkuFullPicturesEditor() {
        return new SkuFullPicturesEditorStub();
    }

    @Override
    public CopyModelImagesPanel createModelImagesCopyPanel(List<ModelImages> models) {
        return new CopyModelImagesPanelStub(models);
    }

    @Override
    public ImageParamValueLinksEditorWidget createImageParamValueLinksEditorWidget() {
        return null;
    }

    @Override
    public AskWidget createAskWidget() {
        return new AskWidget() {
            Consumer<Boolean> consumer;

            @Override
            public void setTitle(String title) {

            }

            @Override
            public void setQuestion(String question) {

            }

            @Override
            public void setYesButtonText(String yesButtonText) {

            }

            @Override
            public void setNoButtonText(String noButtonText) {

            }

            @Override
            public void setConsumer(Consumer<Boolean> consumer) {
                this.consumer = consumer;
            }

            @Override
            public void setPreformatted(boolean pre) {
                return;
            }

            @Override
            public void show(boolean modal) {
                consumer.accept(yesNoState);
            }

            @Override
            public boolean isVisible() {
                return false;
            }

            @Override
            public void setVisible(boolean visible) {

            }

            @Override
            public Widget asWidget() {
                return null;
            }
        };
    }

    @Override
    public RenameWidget createRenameWidget() {
        return new RenameWidget() {
            Consumer<Boolean> consumer;
            String name;

            @Override
            public void setName(String name) {
                this.name = name;
            }

            @Override
            public String getName() {
                return name;
            }


            @Override
            public void setTitle(String title) {

            }

            @Override
            public void setQuestion(String question) {

            }

            @Override
            public void setYesButtonText(String yesButtonText) {

            }

            @Override
            public void setNoButtonText(String noButtonText) {

            }

            @Override
            public void setConsumer(Consumer<Boolean> consumer) {
                this.consumer = consumer;
            }

            @Override
            public void setPreformatted(boolean pre) {
                return;
            }

            @Override
            public void show(boolean modal) {
                consumer.accept(yesNoState);
            }

            @Override
            public boolean isVisible() {
                return false;
            }

            @Override
            public void setVisible(boolean visible) {

            }

            @Override
            public Widget asWidget() {
                return null;
            }
        };
    }

    public SelectSkuSuccessorsWidgetStub getCurrentSelectSkuSuccessorsWidget() {
        return currentSelectSkuSuccessorsWidget;
    }

    public SelectModelSuccessorsWidgetStub getCurrentSelectModelSuccessorsWidget() {
        return currentSelectModelSuccessorsWidget;
    }

    public void whenAskedSayThis(boolean yesNo) {
        yesNoState = yesNo;
    }

    @Override
    public ModelPictureInfoWidget createModelPictureInfoWidget(ModelPictureInfo modelPictureInfo) {
        return null;
    }
}
