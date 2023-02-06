package ru.yandex.direct.core.entity.adgroup.service.complex.cpm;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(Parameterized.class)
public class PriceBidModifierPlatformChoosePlatformTest {
    @Autowired
    private PriceBidModifierPlatformChooserImpl priceBidModifierPlatformChooser;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public Set<PlatformState> platformsFromPackage;

    @Parameterized.Parameter(1)
    public Set<PlatformState> disabledPlatforms;


    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                        {
                                Set.<PlatformState>of(),
                                Set.<PlatformState>of()
                        },
                        {
                                Set.of(PlatformState.DESKTOP),
                                Set.of(PlatformState.ALL_MOBILE)
                        },
                        {
                                Set.of(PlatformState.ANDROID),
                                Set.of(PlatformState.IOS, PlatformState.DESKTOP)
                        },
                        {
                                Set.of(PlatformState.ANDROID, PlatformState.DESKTOP),
                                Set.of(PlatformState.IOS)
                        },
                        {
                                Set.of(PlatformState.IOS),
                                Set.of(PlatformState.ANDROID, PlatformState.DESKTOP)
                        },
                        {
                                Set.of(PlatformState.IOS, PlatformState.DESKTOP),
                                Set.of(PlatformState.ANDROID)
                        },
                        {
                                Set.of(PlatformState.ALL_MOBILE),
                                Set.of(PlatformState.DESKTOP)
                        },
                        {
                                Set.of(PlatformState.ALL_MOBILE, PlatformState.DESKTOP),
                                Set.<PlatformState>of()
                        },
                }
        );
    }

    @Test
    public void disableModifiersTest() {
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(new CpmBannerAdGroup().withId(12341L).withCampaignId(8755L));

        List<BidModifier> result = priceBidModifierPlatformChooser.choosePlatforms(platformsFromPackage,
                    complexCpmAdGroup);

        assertThat(result).hasSize(disabledPlatforms.size());

        for (var pl : disabledPlatforms) {
            switch (pl) {
                case ALL_MOBILE:
                    assertThat(result.stream()
                            .anyMatch(e -> e.getType() == BidModifierType.MOBILE_MULTIPLIER
                                    && ((BidModifierMobile) e).getMobileAdjustment().getOsType() == null
                                    && ((BidModifierMobile) e).getMobileAdjustment().getPercent() == 0
                            )).isTrue();
                    break;
                case ANDROID:
                    assertThat(result.stream()
                            .anyMatch(e -> e.getType() == BidModifierType.MOBILE_MULTIPLIER
                                    && ((BidModifierMobile) e).getMobileAdjustment().getOsType() == OsType.ANDROID
                                    && ((BidModifierMobile) e).getMobileAdjustment().getPercent() == 0
                            )).isTrue();
                    break;
                case IOS:
                    assertThat(result.stream()
                            .anyMatch(e -> e.getType() == BidModifierType.MOBILE_MULTIPLIER
                                    && ((BidModifierMobile) e).getMobileAdjustment().getOsType() == OsType.IOS
                                    && ((BidModifierMobile) e).getMobileAdjustment().getPercent() == 0
                            )).isTrue();
                    break;
                case DESKTOP:
                    assertThat(result.stream()
                            .anyMatch(e -> e.getType() == BidModifierType.DESKTOP_MULTIPLIER
                                    && ((BidModifierDesktop) e).getDesktopAdjustment() != null
                                    && ((BidModifierDesktop) e).getDesktopAdjustment().getPercent() == 0
                            )).isTrue();
                    break;
            }
        }
    }
}
