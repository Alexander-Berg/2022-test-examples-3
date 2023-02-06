package ru.yandex.market.fps.module.campaign.test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.fps.module.campaign.Campaign;
import ru.yandex.market.fps.module.campaign.ComplexCampaign;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.wildfly.common.Assert.assertTrue;
import static ru.yandex.market.fps.module.campaign.CampaignConstants.SKIP_DATE_VALIDATION_CHECK;
import static ru.yandex.market.jmf.logic.wf.bcp.WfConstants.SKIP_WF_PRE_CONDITIONS;

public class IncreaseTermsCompensationCampaignTest extends AbstractComplexCampaignTest {

    @Test
    public void cantCreateFixAndCompensationInSameCampaign() {
        Map<String, Object> params = Map.of(Campaign.ANAPLANS, Set.of(76));
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());

        createCompensationalCampaign(complex, params, Map.of());

        var thrown = assertThrows(
                ValidationException.class,
                () -> createCampaign(complex, Map.of(), Map.of())
        );

        assertEquals("Нельзя создавать акции разных типов", thrown.getMessage());
    }

    @Test
    public void canCreateTwoFixInSameCampaign() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());

        createCampaign(complex, Map.of(), Map.of());
        createCampaign(complex, Map.of(), Map.of());

        assertEquals(2, complex.getCampaigns().size());
    }

    @Test
    public void canCreateTwoCompensationInSameCampaign() {
        Map<String, Object> params = Map.of(Campaign.ANAPLANS, Set.of(76));
        Map<String, Object> params2 = Map.of(Campaign.ANAPLANS, Set.of(77));
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());

        createCompensationalCampaign(complex, params, Map.of());
        createCompensationalCampaign(complex, params2, Map.of());

        assertEquals(2, complex.getCampaigns().size());
    }

    @Test
    public void cantChangeDateEndInActiveComplexWithCompensationCampaign() {
        Map<String, Object> params = Map.of(Campaign.ANAPLANS, Set.of(77));
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());

        createCompensationalCampaign(complex, params, Map.of());
        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE));
        bcpService.edit(complex, Map.of(
                ComplexCampaign.APPROVED_BY_PARTNER_UID, 123,
                ComplexCampaign.STATUS, ComplexCampaign.Statuses.SCHEDULED
        ));

        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.ACTIVE),
                Map.of(SKIP_DATE_VALIDATION_CHECK, true, SKIP_WF_PRE_CONDITIONS, true)
        );
        var thrown = assertThrows(
                ValidationException.class,
                () -> bcpService.edit(complex, Map.of(ComplexCampaign.DATE_END, complex.getDateEnd().minusDays(1)))
        );

        assertTrue(thrown.getMessage().contains("Новая дата окончания акции должна быть больше старой"));
    }

    @Test
    public void cantChangeTitleInActiveComplexWithCompensationCampaign() {
        Map<String, Object> params = Map.of(Campaign.ANAPLANS, Set.of(77));
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());

        createCompensationalCampaign(complex, params, Map.of());
        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE));
        bcpService.edit(complex, Map.of(
                ComplexCampaign.APPROVED_BY_PARTNER_UID, 123,
                ComplexCampaign.STATUS, ComplexCampaign.Statuses.SCHEDULED
        ));

        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.ACTIVE),
                Map.of(SKIP_DATE_VALIDATION_CHECK, true, SKIP_WF_PRE_CONDITIONS, true)
        );
        var thrown = assertThrows(
                ru.yandex.market.jmf.trigger.TriggerServiceException.class,
                () -> bcpService.edit(complex, Map.of(ComplexCampaign.TITLE, Randoms.string()))
        );

        assertTrue(thrown.getMessage().contains("Нельзя изменять название действующей акции"));
    }

    @Test
    public void cantChangeDateStartInActiveComplexWithCompensationCampaign() {
        Map<String, Object> params = Map.of(Campaign.ANAPLANS, Set.of(77));
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());

        createCompensationalCampaign(complex, params, Map.of());
        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE));
        bcpService.edit(complex, Map.of(
                ComplexCampaign.APPROVED_BY_PARTNER_UID, 123,
                ComplexCampaign.STATUS, ComplexCampaign.Statuses.SCHEDULED
        ));

        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.ACTIVE),
                Map.of(SKIP_DATE_VALIDATION_CHECK, true, SKIP_WF_PRE_CONDITIONS, true)
        );

        var thrown = assertThrows(
                ru.yandex.market.jmf.trigger.TriggerServiceException.class,
                () -> bcpService.edit(complex, Map.of(ComplexCampaign.DATE_START, complex.getDateStart().minusDays(1)))
        );

        assertTrue(thrown.getMessage().contains("Нельзя изменять дату начала действующей акции"));
    }

    @Test
    public void cantChangeSumToLessInActiveCompensationCampaign() {
        Map<String, Object> params = Map.of(Campaign.ANAPLANS, Set.of(77));
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());

        Campaign campaign = createCompensationalCampaign(complex, params, Map.of());
        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE));
        bcpService.edit(complex, Map.of(
                ComplexCampaign.APPROVED_BY_PARTNER_UID, 123,
                ComplexCampaign.STATUS, ComplexCampaign.Statuses.SCHEDULED
        ));

        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.ACTIVE),
                Map.of(SKIP_DATE_VALIDATION_CHECK, true, SKIP_WF_PRE_CONDITIONS, true)
        );

        var thrown = assertThrows(
                ValidationException.class,
                () -> bcpService.edit(campaign, Map.of(Campaign.SUM,
                        campaign.getSum().subtract(BigDecimal.valueOf(1000))))
        );

        assertTrue(thrown.getMessage().contains("Бюджет можно изменять только в большую сторону"));
    }

    @Test
    public void canChangeSumInActiveCompensationCampaign() {
        Map<String, Object> params = Map.of(Campaign.ANAPLANS, Set.of(77));
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());

        Campaign campaign = createCompensationalCampaign(complex, params, Map.of());
        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE));
        bcpService.edit(complex, Map.of(
                ComplexCampaign.APPROVED_BY_PARTNER_UID, 123,
                ComplexCampaign.STATUS, ComplexCampaign.Statuses.SCHEDULED
        ));

        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.ACTIVE),
                Map.of(SKIP_DATE_VALIDATION_CHECK, true, SKIP_WF_PRE_CONDITIONS, true)
        );

        var oldSum = campaign.getSum();
        bcpService.edit(campaign, Map.of(Campaign.SUM, campaign.getSum().add(BigDecimal.valueOf(1000))));
        assertEquals(oldSum.add(BigDecimal.valueOf(1000)), campaign.getSum());
    }

    @Test
    public void goldenCase() {
        Map<String, Object> params = Map.of(Campaign.ANAPLANS, Set.of(77));
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Campaign campaign = createCompensationalCampaign(complex, params, Map.of());
        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE));
        bcpService.edit(complex, Map.of(
                ComplexCampaign.APPROVED_BY_PARTNER_UID, 123,
                ComplexCampaign.STATUS, ComplexCampaign.Statuses.SCHEDULED
        ));
        bcpService.edit(complex, Map.of(
                        ComplexCampaign.STATUS, ComplexCampaign.Statuses.ACTIVE,
                        ComplexCampaign.WAS_ACTIVATED, true
                ),
                Map.of(SKIP_DATE_VALIDATION_CHECK, true, SKIP_WF_PRE_CONDITIONS, true)
        );

        bcpService.edit(complex, Map.of(ComplexCampaign.DATE_END, complex.getDateEnd().plusDays(2)));
        assertEquals(ComplexCampaign.Statuses.INCREASE_TERMS, complex.getStatus());
        assertEquals(Campaign.Statuses.INCREASE_TERMS, campaign.getStatus());
        assertEquals(true, complex.getWasActivated());

        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE));
        bcpService.edit(complex, Map.of(
                ComplexCampaign.APPROVED_BY_PARTNER_UID, 123,
                ComplexCampaign.STATUS, ComplexCampaign.Statuses.SCHEDULED
        ));

        bcpService.edit(complex, Map.of(
                ComplexCampaign.STATUS, ComplexCampaign.Statuses.ACTIVE
        ));
    }

}
