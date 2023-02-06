package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 15.09.2017
 */
public class ViewUtils {
    private ViewUtils() {
    }

    public static ValueWidget<?> getFirstWidget(BlockWidget widget) {
        return getValueWidget(widget, 0);
    }

    public static ValueWidget<?> getValueWidget(BlockWidget widget, int index) {
        return getParamWidget(widget, index).getValuesWidget().getValueWidgets().get(0);
    }

    public static ParamWidget<?> getParamWidget(BlockWidget widget, int index) {
        return widget.getWidgets().get(index);
    }
}
