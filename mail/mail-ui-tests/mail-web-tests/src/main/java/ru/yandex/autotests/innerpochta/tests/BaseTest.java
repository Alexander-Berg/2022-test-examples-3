package ru.yandex.autotests.innerpochta.tests;

import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.autotests.innerpochta.atlas.AllureListener;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.atlas.extensions.FindByCssCollectionExtension;
import ru.yandex.autotests.innerpochta.atlas.extensions.FindByCssExtension;
import ru.yandex.autotests.innerpochta.atlas.extensions.HoverMethodExtension;
import ru.yandex.autotests.innerpochta.ns.pages.ComposePage;
import ru.yandex.autotests.innerpochta.ns.pages.ComposePopup;
import ru.yandex.autotests.innerpochta.ns.pages.HomePage;
import ru.yandex.autotests.innerpochta.ns.pages.HomerPage;
import ru.yandex.autotests.innerpochta.ns.pages.PassportPage;
import ru.yandex.autotests.innerpochta.ns.pages.SearchPage;
import ru.yandex.autotests.innerpochta.ns.pages.WizardPage;
import ru.yandex.autotests.innerpochta.ns.pages.abook.pages.AbookPage;
import ru.yandex.autotests.innerpochta.ns.pages.corp.CorpPage;
import ru.yandex.autotests.innerpochta.ns.pages.lite.MailboxListLitePage;
import ru.yandex.autotests.innerpochta.ns.pages.lite.MessageLitePage;
import ru.yandex.autotests.innerpochta.ns.pages.lite.SettingsLitePage;
import ru.yandex.autotests.innerpochta.ns.pages.messages.CustomButtonsPage;
import ru.yandex.autotests.innerpochta.ns.pages.messages.MessageViewPage;
import ru.yandex.autotests.innerpochta.ns.pages.messages.MessagePage;
import ru.yandex.autotests.innerpochta.ns.pages.nonmail.ChromeFileBrowserPage;
import ru.yandex.autotests.innerpochta.ns.pages.nonmail.SocialAuthorizationPages;
import ru.yandex.autotests.innerpochta.ns.pages.settings.FiltersOverviewSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.SettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.AbookSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.CollectorSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.DomainSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.FiltersCreationSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.FoldersAndLabelsSettingPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.JournalSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.MailClientsSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.OtherSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.SecuritySettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.SenderInfoSettingsPage;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.BackupSettingsPage;
import ru.yandex.autotests.innerpochta.rules.ConditionalIgnoreRule;
import ru.yandex.autotests.innerpochta.rules.RetryRule;
import ru.yandex.autotests.innerpochta.rules.SemaphoreRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.WatchRule;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.enviroment.EnvironmentInfoRule;
import ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe;
import ru.yandex.autotests.passport.api.core.rules.LogTestStartRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.autotests.webcommon.util.prop.WebDriverProperties;

import static ru.yandex.autotests.innerpochta.rules.FailScreenRule.failScreenRule;
import static ru.yandex.autotests.innerpochta.rules.FilterRunRule.filterRunRule;
import static ru.yandex.autotests.innerpochta.rules.RetryRule.baseRetry;
import static ru.yandex.autotests.innerpochta.util.ProxyServerConstants.SESSION_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.ProxyServerConstants.SESSION_TIMEOUT_VALUE;

/**
 * Created by mabelpines
 */
public class BaseTest {

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static LogTestStartRule start = new LogTestStartRule();
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private RetryRule retry = baseRetry();
    public WebDriverRule webDriverRule = new WebDriverRule(setCapabilities()).withRetry(retry);
    private WatchRule testWatcher = new WatchRule();
    private EnvironmentInfoRule environmentInfoRule = new EnvironmentInfoRule();
    private TestRule semaphoreRule = SemaphoreRule.semaphoreRule().enableSemaphore();
    private TestRule ignoreRule = new ConditionalIgnoreRule();
    private TestRule filterRule = filterRunRule();
    @Rule
    public RuleChain chain = RuleChain.outerRule(new LogConfigRule())
        .around(ignoreRule)
        .around(filterRule)
        .around(retry)
        .around(testWatcher)
        .around(semaphoreRule)
        .around(webDriverRule)
        .around(failScreenRule(webDriverRule))
        .around(environmentInfoRule);

