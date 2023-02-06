package ru.yandex.direct.logicprocessor.processors.bsexport.strategy.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.strategy.AttributionModel
import ru.yandex.adv.direct.strategy.NDSHistory
import ru.yandex.adv.direct.strategy.Strategy
import ru.yandex.adv.direct.strategy.StrategyType
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.client.model.ClientNds
import ru.yandex.direct.core.entity.client.service.ClientNdsService
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.Percent
import java.math.BigDecimal
import java.time.LocalDate

private const val WALLET_EXPORT_ID = 11L
private const val WALLET_ORDER_ID = 12L

class StrategyCommonFieldsHandlerTest {

    private fun defaultClientNdsService(): ClientNdsService =
        mock {
            on { massGetClientNdsHistory(any()) } doReturn mapOf(
                Pair(
                    CLIENT_ID,
                    (listOf(
                        ClientNds()
                            .withClientId(CLIENT_ID)
                            .withDateFrom(LocalDate.ofEpochDay(0))
                            .withDateTo(LocalDate.ofEpochDay(0))
                            .withNds(Percent.fromPercent(BigDecimal.valueOf(50)))
                    ))
                )
            )
        }

    private fun defaultCampaignTypedRepository(): CampaignTypedRepository =
        mock {
            on { getTypedCampaignsMap(1, listOf(WALLET_EXPORT_ID)) } doReturn mapOf(
                Pair(
                    WALLET_EXPORT_ID,
                    WalletTypedCampaign()
                        .withOrderId(WALLET_ORDER_ID)
                )
            )
        }

    private fun handler(
        clientNdsService: ClientNdsService = defaultClientNdsService(),
        campaignTypedRepository: CampaignTypedRepository = defaultCampaignTypedRepository(),
    ): StrategyCommonFieldsHandler {
        return StrategyCommonFieldsHandler(
            clientNdsService,
            campaignTypedRepository,
        )
    }

    private val defaultStrategy = DefaultManualStrategy()
        .withId(1L)
        .withWalletId(WALLET_EXPORT_ID)
        .withClientId(CLIENT_ID)
        .withAttributionModel(StrategyAttributionModel.LAST_YANDEX_DIRECT_CLICK)
        .withStatusArchived(true)
        .withType(StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER)

    private val defaultExpectedStrategy = Strategy.newBuilder()
        .setClientId(CLIENT_ID)
        .setWalletExportId(WALLET_EXPORT_ID)
        .setAgencyId(AGENCY_CLIENT_ID)
        .setAttributionModel(AttributionModel.LAST_YANDEX_DIRECT_CLICK.number)
        .setIsArchived(true)
        .setType(StrategyType.AUTOBUDGET_AVG_CPA_PER_FILTER.number)
        .setCurrencyId(CurrencyCode.RUB.currency.isoNumCode.toLong())
        .setNdsHistory(
            NDSHistory.newBuilder().addAllNds(
                listOf(
                    ru.yandex.adv.direct.nds.Nds.newBuilder()
                        .setDateFrom(0)
                        .setDateTo(0)
                        .setNds(500000)
                        .build()
                )
            )
        )
        .setWalletOrderId(WALLET_ORDER_ID)
        .buildPartial()

    @Test
    fun `common fields are mapped to proto correctly`() = StrategyHandlerAssertions.assertProtoFilledCorrectly(
        handler(),
        strategy = defaultStrategy,
        expectedProto = defaultExpectedStrategy,
    )
}
