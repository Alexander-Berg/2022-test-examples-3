package ru.yandex.direct.web.entity.adgroup.converter;

import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesAdgroupType;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroup;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AdGroupConverterUtilsTest {

    @Test
    public void extractCpmAdGroupType_CpmDealsCampaign_CpmBannerAdGroup() {
        WebCpmAdGroup webCpmAdGroup = new WebCpmAdGroup();
        AdGroupType adGroupType = AdGroupConverterUtils.extractCpmAdGroupType(webCpmAdGroup, CampaignType.CPM_DEALS);
        assertThat(adGroupType, is(AdGroupType.CPM_BANNER));
    }

    @Test
    public void extractCpmAdGroupType_CpmYndxFrontpageCampaign_CpmBannerAdGroup() {
        WebCpmAdGroup webCpmAdGroup = new WebCpmAdGroup();
        AdGroupType adGroupType =
                AdGroupConverterUtils.extractCpmAdGroupType(webCpmAdGroup, CampaignType.CPM_YNDX_FRONTPAGE);
        assertThat(adGroupType, is(AdGroupType.CPM_YNDX_FRONTPAGE));
    }

    @Test
    public void extractCpmAdGroupType_CpmBannerWebAdGroup_CpmBannerAdGroup() {
        WebCpmAdGroup webCpmAdGroup = new WebCpmAdGroup()
                .withCpmBannersType(PhrasesAdgroupType.cpm_banner.getLiteral());
        AdGroupType adGroupType = AdGroupConverterUtils.extractCpmAdGroupType(webCpmAdGroup, CampaignType.CPM_BANNER);
        assertThat(adGroupType, is(AdGroupType.CPM_BANNER));
    }

    @Test
    public void extractCpmAdGroupType_CpmVideoWebAdGroup_CpmVideoAdGroup() {
        WebCpmAdGroup webCpmAdGroup = new WebCpmAdGroup()
                .withCpmBannersType(PhrasesAdgroupType.cpm_video.getLiteral());
        AdGroupType adGroupType = AdGroupConverterUtils.extractCpmAdGroupType(webCpmAdGroup, CampaignType.CPM_BANNER);
        assertThat(adGroupType, is(AdGroupType.CPM_VIDEO));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void extractCpmAdGroupType_WebAdGroupWithoutType_Exception() {
        WebCpmAdGroup webCpmAdGroup = new WebCpmAdGroup();
        AdGroupConverterUtils.extractCpmAdGroupType(webCpmAdGroup, CampaignType.CPM_BANNER);
    }

}
