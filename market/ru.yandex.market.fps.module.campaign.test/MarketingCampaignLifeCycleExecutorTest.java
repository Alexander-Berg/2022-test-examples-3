package ru.yandex.market.fps.module.campaign.test;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fps.module.campaign.Campaign;
import ru.yandex.market.fps.module.campaign.ComplexCampaign;
import ru.yandex.market.fps.module.campaign.impl.MarketingCampaignLifeCycleExecutor;
import ru.yandex.market.jmf.time.Now;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.fps.module.campaign.CampaignConstants.SKIP_DATE_VALIDATION_CHECK;
import static ru.yandex.market.jmf.logic.wf.bcp.WfConstants.SKIP_WF_PRE_CONDITIONS;
import static ru.yandex.market.jmf.logic.wf.bcp.WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE;

public class MarketingCampaignLifeCycleExecutorTest extends AbstractComplexCampaignTest {
    @Inject
    MarketingCampaignLifeCycleExecutor marketingCampaignLifeCycleExecutor;

    @Test
    public void activateCampaignByLifecycleJob() {
        Map<String, Object> params = Map.of(
                ComplexCampaign.DATE_START, Now.localDate(),
                ComplexCampaign.DATE_END, Now.localDate().plusDays(2)
        );
        ComplexCampaign complex = createComplexCampaign(params, Map.of());

        Campaign campaign1 = createCampaign(complex, Map.of(), Map.of());
        Campaign campaign2 = createCampaign(complex, Map.of(), Map.of());

        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE));

        bcpService.edit(complex, Map.of(
                ComplexCampaign.STATUS, ComplexCampaign.Statuses.SCHEDULED,
                ComplexCampaign.APPROVED_BY_PARTNER_UID, 1
        ));

        marketingCampaignLifeCycleExecutor.tryActivateCampaigns();
        assertEquals(ComplexCampaign.Statuses.ACTIVE, complex.getStatus());
        assertEquals(Campaign.Statuses.ACTIVE, campaign1.getStatus());
        assertEquals(Campaign.Statuses.ACTIVE, campaign2.getStatus());
    }

    @Test
    public void finishedCampaignByLifecycleJob() {
        Map<String, Object> params = buildComplexParams(Now.localDate().minusDays(2), Now.localDate().minusDays(1));
        params.put(ComplexCampaign.APPROVED_BY_PARTNER_UID, 1);
        params.put(ComplexCampaign.STATUS, ComplexCampaign.Statuses.ACTIVE);

        Map<String, Object> skipParams = Map.of(
                SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true,
                SKIP_WF_PRE_CONDITIONS, true,
                SKIP_DATE_VALIDATION_CHECK, true
        );

        ComplexCampaign complex = createComplexCampaign(params, skipParams);

        Campaign campaign1 = createCampaign(complex, Map.of(Campaign.STATUS, Campaign.Statuses.ACTIVE), skipParams);
        Campaign campaign2 = createCampaign(complex, Map.of(Campaign.STATUS, Campaign.Statuses.ACTIVE), skipParams);

        assertEquals(Campaign.Statuses.ACTIVE, campaign1.getStatus());
        assertEquals(Now.localDate().minusDays(1), campaign1.getDateEnd());

        marketingCampaignLifeCycleExecutor.tryFinishedCampaigns();
        assertEquals(ComplexCampaign.Statuses.FINISHED, complex.getStatus());
        assertEquals(Campaign.Statuses.FINISHED, campaign1.getStatus());
        assertEquals(Campaign.Statuses.FINISHED, campaign2.getStatus());
    }

    @Test
    public void staleCampaignByLifecycleJob() {
        Map<String, Object> params = buildComplexParams(Now.localDate().minusDays(2), Now.localDate().minusDays(1));
        params.put(ComplexCampaign.APPROVED_BY_PARTNER_UID, 1);
        params.put(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE);

        Map<String, Object> skipParams = Map.of(
                SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true,
                SKIP_WF_PRE_CONDITIONS, true,
                SKIP_DATE_VALIDATION_CHECK, true
        );

        ComplexCampaign complex = createComplexCampaign(params, skipParams);

        Campaign campaign1 = createCampaign(complex,
                Map.of(Campaign.STATUS, Campaign.Statuses.AWAITING_APPROVE), skipParams);
        Campaign campaign2 = createCampaign(complex,
                Map.of(Campaign.STATUS, Campaign.Statuses.AWAITING_APPROVE), skipParams);

        marketingCampaignLifeCycleExecutor.tryStaleCampaigns();
        assertEquals(ComplexCampaign.Statuses.STALE, complex.getStatus());
        assertEquals(Campaign.Statuses.STALE, campaign1.getStatus());
        assertEquals(Campaign.Statuses.STALE, campaign2.getStatus());
    }

}
