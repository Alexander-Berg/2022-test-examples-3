package ru.yandex.direct.api.v5.entity.ads.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.general.StateEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.campaign.model.Campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.StateAndStatusCalculator.calcState;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class CalcStateTest {

    @Parameterized.Parameter
    public BannerWithSystemFields ad;

    @Parameterized.Parameter(1)
    public Boolean isStoppedByMonitoring;

    @Parameterized.Parameter(2)
    public Campaign campaign;

    @Parameterized.Parameter(3)
    public StateEnum expectedState;

    @Parameterized.Parameters(name = "state {2}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {new TextBanner().withStatusArchived(true), false, new Campaign(), StateEnum.ARCHIVED},
                {new TextBanner().withStatusArchived(false), false, new Campaign().withStatusArchived(true),
                        StateEnum.ARCHIVED},
                {new TextBanner().withStatusArchived(false).withStatusShow(false), false,
                        new Campaign().withStatusArchived(false), StateEnum.SUSPENDED},
                {new TextBanner().withStatusArchived(false).withStatusShow(true), true,
                        new Campaign().withStatusArchived(false), StateEnum.OFF_BY_MONITORING},
                {new TextBanner().withStatusActive(true).withStatusArchived(false).withStatusShow(true), false,
                        new Campaign().withStatusActive(true).withStatusArchived(false).withStatusShow(true),
                        StateEnum.ON},
                {new TextBanner().withStatusArchived(false).withStatusActive(true).withStatusShow(true), false,
                        new Campaign().withStatusArchived(false).withStatusActive(true).withStatusShow(false),
                        StateEnum.OFF},
                {new TextBanner().withStatusArchived(false).withStatusActive(false).withStatusShow(true), false,
                        new Campaign().withStatusArchived(false).withStatusActive(true).withStatusShow(false),
                        StateEnum.OFF},
                {new TextBanner().withStatusArchived(false).withStatusActive(true).withStatusShow(true), false,
                        new Campaign().withStatusArchived(false).withStatusActive(false).withStatusShow(false),
                        StateEnum.OFF},
        };
    }

    @Test
    public void test() {
        StateEnum actualState = calcState(ad, isStoppedByMonitoring, campaign);
        assertThat(actualState).isEqualByComparingTo(expectedState);
    }

}
