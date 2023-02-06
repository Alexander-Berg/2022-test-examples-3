package ru.yandex.direct.api.v5.entity.ads.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdAddItem;
import com.yandex.direct.api.v5.ads.AdBuilderAdAddItem;
import com.yandex.direct.api.v5.ads.AddRequest;
import com.yandex.direct.api.v5.ads.CpcVideoAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.CpmBannerAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.DynamicTextAdAdd;
import com.yandex.direct.api.v5.ads.MobileAppAdAdd;
import com.yandex.direct.api.v5.ads.MobileAppAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.MobileAppCpcVideoAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.MobileAppImageAdAdd;
import com.yandex.direct.api.v5.ads.SmartAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.TextAdAdd;
import com.yandex.direct.api.v5.ads.TextAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.TextImageAdAdd;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.model.TextBanner;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledCpcVideoAdBuilderAd;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledCpmBannerAdBuilderAd;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledDynamicAd;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledMobileAppAd;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledMobileAppAdBuilderAd;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledMobileAppCpcVideoAdBuilderAd;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledMobileAppImageAd;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledSmartAdBuilderAdAdd;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledTextAd;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledTextAdBuilderAd;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledTextImageAd;

@ParametersAreNonnullByDefault
public class AdsAddRequestConverterTest {

    private static final long ADGROUP_ID = 8L;

    private final AdsAddRequestConverter converter = new AdsAddRequestConverter();

    @Test
    public void convert_convertsAllTypes() {
        AddRequest request = new AddRequest().withAds(
                new AdAddItem().withTextAd(filledTextAd()),
                new AdAddItem().withDynamicTextAd(filledDynamicAd()),
                new AdAddItem().withTextImageAd(filledTextImageAd()),
                new AdAddItem().withTextAdBuilderAd(filledTextAdBuilderAd()),
                new AdAddItem().withMobileAppAd(filledMobileAppAd()),
                new AdAddItem().withMobileAppImageAd(filledMobileAppImageAd()),
                new AdAddItem().withMobileAppCpcVideoAdBuilderAd(filledMobileAppCpcVideoAdBuilderAd()),
                new AdAddItem().withMobileAppAdBuilderAd(filledMobileAppAdBuilderAd()),
                new AdAddItem().withCpmBannerAdBuilderAd(filledCpmBannerAdBuilderAd()),
                new AdAddItem().withCpcVideoAdBuilderAd(filledCpcVideoAdBuilderAd()),
                new AdAddItem().withSmartAdBuilderAd(filledSmartAdBuilderAdAdd()));

        var result = converter.convert(request);

        assertThat(result).doesNotContainNull();
        assertThat(result).hasSameSizeAs(request.getAds());
    }

    @Test
    public void convert_EmptyItems_DoesntFail() {
        AddRequest request = new AddRequest().withAds(
                new AdAddItem().withTextAd(new TextAdAdd()),
                new AdAddItem().withDynamicTextAd(new DynamicTextAdAdd()),
                new AdAddItem().withTextImageAd(new TextImageAdAdd()),
                new AdAddItem().withTextAdBuilderAd(new TextAdBuilderAdAdd().withCreative(new AdBuilderAdAddItem())),
                new AdAddItem().withMobileAppAd(new MobileAppAdAdd()),
                new AdAddItem().withMobileAppImageAd(new MobileAppImageAdAdd()),
                new AdAddItem().withMobileAppCpcVideoAdBuilderAd(new MobileAppCpcVideoAdBuilderAdAdd()
                        .withCreative(new AdBuilderAdAddItem())),
                new AdAddItem().withMobileAppAdBuilderAd(new MobileAppAdBuilderAdAdd().withCreative(new AdBuilderAdAddItem())),
                new AdAddItem().withCpmBannerAdBuilderAd(new CpmBannerAdBuilderAdAdd().withCreative(new AdBuilderAdAddItem())),
                new AdAddItem().withCpcVideoAdBuilderAd(new CpcVideoAdBuilderAdAdd().withCreative(new AdBuilderAdAddItem())),
                new AdAddItem().withSmartAdBuilderAd(new SmartAdBuilderAdAdd().withCreative(new AdBuilderAdAddItem())));

        var result = converter.convert(request);

        assertThat(result).doesNotContainNull();
        assertThat(result).hasSameSizeAs(request.getAds());
    }

    @Test
    public void convert_EmptyItemWithAdGroupId_FillsAdGroupId() {
        var actual = converter.convert(new AdAddItem().withTextAd(new TextAdAdd()).withAdGroupId(ADGROUP_ID));

        assertThat(actual.getAdGroupId()).isEqualTo(ADGROUP_ID);
    }

