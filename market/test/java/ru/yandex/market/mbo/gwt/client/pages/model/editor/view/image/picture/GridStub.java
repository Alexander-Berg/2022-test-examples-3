package ru.yandex.market.mbo.gwt.client.pages.model.editor.view.image.picture;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 29.08.2018
 */
public class GridStub extends Grid {

    private Map<Point, Widget> widgets = new HashMap<>();

    @Override
    public void setWidget(int row, int column, Widget widget) {
        super.setWidget(row, column, widget);
        widgets.put(new Point(row, column), widget);
    }

    @Override
    public Widget getWidget(int row, int column) {
        super.getWidget(row, column);
        return widgets.get(new Point(row, column));
    }
}
