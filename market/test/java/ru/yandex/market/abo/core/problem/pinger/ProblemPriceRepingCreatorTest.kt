package ru.yandex.market.abo.core.problem.pinger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.abo.core.pinger.model.MpGeneratorType
import ru.yandex.market.abo.core.problem.model.ProblemStatus
import ru.yandex.market.abo.core.ticket.AbstractCoreHierarchyTest
import ru.yandex.market.abo.gen.HypothesisRepo
import ru.yandex.market.abo.util.FakeUsers

/**
 * @author agavrikov
 */
class ProblemPriceRepingCreatorTest @Autowired constructor(
    var problemPriceRepingTicketCreator: ProblemPriceRepingTicketCreator,
    var hypothesisRepo: HypothesisRepo,
    var jdbcTemplate: JdbcTemplate
) : AbstractCoreHierarchyTest() {

    private val SHOP_ID = 774L
    private val SOURCE_ID = 12345L

    @Test
    fun testGetTickets() {
        jdbcTemplate.update(
                "INSERT INTO pinger_content_task (id, url, creation_time, gen_id) " +
                "VALUES (?, 'url', now(), ?)",
                SOURCE_ID, MpGeneratorType.PROBLEM_PRICE_REPING.getId()
        )
        var pingerProblemType = PingerProblemType.PRICE;
        createProblem(SHOP_ID, pingerProblemType.getGenId(), pingerProblemType.getId(), ProblemStatus.APPROVED,
                tagService.createTag(FakeUsers.PRICE_CONTENT_PINGER.getId()), SOURCE_ID)
        entityManager.flush()

        var tickets = problemPriceRepingTicketCreator.getTickets()
        assertFalse(tickets.isEmpty())
        var ticket = tickets.get(0)

        assertEquals(SHOP_ID, ticket.getShopId())

        assertEquals(ProblemPriceRepingTicketCreator.TICKET_TYPE, ticket.getType())

        var hypIds = hypothesisRepo.findAll().map { h -> h.id }.joinToString(",")
        assertEquals(ProblemPriceRepingTicketCreator.addSynopsis(hypIds), ticket.getSynopsis())
        assertEquals(FakeUsers.PRICE_CONTENT_PINGER.getId(), ticket.userId)
    }
}
