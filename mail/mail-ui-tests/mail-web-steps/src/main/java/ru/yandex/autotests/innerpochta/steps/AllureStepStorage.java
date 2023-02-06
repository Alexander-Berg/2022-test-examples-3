package ru.yandex.autotests.innerpochta.steps;


import ru.yandex.autotests.innerpochta.cal.pages.CalPages;
import ru.yandex.autotests.innerpochta.cal.steps.CalCreateEventSteps;
import ru.yandex.autotests.innerpochta.cal.steps.CalTouchCreateEventSteps;
import ru.yandex.autotests.innerpochta.cal.steps.CalTouchGridSteps;
import ru.yandex.autotests.innerpochta.cal.steps.CalTouchTodoSteps;
import ru.yandex.autotests.innerpochta.cal.steps.SettingsSteps;
import ru.yandex.autotests.innerpochta.cal.steps.api.ApiCalSettingsSteps;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.steps.api.ApiAbookSteps;
import ru.yandex.autotests.innerpochta.steps.api.ApiBackupSteps;
import ru.yandex.autotests.innerpochta.steps.api.ApiCollectorSteps;
import ru.yandex.autotests.innerpochta.steps.api.ApiFiltersSteps;
import ru.yandex.autotests.innerpochta.steps.api.ApiFoldersSteps;
import ru.yandex.autotests.innerpochta.steps.api.ApiLabelSteps;
import ru.yandex.autotests.innerpochta.steps.api.ApiMessagesSteps;
import ru.yandex.autotests.innerpochta.steps.api.ApiSearchSteps;
import ru.yandex.autotests.innerpochta.steps.api.ApiSettingsSteps;
import ru.yandex.autotests.innerpochta.steps.api.ApiTodoSteps;
import ru.yandex.autotests.innerpochta.steps.lite.ComposeLitePageSteps;
import ru.yandex.autotests.innerpochta.steps.lite.LitePageStepGroups;
import ru.yandex.autotests.innerpochta.steps.lite.MailboxListLitePageSteps;
import ru.yandex.autotests.innerpochta.steps.lite.MoreLitePageSteps;
import ru.yandex.autotests.innerpochta.steps.lite.SettingsLitePageSteps;
import ru.yandex.autotests.innerpochta.touch.pages.TouchPages;
import ru.yandex.autotests.innerpochta.touch.steps.TouchSteps;
import ru.yandex.autotests.innerpochta.touch.steps.TouchUrlSteps;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * author @mabelpines
 */
public class AllureStepStorage {

    private WebDriverRule webDriverRule;
    private RestAssuredAuthRule auth;

    private DefaultSteps defaultSteps;
    private MessagePageSteps messagePageSteps;
    private MessageViewSteps messageViewSteps;
    private ComposeSteps composeSteps;
    private LoginPageSteps loginSteps;
    private ComposeLitePageSteps composeLiteSteps;
    private MailboxListLitePageSteps liteMailboxSteps;
    private MoreLitePageSteps liteMoreSteps;
    private LitePageStepGroups liteInterfaceSteps;
    private SettingsLitePageSteps liteSettingsSteps;
    private HotKeySteps hotkeySteps;
    private SettingsPageSteps settingsSteps;
    private WizardPageSteps wizardSteps;
    private FiltersSteps filtersSteps;
    private AbookSteps abookSteps;
    private ApiSettingsSteps apiSettingsSteps;
    private ApiFoldersSteps apiFoldersSteps;
    private ApiLabelSteps apiLabelsSteps;
    private ApiFiltersSteps apiFiltersSteps;
    private ApiCollectorSteps apiCollectorSteps;
    private ApiAbookSteps apiAbookSteps;
    private ApiMessagesSteps apiMessagesSteps;
    private ApiTodoSteps apiTodoSteps;
    private ApiCalSettingsSteps apiCalSettingsSteps;
    private SettingsSteps settingsCalSteps;
    private TouchUrlSteps touchUrlSteps;
    private GetPagesSteps pages;
    private TouchPages touchPages;
    private CalPages calPages;
    private AdvertisementSteps advertisementSteps;
    private LeftColumnSteps leftColumnSteps;
    private CalTouchCreateEventSteps calTouchCreateEventSteps;
    private CalCreateEventSteps calCreateEventSteps;
    private CalTouchTodoSteps calTouchTodoSteps;
    private TouchSteps touchSteps;
    private CalTouchGridSteps calTouchGridSteps;
    private ApiSearchSteps apiSearchSteps;
    private ImapSteps imapSteps;
    private ApiBackupSteps apiBackupSteps;