    @Test
    public void convert_SingleTextAd_CheckFields() {
        TextAdAdd textAd = filledTextAd();
        var result = (TextBanner) converter.convert(new AdAddItem()
                .withAdGroupId(ADGROUP_ID)
                .withTextAd(textAd));

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getAdGroupId()).isNotNull();
        softly.assertThat(result.getVcardId()).isNotNull();
        softly.assertThat(result.getSitelinksSetId()).isNotNull();

        softly.assertThat(result.getImageHash()).isNotNull();

        softly.assertThat(result.getCalloutIds()).isNotNull();
        softly.assertThat(result.getIsMobile()).isNotNull();
        softly.assertThat(result.getTitle()).isNotNull();
        softly.assertThat(result.getTitleExtension()).isNotNull();
        softly.assertThat(result.getBody()).isNotNull();
        softly.assertThat(result.getHref()).isNotNull();

        softly.assertThat(result.getBannerPrice()).isNotNull();
        softly.assertThat(result.getBannerPrice().getPrice()).isNotNull();
        softly.assertThat(result.getBannerPrice().getPriceOld()).isNotNull();
        softly.assertThat(result.getBannerPrice().getPrefix()).isNotNull();
        softly.assertThat(result.getBannerPrice().getCurrency()).isNotNull();

        softly.assertThat(result.getPermalinkId()).isNotNull();
        softly.assertThat(result.getPreferVCardOverPermalink()).isNotNull();

