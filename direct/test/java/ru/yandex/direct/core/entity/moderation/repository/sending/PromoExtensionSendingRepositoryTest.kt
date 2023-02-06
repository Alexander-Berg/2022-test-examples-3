package ru.yandex.direct.core.entity.moderation.repository.sending

import org.apache.commons.lang3.tuple.Pair
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.promoextension.PromoExtensionRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.defaultPromoExtension
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestModerationRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.Ready
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.Sending
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.Yes
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.test.utils.TestUtils.assumeThat

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class PromoExtensionSendingRepositoryTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var testModerationRepository: TestModerationRepository

    @Autowired
    private lateinit var promoExtensionRepository: PromoExtensionRepository

    @Autowired
    private lateinit var promoExtensionSendingRepository: PromoExtensionSendingRepository

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun testUpdateMysqlDataBeforeSending() {
        val readyPromoExtensionToAdd = defaultPromoExtension(clientInfo.clientId!!)
        readyPromoExtensionToAdd.statusModerate = Ready
        val readyPromoExtensionInfo = steps.promoExtensionSteps().createPromoExtension(clientInfo, readyPromoExtensionToAdd)
        val yesStatusPromoExtensionToAdd = defaultPromoExtension(clientInfo.clientId!!)
        yesStatusPromoExtensionToAdd.statusModerate = Yes
        val yesStatusPromoExtensionInfo = steps.promoExtensionSteps().createPromoExtension(clientInfo,
            yesStatusPromoExtensionToAdd)
        assumeThat { sa ->
            sa.assertThat(readyPromoExtensionInfo.promoExtensionId < yesStatusPromoExtensionInfo.promoExtensionId).isTrue
        }

        val objectsForModeration = promoExtensionSendingRepository.loadObjectForModeration(
            listOf(readyPromoExtensionInfo.promoExtensionId, yesStatusPromoExtensionInfo.promoExtensionId),
            dslContextProvider.ppc(clientInfo.shard).configuration(),
        ).sortedBy { it.promoExtensionId }
        promoExtensionSendingRepository.updateMysqlDataBeforeSending(
            dslContextProvider.ppc(clientInfo.shard).configuration(),
            listOf(Pair.of(objectsForModeration[0], 17L), Pair.of(objectsForModeration[1], 25L))
        )

        val promosById = promoExtensionRepository.getByIds(clientInfo.shard,
            listOf(readyPromoExtensionInfo.promoExtensionId, yesStatusPromoExtensionInfo.promoExtensionId)
        ).associateBy { it.promoExtensionId }
        assumeThat { sa -> sa.assertThat(promosById.entries).hasSize(2) }
        softly {
            assertThat(promosById[readyPromoExtensionInfo.promoExtensionId]!!.statusModerate, `is`(Sending))
            assertThat(promosById[yesStatusPromoExtensionInfo.promoExtensionId]!!.statusModerate, `is`(Yes))
            assertThat(
                testModerationRepository.getPromoExtensionVersion(clientInfo.shard, readyPromoExtensionInfo.promoExtensionId),
                `is`(17L)
            )
            assertThat(
                testModerationRepository.getPromoExtensionVersion(clientInfo.shard, yesStatusPromoExtensionInfo.promoExtensionId),
                `is`(25L)
            )
        }
    }
}