    private Set<String> experiments = new HashSet<>();

    public AllureStepStorage(WebDriverRule webDriverRule, RestAssuredAuthRule auth) {
        this.webDriverRule = webDriverRule;
        this.auth = auth;
    }

    public AllureStepStorage(WebDriverRule webDriverRule) {
        this(webDriverRule, null);
    }

    public DefaultSteps defaultSteps() {
        if (defaultSteps == null) {
            defaultSteps = new DefaultSteps(webDriverRule, this);
        }
        return defaultSteps;
    }

    public MessagePageSteps messagesSteps() {
        if (messagePageSteps == null) {
            messagePageSteps = new MessagePageSteps(webDriverRule, this);
        }
        return messagePageSteps;
    }

    public MessageViewSteps messageViewSteps() {
        if (messageViewSteps == null) {
            messageViewSteps = new MessageViewSteps(this);
        }
        return messageViewSteps;
    }

    public LoginPageSteps loginSteps() {
        if (loginSteps == null) {
            loginSteps = new LoginPageSteps(webDriverRule, this);
        }
        return loginSteps;
    }

    public ComposeSteps composeSteps() {
        if (composeSteps == null) {
            composeSteps = new ComposeSteps(webDriverRule, this);
        }
        return composeSteps;
    }

    public ComposeLitePageSteps composeLiteSteps() {
        if (composeLiteSteps == null) {
            composeLiteSteps = new ComposeLitePageSteps(this);
        }
        return composeLiteSteps;
    }

    public MailboxListLitePageSteps liteMailboxSteps() {
        if (liteMailboxSteps == null) {
            liteMailboxSteps = new MailboxListLitePageSteps(webDriverRule, this);
        }
        return liteMailboxSteps;
    }

    public MoreLitePageSteps liteMoreSteps() {
        if (liteMoreSteps == null) {
            liteMoreSteps = new MoreLitePageSteps(this);
        }
        return liteMoreSteps;
    }

    public LitePageStepGroups liteInterfaceSteps() {
        if (liteInterfaceSteps == null) {
            liteInterfaceSteps = new LitePageStepGroups(this);
        }
        return liteInterfaceSteps;
    }

    public SettingsLitePageSteps liteSettingsSteps() {
        if (liteSettingsSteps == null) {
            liteSettingsSteps = new SettingsLitePageSteps(this);
        }
        return liteSettingsSteps;
    }

    public HotKeySteps hotkeySteps() {
        if (hotkeySteps == null) {
            hotkeySteps = new HotKeySteps(webDriverRule, this);
        }
        return hotkeySteps;
    }

    public SettingsPageSteps settingsSteps() {
        if (settingsSteps == null) {
            settingsSteps = new SettingsPageSteps(webDriverRule, this);
        }
        return settingsSteps;
    }

    public WizardPageSteps wizardSteps() {
        if (wizardSteps == null) {
            wizardSteps = new WizardPageSteps(webDriverRule, this);
        }
        return wizardSteps;
    }

    public FiltersSteps filtersSteps() {
        if (filtersSteps == null) {
            filtersSteps = new FiltersSteps(webDriverRule, this);
        }
        return filtersSteps;
    }

    public AbookSteps abookSteps() {
        if (abookSteps == null) {
            abookSteps = new AbookSteps(webDriverRule, this);
        }
        return abookSteps;
    }

    public ApiSettingsSteps apiSettingsSteps() {
        if (apiSettingsSteps == null) {
            apiSettingsSteps = new ApiSettingsSteps();
        }
        return apiSettingsSteps.withAuth(auth);
    }

    public ApiFoldersSteps apiFoldersSteps() {
        if (apiFoldersSteps == null) {
            apiFoldersSteps = new ApiFoldersSteps();
        }
        return apiFoldersSteps.withAuth(auth);
    }

    public ApiLabelSteps apiLabelsSteps() {
        if (apiLabelsSteps == null) {
            apiLabelsSteps = new ApiLabelSteps(this);
        }
        return apiLabelsSteps.withAuth(auth);
    }

    public ApiFiltersSteps apiFiltersSteps() {
        if (apiFiltersSteps == null) {
            apiFiltersSteps = new ApiFiltersSteps(this);
        }
        return apiFiltersSteps.withAuth(auth);
    }

    public ApiCollectorSteps apiCollectorSteps() {
        if (apiCollectorSteps == null) {
            apiCollectorSteps = new ApiCollectorSteps();
        }
        return apiCollectorSteps.withAuth(auth);
    }

