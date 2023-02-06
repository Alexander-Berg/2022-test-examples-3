package ru.yandex.direct.core.entity.moderation.service.receiving.responses

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision
import ru.yandex.direct.core.entity.moderation.model.Verdict
import ru.yandex.direct.core.entity.moderation.model.promoextension.PromoExtensionModerationMeta
import ru.yandex.direct.core.entity.moderation.model.promoextension.PromoExtensionModerationResponse
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames
import ru.yandex.direct.core.entity.moderation.service.receiving.PromoExtensionModerationReceivingService
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed
import ru.yandex.direct.core.entity.promoextension.PromoExtensionRepository
import ru.yandex.direct.core.entity.promoextension.model.PromoExtension
import ru.yandex.direct.core.entity.promoextension.model.PromoExtensionUnit.RUB
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestModerationRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsPrefix.from
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.Sent
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.profit
import ru.yandex.direct.dbutil.model.ClientId
import java.time.LocalDate
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ReceivePromoExtensionModerationResponseTest
    : AbstractModerationResponseTest<PromoExtensionModerationMeta, Verdict, PromoExtensionModerationResponse>() {

    companion object {
        private const val DEFAULT_VERSION = 1L
        private val DEFAULT_REASONS =
            listOf(ModerationReasonDetailed().withId(2L), ModerationReasonDetailed().withId(3L))
    }

    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var promoExtensionRepository: PromoExtensionRepository
    @Autowired
    private lateinit var testModerationRepository: TestModerationRepository
    @Autowired
    private lateinit var promoExtensionModerationReceivingService: PromoExtensionModerationReceivingService

    private var shard = 0
    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var defaultPromoExtensionId: Long = -1

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        shard = clientInfo.shard
        clientId = clientInfo.clientId!!

        defaultPromoExtensionId = createPromoExtension(DEFAULT_VERSION)
    }

    override fun getShard() = shard

    override fun checkInDbForId(id: Long, response: PromoExtensionModerationResponse) {
        val promoExtensions = promoExtensionRepository.getByIds(shard, setOf(id))
        assertThat(promoExtensions).hasSize(1)
        assertThat(promoExtensions[0].statusModerate).isEqualTo(
            PromoactionsStatusmoderate.valueOf(response.result!!.verdict.toString()))
    }

    override fun getReceivingService() = promoExtensionModerationReceivingService

    override fun createObjectInDb(version: Long) = createPromoExtension(version)

    override fun getObjectType() = ModerationObjectType.PROMO_EXTENSION

    override fun createResponse(
        id: Long,
        status: ModerationDecision?,
        language: String?,
        version: Long,
        flags: MutableMap<String, String>?,
        minusRegions: MutableList<Long>?,
        clientInfo: ClientInfo,
        reasons: MutableList<ModerationReasonDetailed>?
    ): PromoExtensionModerationResponse {
        return PromoExtensionModerationResponse().apply {
            service = ModerationServiceNames.DIRECT_SERVICE
            type = ModerationObjectType.PROMO_EXTENSION
            meta = PromoExtensionModerationMeta(id, clientInfo.uid, clientInfo.clientId!!.asLong(), version)
            result = Verdict().apply {
                verdict = status
                if (status == ModerationDecision.No) {
                    this.reasons = DEFAULT_REASONS.map { it.id }
                    detailedReasons = DEFAULT_REASONS
                }
            }
        }
    }

    override fun getDefaultObjectId() = defaultPromoExtensionId

    override fun getDefaultObjectClientInfo() = clientInfo

    override fun deleteDefaultObjectVersion() {}

    private fun createPromoExtension(version: Long): Long {
        val promoExtension = PromoExtension(
            promoExtensionId = null,
            clientId = clientId,
            type = profit,
            prefix = from,
            amount = 15,
            unit = RUB,
            description = "На Смартфоны",
            finishDate = LocalDate.of(2021, 12, 7),
            startDate = null,
            statusModerate = Sent,
            href = null,
        )
        promoExtensionRepository.add(shard, clientId, listOf(promoExtension))
        testModerationRepository.setPromoExtensionVersion(shard, promoExtension.promoExtensionId!!, version)
        return promoExtension.promoExtensionId!!
    }

    override fun getDefaultVersion() = DEFAULT_VERSION
}
