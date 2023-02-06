package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import com.google.gwt.user.client.ui.Widget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.LinksPanel;

public class LinksPanelStub implements LinksPanel {
    private Boolean publishOnBlueLinkVisible = null;
    private boolean visible;

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void setEventBus(EditorEventBus bus) {

    }

    @Override
    public void setEditAllLink(String historyToken, boolean visible) {

    }

    @Override
    public void setParametersLink(long categoryId, boolean visible) {

    }

    @Override
    public void setHistoryLink(long modelId, boolean visible) {

    }

    @Override
    public void setAuditLink(long modelId, boolean visible) {

    }

    @Override
    public void setModelPriceRangeLink(long modelId, boolean visible) {

    }

    @Override
    public void setVideoReviewLink(long modelId, boolean visible) {

    }

    @Override
    public void setOffersLink(long modelId, boolean visible) {

    }

    @Override
    public void setPublishOnBlueLink(boolean visible) {
        publishOnBlueLinkVisible = visible;
    }

    @Override
    public void setRawModelsLink(long categoryId, long id,
            boolean isGenerated) {

    }

    public Boolean getPublishOnBlueLinkVisible() {
        return publishOnBlueLinkVisible;
    }

    @Override
    public Widget asWidget() {
        return null;
    }
}
