package ru.yandex.direct.core.entity.promoextension

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.promoextension.model.PromoExtension
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.testPromoExtension
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate
import ru.yandex.direct.model.KtModelChanges
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotNull
import java.time.LocalDate

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class PromoExtensionRepositoryTest {
    @Autowired
    private lateinit var promoExtensionRepository: PromoExtensionRepository

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun testSuccessfulAdd() {
        val promoToAdd = testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "суперпромо",
            href = "ya.ru",
            startDate = null,
            finishDate = null,
            statusModerate = PromoactionsStatusmoderate.Sending,
        )
        promoExtensionRepository.add(clientInfo.shard, clientInfo.clientId!!, listOf(promoToAdd))
        promoToAdd.promoExtensionId?.checkNotNull()
        val promosInDb = promoExtensionRepository.getByIds(clientInfo.shard, listOf(promoToAdd.promoExtensionId!!))
        promosInDb.checkEquals(listOf(promoToAdd))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAddWithNonNullIdExceptionThrown() {
        val promoToAdd = testPromoExtension(
            id = 123L,
            clientId = clientInfo.clientId!!,
            description = "суперпромо",
            href = "ya.ru",
            startDate = null,
            finishDate = null,
            statusModerate = PromoactionsStatusmoderate.Sending,
        )
        promoExtensionRepository.add(clientInfo.shard, clientInfo.clientId!!, listOf(promoToAdd))
    }

    @Test
    fun testSuccessfulUpdateHrefIsDeleted() {
        val promoToAdd = testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "суперпромо",
            href = "ya.ru",
            startDate = null,
            finishDate = null,
            statusModerate = PromoactionsStatusmoderate.Sending,
        )
        promoExtensionRepository.add(clientInfo.shard, clientInfo.clientId!!, listOf(promoToAdd))
        val ktModelChanges = KtModelChanges<Long, PromoExtension>(promoToAdd.promoExtensionId!!)
        ktModelChanges.process(PromoExtension::href, null)
        promoExtensionRepository.update(clientInfo.shard, listOf(ktModelChanges))
        val expectedPromoaction = promoToAdd.copy(href = null)
        val promosInDb = promoExtensionRepository.getByIds(clientInfo.shard, listOf(promoToAdd.promoExtensionId!!))
        promosInDb.checkEquals(listOf(expectedPromoaction))
    }

    @Test
    fun updateTwoPromoactionsBothUpdated() {
        val now = LocalDate.now()
        val firstPromoaction = testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "суперпромо1",
            href = "ya.ru",
            startDate = now,
            finishDate = now.plusDays(17),
            statusModerate = PromoactionsStatusmoderate.Sending,
        )
        val secondPromoaction = testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "суперпромо2",
            href = "yandex.ru",
            startDate = now.plusDays(11),
            finishDate = null,
            statusModerate = PromoactionsStatusmoderate.Sending,
        )
        promoExtensionRepository.add(clientInfo.shard, clientInfo.clientId!!, listOf(firstPromoaction, secondPromoaction))

        val ktModelChangesFirst = KtModelChanges<Long, PromoExtension>(firstPromoaction.promoExtensionId!!)
        ktModelChangesFirst.process(PromoExtension::startDate, null)
        ktModelChangesFirst.process(PromoExtension::description, "суперпромо11")
        val ktModelChangesSecond = KtModelChanges<Long, PromoExtension>(secondPromoaction.promoExtensionId!!)
        ktModelChangesSecond.process(PromoExtension::finishDate, now.plusDays(16))
        ktModelChangesSecond.process(PromoExtension::href, "yandex.com")
        promoExtensionRepository.update(clientInfo.shard, listOf(ktModelChangesFirst, ktModelChangesSecond))

        val expectedPromoactionFirst = firstPromoaction.copy(
            startDate = null,
            description = "суперпромо11",
        )
        val expectedPromoactionSecond = secondPromoaction.copy(
            finishDate = now.plusDays(16),
            href = "yandex.com",
        )
        val promosInDb = promoExtensionRepository.getByIds(clientInfo.shard,
            listOf(firstPromoaction.promoExtensionId!!, secondPromoaction.promoExtensionId!!)
        ).toSet()
        promosInDb.checkEquals(setOf(expectedPromoactionFirst, expectedPromoactionSecond))
    }

    @Test
    fun addTwoPromoactionsDeleteFirstOnlyFirstDeleted() {
        val now = LocalDate.now()
        val firstPromoaction = testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "суперпромо1",
            href = "ya.ru",
            startDate = now,
            finishDate = now.plusDays(17),
            statusModerate = PromoactionsStatusmoderate.Sending,
        )
        val secondPromoaction = testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "суперпромо2",
            href = "yandex.ru",
            startDate = now.plusDays(17),
            finishDate = null,
            statusModerate = PromoactionsStatusmoderate.Sending,
        )
        promoExtensionRepository.add(clientInfo.shard, clientInfo.clientId!!, listOf(firstPromoaction, secondPromoaction))

        promoExtensionRepository.delete(clientInfo.shard, setOf(firstPromoaction.promoExtensionId!!))

        val promosInDb = promoExtensionRepository.getByIds(clientInfo.shard,
            listOf(firstPromoaction.promoExtensionId!!, secondPromoaction.promoExtensionId!!)
        )
        promosInDb.size.checkEquals(1)
        promosInDb[0].promoExtensionId.checkEquals(secondPromoaction.promoExtensionId)
    }
}
