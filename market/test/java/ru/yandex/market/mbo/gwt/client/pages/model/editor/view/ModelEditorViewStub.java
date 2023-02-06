package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.common.gwt.shared.User;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.gwt.models.Role;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditorUrl;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.CopyModelImagesPanel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.DescriptionBlockView;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.LinksPanel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ModelEditorView;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.NameSamplesView;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.NavigationPanel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.PopupWidgetWrapper;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.TaskActionsPanel;
import ru.yandex.market.mbo.gwt.client.utils.messages.MessageType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.common.processing.ProcessingResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author gilmulla
 */
public class ModelEditorViewStub implements ModelEditorView {

    public static final int USER_UPDATE_TIME = 10000;

    public enum TitleBarStatus {
        UNPUBLISHED_UNSIGNED,
        UNPUBLISHED_SIGNED,
        PUBLISHED_UNSIGNED,
        PUBLISHED_SIGNED,
        BLUE_PUBLISHED
    }

    private Mode mode;
    private EditorEventBus bus;

    MessageType messageType;
    String message;
    List<ProcessingResult> errors;
    private boolean layoutRefreshed;
    Map<String, EditorWidget> tabs = new LinkedHashMap<>();
    private String windowTitle;
    private boolean noEditableModelMessageVisible;
    private User user;

    private List<Pair<String, String>> relationLinks = new ArrayList<>();
    private String relationLink;
    private String relationLinkText;
    private EditorWidget operationWidget;
    private String operationWaitingText = "";
    private String operationWarningText = "";
    private String operationSuccessText = "";
    private EditorWidget dialogWidget;
    private EditorWidget popupWidget;
    private boolean renameButtonVisible;
    private boolean renameButtonEnabled;
    private boolean saveButtonVisible;
    private boolean saveButtonEnabled;
    private boolean deleteButtonVisible;
    private boolean deleteButtonEnabled;
    private boolean saveAsGuruButtonEnabled;
    private boolean saveCompatibilityButtonVisible;
    private boolean saveCompatibilityButtonEnabled;
    private boolean saveMappingsButtonVisible;
    private boolean saveMappingsButtonEnabled;
    private NavigationPanelStub navPanel = new NavigationPanelStub();
    private NameSamplesView nsView = new NameSamplesViewStub();
    private String page;
    private CopyModelSourcePanelStub copyModelPanel = new CopyModelSourcePanelStub();
    private TaskActionsPanel taskActionsPanel = new TaskActionsPanelStub();
    private DescriptionBlockView descriptionView = new DescriptionBlockViewStub();
    private LinksPanel linksPanel = new LinksPanelStub();
    private String leaveConfirmationMessage;
    private boolean imageCopyVisible;
    private Set<String> darkTabs = new HashSet<>();
    private boolean autoSaveActivated;
    private boolean autoSaveCheckBoxEnabled;
    private boolean autoSaveCheckBoxVisible;
    private CopyModelImagesPanel copyModelImagesPanel;
    private TitleOperatorHintViewStub titleOperatorHintView = new TitleOperatorHintViewStub();
    private boolean isDeleteAllImagesEnabled;
    private boolean deleteAllImagesVisible = false;
    private boolean importPicturesButtonVisible;
    private boolean editAllSkuPicturesVisible = false;
    private boolean saveAndPublishButtonEnabled;
    private boolean saveAndPublishButtonVisible;
    private TitleBarStatus titleBarStatus;

    @Override
    public Mode getMode() {
        return this.mode;
    }

    @Override
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public EditorEventBus getEventBus() {
        return bus;
    }

    @Override
    public void setEventBus(EditorEventBus bus) {
        this.bus = bus;
    }

    @Override
    public LinksPanel getLinksPanel() {
        return linksPanel;
    }

    @Override
    public NavigationPanel getNavigationPanel() {
        return navPanel;
    }

    @Override
    public NameSamplesView getNameSamplesView() {
        return this.nsView;
    }

