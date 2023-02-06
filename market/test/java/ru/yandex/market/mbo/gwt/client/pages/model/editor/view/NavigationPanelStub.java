package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.NavigationPanel;

/**
 * @author gilmulla
 *
 */
public class NavigationPanelStub extends EditorWidgetStub implements NavigationPanel {

    private boolean parentLinkVisible;
    private boolean modelLinkVisible;
    private boolean modelLabelVisible;

    @Override
    public void setupCategoryLink(Long guruCategoryId,
            String categoryUniqueName) {
    }

    @Override
    public void setupParentLink(Long parentId, String parentTitle) {

    }

    @Override
    public void setupModelLink(Long modelId, String modelTitle) {

    }

    @Override
    public void setupModelLabel(String text) {
    }

    @Override
    public void setupVendorLink(Long localVendorId, Long hid, String vendorName, boolean favoriteVendor) {
    }

    @Override
    public boolean isParentLinkVisible() {
        return this.parentLinkVisible;
    }

    @Override
    public void setParentLinkVisible(boolean visible) {
        this.parentLinkVisible = visible;
    }

    @Override
    public boolean isModelLinkVisible() {
        return this.modelLinkVisible;
    }

    @Override
    public void setModelLinkVisible(boolean visible) {
        this.modelLinkVisible = visible;
    }

    @Override
    public boolean isModelLabelVisible() {
        return this.modelLabelVisible;
    }

    @Override
    public void setModelLableVisible(boolean visible) {
        this.modelLabelVisible = visible;
    }

    @Override
    public String getGoogleQuery() {
        return null;
    }

    @Override
    public String getYandexQuery() {
        return null;
    }

    @Override
    public void setGoogleSearch(String query) {

    }

    @Override
    public void setYandexSearch(String query) {

    }

    @Override
    public String getGoogleImageQuery() {
        return null;
    }

    @Override
    public void setGoogleImageSearch(String query) {

    }

    @Override
    public String getYandexImageQuery() {
        return null;
    }

    @Override
    public void setYandexImageSearch(String query) {

    }



}
