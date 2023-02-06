package ru.yandex.market.abo.core.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.ticket.model.LocalDeliveryProblem;
import ru.yandex.market.abo.gen.HypothesisService;
import ru.yandex.market.abo.gen.model.GenId;
import ru.yandex.market.abo.gen.model.Hypothesis;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalDeliveryPriceServiceTest extends AbstractCoreHierarchyTest {
    private static final int UNKNOWN_PARAM = -1;
    private static final Random rand = new Random();
    private static final long SHOP_ID = -1;
    private static final int CURRENT_HOUR = 9;
    private static final long REGION_WITH_OFFSET_0 = 0;
    private static final long REGION_WITH_OFFSET_2 = 1;

    @Autowired
    private LocalDeliveryPriceService localDeliveryPriceService;

    @Test
    void testStoreLocalDelivery() {
        List<LocalDeliveryProblem> problemOptionsDetails = new ArrayList<>();
        LocalDeliveryProblem p = new LocalDeliveryProblem();
        p.setReportOption(createLocalDeliveryOption(100, 2, 3, 24));
        p.setAssessorOption(createLocalDeliveryOption(200, 2, 4, 24));
        problemOptionsDetails.add(p);

        p = new LocalDeliveryProblem();
        p.setReportOption(createLocalDeliveryOption(400, 0, 0, 14));
        p.setAssessorOption(createLocalDeliveryOption(500, UNKNOWN_PARAM, UNKNOWN_PARAM, UNKNOWN_PARAM));
        problemOptionsDetails.add(p);

        Problem problem = createProblem();
        localDeliveryPriceService.storeLocalDeliveryProblem(problemOptionsDetails, problem);

        List<LocalDeliveryProblem> foundProblems = localDeliveryPriceService
                .loadLocalDeliveryByProblemId(problem.getId());
        for (int i = 0; i < foundProblems.size(); i++) {
            LocalDeliveryProblem fp = foundProblems.get(i);
            LocalDeliveryProblem pp = problemOptionsDetails.get(i);
            assertEquals(pp.getReportOption().getCost(), fp.getReportOption().getCost());
            assertEquals(pp.getReportOption().getDayFrom(), fp.getReportOption().getDayFrom());
        }
    }

    @Test
    void loadHypIdsForCancelByOrderBeforeTest() {
        int timeZoneOffset = CURRENT_HOUR - LocalDateTime.now(ZoneOffset.UTC).getHour();
        jdbcTemplate.update("" +
                        "INSERT INTO region VALUES " +
                        "   (?, -1, 'name', 'name', 0, NULL, ?, NULL, '', FALSE, 0), " +
                        "   (?, -1, 'name', 'name', 0, NULL, ?, NULL, '', FALSE, 0)",
                REGION_WITH_OFFSET_0, timeZoneOffset,
                REGION_WITH_OFFSET_2, timeZoneOffset + 2
        );

        long ticketOffset0 = createTicket(REGION_WITH_OFFSET_0);
        long ticketOffset2 = createTicket(REGION_WITH_OFFSET_2);
        entityManager.flush();

        int orderBefore = LocalDateTime.now(ZoneOffset.ofHours(timeZoneOffset)).plusHours(2).getHour();

        jdbcTemplate.update("" +
                        "INSERT INTO core_offer_delivery (id, hyp_id, order_before) VALUES " +
                        "   (0, ?, ?), " +
                        "   (1, ? ,?)",
                ticketOffset0, orderBefore, ticketOffset2, orderBefore
        );

        List<Long> ticketsToCancel = localDeliveryPriceService.loadHypIdsForCancelByOrderBefore();
        assertEquals(Collections.singletonList(ticketOffset2), ticketsToCancel);
    }

    private long createTicket(long regionId) {
        Hypothesis hypothesis = new Hypothesis(SHOP_ID, 0, GenId.DELIVERY_TODAY, "", 1, 0, "");
        hypothesis.setRegionId(regionId);
        hypothesisService.save(hypothesis);
        return createTicket(hypothesis);
    }

    private static LocalDeliveryOption createLocalDeliveryOption(int cost, int dayFrom, int dayTo, int orderBefore) {
        LocalDeliveryOption o = new LocalDeliveryOption();
        o.setCurrency(Currency.RUR);
        o.setCost(new BigDecimal(cost));
        o.setDayFrom(dayFrom);
        o.setDayTo(dayTo);
        o.setOrderBefore(orderBefore);
        return o;
    }

    private static Problem createProblem() {
        return Problem.newBuilder()
                .ticketId(rand.nextLong())
                .id(rand.nextLong())
                .status(ProblemStatus.NEW)
                .build();
    }
}