    protected <T extends MailPage> T on(Class<T> pageClass) {
        Atlas atlas = new Atlas(new WebDriverConfiguration(webDriverRule.getDriver()));
        atlas.listener(new AllureListener());
        atlas.extension(new FindByCssExtension());
        atlas.extension(new FindByCssCollectionExtension());
        atlas.extension(new HoverMethodExtension());
        return atlas.create(webDriverRule.getDriver(), pageClass);
    }

    protected DesiredCapabilities setCapabilities() {
        DesiredCapabilities capabilities = new DesiredCapabilities(
            WebDriverProperties.props().driverType(),
            WebDriverProperties.props().version(),
            WebDriverProperties.props().platform()
        );
        capabilities.setCapability(SESSION_TIMEOUT, SESSION_TIMEOUT_VALUE);
        capabilities.setCapability("acceptInsecureCerts", true);
        return capabilities;
    }

    public HomePage onHomePage() {
        return on(HomePage.class);
    }

    public MessagePage onMessagePage() {
        return on(MessagePage.class);
    }

    public SearchPage onSearchPage() {
        return on(SearchPage.class);
    }

    public SettingsPage onSettingsPage() { return on(SettingsPage.class); }

    public SenderInfoSettingsPage onSenderInfoSettingsPage() {
        return on(SenderInfoSettingsPage.class);
    }

    public FoldersAndLabelsSettingPage onFoldersAndLabelsSetup() {
        return on(FoldersAndLabelsSettingPage.class);
    }

    public AbookSettingsPage onAbookSettingsPage() {
        return on(AbookSettingsPage.class);
    }

    public OtherSettingsPage onOtherSettings() {
        return on(OtherSettingsPage.class);
    }

    public MailClientsSettingsPage clients() {
        return on(MailClientsSettingsPage.class);
    }

    public FiltersCreationSettingsPage onFiltersCreationPage() {
        return on(FiltersCreationSettingsPage.class);
    }

    public FiltersOverviewSettingsPage onFiltersOverview() {
        return on(FiltersOverviewSettingsPage.class);
    }

    public CollectorSettingsPage onCollectorSettingsPage() {
        return on(CollectorSettingsPage.class);
    }

    public SecuritySettingsPage onSecuritySettingsPage() {
        return on(SecuritySettingsPage.class);
    }

    public DomainSettingsPage domainSettingsPage() { return on(DomainSettingsPage.class); }

    public AbookPage onAbookPage() {
        return on(AbookPage.class);
    }

    public ComposePage onComposePage() {
        return on(ComposePage.class);
    }

    public ComposePopup onComposePopup() {
        return on(ComposePopup.class);
    }

    public MessageViewPage onMessageView() {
        return on(MessageViewPage.class);
    }

    public CustomButtonsPage onCustomButtons() {
        return on(CustomButtonsPage.class);
    }

    public WizardPage onWizardPage() {
        return on(WizardPage.class);
    }

    public MessageLitePage onMessageLitePage() {
        return on(MessageLitePage.class);
    }

    public MailboxListLitePage onMailboxListLitePage() {
        return on(MailboxListLitePage.class);
    }

    public SettingsLitePage onSettingsLitePage() {
        return on(SettingsLitePage.class);
    }

    public HomerPage onHomerPage() {
        return on(HomerPage.class);
    }

    public PassportPage onPassportPage() {
        return on(PassportPage.class);
    }

    public JournalSettingsPage onJournalSettingsPage() {
        return on(JournalSettingsPage.class);
    }

    public UnsubscribeIframe onUnsubscribePopupPage() {
        return on(UnsubscribeIframe.class);
    }

    public CorpPage onCorpPage() {
        return on(CorpPage.class);
    }

    public SocialAuthorizationPages onSocialAuthorizationPages() {
        return on(SocialAuthorizationPages.class);
    }

    public ChromeFileBrowserPage onChromeFileBrowserPage() {
        return on(ChromeFileBrowserPage.class);
    }

    public BackupSettingsPage onBackupSettingsPage() {
        return on(BackupSettingsPage.class);
    }
}