        softly.assertAll();
    }

    @Test
    public void convert_SingleDynamicAd_CheckFields() {
        DynamicTextAdAdd dynamicTextAd = filledDynamicAd();
        var result = (DynamicBanner) converter.convert(new AdAddItem().withAdGroupId(ADGROUP_ID)
                .withDynamicTextAd(dynamicTextAd));

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getAdGroupId()).isNotNull();
        softly.assertThat(result.getVcardId()).isNotNull();
        softly.assertThat(result.getSitelinksSetId()).isNotNull();

        softly.assertThat(result.getImageHash()).isNotNull();

        softly.assertThat(result.getCalloutIds()).isNotNull();
        softly.assertThat(result.getBody()).isNotNull();

        // nulls
        softly.assertThat(result.getHref()).isNull();

        softly.assertAll();
    }

    @Test
    public void convert_SingleImageAd_CheckFields() {
        TextImageAdAdd textImageAd = filledTextImageAd();
        var result = (ImageBanner) converter.convert(new AdAddItem().withAdGroupId(ADGROUP_ID)
                .withTextImageAd(textImageAd));

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getAdGroupId()).isNotNull();

        softly.assertThat(result.getImageHash()).isNotNull();

        softly.assertThat(result.getHref()).isNotNull();

        softly.assertAll();
    }

    @Test
    public void convert_SingleMobileAppAd_CheckFields() {
        MobileAppAdAdd mobileAppAd = filledMobileAppAd();
        var result = (MobileAppBanner) converter.convert(
                new AdAddItem().withAdGroupId(ADGROUP_ID).withMobileAppAd(mobileAppAd));

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getAdGroupId()).isNotNull();

        softly.assertThat(result.getImageHash()).isNotNull();

        softly.assertThat(result.getTitle()).isNotNull();
        softly.assertThat(result.getBody()).isNotNull();
        softly.assertThat(result.getHref()).isNotNull();
        softly.assertThat(result.getImpressionUrl()).isNotNull();
        softly.assertThat(result.getPrimaryAction()).isNotNull();
        softly.assertThat(result.getReflectedAttributes()).isNotNull();
        softly.assertThat(result.getFlags()).isNotNull();

        softly.assertAll();
    }

    @Test
    public void convert_SingleMobileAppAd_NullImpressionUrl_CheckFields() {
        MobileAppAdAdd mobileAppAd = filledMobileAppAd().withImpressionUrl(null);
        var result = (MobileAppBanner) converter.convert(
                new AdAddItem().withAdGroupId(ADGROUP_ID).withMobileAppAd(mobileAppAd));

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getAdGroupId()).isNotNull();

        softly.assertThat(result.getImageHash()).isNotNull();

        softly.assertThat(result.getTitle()).isNotNull();
        softly.assertThat(result.getBody()).isNotNull();
        softly.assertThat(result.getHref()).isNotNull();
        softly.assertThat(result.getImpressionUrl()).isNull();
        softly.assertThat(result.getPrimaryAction()).isNotNull();
        softly.assertThat(result.getReflectedAttributes()).isNotNull();
        softly.assertThat(result.getFlags()).isNotNull();

        softly.assertAll();
    }

    @Test
    public void convert_SingleMobileAppCpcVideoAd_CheckFields() {
        MobileAppCpcVideoAdBuilderAdAdd mobileAppCpcVideoAdBuilderAd = filledMobileAppCpcVideoAdBuilderAd();
        var result = (CpcVideoBanner) converter.convert(new AdAddItem()
                .withAdGroupId(ADGROUP_ID)
                .withMobileAppCpcVideoAdBuilderAd(mobileAppCpcVideoAdBuilderAd));

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getAdGroupId()).isNotNull();

        softly.assertThat(result.getHref()).isNotNull();
        softly.assertThat(result.getCreativeId()).isNotNull();

        softly.assertAll();
    }

    @Test
    public void convert_SingleCpcVideoAd_CheckFields() {
        CpcVideoAdBuilderAdAdd cpcVideoAdBuilderAd = filledCpcVideoAdBuilderAd();
        var result = (CpcVideoBanner) converter.convert(
                new AdAddItem().withAdGroupId(ADGROUP_ID).withCpcVideoAdBuilderAd(cpcVideoAdBuilderAd));

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.getAdGroupId()).isNotNull();

        softly.assertThat(result.getHref()).isNotNull();
        softly.assertThat(result.getCreativeId()).isNotNull();

        softly.assertAll();
    }

    @Test
    public void convert_SingleSmartAd_CheckFields() {
        SmartAdBuilderAdAdd smartAdBuilderAd = filledSmartAdBuilderAdAdd();
        var result = (PerformanceBanner) converter.convert(
                new AdAddItem().withAdGroupId(ADGROUP_ID).withSmartAdBuilderAd(smartAdBuilderAd));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result.getAdGroupId()).isNotNull();
        softly.assertThat(result.getCreativeId()).isNotNull();
        softly.assertAll();
    }

    @Test
    public void convert_SingleSmartAd_NoCreative() {
        SmartAdBuilderAdAdd smartAdBuilderAd = new SmartAdBuilderAdAdd().withCreative(null);
        var result = converter.convert(
                new AdAddItem().withAdGroupId(ADGROUP_ID).withSmartAdBuilderAd(smartAdBuilderAd));

        assertThat(result).isInstanceOf(PerformanceBannerMain.class);
    }

    @Test
    public void convert_ImageAds_AllSubTypesAreSet() {
        converter.convert(new AddRequest().withAds(
                new AdAddItem().withTextImageAd(filledTextImageAd()),
                new AdAddItem().withTextAdBuilderAd(filledTextAdBuilderAd()),
                new AdAddItem().withMobileAppImageAd(filledMobileAppImageAd()),
                new AdAddItem().withMobileAppAdBuilderAd(filledMobileAppAdBuilderAd())))
                .stream()
                .map(ImageBanner.class::cast)
                .forEach(banner -> assertThat(banner.getIsMobileImage()).isNotNull());
    }

    @Test
    public void convert_TextAd_VideoExtIsNull_CreativeIdIsNull() {
        var banner = (TextBanner) converter.convert(new AdAddItem().withTextAd(
                filledTextAd().withVideoExtension(null)));
        assert banner != null;
        assertThat(banner.getCreativeId()).isNull();
    }

    @Test
    public void convert_MobileAppAd_VideoExtIsNull_CreativeIdIsNull() {
        var banner = (MobileAppBanner) converter.convert(new AdAddItem().withMobileAppAd(
                filledMobileAppAd().withVideoExtension(null)));
        assert banner != null;
        assertThat(banner.getCreativeId()).isNull();
    }

    @Test
    public void convert_CpmAd_TnsIsNull() {
        var cpmAd = filledCpmBannerAdBuilderAd();
        var banner = (CpmBanner) converter.convert(new AdAddItem().withCpmBannerAdBuilderAd(cpmAd));

        assertThat(banner).isNotNull();

        assertThat(banner.getTnsId()).isNull();
    }

    @Test
    public void convert_CpmAd_TnsIsNotNull() {
        var cpmAd = filledCpmBannerAdBuilderAd().withTnsId("SomeValidTnsId");
        var banner = (CpmBanner) converter.convert(new AdAddItem().withCpmBannerAdBuilderAd(cpmAd));

        assertThat(banner).isNotNull();

        assertThat(banner.getTnsId())
                .isNotNull()
                .isNotBlank()
                .isEqualTo("SomeValidTnsId");
    }

}
