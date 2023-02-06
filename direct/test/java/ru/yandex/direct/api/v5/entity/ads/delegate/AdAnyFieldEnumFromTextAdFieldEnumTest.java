package ru.yandex.direct.api.v5.entity.ads.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.TextAdFieldEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.delegate.AdAnyFieldEnum.fromTextAdFieldEnum;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AdAnyFieldEnumFromTextAdFieldEnumTest {

    @Parameterized.Parameter
    public TextAdFieldEnum textAdFieldEnum;

    @Parameterized.Parameter(1)
    public AdAnyFieldEnum adAnyFieldEnum;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {TextAdFieldEnum.AD_IMAGE_HASH, AdAnyFieldEnum.TEXT_AD_IMAGE_HASH},
                {TextAdFieldEnum.DISPLAY_DOMAIN, AdAnyFieldEnum.TEXT_AD_DISPLAY_DOMAIN},
                {TextAdFieldEnum.HREF, AdAnyFieldEnum.TEXT_AD_HREF},
                {TextAdFieldEnum.SITELINK_SET_ID, AdAnyFieldEnum.TEXT_AD_SITELINK_SET_ID},
                {TextAdFieldEnum.TEXT, AdAnyFieldEnum.TEXT_AD_TEXT},
                {TextAdFieldEnum.TITLE, AdAnyFieldEnum.TEXT_AD_TITLE},
                {TextAdFieldEnum.TITLE_2, AdAnyFieldEnum.TEXT_AD_TITLE_2},
                {TextAdFieldEnum.MOBILE, AdAnyFieldEnum.TEXT_AD_MOBILE},
                {TextAdFieldEnum.V_CARD_ID, AdAnyFieldEnum.TEXT_AD_V_CARD_ID},
                {TextAdFieldEnum.DISPLAY_URL_PATH, AdAnyFieldEnum.TEXT_AD_DISPLAY_URL_PATH},
                {TextAdFieldEnum.DUT_PREFIX, AdAnyFieldEnum.TEXT_AD_DUT_PREFIX},
                {TextAdFieldEnum.DUT_SUFFIX, AdAnyFieldEnum.TEXT_AD_DUT_SUFFIX},
                {TextAdFieldEnum.AD_IMAGE_MODERATION, AdAnyFieldEnum.TEXT_AD_IMAGE_MODERATION},
                {TextAdFieldEnum.SITELINKS_MODERATION, AdAnyFieldEnum.TEXT_AD_SITELINKS_MODERATION},
                {TextAdFieldEnum.V_CARD_MODERATION, AdAnyFieldEnum.TEXT_AD_V_CARD_MODERATION},
                {TextAdFieldEnum.AD_EXTENSIONS, AdAnyFieldEnum.TEXT_AD_EXTENSIONS},
                {TextAdFieldEnum.DISPLAY_URL_PATH_MODERATION, AdAnyFieldEnum.TEXT_AD_DISPLAY_URL_PATH_MODERATION},
                {TextAdFieldEnum.VIDEO_EXTENSION, AdAnyFieldEnum.TEXT_AD_VIDEO_EXTENSION},
                {TextAdFieldEnum.TRACKING_PHONE_ID, AdAnyFieldEnum.TEXT_AD_TRACKING_PHONE_ID},
                {TextAdFieldEnum.TURBO_PAGE_ID, AdAnyFieldEnum.TEXT_AD_TURBO_PAGE_ID},
                {TextAdFieldEnum.TURBO_PAGE_MODERATION, AdAnyFieldEnum.TEXT_AD_TURBO_PAGE_MODERATION},
                {TextAdFieldEnum.LF_HREF, AdAnyFieldEnum.TEXT_AD_LF_HREF},
                {TextAdFieldEnum.LF_BUTTON_TEXT, AdAnyFieldEnum.TEXT_AD_LF_BUTTON_TEXT}
        };
    }

    @Test
    public void test() {
        assertThat(fromTextAdFieldEnum(textAdFieldEnum)).isSameAs(adAnyFieldEnum);
    }
}
