package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SourcesWidget;

/**
 * @author gilmulla
 *
 */
public class SourcesWidgetStub extends EditorWidgetStub implements SourcesWidget {

    private List<TableRow> rows = new ArrayList<>();

    @Override
    public List<TableRow> getRows() {
        return this.rows;
    }

    @Override
    public void addRow(TableRow row) {
        this.rows.add(row);
    }
}
