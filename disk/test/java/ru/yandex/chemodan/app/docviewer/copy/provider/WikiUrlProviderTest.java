package ru.yandex.chemodan.app.docviewer.copy.provider;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class WikiUrlProviderTest {

    @Test
    public void isSupportedActualUri() {
        WikiUrlProvider urlProvider = new WikiUrlProvider(
                "", Cf.set("wiki-api.test.yandex-team.ru", "wiki-api.eva.test.yandex-team.ru"), true);

        ActualUri wikiTestTools = new ActualUri(
                "https://wiki-api.test.yandex-team.ru/_api/docviewer/download/?fileid=page_supertag%2Ffile_url&uid=123456789");
        ActualUri wikiEvaluationTestTools = new ActualUri(
                "https://wiki-api.eva.test.yandex-team.ru/_api/docviewer/download/?fileid=page_supertag%2Ffile_url&uid=123456789");

        Assert.isTrue(urlProvider.isSupportedActualUri(wikiTestTools));
        Assert.isTrue(urlProvider.isSupportedActualUri(wikiEvaluationTestTools));

        Assert.isTrue(urlProvider.isYaTeamServiceProvider());
    }

    @Test
    public void isSupportedUrl() {
        WikiUrlProvider urlProvider = new WikiUrlProvider(
                "", Cf.set("wiki-api.test.yandex-team.ru", "wiki-api.eva.test.yandex-team.ru"), true);

        String wikiYaTeamUrl = "ya-wiki://wiki-api.test.yandex-team.ru/page_supertag/file_url";
        String wikiEvaUrl = "ya-wiki://wiki-api.eva.test.yandex-team.ru/page_supertag/file_url";

        Assert.isTrue(urlProvider.isSupportedUrl(wikiYaTeamUrl));
        Assert.isTrue(urlProvider.isSupportedUrl(wikiEvaUrl));
    }
}
