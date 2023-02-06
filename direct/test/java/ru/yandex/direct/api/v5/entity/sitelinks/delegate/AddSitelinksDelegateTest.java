package ru.yandex.direct.api.v5.entity.sitelinks.delegate;

import java.util.List;

import com.yandex.direct.api.v5.sitelinks.AddRequest;
import com.yandex.direct.api.v5.sitelinks.SitelinkAddItem;
import com.yandex.direct.api.v5.sitelinks.SitelinksSetAddItem;
import org.junit.Test;

import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;

import static org.assertj.core.api.Assertions.assertThat;

public class AddSitelinksDelegateTest {
    @Test
    public void convertRequest_convertsCorrectNumberOfSitelinkSets() {
        AddSitelinksDelegate delegate = new AddSitelinksDelegate(null, null, null);

        List<SitelinkSet> sitelinkSets = delegate.convertRequest(new AddRequest().withSitelinksSets(
                new SitelinksSetAddItem().withSitelinks(new SitelinkAddItem().withTitle("title1").withHref("href1")),
                new SitelinksSetAddItem().withSitelinks(new SitelinkAddItem().withTitle("title2").withHref("href2"))));

        assertThat(sitelinkSets.size()).isEqualTo(2);
    }

    @Test
    public void convertRequest_convertsCorrectNumberOfSitelinks() {
        AddSitelinksDelegate delegate = new AddSitelinksDelegate(null, null, null);

        List<SitelinkSet> sitelinkSets = delegate.convertRequest(new AddRequest().withSitelinksSets(
                new SitelinksSetAddItem().withSitelinks(
                        new SitelinkAddItem().withTitle("title1").withHref("href1"),
                        new SitelinkAddItem().withTitle("title2").withHref("href2"))));

        assertThat(sitelinkSets.get(0).getSitelinks().size()).isEqualTo(2);
    }

    @Test
    public void convertRequest_convertsTitle() {
        String title = "title";
        AddSitelinksDelegate delegate = new AddSitelinksDelegate(null, null, null);

        List<SitelinkSet> sitelinkSets = delegate.convertRequest(new AddRequest().withSitelinksSets(
                new SitelinksSetAddItem().withSitelinks(
                        new SitelinkAddItem().withTitle(title).withHref("href1"))));

        assertThat(sitelinkSets.get(0).getSitelinks().get(0).getTitle()).isEqualTo(title);
    }

    @Test
    public void convertRequest_convertsHref() {
        String href = "href";
        AddSitelinksDelegate delegate = new AddSitelinksDelegate(null, null, null);

        List<SitelinkSet> sitelinkSets = delegate.convertRequest(new AddRequest().withSitelinksSets(
                new SitelinksSetAddItem().withSitelinks(
                        new SitelinkAddItem().withTitle("title").withHref(href))));

        assertThat(sitelinkSets.get(0).getSitelinks().get(0).getHref()).isEqualTo(href);
    }

    @Test
    public void convertRequest_convertsDescription() {
        String description = "description";
        AddSitelinksDelegate delegate = new AddSitelinksDelegate(null, null, null);

        List<SitelinkSet> sitelinkSets = delegate.convertRequest(new AddRequest().withSitelinksSets(
                new SitelinksSetAddItem().withSitelinks(
                        new SitelinkAddItem().withTitle("title").withHref("href1").withDescription(description))));

        assertThat(sitelinkSets.get(0).getSitelinks().get(0).getDescription()).isEqualTo(description);
    }
}
