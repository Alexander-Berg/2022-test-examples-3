package ru.yandex.market.mbo.gwt.client.pages.model.editor.view.sku.widgets;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.widget.SkuTablePager;

import java.util.function.Consumer;

public class SkuTablePagerStub implements SkuTablePager {

    private Consumer<Integer> onPageChangeHandler;
    private int page = 1;

    @Override
    public void onPageChange(Consumer<Integer> onPageChangeHandler) {
        this.onPageChangeHandler = onPageChangeHandler;
    }

    @Override
    public void setMaxPages(int maxPages) {

    }

    @Override
    public void setPage(int page, boolean firePageChangeEvent) {
        this.page = page;
        if (firePageChangeEvent) {
            onPageChangeHandler.accept(page);
        }
    }

    @Override
    public int getCurrentPage() {
        return page;
    }
}
