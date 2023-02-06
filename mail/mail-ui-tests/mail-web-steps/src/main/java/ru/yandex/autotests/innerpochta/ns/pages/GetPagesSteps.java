package ru.yandex.autotests.innerpochta.ns.pages;

import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import ru.yandex.autotests.innerpochta.atlas.AllureListener;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.atlas.extensions.FindByCssCollectionExtension;
import ru.yandex.autotests.innerpochta.atlas.extensions.FindByCssExtension;
import ru.yandex.autotests.innerpochta.atlas.extensions.HoverMethodExtension;
import ru.yandex.autotests.innerpochta.cal.pages.TouchHomePage;
import ru.yandex.autotests.innerpochta.ns.pages.abook.pages.AbookPage;
import ru.yandex.autotests.innerpochta.ns.pages.lite.ComposeLitePage;
import ru.yandex.autotests.innerpochta.ns.pages.lite.MailboxListLitePage;
import ru.yandex.autotests.innerpochta.ns.pages.lite.MessageLitePage;
import ru.yandex.autotests.innerpochta.ns.pages.lite.MoreLitePage;
import ru.yandex.autotests.innerpochta.ns.pages.lite.SettingsLitePage;
import ru.yandex.autotests.innerpochta.ns.pages.messages.CustomButtonsPage;
import ru.yandex.autotests.innerpochta.ns.pages.messages.MessageViewPage;
import ru.yandex.autotests.innerpochta.ns.pages.messages.MessagePage;
import ru.yandex.autotests.innerpochta.ns.pages.nonmail.ChromeFileBrowserPage;
import ru.yandex.autotests.innerpochta.ns.pages.passport.PersonalDataPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.FiltersOverviewSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.SettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.AbookSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.CollectorSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.FiltersCreationSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.FoldersAndLabelsSettingPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.MailClientsSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.OtherSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.SecuritySettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.SenderInfoSettingsPage;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe;

/**
 * Created by angrybird on 29/10/14.
 * Here we can take all pages
 */
public class GetPagesSteps {

    private WebDriverRule driverRule;

    public GetPagesSteps(WebDriverRule driverRule) {
        this.driverRule = driverRule;
    }

    protected <T extends MailPage> T on(Class<T> pageClass) {
        Atlas atlas = new Atlas(new WebDriverConfiguration(driverRule.getDriver()));
        atlas.listener(new AllureListener());
        atlas.extension(new FindByCssExtension());
        atlas.extension(new FindByCssCollectionExtension());
        atlas.extension(new HoverMethodExtension());
        return atlas.create(driverRule.getDriver(), pageClass);
    }

    public HomePage HomePage() {
        return on(HomePage.class);
    }

    public MessagePage MessagePage() {
        return on(MessagePage.class);
    }

    public SearchPage SearchPage() {
        return on(SearchPage.class);
    }

    public SettingsPage SettingsPage() {
        return on(SettingsPage.class);
    }

    public SenderInfoSettingsPage SenderInfoSettingsPage() {
        return on(SenderInfoSettingsPage.class);
    }

    public FoldersAndLabelsSettingPage FoldersAndLabelsSettingPage() {
        return on(FoldersAndLabelsSettingPage.class);
    }

    public AbookSettingsPage AbookSettingsPage() {
        return on(AbookSettingsPage.class);
    }

    public OtherSettingsPage OtherSettingsPage() {
        return on(OtherSettingsPage.class);
    }

    public MailClientsSettingsPage MailClientsSettingsPage() {
        return on(MailClientsSettingsPage.class);
    }

    public FiltersCreationSettingsPage FiltersCreationSettingsPage() {
        return on(FiltersCreationSettingsPage.class);
    }

    public FiltersOverviewSettingsPage FiltersOverviewSettingsPage() {
        return on(FiltersOverviewSettingsPage.class);
    }

    public CollectorSettingsPage CollectorSettingsPage() {
        return on(CollectorSettingsPage.class);
    }

    public SecuritySettingsPage SecuritySettingsPage() {
        return on(SecuritySettingsPage.class);
    }

    public AbookPage AbookPage() {
        return on(AbookPage.class);
    }

    public ComposePage ComposePage() {
        return on(ComposePage.class);
    }

    public ComposePopup ComposePopup() {
        return on(ComposePopup.class);
    }

    public MessageViewPage MessageViewPage() {
        return on(MessageViewPage.class);
    }

    public CustomButtonsPage CustomButtonsPage() {
        return on(CustomButtonsPage.class);
    }

    public WizardPage WizardPage() {
        return on(WizardPage.class);
    }

    public MessageLitePage MessageLitePage() {
        return on(MessageLitePage.class);
    }

    public MailboxListLitePage MailboxListLitePage() {
        return on(MailboxListLitePage.class);
    }

    public SettingsLitePage SettingsLitePage() {
        return on(SettingsLitePage.class);
    }


    public HomerPage HomerPage() {
        return on(HomerPage.class);
    }

    public PassportPage PassportPage() {
        return on(PassportPage.class);
    }

    public PersonalDataPage PersonalDataPage() {
        return on(PersonalDataPage.class);
    }

    public UnsubscribeIframe SubscriptionsSettingsPage() { return on(UnsubscribeIframe.class); }

    public ComposeLitePage ComposeLitePage() { return on(ComposeLitePage.class); }

    public MoreLitePage MoreLitePage() { return on(MoreLitePage.class); }

    public TouchHomePage calTouch() {
        return on(TouchHomePage.class);
    }

    public ChromeFileBrowserPage chromeFileBrowserPage() {return on(ChromeFileBrowserPage.class); }
}
