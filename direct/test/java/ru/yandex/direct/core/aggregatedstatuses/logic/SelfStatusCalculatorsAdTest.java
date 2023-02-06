package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.ARCHIVED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.DRAFT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_PROCESSING;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_CRIT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.AD_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.OFF_BY_MONITORING;

@RunWith(Parameterized.class)
public class SelfStatusCalculatorsAdTest {
    @Parameterized.Parameter()
    public Collection<AdStatesEnum> states;

    @Parameterized.Parameter(1)
    public GdSelfStatusEnum status;

    @Parameterized.Parameter(2)
    public List<GdSelfStatusReason> reasons;

    @Parameterized.Parameters(name = "states = {0} => status: {1}, reason: {2}")
    public static Object[][] params() {
        return new Object[][]{
                {List.of(AdStatesEnum.ARCHIVED),
                        ARCHIVED, null},

                {List.of(AdStatesEnum.DRAFT),
                        DRAFT, null},

                {List.of(AdStatesEnum.SUSPENDED),
                        STOP_OK, List.of(GdSelfStatusReason.AD_SUSPENDED_BY_USER)},

                {List.of(AdStatesEnum.SUSPENDED_BY_MONITORING),
                        STOP_OK, List.of(GdSelfStatusReason.OFF_BY_MONITORING)},

                {List.of(AdStatesEnum.REJECTED),
                        STOP_CRIT, List.of(GdSelfStatusReason.REJECTED_ON_MODERATION)},

                {List.of(AdStatesEnum.REJECTED, AdStatesEnum.ACTIVE_IN_BS),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_REJECTED_BUT_PREVIOUS_VERSION_SHOWN)},

                {List.of(AdStatesEnum.REJECTED, AdStatesEnum.MODERATION),
                        STOP_CRIT, List.of(AD_ON_MODERATION)},

                {List.of(AdStatesEnum.SUSPENDED, AdStatesEnum.REJECTED, AdStatesEnum.MODERATION),
                        STOP_CRIT, List.of(AD_ON_MODERATION)},

                {List.of(AdStatesEnum.SUSPENDED, AdStatesEnum.MODERATION),
                        STOP_OK, List.of(AD_ON_MODERATION)},

                {List.of(AdStatesEnum.SUSPENDED_BY_MONITORING, AdStatesEnum.MODERATION),
                        STOP_OK, List.of(OFF_BY_MONITORING, AD_ON_MODERATION)},

                {List.of(AdStatesEnum.ALL_PLACEMENTS_REJECTED),
                        STOP_CRIT, List.of(GdSelfStatusReason.REJECTED_ON_MODERATION)},

                {List.of(AdStatesEnum.ALL_PLACEMENTS_REJECTED, AdStatesEnum.ACTIVE_IN_BS),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_REJECTED_BUT_PREVIOUS_VERSION_SHOWN)},

                {List.of(AdStatesEnum.HAS_PLACEMENTS_ON_MODERATION),
                        DRAFT, List.of(GdSelfStatusReason.AD_HAS_PLACEMENTS_ON_OPERATOR_MODERATION)},

                {List.of(AdStatesEnum.HAS_PLACEMENTS_ON_MODERATION, AdStatesEnum.ACTIVE_IN_BS),
                        RUN_OK, List.of(GdSelfStatusReason.AD_ON_MODERATION_PREVIOUS_VERSION_SHOWN)},

                {List.of(AdStatesEnum.HAS_PLACEMENTS_ON_MODERATION, AdStatesEnum.HAS_ACCEPTED_PLACEMENTS),
                        RUN_PROCESSING, null},

                {List.of(AdStatesEnum.HAS_PLACEMENTS_ON_MODERATION, AdStatesEnum.HAS_ACCEPTED_PLACEMENTS,
                        AdStatesEnum.ACTIVE_IN_BS),
                        RUN_OK, null},

                {List.of(AdStatesEnum.MODERATION),
                        ON_MODERATION, List.of(AD_ON_MODERATION)},

                {List.of(AdStatesEnum.MODERATION, AdStatesEnum.DRAFT),
                        ON_MODERATION, List.of(AD_ON_MODERATION)},

                {List.of(AdStatesEnum.MODERATION, AdStatesEnum.ACTIVE_IN_BS),
                        RUN_OK, List.of(GdSelfStatusReason.AD_ON_MODERATION_PREVIOUS_VERSION_SHOWN)},

                {List.of(AdStatesEnum.PLACEMENTS_REQUIRED),
                        STOP_CRIT, List.of(GdSelfStatusReason.AD_NO_SUITABLE_PLACEMENTS_SELECTED)},

