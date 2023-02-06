package ru.yandex.direct.web.entity.banner.converter;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.CpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.web.entity.adgroup.model.PixelKind;
import ru.yandex.direct.web.entity.banner.model.WebBannerCreative;
import ru.yandex.direct.web.entity.banner.model.WebBannerMeasurer;
import ru.yandex.direct.web.entity.banner.model.WebBannerMeasurerSystem;
import ru.yandex.direct.web.entity.banner.model.WebBannerTurbolanding;
import ru.yandex.direct.web.entity.banner.model.WebCpmBanner;
import ru.yandex.direct.web.entity.banner.model.WebPixel;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.web.entity.banner.converter.CpmBannerConverter.webBannerToCoreCpmBanner;

public class CpmBannerConverterTest {

    @Test
    public void webBannerToCoreCpmBanner_PrimitiveFields() {
        WebCpmBanner webCpmBanner = new WebCpmBanner()
                .withId(1L)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withPixels(singletonList(new WebPixel().withKind(PixelKind.AUDIENCE).withUrl(yaAudiencePixelUrl())))
                .withUrlProtocol("https")
                .withHref("bannerHref")
                .withCreative(new WebBannerCreative().withCreativeId("2"))
                .withTurbolanding(new WebBannerTurbolanding().withId(3L))
                .withTurbolandingHrefParams("param1=value1&param2=value2");

        var banner = (CpmBanner) webBannerToCoreCpmBanner(AdGroupType.CPM_BANNER, webCpmBanner);

        var expected = new CpmBanner()
                .withId(1L)
                .withPixels(singletonList(yaAudiencePixelUrl()))
                .withHref("https" + "bannerHref")
                .withCreativeId(2L)
                .withTurboLandingId(3L)
                .withTurboLandingHrefParams("param1=value1&param2=value2")
                .withMeasurers(emptyList());
        assertThat(banner, beanDiffer(expected));
    }

    @Test
    public void webBannerToCoreCpmBanner_WebBannerTurbolanding() {
        WebBannerTurbolanding webTurbolanding = new WebBannerTurbolanding()
                .withId(1L);
        WebCpmBanner webBanner = new WebCpmBanner().withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withTurbolanding(webTurbolanding);

        var banner = (CpmBanner) webBannerToCoreCpmBanner(AdGroupType.CPM_BANNER, webBanner);
        //noinspection ConstantConditions
        assertThat(banner.getTurboLandingId(), equalTo(1L));
    }

    @Test
    public void webBannerToCoreCpmOutdoorBanner() {
        WebCpmBanner webCpmBanner = new WebCpmBanner()
                .withId(1L)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withUrlProtocol("https")
                .withHref("bannerHref")
                .withCreative(new WebBannerCreative().withCreativeId("2"));

        var banner = (CpmOutdoorBanner) webBannerToCoreCpmBanner(AdGroupType.CPM_OUTDOOR, webCpmBanner);

        var expected = new CpmOutdoorBanner()
                .withId(1L)
                .withHref("https" + "bannerHref")
                .withCreativeId(2L);
        assertThat(banner, beanDiffer(expected));
    }

    @Test
    public void webBannerToCoreCpmIndoorBanner() {
        WebCpmBanner webCpmBanner = new WebCpmBanner()
                .withId(1L)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withUrlProtocol("https")
                .withHref("bannerHref")
                .withCreative(new WebBannerCreative().withCreativeId("2"));

        var banner = (CpmIndoorBanner) webBannerToCoreCpmBanner(AdGroupType.CPM_INDOOR, webCpmBanner);

        var expected = new CpmIndoorBanner()
                .withId(1L)
                .withHref("https" + "bannerHref")
                .withCreativeId(2L);
        assertThat(banner, beanDiffer(expected));
    }

    @Test
    public void webBannerToCoreCpmBannerWithAdmetricaMeasurer() {
        WebCpmBanner webCpmBanner = new WebCpmBanner()
                .withId(1L)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withUrlProtocol("https")
                .withHref("bannerHref")
                .withCreative(new WebBannerCreative().withCreativeId("2"))
                .withMeasurers(List.of(
                        new WebBannerMeasurer()
                                .withMeasurerSystem(WebBannerMeasurerSystem.ADMETRICA)
                                .withParams("params")));

        var banner = (CpmBanner) webBannerToCoreCpmBanner(AdGroupType.CPM_BANNER, webCpmBanner);

        var expected = new CpmBanner()
                .withId(1L)
                .withPixels(emptyList())
                .withHref("https" + "bannerHref")
                .withCreativeId(2L)
                .withMeasurers(List.of(
                        new BannerMeasurer()
                                .withBannerMeasurerSystem(BannerMeasurerSystem.ADMETRICA)
                                .withParams("params")));
        assertThat(banner, beanDiffer(expected));
    }

    @Test
    public void webBannerToCoreCpmBannerWithWeboramaMeasurer() {
        WebCpmBanner webCpmBanner = new WebCpmBanner()
                .withId(1L)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withUrlProtocol("https")
                .withHref("bannerHref")
                .withCreative(new WebBannerCreative().withCreativeId("2"))
                .withMeasurers(List.of(
                        new WebBannerMeasurer()
                                .withMeasurerSystem(WebBannerMeasurerSystem.WEBORAMA)
                                .withParams("{\"account\":1,\"tte\":2,\"aap\":3}")));

        var banner = (CpmBanner) webBannerToCoreCpmBanner(AdGroupType.CPM_BANNER, webCpmBanner);

        var expected = new CpmBanner()
                .withId(1L)
                .withPixels(emptyList())
                .withHref("https" + "bannerHref")
                .withCreativeId(2L)
                .withMeasurers(List.of(
                        new BannerMeasurer()
                                .withBannerMeasurerSystem(BannerMeasurerSystem.WEBORAMA)
                                .withParams("{\"account\":1,\"tte\":2,\"aap\":3}")));
        assertThat(banner, beanDiffer(expected));
    }

    @Test
    public void webBannerToCoreCpmBannerWithEmptyAdmetricaMeasurers() {
        WebCpmBanner webCpmBanner = new WebCpmBanner()
                .withId(1L)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withUrlProtocol("https")
                .withHref("bannerHref")
                .withCreative(new WebBannerCreative().withCreativeId("2"))
                .withMeasurers(null);

        var banner = (CpmBanner) webBannerToCoreCpmBanner(AdGroupType.CPM_BANNER, webCpmBanner);

        var expected = new CpmBanner()
                .withId(1L)
                .withPixels(emptyList())
                .withHref("https" + "bannerHref")
                .withCreativeId(2L)
                .withMeasurers(emptyList());
        assertThat(banner, beanDiffer(expected));
    }
}
