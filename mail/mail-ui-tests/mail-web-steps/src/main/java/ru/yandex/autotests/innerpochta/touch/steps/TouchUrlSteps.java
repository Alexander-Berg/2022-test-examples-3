package ru.yandex.autotests.innerpochta.touch.steps;

import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.qatools.allure.annotations.Step;

import javax.ws.rs.core.UriBuilder;

import static javax.ws.rs.core.UriBuilder.fromUri;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;

/**
 * Created by puffyfloof.
 */
public class TouchUrlSteps {

    private WebDriverRule webDriverRule;

    public TouchUrlSteps(WebDriverRule webDriverRule) {
        this.webDriverRule = webDriverRule;
    }

    public TouchUrlSteps() {
    }

    private UriBuilder uri = fromUri(urlProps().getBaseUri()).path("touch");

    public TouchUrlSteps searchAllMailWithBaseFid(String fid, String query){
        uri.path("search").queryParam("request", query);
        return this;
    }

    public TouchUrlSteps searchFid(String fid, String query){
        uri.path("search").queryParam("current_fid", fid).queryParam("search_fid", fid).queryParam("request", query);
        return this;
    }

    public TouchUrlSteps messageListInFid(String fid){
        uri.path("folder").path(fid);
        return this;
    }

    public void open() {
        openUrl(uri.build().toString());
    }

    @Step("Открываем прямой URL «{0}»")
    private void openUrl(String url) {
        webDriverRule.getDriver().get(url);
    }
}
