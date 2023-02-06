package ru.yandex.market.fps.module.campaign.test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.marketing.MarketingCampaignType;
import ru.yandex.market.fps.module.campaign.Campaign;
import ru.yandex.market.fps.module.campaign.ComplexCampaign;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.time.Now;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.fps.module.campaign.CampaignConstants.CAMPAIGN_NAME_MAX_LENGTH;

class ComplexCampaignCreateTest extends AbstractComplexCampaignTest {

    @Test
    void testCantCreateComplexWithDateStartInPast() {
        Map<String, Object> params = buildComplexParams(Now.localDate().minusDays(1), Now.localDate().plusDays(1));
        var thrown = assertThrows(
                ValidationException.class,
                () -> bcpService.create(ComplexCampaign.FQN, params)
        );
        assertEquals("Дата начала акции не должна быть в прошлом", thrown.getMessage());
    }

    @Test
    void testCantCreateComplexWithDateStartAfterDateEnd() {
        Map<String, Object> params = buildComplexParams(Now.localDate().plusDays(2), Now.localDate().plusDays(1));
        var thrown = assertThrows(
                ValidationException.class,
                () -> bcpService.create(ComplexCampaign.FQN, params)
        );
        assertEquals("Дата начала акции должна быть до даты окончания", thrown.getMessage());
    }

    @Test
    void testCantCreateComplexWithDateEndInPast() {
        Map<String, Object> params = buildComplexParams(Now.localDate(), Now.localDate().minusDays(2));
        var thrown = assertThrows(
                ValidationException.class,
                () -> bcpService.create(ComplexCampaign.FQN, params)
        );
        assertEquals("Дата окончания акции должна быть в будущем", thrown.getMessage());
    }

    @Test
    void testCantCreateComplexWithLongTitle() {
        Map<String, Object> params = buildComplexParams(Now.localDate().plusDays(2), Now.localDate().plusDays(4));
        params.put(ComplexCampaign.TITLE, RandomStringUtils.random(202, true, true));
        var thrown = assertThrows(
                ValidationException.class,
                () -> bcpService.create(ComplexCampaign.FQN, params)
        );
        assertEquals(String.format("Название акции слишком длинное (макс. %d)", CAMPAIGN_NAME_MAX_LENGTH),
                thrown.getMessage());
    }

    @Test
    void testCanCreateValidComplexCampaignFromCurrentDay() {
        Map<String, Object> params = buildComplexParams(Now.localDate(), Now.localDate().plusDays(2));
        ComplexCampaign complex = createComplexCampaign(params, Map.of());

        assertEquals(complex.getDateStart(), Now.localDate());
        assertEquals(complex.getDateEnd(), Now.localDate().plusDays(2));
    }