    public ApiAbookSteps apiAbookSteps() {
        if (apiAbookSteps == null) {
            apiAbookSteps = new ApiAbookSteps();
        }
        return apiAbookSteps.withAuth(auth);
    }

    public ApiMessagesSteps apiMessagesSteps() {
        if (apiMessagesSteps == null) {
            apiMessagesSteps = new ApiMessagesSteps(this);
        }
        return apiMessagesSteps.withAuth(auth);
    }

    public ApiTodoSteps apiTodoSteps() {
        if (apiTodoSteps == null) {
            apiTodoSteps = new ApiTodoSteps();
        }
        return apiTodoSteps.withAuth(auth);
    }

    public ApiCalSettingsSteps apiCalSettingsSteps() {
        if (apiCalSettingsSteps == null) {
            apiCalSettingsSteps = new ApiCalSettingsSteps(this);
        }
        return apiCalSettingsSteps.withAuth(auth);
    }

    public SettingsSteps settingsCalSteps() {
        if (settingsCalSteps == null) {
            settingsCalSteps = new SettingsSteps();
        }
        return settingsCalSteps;
    }

    public TouchUrlSteps touchUrlSteps() {
        if (touchUrlSteps == null) {
            touchUrlSteps = new TouchUrlSteps(webDriverRule);
        }
        return touchUrlSteps;
    }

    public GetPagesSteps pages() {
        if (pages == null) {
            pages = new GetPagesSteps(webDriverRule);
        }
        return pages;
    }

    public TouchPages touchPages() {
        if (touchPages == null) {
            touchPages = new TouchPages(webDriverRule);
        }
        return touchPages;
    }

    public CalPages calPages() {
        if (calPages == null) {
            calPages = new CalPages(webDriverRule);
        }
        return calPages;
    }

    public AdvertisementSteps advertisementSteps() {
        if (advertisementSteps == null) {
            advertisementSteps = new AdvertisementSteps(webDriverRule, this);
        }
        return advertisementSteps;
    }

    public LeftColumnSteps leftColumnSteps() {
        if (leftColumnSteps == null) {
            leftColumnSteps = new LeftColumnSteps(webDriverRule, this);
        }
        return leftColumnSteps;
    }

    public CalTouchCreateEventSteps calTouchCreateEventSteps() {
        if (calTouchCreateEventSteps == null) {
            calTouchCreateEventSteps = new CalTouchCreateEventSteps(webDriverRule, this);
        }
        return calTouchCreateEventSteps;
    }

    public CalCreateEventSteps calCreateEventSteps() {
        if (calCreateEventSteps == null) {
            calCreateEventSteps = new CalCreateEventSteps(webDriverRule, this);
        }
        return calCreateEventSteps;
    }

    public CalTouchTodoSteps calTouchTodoSteps() {
        if (calTouchTodoSteps == null) {
            calTouchTodoSteps = new CalTouchTodoSteps(webDriverRule, this);
        }
        return calTouchTodoSteps;
    }

    public TouchSteps touchSteps() {
        if (touchSteps == null) {
            touchSteps = new TouchSteps(webDriverRule, this);
        }
        return touchSteps;
    }

    public CalTouchGridSteps calTouchGridSteps() {
        if (calTouchGridSteps == null) {
            calTouchGridSteps = new CalTouchGridSteps(webDriverRule, this);
        }
        return calTouchGridSteps;
    }

    public ApiSearchSteps apiSearchSteps() {
        if (apiSearchSteps == null) {
            apiSearchSteps = new ApiSearchSteps();
        }
        return apiSearchSteps.withAuth(auth);
    }

    public ImapSteps imapSteps() {
        if (imapSteps == null) {
            imapSteps = new ImapSteps(this);
        }
        return imapSteps.withAuth(auth);
    }

    public ApiBackupSteps apiBackupSteps() {
        if (apiBackupSteps == null) {
            apiBackupSteps = new ApiBackupSteps(this);
        }
        return apiBackupSteps.withAuth(auth);
    }

    public void putExperiments(String... exps) {
        experiments.addAll(Arrays.asList(exps));
    }

    public void clearExperiments() {
        experiments = new HashSet<>();
    }

    public Set<String> getExperiments() {
        return experiments;
    }

    public boolean isAuthPresent() {
        return auth != null;
    }

    public RestAssuredAuthRule getAuth() {
        return auth;
    }
}
