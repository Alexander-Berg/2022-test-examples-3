package ru.yandex.direct.core.entity.banner.service.internal;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.BannerWithVcard;
import ru.yandex.direct.core.entity.banner.model.TextBanner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static ru.yandex.direct.core.entity.banner.type.vcard.BannerWithVcardUtils.hasCommonVcard;

@ParametersAreNonnullByDefault
public class BannersHasCommonVcard {
    @Test
    public void twoBannersWithSameVcardsIds() {
        List<BannerWithVcard> banners = List.of(new TextBanner().withVcardId(1L),
                new TextBanner().withVcardId(1L));
        boolean result = hasCommonVcard(banners);
        assertThat(result).isTrue();
    }

    @Test
    public void twoBannersWithDifferentVcardsIds() {
        List<BannerWithVcard> banners = List.of(new TextBanner().withVcardId(1L),
                new TextBanner().withVcardId(2L));
        boolean result = hasCommonVcard(banners);
        assertThat(result).isFalse();
    }

    @Test
    public void twoBannersOneWithVcardIdOneWithout() {
        List<BannerWithVcard> banners = List.of(new TextBanner().withVcardId(1L), new TextBanner());
        boolean result = hasCommonVcard(banners);
        assertThat(result).isFalse();
    }

    @Test
    public void twoBannersWithoutVcardId() {
        List<BannerWithVcard> banners = List.of(new TextBanner(), new TextBanner());
        boolean result = hasCommonVcard(banners);
        assertThat(result).isFalse();
    }
}
