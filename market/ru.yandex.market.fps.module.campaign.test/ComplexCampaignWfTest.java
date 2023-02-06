package ru.yandex.market.fps.module.campaign.test;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fps.module.campaign.Campaign;
import ru.yandex.market.fps.module.campaign.ComplexCampaign;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.time.Now;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.jmf.logic.wf.bcp.WfConstants.SKIP_WF_PRE_CONDITIONS;
import static ru.yandex.market.jmf.logic.wf.bcp.WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE;

class ComplexCampaignWfTest extends AbstractComplexCampaignTest {
    @Test
    void testComplexCantBeTransferToAwaitingApproveWithoutInnerCampaigns() {
        Map<String, Object> editParams = Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE);

        Map<String, Object> params = buildComplexParams(Now.localDate().plusDays(1), Now.localDate().plusDays(2));
        var complex = bcpService.create(ComplexCampaign.FQN, params);
        var thrown = assertThrows(
                ValidationException.class,
                () -> bcpService.edit(complex, editParams)
        );

        assertEquals("Для передачи на подтверждение должна быть хотя бы 1 вложенная акция", thrown.getMessage());
    }

    @Test
    void testComplexCanBeTransferToAwaitingApprove() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Campaign campaign = createCampaign(complex, Map.of(), Map.of());

        assertEquals(ComplexCampaign.Statuses.START_CREATION, complex.getStatus());
        assertEquals(Campaign.Statuses.START_CREATION, campaign.getStatus());

        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE));

        assertEquals(ComplexCampaign.Statuses.AWAITING_APPROVE, complex.getStatus());
        assertEquals(Campaign.Statuses.AWAITING_APPROVE, campaign.getStatus());
    }

    @Test
    void testComplexCanBeCanceledFromInitStatus() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Campaign campaign = createCampaign(complex, Map.of(), Map.of());

        assertEquals(ComplexCampaign.Statuses.START_CREATION, complex.getStatus());
        assertEquals(Campaign.Statuses.START_CREATION, campaign.getStatus());

        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.CANCELED));

        assertEquals(ComplexCampaign.Statuses.CANCELED, complex.getStatus());
        assertEquals(Campaign.Statuses.CANCELED, campaign.getStatus());
    }

    @Test
    void testComplexCanBeTransferFromAwaitingApproveToStartCreation() {
        Map<String, Object> skipParams = Map.of(
                SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true,
                SKIP_WF_PRE_CONDITIONS, true
        );
        Map<String, Object> statusParams = Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE);

        ComplexCampaign complex = createComplexCampaign(statusParams, skipParams);
        Campaign campaign = createCampaign(complex, statusParams, skipParams);

        assertEquals(ComplexCampaign.Statuses.AWAITING_APPROVE, complex.getStatus());
        assertEquals(Campaign.Statuses.AWAITING_APPROVE, campaign.getStatus());

        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.START_CREATION), skipParams);

        assertEquals(ComplexCampaign.Statuses.START_CREATION, complex.getStatus());
        assertEquals(Campaign.Statuses.START_CREATION, campaign.getStatus());
    }

    @Test
    void testComplexCanBeTransferFromScheduledToStartCreation() {
        Map<String, Object> skipParams = Map.of(
                SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true,
                SKIP_WF_PRE_CONDITIONS, true
        );
        Map<String, Object> statusParams = Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.SCHEDULED);

        ComplexCampaign complex = createComplexCampaign(statusParams, skipParams);
        Campaign campaign = createCampaign(complex, statusParams, skipParams);

        assertEquals(ComplexCampaign.Statuses.SCHEDULED, complex.getStatus());
        assertEquals(Campaign.Statuses.SCHEDULED, campaign.getStatus());

        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.START_CREATION), skipParams);

        assertEquals(ComplexCampaign.Statuses.START_CREATION, complex.getStatus());
        assertEquals(Campaign.Statuses.START_CREATION, campaign.getStatus());
    }

}
