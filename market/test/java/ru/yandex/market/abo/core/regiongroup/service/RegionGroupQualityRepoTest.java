package ru.yandex.market.abo.core.regiongroup.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.assessor.AssessorService;
import ru.yandex.market.abo.core.assessor.model.Assessor;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.model.ProblemTypeId;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.core.regiongroup.model.AboRegionGroup;
import ru.yandex.market.abo.core.regiongroup.model.RegionGroupQuality;
import ru.yandex.market.abo.core.ticket.AbstractCoreHierarchyTest;
import ru.yandex.market.abo.core.ticket.ProblemManager;
import ru.yandex.market.abo.core.ticket.model.LocalDeliveryProblem;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicket;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketService;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketStatus;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 07.05.2020
 */
class RegionGroupQualityRepoTest extends AbstractCoreHierarchyTest {

    private static final long SHOP_ID = 123L;

    private static final long REGION_GROUP_ID = 321L;
    private static final LocalDateTime REGION_GROUP_TICKET_CREATION_TIME = LocalDateTime.now().minusDays(40);
    private static final LocalDateTime REGION_GROUP_APPROVED_TIME = REGION_GROUP_TICKET_CREATION_TIME.plusDays(2);

    private static final int TICKET_GEN_ID = 1;

    private static final long ASSESSOR_ID = 111L;
    private static final String ASSESSOR_LOGIN = "login";
    private static final String ASSESSOR_STAFF_LOGIN = "staff_login";

    @Autowired
    private RegionGroupQualityRepo regionGroupQualityRepo;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;
    @Autowired
    private AssessorService assessorService;
    @Autowired
    private RegionGroupService regionGroupService;
    @Autowired
    private RecheckTicketService recheckTicketService;
    @Autowired
    private ProblemManager problemManager;

    @BeforeEach
    void init() {
        pgJdbcTemplate.update("insert into shop(id, shopname) values (?, 'super_shop')", SHOP_ID);

        var assessor = new Assessor(ASSESSOR_ID, ASSESSOR_LOGIN);
        assessorService.saveAssessor(assessor);
        assessorService.saveAssessorInfo(ASSESSOR_ID, ASSESSOR_STAFF_LOGIN, "", true);

        var recheckTicket = recheckTicketService.save(new RecheckTicket.Builder()
                .withShopId(SHOP_ID)
                .withType(RecheckTicketType.REGION_GROUP_MODERATION)
                .withCreationTime(DateUtil.asDate(REGION_GROUP_TICKET_CREATION_TIME))
                .withModificationTime(DateUtil.asDate(REGION_GROUP_APPROVED_TIME))
                .withUserId(ASSESSOR_ID)
                .withStatus(RecheckTicketStatus.PASS)
                .build());
        recheckTicketService.save(recheckTicket);

        var regionGroup = new AboRegionGroup(
                REGION_GROUP_ID, SHOP_ID, "Группа Москва", true, new Long[]{(long) Regions.MOSCOW}
        );
        regionGroup.setTicketId(recheckTicket.getId());
        regionGroupService.save(regionGroup);

        flushAndClear();
    }

    @Test
    void testRegionGroupQualities__problemNotExists() {
        var regionGroupQualities = regionGroupQualityRepo.findAll();
        assertFalse(containsProblemRegionGroup(regionGroupQualities));
    }

    @ParameterizedTest
    @CsvSource({"APPROVED, true", "NEW, false", "APPROVED_MASS, true"})
    void testRegionGroupQualities__differentProblemStatuses(ProblemStatus status, boolean isGroupProblem) {
        boolean problemCreatedLessThanMonthAfterRecheck = true;
        boolean regionFromGroup = true;
        createProblem(status, problemCreatedLessThanMonthAfterRecheck, regionFromGroup);
        flushAndClear();

        assertEquals(isGroupProblem, containsProblemRegionGroup(regionGroupQualityRepo.findAll()));
    }

    @ParameterizedTest
    @CsvSource({"true, true", "false, false"})
    void testRegionGroupQualities__differentProblemCreationTime(boolean problemCreatedLessThanMonthAfterRecheck, boolean isGroupProblem) {
        boolean regionFromGroup = true;
        createProblem(ProblemStatus.APPROVED, problemCreatedLessThanMonthAfterRecheck, regionFromGroup);
        flushAndClear();

        assertEquals(isGroupProblem, containsProblemRegionGroup(regionGroupQualityRepo.findAll()));
    }

    @ParameterizedTest
    @CsvSource({"true, true", "false, false"})
    void testRegionGroupQualities__differentRegionsWithProblem(boolean regionFromGroup, boolean isGroupProblem) {
        boolean problemCreatedLessThanMonthAfterRecheck = true;
        createProblem(ProblemStatus.APPROVED, problemCreatedLessThanMonthAfterRecheck, regionFromGroup);
        flushAndClear();

        assertEquals(isGroupProblem, containsProblemRegionGroup(regionGroupQualityRepo.findAll()));
    }

    private void createProblem(ProblemStatus status, boolean problemCreatedLessThanMonthAfterRecheck, boolean regionFromGroup) {
        var problemCreationTime = problemCreatedLessThanMonthAfterRecheck
                ? REGION_GROUP_APPROVED_TIME.plusDays(15)
                : REGION_GROUP_APPROVED_TIME.plusDays(35);
        var problem = createProblem(SHOP_ID, TICKET_GEN_ID, ProblemTypeId.BAD_DELIVERY_DATE, status, problemCreationTime);

        LocalDeliveryProblem deliveryProblem = new LocalDeliveryProblem();
        deliveryProblem.setReportOption(createLocalDeliveryOption());
        deliveryProblem.setAssessorOption(createLocalDeliveryOption());

        problemManager.createDeliveryProblemOptionsDetails(
                problem, regionFromGroup ? (long) Regions.MOSCOW : (long) Regions.MOSCOW - 1, List.of(deliveryProblem)
        );
    }

    private static LocalDeliveryOption createLocalDeliveryOption() {
        LocalDeliveryOption o = new LocalDeliveryOption();
        o.setCurrency(Currency.RUR);
        o.setCost(new BigDecimal(100));
        o.setDayFrom(2);
        o.setDayTo(3);
        o.setOrderBefore(24);
        return o;
    }

    private static boolean containsProblemRegionGroup(Collection<RegionGroupQuality> regionGroupQualities) {
        return StreamEx.of(regionGroupQualities)
                .findAny(RegionGroupQuality::isGroupProblem)
                .isPresent();
    }
}