    @Test
    void testCanCreateValidComplexCampaignInFuture() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());

        assertEquals(complex.getDateStart(), Now.localDate().plusDays(1));
        assertEquals(complex.getDateEnd(), Now.localDate().plusDays(2));
    }

    @Test
    void testCanCreateValidComplexCampaignInOneDayAndApproved() {
        var date = Now.localDate().plusDays(1);
        Map<String, Object> params = buildComplexParams(date, date);
        ComplexCampaign complex = createComplexCampaign(params, Map.of());

        assertEquals(complex.getDateStart(), date);
        assertEquals(complex.getDateEnd(), date);
        Campaign campaign = createCampaign(complex, Map.of(), Map.of());

        bcpService.edit(complex, Map.of(Campaign.STATUS, Campaign.Statuses.AWAITING_APPROVE));
        params = Map.of(
                Campaign.APPROVED_BY_PARTNER_UID, 2323,
                Campaign.STATUS, Campaign.Statuses.SCHEDULED
        );
        bcpService.edit(complex, params);

        assertEquals(Campaign.Statuses.SCHEDULED, complex.getStatus());
    }

    @Test
    void testCanCreateValidComplexCampaignForTodayAndApproved() {
        var date = Now.localDate();
        Map<String, Object> params = buildComplexParams(date, date);
        ComplexCampaign complex = createComplexCampaign(params, Map.of());

        assertEquals(complex.getDateStart(), date);
        assertEquals(complex.getDateEnd(), date);
        Campaign campaign = createCampaign(complex, Map.of(), Map.of());

        bcpService.edit(complex, Map.of(Campaign.STATUS, Campaign.Statuses.AWAITING_APPROVE));
        params = Map.of(
                Campaign.APPROVED_BY_PARTNER_UID, 2323,
                Campaign.STATUS, Campaign.Statuses.SCHEDULED
        );
        bcpService.edit(complex, params);

        assertEquals(Campaign.Statuses.SCHEDULED, complex.getStatus());
    }

    @Test
    void testTitleChangeInChildrenCampaign() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Campaign campaign = createCampaign(complex, Map.of(), Map.of());

        assertEquals(complex.getTitle(), campaign.getTitle());

        String newTitle = "New title";
        bcpService.edit(complex, Map.of(ComplexCampaign.TITLE, newTitle));

        assertEquals(newTitle, complex.getTitle());
        assertEquals(complex.getTitle(), campaign.getTitle());
    }

    @Test
    void testDateStartChangeInChildrenCampaign() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Campaign campaign = createCampaign(complex, Map.of(), Map.of());

        assertEquals(complex.getTitle(), campaign.getTitle());

        LocalDate newStartDate = Now.localDate().plusDays(4);
        LocalDate newEndDate = Now.localDate().plusDays(6);
        bcpService.edit(complex, Map.of(
                ComplexCampaign.DATE_START, newStartDate,
                ComplexCampaign.DATE_END, newEndDate
        ));

        assertEquals(newStartDate, complex.getDateStart());
        assertEquals(campaign.getDateStart(), complex.getDateStart());
    }

    @Test
    void testDateEndChangeInChildrenCampaign() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Campaign campaign = createCampaign(complex, Map.of(), Map.of());

        assertEquals(complex.getTitle(), campaign.getTitle());

        LocalDate newEndDate = Now.localDate().plusDays(6);
        bcpService.edit(complex, Map.of(ComplexCampaign.DATE_END, newEndDate));

        assertEquals(newEndDate, complex.getDateEnd());
        assertEquals(campaign.getDateEnd(), complex.getDateEnd());
    }

    @Test
    void testResetPartnerApproveAndStatusWhenComplexTitleChange() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Campaign campaign = createCampaign(complex, Map.of(), Map.of());

        bcpService.edit(complex, Map.of(Campaign.STATUS, Campaign.Statuses.AWAITING_APPROVE));
        Map<String, Object> params = Map.of(
                Campaign.APPROVED_BY_PARTNER_UID, 2323,
                Campaign.STATUS, Campaign.Statuses.SCHEDULED
        );
        bcpService.edit(complex, params);

        assertEquals(2323, complex.getApprovedByPartnerUid());
        assertEquals(2323, campaign.getApprovedByPartnerUid());
        assertEquals(ComplexCampaign.Statuses.SCHEDULED, complex.getStatus());
        assertEquals(Campaign.Statuses.SCHEDULED, campaign.getStatus());

        String newTitle = "New title";
        bcpService.edit(complex, Map.of(ComplexCampaign.TITLE, newTitle));

        assertNull(complex.getApprovedByPartnerUid());
        assertNull(campaign.getApprovedByPartnerUid());

        assertEquals(ComplexCampaign.Statuses.AWAITING_APPROVE, complex.getStatus());
        assertEquals(Campaign.Statuses.AWAITING_APPROVE, complex.getStatus());
    }

    @Test
    void testResetPartnerApproveAndStatusWhenComplexDateEndChange() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Campaign campaign = createCampaign(complex, Map.of(), Map.of());

        bcpService.edit(complex, Map.of(Campaign.STATUS, Campaign.Statuses.AWAITING_APPROVE));
        Map<String, Object> params = Map.of(
                Campaign.APPROVED_BY_PARTNER_UID, 2324,
                Campaign.STATUS, Campaign.Statuses.SCHEDULED
        );
        bcpService.edit(complex, params);

        assertEquals(2324, complex.getApprovedByPartnerUid());
        assertEquals(2324, campaign.getApprovedByPartnerUid());
        assertEquals(ComplexCampaign.Statuses.SCHEDULED, complex.getStatus());
        assertEquals(Campaign.Statuses.SCHEDULED, campaign.getStatus());

        LocalDate newEndDate = Now.localDate().plusDays(6);
        bcpService.edit(complex, Map.of(ComplexCampaign.DATE_END, newEndDate));

        assertNull(complex.getApprovedByPartnerUid());
        assertNull(campaign.getApprovedByPartnerUid());

        assertEquals(ComplexCampaign.Statuses.START_CREATION, complex.getStatus());
        assertEquals(Campaign.Statuses.START_CREATION, complex.getStatus());
    }

    @Test
    void testResetPartnerApproveAndStatusWhenComplexDateStartChange() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Campaign campaign = createCampaign(complex, Map.of(), Map.of());

        bcpService.edit(complex, Map.of(Campaign.STATUS, Campaign.Statuses.AWAITING_APPROVE));
        Map<String, Object> params = Map.of(
                Campaign.APPROVED_BY_PARTNER_UID, 2324,
                Campaign.STATUS, Campaign.Statuses.SCHEDULED
        );
        bcpService.edit(complex, params);

        assertEquals(2324, complex.getApprovedByPartnerUid());
        assertEquals(2324, campaign.getApprovedByPartnerUid());
        assertEquals(ComplexCampaign.Statuses.SCHEDULED, complex.getStatus());
        assertEquals(Campaign.Statuses.SCHEDULED, campaign.getStatus());

        LocalDate newStartDate = Now.localDate().plusDays(4);
        LocalDate newEndDate = Now.localDate().plusDays(6);
        bcpService.edit(complex, Map.of(
                ComplexCampaign.DATE_START, newStartDate,
                ComplexCampaign.DATE_END, newEndDate
        ));

        assertNull(complex.getApprovedByPartnerUid());
        assertNull(campaign.getApprovedByPartnerUid());

        assertEquals(ComplexCampaign.Statuses.START_CREATION, complex.getStatus());
        assertEquals(Campaign.Statuses.START_CREATION, complex.getStatus());
    }

    @Test
    void testResetPartnerApproveAndStatusWhenCampaignTypeChange() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Campaign campaign = createCampaign(complex, Map.of(), Map.of());

        bcpService.edit(complex, Map.of(Campaign.STATUS, Campaign.Statuses.AWAITING_APPROVE));
        Map<String, Object> params = Map.of(
                Campaign.APPROVED_BY_PARTNER_UID, 2324,
                Campaign.STATUS, Campaign.Statuses.SCHEDULED
        );
        bcpService.edit(complex, params);

        assertEquals(2324, complex.getApprovedByPartnerUid());
        assertEquals(2324, campaign.getApprovedByPartnerUid());
        assertEquals(ComplexCampaign.Statuses.SCHEDULED, complex.getStatus());
        assertEquals(Campaign.Statuses.SCHEDULED, campaign.getStatus());

        bcpService.edit(campaign, Map.of(
                campaign.TYPE, MarketingCampaignType.PROMO_TV
        ));

        assertNull(complex.getApprovedByPartnerUid());
        assertNull(campaign.getApprovedByPartnerUid());

        assertEquals(ComplexCampaign.Statuses.AWAITING_APPROVE, complex.getStatus());
        assertEquals(Campaign.Statuses.AWAITING_APPROVE, complex.getStatus());
        assertEquals(campaign.getType().getCode(), MarketingCampaignType.PROMO_TV.name());
    }

    @Test
    void testResetPartnerApproveAndStatusWhenCampaignSumChange() {
        ComplexCampaign complex = createComplexCampaign(Map.of(), Map.of());
        Campaign campaign = createCampaign(complex, Map.of(), Map.of());

        bcpService.edit(complex, Map.of(Campaign.STATUS, Campaign.Statuses.AWAITING_APPROVE));
        Map<String, Object> params = Map.of(
                Campaign.APPROVED_BY_PARTNER_UID, 2324,
                Campaign.STATUS, Campaign.Statuses.SCHEDULED
        );
        bcpService.edit(complex, params);

        assertEquals(2324, complex.getApprovedByPartnerUid());
        assertEquals(2324, campaign.getApprovedByPartnerUid());
        assertEquals(ComplexCampaign.Statuses.SCHEDULED, complex.getStatus());
        assertEquals(Campaign.Statuses.SCHEDULED, campaign.getStatus());

        bcpService.edit(campaign, Map.of(
                campaign.SUM, 40_000
        ));

        assertNull(complex.getApprovedByPartnerUid());
        assertNull(campaign.getApprovedByPartnerUid());

        assertEquals(ComplexCampaign.Statuses.AWAITING_APPROVE, complex.getStatus());
        assertEquals(Campaign.Statuses.AWAITING_APPROVE, complex.getStatus());
        assertEquals(campaign.getSum(), BigDecimal.valueOf(40_000));
    }

}
