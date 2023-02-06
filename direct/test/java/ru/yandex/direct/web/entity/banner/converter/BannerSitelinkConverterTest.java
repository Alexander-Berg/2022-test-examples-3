package ru.yandex.direct.web.entity.banner.converter;

import java.util.ArrayList;

import org.junit.Test;

import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.web.entity.banner.model.WebBannerSitelink;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.web.entity.banner.converter.BannerSitelinkConverter.toCoreSitelinks;

@SuppressWarnings("ConstantConditions")
public class BannerSitelinkConverterTest {

    private static final String URL_PROTOCOL_1 = "https://";
    private static final String HREF_1 = "yandex.ru";
    private static final String TITLE_1 = "t1";
    private static final String DESC_1 = "d1";

    private static final String URL_PROTOCOL_2 = "http://";
    private static final String HREF_2 = "ya.ru";
    private static final String TITLE_2 = "t2";
    private static final String DESC_2 = "d2";

    @Test
    public void nullSitelinksIsConvertedToNull() {
        assertThat(toCoreSitelinks(null), nullValue());
    }

    @Test
    public void emptySitelinksIsConvertedToNull() {
        assertThat(toCoreSitelinks(new ArrayList<>()), nullValue());
    }

    @Test
    public void oneEmptySitelinkIsConvertedToNull() {
        WebBannerSitelink webSitelink = new WebBannerSitelink()
                .withHref("")
                .withTitle("")
                .withDescription("");
        assertThat(toCoreSitelinks(singletonList(webSitelink)), nullValue());
    }

    @Test
    public void twoEmptySitelinkIsConvertedToNull() {
        WebBannerSitelink webSitelink = new WebBannerSitelink()
                .withHref("")
                .withTitle("")
                .withDescription("");
        assertThat(toCoreSitelinks(asList(webSitelink, webSitelink)), nullValue());
    }

    @Test
    public void oneSitelinkIsConverted() {
        WebBannerSitelink webSitelink = new WebBannerSitelink()
                .withUrlProtocol(URL_PROTOCOL_1)
                .withHref(HREF_1)
                .withTitle(TITLE_1)
                .withDescription(DESC_1);

        Sitelink expectedSitelink = new Sitelink()
                .withHref(URL_PROTOCOL_1 + HREF_1)
                .withTitle(TITLE_1)
                .withDescription(DESC_1);

        SitelinkSet expectedSitelinkSet = new SitelinkSet()
                .withSitelinks(singletonList(expectedSitelink));
        SitelinkSet actualSitelinkSet = toCoreSitelinks(singletonList(webSitelink));

        assertThat(actualSitelinkSet, beanDiffer(expectedSitelinkSet));
    }

    @Test
    public void oneNullSitelinkAndOneFilledSitelinkAndNullSitelinksIsConverted() {
        WebBannerSitelink webSitelink = new WebBannerSitelink()
                .withUrlProtocol(URL_PROTOCOL_1)
                .withHref(HREF_1)
                .withTitle(TITLE_1)
                .withDescription(DESC_1);

        Sitelink expectedSitelink = new Sitelink()
                .withHref(URL_PROTOCOL_1 + HREF_1)
                .withTitle(TITLE_1)
                .withDescription(DESC_1);

        SitelinkSet expectedSitelinkSet = new SitelinkSet()
                .withSitelinks(asList(null, expectedSitelink, null, null));
        SitelinkSet actualSitelinkSet = toCoreSitelinks(asList(null, webSitelink, null, null));

        assertThat(actualSitelinkSet, beanDiffer(expectedSitelinkSet));
    }

    @Test
    public void twoSitelinksIsConverted() {
        WebBannerSitelink webSitelink1 = new WebBannerSitelink()
                .withUrlProtocol(URL_PROTOCOL_1)
                .withHref(HREF_1)
                .withTitle(TITLE_1)
                .withDescription(DESC_1);
        WebBannerSitelink webSitelink2 = new WebBannerSitelink()
                .withUrlProtocol(URL_PROTOCOL_2)
                .withHref(HREF_2)
                .withTitle(TITLE_2)
                .withDescription(DESC_2);

        Sitelink expectedSitelink1 = new Sitelink()
                .withHref(URL_PROTOCOL_1 + HREF_1)
                .withTitle(TITLE_1)
                .withDescription(DESC_1);
        Sitelink expectedSitelink2 = new Sitelink()
                .withHref(URL_PROTOCOL_2 + HREF_2)
                .withTitle(TITLE_2)
                .withDescription(DESC_2);

        SitelinkSet expectedSitelinkSet = new SitelinkSet()
                .withSitelinks(asList(expectedSitelink1, expectedSitelink2));
        SitelinkSet actualSitelinkSet = toCoreSitelinks(asList(webSitelink1, webSitelink2));

        assertThat(actualSitelinkSet, beanDiffer(expectedSitelinkSet));
    }

    @Test
    public void twoRarefiedSitelinksIsConverted() {
        WebBannerSitelink webSitelink1 = new WebBannerSitelink()
                .withUrlProtocol(URL_PROTOCOL_1)
                .withHref(HREF_1)
                .withTitle(TITLE_1)
                .withDescription(DESC_1);
        WebBannerSitelink webSitelink2 = new WebBannerSitelink()
                .withUrlProtocol(URL_PROTOCOL_2)
                .withHref(HREF_2)
                .withTitle(TITLE_2)
                .withDescription(DESC_2);

        Sitelink expectedSitelink1 = new Sitelink()
                .withHref(URL_PROTOCOL_1 + HREF_1)
                .withTitle(TITLE_1)
                .withDescription(DESC_1);
        Sitelink expectedSitelink2 = new Sitelink()
                .withHref(URL_PROTOCOL_2 + HREF_2)
                .withTitle(TITLE_2)
                .withDescription(DESC_2);

        SitelinkSet expectedSitelinkSet = new SitelinkSet()
                .withSitelinks(asList(null, expectedSitelink1, null, expectedSitelink2));
        SitelinkSet actualSitelinkSet = toCoreSitelinks(asList(null, webSitelink1, null, webSitelink2));

        assertThat(actualSitelinkSet, beanDiffer(expectedSitelinkSet));
    }
}
