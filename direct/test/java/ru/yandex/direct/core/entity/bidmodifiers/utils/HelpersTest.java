package ru.yandex.direct.core.entity.bidmodifiers.utils;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;

import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getExternalId;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getRealId;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getRealType;

public class HelpersTest {
    @Test
    public void getRealIdTest() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(getRealId(103000L)).isEqualTo(3000L);
            softly.assertThat(getRealId(134567823L)).isEqualTo(4567823L);
            softly.assertThat(getRealId(23L)).isNull();
        });
    }

    @Test
    public void getRealTypeTest() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(getRealType(103000L)).isEqualTo(BidModifierType.MOBILE_MULTIPLIER);
            softly.assertThat(getRealType(113000L)).isEqualTo(BidModifierType.DEMOGRAPHY_MULTIPLIER);
            softly.assertThat(getRealType(123000L)).isEqualTo(BidModifierType.RETARGETING_MULTIPLIER);
            softly.assertThat(getRealType(133000L)).isEqualTo(BidModifierType.GEO_MULTIPLIER);
            softly.assertThat(getRealType(143000L)).isEqualTo(BidModifierType.VIDEO_MULTIPLIER);
            softly.assertThat(getRealType(783000L)).isNull();
            softly.assertThat(getRealType(12L)).isNull();
        });
    }

    @Test
    public void getExternalIdTest() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(getExternalId(3000L, BidModifierType.MOBILE_MULTIPLIER)).isEqualTo(103000L);
            softly.assertThat(getExternalId(4000L, BidModifierType.DEMOGRAPHY_MULTIPLIER))
                    .isEqualTo(114000L);
            softly.assertThat(getExternalId(3000L, BidModifierType.RETARGETING_MULTIPLIER))
                    .isEqualTo(123000L);
            softly.assertThat(getExternalId(4000L, BidModifierType.GEO_MULTIPLIER)).isEqualTo(134000L);
            softly.assertThat(getExternalId(4000L, BidModifierType.VIDEO_MULTIPLIER)).isEqualTo(144000L);
        });
    }
}
