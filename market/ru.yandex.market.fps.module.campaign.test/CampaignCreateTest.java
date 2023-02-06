package ru.yandex.market.fps.module.campaign.test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fps.module.campaign.Campaign;
import ru.yandex.market.fps.module.campaign.ComplexCampaign;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CampaignCreateTest extends AbstractComplexCampaignTest {
    @Test
    void cantCreateCampaignWithDuplicateAnaplan() {
        Map<String, Object> params = Map.of(Campaign.ANAPLANS, Set.of(76));
        Map<String, Object> params2 = Map.of(Campaign.ANAPLANS, Set.of(76));

        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Campaign campaign = createCompensationalCampaign(complex, params, Map.of());

        assertEquals(1, campaign.getAnaplans().size());

        var thrown = assertThrows(
                ValidationException.class,
                () -> createCompensationalCampaign(complex, params2, Map.of())
        );

        assertThat(thrown.getMessage(),
                CoreMatchers.containsString("Значение у атрибута 'Anaplan IDs' должно быть уникальным"));
    }

    @Test
    void cantCreateCampaignWithZeroBudget() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Map<String, Object> params = Map.of(Campaign.SUM, 0);

        var thrown = assertThrows(
                ValidationException.class,
                () -> createCampaign(complex, params, Map.of())
        );

        assertEquals("Бюджет должен быть больше 0", thrown.getMessage());
    }

    @Test
    void cantCreateCampaignWithNegativeBudget() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Map<String, Object> params = Map.of(Campaign.SUM, -10);

        var thrown = assertThrows(
                ValidationException.class,
                () -> createCampaign(complex, params, Map.of())
        );

        assertEquals("Бюджет должен быть больше 0", thrown.getMessage());
    }

    @Test
    void canCreateCampaignWithNonZeroBudget() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Map<String, Object> params = Map.of(Campaign.SUM, 10);

        Campaign campaign = createCampaign(complex, params, Map.of());

        assertEquals(BigDecimal.valueOf(10), campaign.getSum());
    }

    @Test
    void canPartnerApprovedCampaign() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Map<String, Object> params = Map.of(Campaign.SUM, 10);

        Campaign campaign = createCampaign(complex, params, Map.of());
        assertEquals(BigDecimal.valueOf(10), campaign.getSum());

        bcpService.edit(complex, Map.of(ComplexCampaign.STATUS, ComplexCampaign.Statuses.AWAITING_APPROVE));
        assertEquals(Campaign.Statuses.AWAITING_APPROVE, campaign.getStatus());

        bcpService.edit(complex, Map.of(
                ComplexCampaign.STATUS, ComplexCampaign.Statuses.SCHEDULED,
                ComplexCampaign.APPROVED_BY_PARTNER_UID, 2222
        ));
        assertEquals(Campaign.Statuses.SCHEDULED, campaign.getStatus());
        assertEquals(2222, campaign.getApprovedByPartnerUid());
    }

}
