package ru.yandex.market.crm.campaign.services.sending;

import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.crm.core.domain.HasUtmLinks;
import ru.yandex.market.crm.core.services.sending.UtmLinks;
import ru.yandex.market.crm.mapreduce.domain.ImageLink;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.BannerBlockData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UtmLinksTest {

    @Test
    public void updateBlockLinks() {
        UtmLinks utmLinks = UtmLinks.forEmailSending(
                "campaignId",
                HasUtmLinks.from("campaign_1", "source_1",
                        "medium_1", "referrer_1", 619L)
        );

        BannerBlockData bannerBlockData = new BannerBlockData();
        ImageLink imageLink = new ImageLink();
        imageLink.setLink("https://market.yandex.ru?utm_campaign=origin");
        bannerBlockData.setBanners(Collections.singletonList(imageLink));

        utmLinks.updateBlockLinks(bannerBlockData);

        assertEquals(
                "https://market.yandex.ru?" +
                        "utm_campaign=origin&" +
                        "utm_source=source_1&" +
                        "utm_medium=medium_1&" +
                        "utm_referrer=referrer_1&" +
                        "eh=23463b99b62a72f26ed677cc556c44e8&" +
                        "ecid=campaignId&" +
                        "clid=619",
                imageLink.getLink()
        );
    }

    @Test
    public void testPreview() {
        UtmLinks utmLinks = UtmLinks.forEmailSending();
        assertEquals("preview", utmLinks.getCampaignId());
        assertEquals("campaign", utmLinks.getUtmCampaign());
        assertEquals("email", utmLinks.getUtmSource());
        assertEquals("massmail", utmLinks.getUtmMedium());

        utmLinks = UtmLinks.forEmailTrigger("id");
        assertEquals("id", utmLinks.getCampaignId());
        assertEquals("id", utmLinks.getUtmCampaign());
        assertEquals("email", utmLinks.getUtmSource());
        assertEquals("trigger", utmLinks.getUtmMedium());
    }

    @Test
    public void testForTrigger() {
        UtmLinks utmLinks = UtmLinks.forEmailTrigger(
                "someId",
                HasUtmLinks.from(null, null, "  ", "   ", null));

        assertEquals("someId", utmLinks.getCampaignId());
        assertEquals("someId", utmLinks.getUtmCampaign());
        assertEquals("email", utmLinks.getUtmSource());
        assertEquals("trigger", utmLinks.getUtmMedium());
    }

    @Test
    public void testClidForEmailSendingLink() {
        String link = UtmLinks.forEmailSending()
                .updateUrl("https://market.yandex.ru/products/111");

        assertTrue("Clid parameter is either not set or has invalid value", link.contains("clid=619"));
    }

    @Test
    public void testDoNotSetUtmContentForNotYandexUrl() {
        var original = "https://ultrasport.ru/velosipedy/velosiped-stels-navigator-900-md-29-f010-2019/?from=ya" +
                "&variant=1405&model=175-serebristo-siniy&utm_source=market.yandex" +
                ".ru&utm_term=14601-MTcsNSAvINGB0LXRgNC10LHRgNC40YHRgtC\\LdGB0LjQvdC40Lk=";

        var processed = UtmLinks.forEmailSending().updateUrl(original);

        assertEquals(original, processed);
    }
}
