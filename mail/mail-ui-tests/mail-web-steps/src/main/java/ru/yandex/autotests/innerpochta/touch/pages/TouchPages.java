package ru.yandex.autotests.innerpochta.touch.pages;

import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import ru.yandex.autotests.innerpochta.atlas.AllureListener;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.atlas.extensions.FindByCssCollectionExtension;
import ru.yandex.autotests.innerpochta.atlas.extensions.FindByCssExtension;
import ru.yandex.autotests.innerpochta.atlas.extensions.HoverMethodExtension;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;

public class TouchPages {

    private static WebDriverRule webDriverRule;

    public TouchPages(WebDriverRule webDriverRule) {
        this.webDriverRule = webDriverRule;
    }

    protected <T extends MailPage> T on(Class<T> pageClass) {
        Atlas atlas = new Atlas(new WebDriverConfiguration(webDriverRule.getDriver()));
        atlas.listener(new AllureListener());
        atlas.extension(new FindByCssExtension());
        atlas.extension(new FindByCssCollectionExtension());
        atlas.extension(new HoverMethodExtension());
        return atlas.create(webDriverRule.getDriver(), pageClass);
    }

    public HostrootPage hostroot() {
        return on(HostrootPage.class);
    }

    public MordaPage mordaPage() {
        return on(MordaPage.class);
    }

    public MessageListPage messageList() {
        return on(MessageListPage.class);
    }

    public SidebarPage sidebar() {
        return on(SidebarPage.class);
    }

    public SearchPage search() {
        return on(SearchPage.class);
    }

    public SearchResultPage searchResult() {
        return on(SearchResultPage.class);
    }

    public MessageViewPage messageView(){
        return on(MessageViewPage.class);
    }

    public ComposePage compose() {
        return on(ComposePage.class);
    }

    public SettingsPage settings() {
        return on(SettingsPage.class);
    }

    public PassportPage passport() { return on(PassportPage.class); }

    public UnsubscribeIframe unsubscribe() { return on(UnsubscribeIframe.class); }

    public ComposeIframePage composeIframe() { return on(ComposeIframePage.class); }
}
