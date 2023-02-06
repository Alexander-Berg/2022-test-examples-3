package ru.yandex.market.mbo.gwt.client.pages.vendor.editor;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.ListDataProvider;
import ru.yandex.market.mbo.gwt.models.ShopPatternMatching;

import java.util.Collections;
import java.util.List;

/**
 * @author galaev@yandex-team.ru
 * @since 07/07/14
 */
public class TestResultsTable extends CellTable<ShopPatternMatching>
        implements LeafValueEditor<List<ShopPatternMatching>> {

    private static final int RESULTS_PAGE_SIZE = 1000;

    protected final ListDataProvider<ShopPatternMatching> provider = new ListDataProvider<ShopPatternMatching>();

    private final Column<ShopPatternMatching, String> shopurlColumn = new ShopurlColumn();

    private final Column<ShopPatternMatching, String> patternColumn = new PatternColumn();

    @UiConstructor
    public TestResultsTable() {
        super(RESULTS_PAGE_SIZE);
        provider.addDataDisplay(this);
        this.addColumn(shopurlColumn, "URL");
        this.addColumn(patternColumn, "Шаблон");
    }

    @Override
    public void setValue(List<ShopPatternMatching> value) {
        List<ShopPatternMatching> data = (value == null) ? Collections.<ShopPatternMatching>emptyList() : value;
        provider.setList(data);
    }

    @Override
    public List<ShopPatternMatching> getValue() {
        return provider.getList();
    }

    private class ShopurlColumn extends Column<ShopPatternMatching, String> {

        ShopurlColumn() {
            super(new TextCell());
        }

        @Override
        public String getValue(ShopPatternMatching res) {
            return res.getShopUrl();
        }
    }

    private class PatternColumn extends Column<ShopPatternMatching, String> {

        PatternColumn() {
            super(new TextCell());
        }

        @Override
        public String getValue(ShopPatternMatching res) {
            return res.getPattern();
        }
    }
}
