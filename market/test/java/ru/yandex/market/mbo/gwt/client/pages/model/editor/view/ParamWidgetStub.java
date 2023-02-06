package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import com.google.gwt.user.client.ui.Widget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ParamMeta;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValuesWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author gilmulla
 *
 */
public class ParamWidgetStub<T> implements ParamWidget<T> {

    private final ValuesWidget<T> valuesWidget;
    private String nameLabel;
    private boolean ruleLinkVisible;

    private final List<Consumer<Coordinates>> showPopupInfoConsumers = new ArrayList<>();
    private final List<Consumer<Coordinates>> showRuleInfoConsumers = new ArrayList<>();
    private final List<Consumer<Action>> paramWidgetActionConsumers = new ArrayList<>();

    private BackgroundColor bgColor;

    private List<String> warnings = new ArrayList<>();

    public ParamWidgetStub(ValuesWidget<T> valuesWidget) {
        this.valuesWidget = valuesWidget;
    }

    @Override
    public ValuesWidget<T> getValuesWidget() {
        return valuesWidget;
    }

    @Override
    public ValuesWidget<List<String>> getHypothesisWidget() {
        return null;
    }

    @Override
    public void addAction(Action action) {

    }

    @Override
    public String getNameLabel() {
        return nameLabel;
    }

    @Override
    public void setNameLabel(String nameLabel) {
        this.nameLabel = nameLabel;
    }

    @Override
    public void addWarning(String warning) {
        warnings.add(warning);
    }

    @Override
    public void removeWarning(String warning) {
        warnings.remove(warning);
    }

    @Override
    public void enableNameLabelStyle(boolean styled) {

    }

    @Override
    public boolean isRuleLinkVisible() {
        return ruleLinkVisible;
    }

    @Override
    public void setRuleLinkVisible(boolean visible) {
        ruleLinkVisible = visible;
    }

    @Override
    public void createStructure() {

    }

    @Override
    public void addShowPopupInfoConsumer(Consumer<Coordinates> showPopupInfoConsumer) {
        showPopupInfoConsumers.add(showPopupInfoConsumer);
    }

    @Override
    public void addShowRuleInfoConsumer(Consumer<Coordinates> showPopupInfoConsumer) {
        showRuleInfoConsumers.add(showPopupInfoConsumer);
    }

    @Override
    public void addParamWidgetActionConsumer(Consumer<Action> paramWidgetActionConsumer) {
        paramWidgetActionConsumers.add(paramWidgetActionConsumer);
    }

    @Override
    public BackgroundColor getBackgroundColor() {
        return bgColor;
    }

    @Override
    public void setBackgroundColor(BackgroundColor source) {
        bgColor = source;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public void setVisible(boolean visible) {

    }

    @Override
    public ParamMeta getParamMeta() {
        return valuesWidget.getParamMeta();
    }

    @Override
    public ValueWidget<T> getFirstValueWidget() {
        return valuesWidget.getFirstValueWidget();
    }

    @Override
    public Widget asWidget() {
        return null;
    }
}
