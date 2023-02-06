package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum;
import ru.yandex.direct.core.entity.banner.model.ModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.StatusModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.StatusModerateOperator;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum.ALL_PLACEMENTS_REJECTED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum.HAS_ACCEPTED_PLACEMENTS;
import static ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum.HAS_PLACEMENTS_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum.HAS_PLACEMENTS_ON_OPERATOR_ACTIVATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum.HAS_REJECTED_PLACEMENTS;

@RunWith(Parameterized.class)
public class StatesAdPagesModerationTest {
    private static final ModerateBannerPage yy = page(StatusModerateOperator.YES, StatusModerateBannerPage.YES);
    private static final ModerateBannerPage yn = page(StatusModerateOperator.YES, StatusModerateBannerPage.NO);
    private static final ModerateBannerPage ym = page(StatusModerateOperator.YES, StatusModerateBannerPage.MAYBE);
    private static final ModerateBannerPage yr = page(StatusModerateOperator.YES, StatusModerateBannerPage.READY);
    private static final ModerateBannerPage ys = page(StatusModerateOperator.YES, StatusModerateBannerPage.SENT);

    private static final ModerateBannerPage ny = page(StatusModerateOperator.NO, StatusModerateBannerPage.YES);
    private static final ModerateBannerPage nn = page(StatusModerateOperator.NO, StatusModerateBannerPage.NO);
    private static final ModerateBannerPage nm = page(StatusModerateOperator.NO, StatusModerateBannerPage.MAYBE);
    private static final ModerateBannerPage nr = page(StatusModerateOperator.NO, StatusModerateBannerPage.READY);
    private static final ModerateBannerPage ns = page(StatusModerateOperator.NO, StatusModerateBannerPage.SENT);

    private static final ModerateBannerPage noneY = page(StatusModerateOperator.NONE, StatusModerateBannerPage.YES);
    private static final ModerateBannerPage noneN = page(StatusModerateOperator.NONE, StatusModerateBannerPage.NO);
    private static final ModerateBannerPage noneM = page(StatusModerateOperator.NONE, StatusModerateBannerPage.MAYBE);
    private static final ModerateBannerPage noneR = page(StatusModerateOperator.NONE, StatusModerateBannerPage.READY);
    private static final ModerateBannerPage noneS = page(StatusModerateOperator.NONE, StatusModerateBannerPage.SENT);


    @Parameterized.Parameter
    public List<ModerateBannerPage> pages;

    @Parameterized.Parameter(1)
    public List<AdStatesEnum> expectedStates;

    @Parameterized.Parameters(name = "{index}: {1}")
    public static Object[][] params() {
        return new Object[][] {
                {List.of(yy), List.of(HAS_ACCEPTED_PLACEMENTS)},

                // при status_moderate - 'No','Ready','Sent','Maybe'
                // status_moderate_operator - не влияет на отображение статуса
                {List.of(noneN), List.of(ALL_PLACEMENTS_REJECTED)},
                {List.of(noneM), List.of(HAS_PLACEMENTS_ON_MODERATION)},
                {List.of(noneR), List.of(HAS_PLACEMENTS_ON_MODERATION)},
                {List.of(noneS), List.of(HAS_PLACEMENTS_ON_MODERATION)}, // [24]

                {List.of(yn), List.of(ALL_PLACEMENTS_REJECTED)},
                {List.of(ym), List.of(HAS_PLACEMENTS_ON_MODERATION)},
                {List.of(yr), List.of(HAS_PLACEMENTS_ON_MODERATION)},
                {List.of(ys), List.of(HAS_PLACEMENTS_ON_MODERATION)}, // [28]

                {List.of(nn), List.of(ALL_PLACEMENTS_REJECTED)},
                {List.of(nm), List.of(HAS_PLACEMENTS_ON_MODERATION)},
                {List.of(nr), List.of(HAS_PLACEMENTS_ON_MODERATION)},
                {List.of(ns), List.of(HAS_PLACEMENTS_ON_MODERATION)}, // 32


                // status_moderate - 'Yes'и status_moderate_operator 'No','None' - Активизация
                {List.of(ny), List.of(HAS_PLACEMENTS_ON_OPERATOR_ACTIVATION)},
                {List.of(noneY), List.of(HAS_PLACEMENTS_ON_OPERATOR_ACTIVATION)},

                // status_moderate - 'Yes'и status_moderate_operator 'Yes' - Принято
                {List.of(yy), List.of(HAS_ACCEPTED_PLACEMENTS)}, // 35

                {List.of(yy, nn), List.of(HAS_ACCEPTED_PLACEMENTS, HAS_REJECTED_PLACEMENTS)},
                {List.of(nn, nn), List.of(ALL_PLACEMENTS_REJECTED)},
                {List.of(yy, yy), List.of(HAS_ACCEPTED_PLACEMENTS)},
                {List.of(yy, nn, noneR), List.of(HAS_ACCEPTED_PLACEMENTS, HAS_REJECTED_PLACEMENTS,
                        HAS_PLACEMENTS_ON_MODERATION)},
                {List.of(noneR, nn, yy), List.of(HAS_ACCEPTED_PLACEMENTS, HAS_REJECTED_PLACEMENTS,
                        HAS_PLACEMENTS_ON_MODERATION)},
                {List.of(noneR, nn, nn, yy), List.of(HAS_ACCEPTED_PLACEMENTS, HAS_REJECTED_PLACEMENTS,
                        HAS_PLACEMENTS_ON_MODERATION)}, // [20]

        };
    }

    @Test
    public void test() {
        List<AdStatesEnum> gotStates = new AdStates().statesByPagesModeration(pages);
        assertThat("Got right set of states", gotStates, containsInAnyOrder(expectedStates.toArray()));
    }

    private static ModerateBannerPage page(StatusModerateOperator opsm, StatusModerateBannerPage sm) {
        return new ModerateBannerPage().withStatusModerateOperator(opsm).withStatusModerate(sm);
    }

}