    @Override
    public DescriptionBlockView getDescriptionView() {
        return descriptionView;
    }

    @Override
    public TaskActionsPanel getTaskActionsPanel() {
        return taskActionsPanel;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    @Override
    public void setTitle(String windowTitle) {
        this.windowTitle = windowTitle;
    }

    @Override
    public void setNoEditableModelMessageVisible(boolean visible) {
        noEditableModelMessageVisible = visible;
    }

    public boolean isNoEditableModelMessageVisible() {
        return noEditableModelMessageVisible;
    }

    @Override
    public boolean hasTabs() {
        return !this.tabs.isEmpty();
    }

    @Override
    public EditorWidget getTab(String tabTitle) {
        return this.tabs.get(tabTitle);
    }

    @Override
    public Collection<EditorWidget> getTabs() {
        return this.tabs.values();
    }

    @Override
    public void addTab(String tabTitle, EditorWidget widget) {
        this.tabs.put(tabTitle, widget);
    }

    @Override
    public void overrideTab(String tabTitle, EditorWidget widget) {
        addTab(tabTitle, widget);
    }

    @Override
    public boolean isTabHeaderDark(String tabTitle) {
        return this.darkTabs.contains(tabTitle);
    }

    @Override
    public void setTabHeaderToDark(String tabTitle, boolean isDark) {
        if (isDark) {
            this.darkTabs.add(tabTitle);
        } else {
            this.darkTabs.remove(tabTitle);
        }
    }

    @Override
    public void clearTabs() {
        this.tabs.clear();
    }

    @Override
    public boolean isRenameButtonVisible() {
        return renameButtonVisible;
    }

    @Override
    public void setRenameButtonVisible(boolean visible) {
        this.renameButtonVisible = visible;
    }

    @Override
    public boolean isRenameButtonEnabled() {
        return renameButtonEnabled;
    }

    @Override
    public void setRenameButtonEnabled(boolean enabled) {
        this.renameButtonEnabled = enabled;
    }

    @Override
    public void setSaveAsGuruEnabled(boolean saveEnabled) {

    }

    @Override
    public void setSaveEnabled(boolean saveEnabled) {

    }

    @Override
    public void setSaveAndPublishButtonEnabled(boolean enabled) {
        this.saveAndPublishButtonEnabled = enabled;
    }

    @Override
    public boolean isSaveAndPublishButtonEnabled() {
        return saveAndPublishButtonEnabled;
    }

    @Override
    public void setSaveAndPublishButtonVisible(boolean visible) {
        this.saveAndPublishButtonVisible = visible;
    }

    @Override
    public boolean isSaveAndPublishButtonVisible() {
        return saveAndPublishButtonVisible;
    }

    @Override
    public void updateTitleBarColor(boolean published, boolean publishedOnBlue, boolean operatorSign) {
        if (published) {
            titleBarStatus = operatorSign ? TitleBarStatus.PUBLISHED_SIGNED : TitleBarStatus.PUBLISHED_UNSIGNED;
        } else if (publishedOnBlue) {
            titleBarStatus = TitleBarStatus.BLUE_PUBLISHED;
        } else {
            titleBarStatus = operatorSign ? TitleBarStatus.UNPUBLISHED_SIGNED : TitleBarStatus.UNPUBLISHED_UNSIGNED;
        }
    }

    public TitleBarStatus getTitleBarStatus() {
        return titleBarStatus;
    }

    @Override
    public boolean isImageCopyVisible() {
        return this.imageCopyVisible;
    }

    @Override
    public void setImageCopyVisible(boolean imageCopyVisible) {
        this.imageCopyVisible = imageCopyVisible;
    }

    @Override
    public boolean isMovePicturesButtonVisible() {
        return importPicturesButtonVisible;
    }

    @Override
    public void setMovePicturesButtonVisible(boolean visible) {
        this.importPicturesButtonVisible = visible;
    }

    @Override
    public boolean isEditAllSkuPicturesVisible() {
        return editAllSkuPicturesVisible;
    }

    @Override
    public void setEditAllSkuPictures(boolean visible) {
        editAllSkuPicturesVisible = visible;
    }

    @Override
    public void setCopyPanelValue(CommonModel model) {
        copyModelPanel.setValue(model);
    }

    @Override
    public boolean isCopyPanelVisible() {
        return copyModelPanel.isVisible();
    }

    @Override
    public void setCopyPanelVisible(boolean visible) {
        copyModelPanel.setVisible(visible);
    }

    @Override
    public void resetUI() {
        messageType = null;
        message = null;
        errors = null;

        descriptionView.clear();

        this.layoutRefreshed = false;
        this.clearTabs();
    }

    public boolean isLayoutRefreshed() {
        return layoutRefreshed;
    }

    @Override
    public void refreshLayout() {
        this.layoutRefreshed = true;
    }

    @Override
    public EditorWidget getPopupWidget() {
        return this.popupWidget;
    }

    @Override
    public void showPopup(EditorWidget widget) {
        this.popupWidget = widget;
    }

    @Override
    public void showPopup(PopupWidgetWrapper wrapper) {
        this.popupWidget = wrapper.getEditorWidget();
    }

    @Override
    public void hidePopup() {
        this.popupWidget = null;
    }

    @Override
    public EditorWidget getDialogWidget() {
        return this.dialogWidget;
    }

    @Override
    public void showDialog(EditorWidget widget, String title, int x, int y) {
        this.dialogWidget = widget;
    }

    @Override
    public void showSaveCancelDialog(EditorWidget widget, String title, int x, int y) {
        this.dialogWidget = widget;
    }

    @Override
    public void showPopupNotification(String message, MessageType messageType) { }

    @Override
    public void showPopupNotification(String message, MessageType messageType, int timeoutInMillis) { }

    @Override
    public boolean isSaveButtonEnabled() {
        return this.saveButtonEnabled;
    }

    @Override
    public void setSaveButtonEnabled(boolean enabled) {
        this.saveButtonEnabled = enabled;
    }

    @Override
    public void setSaveMappingsButtonHint(String hint) {
    }

    @Override
    public boolean isDeleteButtonVisible() {
        return this.deleteButtonVisible;
    }

    @Override
    public void setDeleteButtonVisible(boolean visible) {
        this.deleteButtonVisible = visible;
    }

    @Override
    public boolean isDeleteButtonEnabled() {
        return this.deleteButtonEnabled;
    }

    @Override
    public void setDeleteButtonEnabled(boolean enabled) {
        this.deleteButtonEnabled = enabled;
    }

    @Override
    public boolean isSaveAsGuruButtonEnabled() {
        return this.saveAsGuruButtonEnabled;
    }

    @Override
    public void setSaveAsGuruButtonEnabled(boolean enabled) {
        this.saveAsGuruButtonEnabled = enabled;
    }

    @Override
    @SuppressWarnings("checkstyle:MagicNumber")
    public User getUser() {
        if (this.user == null) {
            this.user = new User("user", 1, USER_UPDATE_TIME);
            this.user.setRole(Role.ADMIN);
        }
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void clearRelationLinks() {
        relationLinks.clear();
    }

    public void addRelationLink(String href, String text) {
        relationLinks.add(new Pair<>(href, text));
    }

    @Override
    public String getOperationSuccessText() {
        return this.operationSuccessText;
    }

    @Override
    public void setOperationSuccessText(String text) {
        this.operationSuccessText = text;
        this.operationWaitingText = "";
        this.operationWarningText = "";
        this.operationWidget = null;
    }

    @Override
    public String getOperationWaitingText() {
        return this.operationWaitingText;
    }

    @Override
    public void setOperationWaitingText(String text) {
        this.operationSuccessText = "";
        this.operationWaitingText = text;
        this.operationWarningText = "";
        this.operationWidget = null;
    }

    @Override
    public void setOperationWarningText(String text) {
        this.operationSuccessText = "";
        this.operationWaitingText = "";
        this.operationWarningText = text;
    }

    @Override
    public String getOperationWarningText() {
        return this.operationWarningText;
    }

    @Override
    public EditorWidget getOperationWidget() {
        return this.operationWidget;
    }

    @Override
    public void setOperationWidget(EditorWidget widget) {
        this.operationSuccessText = null;
        this.operationWaitingText = null;
        this.operationWarningText = null;
        this.operationWidget = widget;
    }

    @Override
    public boolean isSaveCompatibilityButtonEnabled() {
        return this.saveCompatibilityButtonEnabled;
    }

    @Override
    public void setSaveCompatibilityButtonEnabled(boolean enabled) {
        this.saveCompatibilityButtonEnabled = enabled;
    }

    @Override
    public boolean isSaveButtonVisible() {
        return this.saveButtonVisible;
    }

    @Override
    public void setEncodeButtonVisible(boolean enabled) {

    }

    @Override
    public void setSaveButtonVisible(boolean visible) {
        this.saveButtonVisible = visible;
    }

    @Override
    public boolean isAutoSaveActivated() {
        return this.autoSaveActivated;
    }

    @Override
    public void setAutoSaveActivated(boolean activated) {
        this.autoSaveActivated = activated;
    }

    @Override
    public boolean isAutoSaveCheckBoxEnabled() {
        return this.autoSaveCheckBoxEnabled;
    }

    @Override
    public void setAutoSaveCheckBoxEnabled(boolean enabled) {
        this.autoSaveCheckBoxEnabled = enabled;
    }

    @Override
    public boolean isAutoSaveCheckBoxVisible() {
        return this.autoSaveCheckBoxVisible;
    }

    @Override
    public void setAutoSaveCheckBoxVisible(boolean visible) {
        this.autoSaveCheckBoxVisible = visible;
    }

    @Override
    public boolean isSaveCompatibilityButtonVisible() {
        return this.saveCompatibilityButtonVisible;
    }

    @Override
    public void setSaveCompatibilityButtonVisible(boolean visible) {
        this.saveCompatibilityButtonVisible = visible;
    }

    @Override
    public boolean isSaveMappingsButtonVisible() {
        return saveMappingsButtonVisible;
    }

    @Override
    public void setSaveMappingsButtonVisible(boolean visible) {
        saveMappingsButtonVisible = visible;
    }

    @Override
    public boolean isSaveMappingsButtonEnabled() {
        return saveMappingsButtonEnabled;
    }

    @Override
    public void setSaveMappingsButtonEnabled(boolean enabled) {
        saveMappingsButtonEnabled = enabled;
    }

    @Override
    public boolean isDeleteAllImagesEnabled() {
        return this.isDeleteAllImagesEnabled;
    }

    @Override
    public void setDeleteAllImagesEnabled(boolean enabled) {
        this.isDeleteAllImagesEnabled = enabled;
    }

    @Override
    public boolean isDeleteAllImagesVisible() {
        return deleteAllImagesVisible;
    }

    @Override
    public void setDeleteAllImagesVisible(boolean visible) {
        this.deleteAllImagesVisible = visible;
    }

    public String getPage() {
        return page;
    }

    @Override
    public void goToPage(String page) {
        this.page = page;

        int s = page.indexOf('/');
        String anchor = page.substring(0, s);
        String params = page.substring(s + 1);

        EditorUrl url = EditorUrlStub.of(anchor, params);
        bus.fireEvent(new PlaceShowEvent(url));
    }

    @Override
    public String getLeaveConfirmationMessage() {
        return leaveConfirmationMessage;
    }

    @Override
    public void setLeaveConfirmationMessage(String message) {
        this.leaveConfirmationMessage = message;
    }

    @Override
    public TitleOperatorHintViewStub getTitleOperatorHintView() {
        return titleOperatorHintView;
    }
}
