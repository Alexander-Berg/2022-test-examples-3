package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;

/**
 * @author gilmulla
 *
 */
public class ParamsTabStub extends EditorWidgetStub implements ParamsTab {

    List<EditorWidget> left = new ArrayList<>();
    List<EditorWidget> right = new ArrayList<>();
    private String title;

    @Override
    public int getWidgetCountAtLeft() {
        return this.left.size();
    }

    @Override
    public int getWidgetCountAtRight() {
        return this.right.size();
    }

    @Override
    public List<EditorWidget> getWidgetsAtLeft() {
        return this.left;
    }

    @Override
    public List<EditorWidget> getWidgetsAtRight() {
        return this.right;
    }

    @Override
    public void addWidgetToLeft(EditorWidget widget) {
        this.left.add(widget);
    }

    @Override
    public void addWidgetToRight(EditorWidget widget) {
        this.right.add(widget);
    }

    @Override
    public String getTabTitle() {
        return this.title;
    }

    @Override
    public void setTabTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean isDark() {
        return false;
    }

    @Override
    public void setDark(boolean dark) {

    }
}