                {List.of(AdStatesEnum.HAS_REJECTED_PLACEMENTS),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_HAS_REJECTED_PLACEMENTS)},

                {List.of(AdStatesEnum.REJECTED_VCARD),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_VCARD_REJECTED_ON_MODERATION)},

                {List.of(AdStatesEnum.REJECTED_VIDEO_ADDITION),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_VIDEO_ADDITION_REJECTED_ON_MODERATION)},

                {List.of(AdStatesEnum.HAS_REJECTED_CALLOUTS),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_HAS_REJECTED_CALLOUTS)},

                {List.of(AdStatesEnum.HAS_REJECTED_SITELINKS),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_HAS_REJECTED_SITELINKS)},

                {List.of(AdStatesEnum.REJECTED_LOGO),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_LOGO_REJECTED_MODERATION)},

                {List.of(AdStatesEnum.REJECTED_BUTTON),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_BUTTON_REJECTED_MODERATION)},

                {List.of(AdStatesEnum.REJECTED_MULTICARD_SET),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_MULTICARD_SET_REJECTRED_MODERATION)},

                {List.of(AdStatesEnum.REJECTED_IMAGE),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_IMAGE_REJECTED_ON_MODERATION)},

                {List.of(AdStatesEnum.REJECTED_DISPLAY_HREF),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_DISPLAY_HREF_REJECTED_ON_MODERATION)},

                {List.of(AdStatesEnum.REJECTED_TURBOLANDING),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_TURBOLANDING_REJECTED_ON_MODERATION)},

                {List.of(AdStatesEnum.HAS_REJECTED_CALLOUTS, AdStatesEnum.HAS_PLACEMENTS_ON_OPERATOR_ACTIVATION),
                        RUN_WARN,
                        List.of(GdSelfStatusReason.AD_HAS_REJECTED_CALLOUTS,
                                GdSelfStatusReason.AD_HAS_PLACEMENTS_ON_OPERATOR_ACTIVATION)},

                {List.of(AdStatesEnum.HAS_ASOCIAL_FLAG, AdStatesEnum.ACTIVE_IN_BS),
                        RUN_OK,
                        null},

                {List.of(AdStatesEnum.REJECTED_LOGO, AdStatesEnum.REJECTED_BUTTON),
                        RUN_WARN,
                        List.of(GdSelfStatusReason.AD_LOGO_REJECTED_MODERATION,
                                GdSelfStatusReason.AD_BUTTON_REJECTED_MODERATION)},

                {List.of(AdStatesEnum.ALL_PLACEMENTS_REJECTED, AdStatesEnum.ACTIVE_IN_BS,
                        AdStatesEnum.HAS_PLACEMENTS_ON_OPERATOR_ACTIVATION, AdStatesEnum.REJECTED_LOGO),
                        RUN_WARN,
                        List.of(GdSelfStatusReason.AD_REJECTED_BUT_PREVIOUS_VERSION_SHOWN,
                                GdSelfStatusReason.AD_HAS_PLACEMENTS_ON_OPERATOR_ACTIVATION,
                                GdSelfStatusReason.AD_LOGO_REJECTED_MODERATION)},

                // On Moderation (only for RUN_OK)
                {List.of(AdStatesEnum.VCARD_ON_MODERATION),
                        RUN_OK, List.of(GdSelfStatusReason.AD_VCARD_ON_MODERATION)},

                {List.of(AdStatesEnum.VIDEO_ADDITION_ON_MODERATION),
                        RUN_OK, List.of(GdSelfStatusReason.AD_VIDEO_ADDITION_ON_MODERATION)},

                {List.of(AdStatesEnum.CALLOUTS_ON_MODERATION),
                        RUN_OK, List.of(GdSelfStatusReason.AD_CALLOUTS_ON_MODERATION)},

                {List.of(AdStatesEnum.SITELINKS_ON_MODERATION),
                        RUN_OK, List.of(GdSelfStatusReason.AD_SITELINKS_ON_MODERATION)},

                {List.of(AdStatesEnum.LOGO_ON_MODERATION),
                        RUN_OK, List.of(GdSelfStatusReason.AD_LOGO_ON_MODERATION)},

                {List.of(AdStatesEnum.BUTTON_ON_MODERATION),
                        RUN_OK, List.of(GdSelfStatusReason.AD_BUTTON_ON_MODERATION)},

                {List.of(AdStatesEnum.MULTICARD_SET_ON_MODERATION),
                        RUN_OK, List.of(GdSelfStatusReason.AD_MULTICARD_SET_ON_MODERATION)},

                {List.of(AdStatesEnum.IMAGE_ON_MODERATION),
                        RUN_OK, List.of(GdSelfStatusReason.AD_IMAGE_ON_MODERATION)},

                {List.of(AdStatesEnum.DISPLAY_HREF_ON_MODERATION),
                        RUN_OK, List.of(GdSelfStatusReason.AD_DISPLAY_HREF_ON_MODERATION)},

                {List.of(AdStatesEnum.TURBOLANDING_ON_MODERATION),
                        RUN_OK, List.of(GdSelfStatusReason.AD_TURBOLANDING_ON_MODERATION)},

                {List.of(AdStatesEnum.TURBOLANDING_ON_MODERATION, AdStatesEnum.DISPLAY_HREF_ON_MODERATION),
                        RUN_OK, List.of(GdSelfStatusReason.AD_TURBOLANDING_ON_MODERATION,
                        GdSelfStatusReason.AD_DISPLAY_HREF_ON_MODERATION)},
                // End of On moderation

                {List.of(AdStatesEnum.ACTIVE_IN_BS),
                        RUN_OK, null},

                {List.of(AdStatesEnum.READY_TO_BS),
                        RUN_PROCESSING, List.of(GdSelfStatusReason.AD_SENT_OR_READY_TO_BS)},

                {List.of(),
                        RUN_PROCESSING, null},

                {List.of(AdStatesEnum.HAS_TRAGIC_FLAG, AdStatesEnum.ACTIVE_IN_BS),
                        RUN_OK,
                        null},

                {List.of(AdStatesEnum.HAS_UNFAMILY_FLAG, AdStatesEnum.HAS_ASOCIAL_FLAG,
                        AdStatesEnum.ACTIVE_IN_BS),
                        RUN_OK,
                        null},
        };
    }

    @Test
    public void test() {
        var calculatedSelfStatus = SelfStatusCalculators.calcAdSelfStatus(states, Collections.emptyMap());

        assertEquals("Status ok", status, calculatedSelfStatus.getStatus());

        if (reasons == null) {
            assertThat("Reason is null", calculatedSelfStatus.getReasons(), Matchers.nullValue());
        } else {
            assertThat("Reason count ok", calculatedSelfStatus.getReasons(), hasSize(reasons.size()));
            assertThat("Reason ok", calculatedSelfStatus.getReasons(), containsInAnyOrder(reasons.toArray()));
        }
    }
}
