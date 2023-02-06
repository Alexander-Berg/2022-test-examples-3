package ru.yandex.market.abo.gen;

import java.time.LocalDateTime;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.abo.core.ticket.AbstractCoreHierarchyTest;
import ru.yandex.market.abo.gen.model.Hypothesis;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 * created on 19.06.17.
 */
public class HypothesisServiceTest extends AbstractCoreHierarchyTest {
    @Autowired
    private HypothesisService hypothesisService;
    @Autowired
    private HypothesisRepo hypothesisRepo;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * checks that all needed generator spring beans are present
     */
    @Test
    public void initGenerators() {
        hypothesisService.instantiateTicketBuilders();
    }

    @Test
    public void createHypothesis() {
        Hypothesis hypothesis = new Hypothesis(774, 0, 1, null, 0, 0, null);
        long hypId = hypothesisService.createHypothesis(hypothesis);
        Hypothesis dbHyp = hypothesisService.loadHypothesis(hypId);
        assertEquals(hypothesis.getId(), dbHyp.getId());
        assertEquals(hypothesis.getShopId(), dbHyp.getShopId());
        assertEquals(hypothesis.getRegionId(), dbHyp.getRegionId());
        flushAndClear();
    }

    @Test
    @SuppressWarnings("unused")
    public void deleteHypothesisWithoutTicketsTest() {
        var now = LocalDateTime.now();
        var weekAgo = now.minusWeeks(1);
        int deleteOlderThanDays = 5;

        long newWithTicket = createHyp(now, true);
        long newWithoutTicket = createHyp(now, false);
        long oldWithTicket = createHyp(weekAgo, true);
        long oldWithoutTicket = createHyp(weekAgo, false);

        hypothesisService.deleteHypothesisWithoutTickets(deleteOlderThanDays);

        var hypIds = StreamEx.of(hypothesisRepo.findAll())
                .map(Hypothesis::getId)
                .toSet();
        assertEquals(Set.of(newWithTicket, newWithoutTicket, oldWithTicket), hypIds);
    }

    private long createHyp(LocalDateTime creationTime, boolean withTicket) {
        var hypId = withTicket
                ? createTicket(0, 0)
                : createHypothesis(0, 0).getId();
        flushAndClear();
        jdbcTemplate.update("UPDATE hypothesis SET create_time = ? WHERE id = ?", creationTime, hypId);
        return hypId;
    }
}
