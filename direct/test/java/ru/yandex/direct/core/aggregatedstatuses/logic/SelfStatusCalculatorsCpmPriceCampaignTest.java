package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignCounters;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignStatesEnum;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.DRAFT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_CRIT;

@RunWith(Parameterized.class)
public class SelfStatusCalculatorsCpmPriceCampaignTest {

    private static final CampaignCounters EMPTY_COUNTERS = new CampaignCounters();

    private static final CampaignCounters ACTIVE_COUNTERS = new CampaignCounters(2,
            Map.of(RUN_OK, 1, RUN_WARN, 1), Map.of());

    @Parameterized.Parameter()
    public Collection<CampaignStatesEnum> states;

    @Parameterized.Parameter(1)
    public CampaignCounters counters;

    @Parameterized.Parameter(2)
    public Boolean isDraftApproveAllowed;

    @Parameterized.Parameter(3)
    public GdSelfStatusEnum status;

    @Parameterized.Parameter(4)
    public List<GdSelfStatusReason> reasons;

    @Parameterized.Parameters(name = "states = {0} => status: {2}, reason: {3}")
    public static Object[][] params() {
        return new Object[][]{

                {List.of(CampaignStatesEnum.CPM_PRICE_INCORRECT),
                        EMPTY_COUNTERS, false,
                        DRAFT, null},

                {List.of(CampaignStatesEnum.CPM_PRICE_INCORRECT),
                        ACTIVE_COUNTERS, false,
                        STOP_CRIT, List.of(GdSelfStatusReason.CPM_PRICE_INCORRECT)},

                {List.of(CampaignStatesEnum.CPM_PRICE_NOT_APPROVED),
                        ACTIVE_COUNTERS, false,
                        STOP_CRIT, List.of(GdSelfStatusReason.CPM_PRICE_NOT_APPROVED)},

                {List.of(CampaignStatesEnum.CPM_PRICE_WAITING_FOR_APPROVE),
                        ACTIVE_COUNTERS, false,
                        STOP_CRIT, List.of(GdSelfStatusReason.CPM_PRICE_WAITING_FOR_APPROVE)},

                {List.of(CampaignStatesEnum.CPM_PRICE_WAITING_FOR_APPROVE, CampaignStatesEnum.DRAFT),
                        EMPTY_COUNTERS, false,
                        DRAFT, null},

                {List.of(CampaignStatesEnum.CPM_PRICE_NOT_APPROVED, CampaignStatesEnum.DRAFT),
                        EMPTY_COUNTERS, false,
                        DRAFT, List.of(GdSelfStatusReason.CPM_PRICE_NOT_APPROVED)},

                {List.of(CampaignStatesEnum.CPM_PRICE_INCORRECT, CampaignStatesEnum.DRAFT),
                        EMPTY_COUNTERS, false,
                        DRAFT, List.of(GdSelfStatusReason.CPM_PRICE_INCORRECT)},

                {List.of(CampaignStatesEnum.CPM_PRICE_WAITING_FOR_APPROVE, CampaignStatesEnum.DRAFT),
                        EMPTY_COUNTERS, true,
                        DRAFT, List.of(GdSelfStatusReason.CPM_PRICE_WAITING_FOR_APPROVE)},

                {List.of(CampaignStatesEnum.CPM_PRICE_NOT_APPROVED, CampaignStatesEnum.DRAFT),
                        EMPTY_COUNTERS, true,
                        DRAFT, List.of(GdSelfStatusReason.CPM_PRICE_NOT_APPROVED)},

                {List.of(CampaignStatesEnum.CPM_PRICE_INCORRECT, CampaignStatesEnum.DRAFT),
                        EMPTY_COUNTERS, true,
                        DRAFT, List.of(GdSelfStatusReason.CPM_PRICE_INCORRECT)},

        };
    }

    @Test
    public void test() {
        var calculatedStatus = SelfStatusCalculators.calcCampaignSelfStatus(1L, false, states,
                Collections.emptyMap(), counters, isDraftApproveAllowed, true);

        if (status == null) {
            assertThat("Status is null", calculatedStatus, Matchers.nullValue());
            return;
        }

        assertEquals("Status ok", status, calculatedStatus.getStatus());

        if (reasons == null) {
            assertThat("Reason is null", calculatedStatus.getReasons(), Matchers.nullValue());
        } else {
            assertThat("Reason count ok", calculatedStatus.getReasons(), hasSize(reasons.size()));
            assertThat("Reason ok", calculatedStatus.getReasons(), containsInAnyOrder(reasons.toArray()));
        }
    }
}
