package ru.yandex.autotests.innerpochta.ns.pages;

import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import ru.yandex.autotests.innerpochta.atlas.AllureListener;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.atlas.extensions.FindByCssCollectionExtension;
import ru.yandex.autotests.innerpochta.atlas.extensions.FindByCssExtension;
import ru.yandex.autotests.innerpochta.atlas.extensions.HoverMethodExtension;
import ru.yandex.autotests.innerpochta.ns.pages.abook.pages.AbookPage;
import ru.yandex.autotests.innerpochta.ns.pages.corp.CorpPage;
import ru.yandex.autotests.innerpochta.ns.pages.messages.CustomButtonsPage;
import ru.yandex.autotests.innerpochta.ns.pages.messages.MessagePage;
import ru.yandex.autotests.innerpochta.ns.pages.messages.MessageViewPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.FiltersOverviewSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.SettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.AbookSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.CollectorSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.DomainSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.FiltersCreationSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.FoldersAndLabelsSettingPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.MailClientsSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.OtherSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.SecuritySettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.SenderInfoSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.BackupSettingsPage;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe;

/**
 * @author cosmopanda
 */
public class MailPages {

    private static WebDriverRule webDriverRule;

    public MailPages(WebDriverRule webDriverRule) {
        MailPages.webDriverRule = webDriverRule;
    }

    protected <T extends MailPage> T on(Class<T> pageClass) {
        Atlas atlas = new Atlas(new WebDriverConfiguration(webDriverRule.getDriver()));
        atlas.listener(new AllureListener());
        atlas.extension(new FindByCssExtension());
        atlas.extension(new FindByCssCollectionExtension());
        atlas.extension(new HoverMethodExtension());
        return atlas.create(webDriverRule.getDriver(), pageClass);
    }

    public MessagePage home() {
        return on(MessagePage.class);
    }

    public SearchPage search() {
        return on(SearchPage.class);
    }

    public SettingsPage settingsCommon() {
        return on(SettingsPage.class);
    }

    public SenderInfoSettingsPage settingsSender() {
        return on(SenderInfoSettingsPage.class);
    }

    public FoldersAndLabelsSettingPage settingsFoldersAndLabels() {
        return on(FoldersAndLabelsSettingPage.class);
    }

    public AbookSettingsPage settingsContacts() {
        return on(AbookSettingsPage.class);
    }

    public OtherSettingsPage settingsOther() {
        return on(OtherSettingsPage.class);
    }

    public MailClientsSettingsPage settingsClient() {
        return on(MailClientsSettingsPage.class);
    }

    public FiltersCreationSettingsPage createFilters() {
        return on(FiltersCreationSettingsPage.class);
    }

    public FiltersOverviewSettingsPage filtersCommon() {
        return on(FiltersOverviewSettingsPage.class);
    }

    public CollectorSettingsPage settingsCollectors() {
        return on(CollectorSettingsPage.class);
    }

    public BackupSettingsPage settingsBackup() {
        return on(BackupSettingsPage.class);
    }

    public SecuritySettingsPage settingsSecurity() {
        return on(SecuritySettingsPage.class);
    }

    public DomainSettingsPage domainSettingsPage() {
        return on(DomainSettingsPage.class);
    }

    public UnsubscribeIframe settingsSubscriptions() {
        return on(UnsubscribeIframe.class);
    }

    public AbookPage abook() {
        return on(AbookPage.class);
    }

    public ComposePage compose() {
        return on(ComposePage.class);
    }

    public ComposePopup composePopup() {
        return on(ComposePopup.class);
    }

    public MessageViewPage msgView() {
        return on(MessageViewPage.class);
    }

    public CustomButtonsPage customButtons() {
        return on(CustomButtonsPage.class);
    }

    public WizardPage wizard() {
        return on(WizardPage.class);
    }

    public CorpPage corpPage() {
        return on(CorpPage.class);
    }
}
