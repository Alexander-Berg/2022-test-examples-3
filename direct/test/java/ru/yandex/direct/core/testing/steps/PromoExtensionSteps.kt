package ru.yandex.direct.core.testing.steps

import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.promoextension.PromoExtensionRepository
import ru.yandex.direct.core.entity.promoextension.model.PromoExtension
import ru.yandex.direct.core.testing.data.testPromoExtension
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.PromoExtensionInfo
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate

@Component
class PromoExtensionSteps(
    private val promoExtensionRepository: PromoExtensionRepository,
) {
    fun createDefaultPromoExtension(clientInfo: ClientInfo): PromoExtensionInfo {
        val promoExtension = testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "промоакция",
            href = "https://ya.ru",
            startDate = null,
            finishDate = null,
            statusModerate = PromoactionsStatusmoderate.Ready,
        )
        return createPromoExtension(clientInfo, promoExtension)
    }

    fun createPromoExtension(clientInfo: ClientInfo, promoExtension: PromoExtension): PromoExtensionInfo {
        promoExtensionRepository.add(clientInfo.shard, clientInfo.clientId!!, listOf(promoExtension))
        return PromoExtensionInfo(clientInfo, promoExtension)
    }
}
