package ru.yandex.market.abo.logbroker.hidings

import Market.DataCamp.DataCampExplanation
import Market.DataCamp.DataCampOfferMeta
import Market.DataCamp.DataCampValidationResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.yandex.market.abo.api.entity.offer.hidden.blue.BlueOfferHidingReason
import ru.yandex.market.abo.core.export.hidden.snapshot.blue.BlueHiddenOfferSnapshot
import java.time.LocalDateTime

class DatacampMessageForHidingsConverterTest {

    @Test
    fun `convert shop_id + shop_sku`() {
        val blueHiddenOfferSnapshot = BlueHiddenOfferSnapshot().apply {
            hidingRuleId = 1
            shopId = 2
            businessId = 3
            shopSku = "5"
            publicComment = "public comment"
            hidingReason = BlueOfferHidingReason.MANUALLY_HIDDEN
            deleted = false
            creationTime = LocalDateTime.now()
        }

        val datacampMessage = DatacampMessageForHidingsConverter.convert(blueHiddenOfferSnapshot)

        val datacampOffer = datacampMessage.unitedOffersList.first().offerList.first().serviceMap.values.first()
        assertEquals(datacampOffer.identifiers.offerId, blueHiddenOfferSnapshot.shopSku)
        assertEquals(datacampOffer.identifiers.shopId, Math.toIntExact(blueHiddenOfferSnapshot.shopId))
        assertEquals(datacampOffer.identifiers.businessId, Math.toIntExact(blueHiddenOfferSnapshot.businessId))
        assertEquals(datacampOffer.meta.rgb, DataCampOfferMeta.MarketColor.BLUE)
        assertEquals(datacampOffer.meta.scope, DataCampOfferMeta.OfferScope.SERVICE)

        val datacampStatus = datacampOffer.status.disabledList.first()
        assertEquals(datacampStatus.flag, !blueHiddenOfferSnapshot.deleted)
        assertEquals(datacampStatus.meta.source, DataCampOfferMeta.DataSource.MARKET_ABO_SHOP_SKU)

        val datacampVerdict = datacampOffer.resolution.bySourceList.first().verdictList.first().resultsList.first()
        assertEquals(datacampVerdict.isBanned, !blueHiddenOfferSnapshot.deleted)
        assertEquals(datacampVerdict.aboReason, DataCampValidationResult.AboReason.MANUALLY_HIDDEN)

        val datacampVerdictMessage = datacampVerdict.messagesList.first()
        assertEquals(datacampVerdictMessage.namespace, "ABO")
        assertEquals(datacampVerdictMessage.level, DataCampExplanation.Explanation.Level.ERROR)
        assertEquals(datacampVerdictMessage.code, DataCampValidationResult.AboReason.MANUALLY_HIDDEN.name)

        val datacampVerdictMessageParams = datacampVerdictMessage.paramsList.first()
        assertEquals(datacampVerdictMessageParams.name, DatacampMessageForHidingsConverter.PARAM_NAME_PUBLIC_COMMENT)
        assertEquals(datacampVerdictMessageParams.value, blueHiddenOfferSnapshot.publicComment)

        val datacampBasicOffer = datacampMessage.unitedOffersList.first().offerList.first().basic
        assertEquals(datacampBasicOffer.identifiers.offerId, blueHiddenOfferSnapshot.shopSku)
        assertEquals(datacampBasicOffer.identifiers.businessId, Math.toIntExact(blueHiddenOfferSnapshot.businessId))
    }

    @Test
    fun `convert msku + shop_id`() {
        val blueHiddenOfferSnapshot = BlueHiddenOfferSnapshot().apply {
            hidingRuleId = 1
            shopId = 2
            businessId = 3
            marketSku = 4
            publicComment = "public comment"
            hidingReason = BlueOfferHidingReason.MANUALLY_HIDDEN
            deleted = false
            creationTime = LocalDateTime.now()
        }

        val datacampMessage = DatacampMessageForHidingsConverter.convert(blueHiddenOfferSnapshot)

        val datacampMsku = datacampMessage.marketSkus.mskuList.first()
        assertEquals(datacampMsku.id, blueHiddenOfferSnapshot.marketSku)

        val datacampHidingReason = datacampMsku.status.aboShopStatusMap.values.first()
        assertEquals(datacampHidingReason.meta.source, DataCampOfferMeta.DataSource.MARKET_ABO_MSKU_SHOP)
        assertEquals(datacampHidingReason.reason, DataCampValidationResult.AboReason.MANUALLY_HIDDEN)
    }

    @Test
    fun `convert msku`() {
        val blueHiddenOfferSnapshot = BlueHiddenOfferSnapshot().apply {
            hidingRuleId = 1
            marketSku = 4
            publicComment = "public comment"
            hidingReason = BlueOfferHidingReason.MANUALLY_HIDDEN
            deleted = false
            creationTime = LocalDateTime.now()
        }

        val datacampMessage = DatacampMessageForHidingsConverter.convert(blueHiddenOfferSnapshot)

        val datacampMsku = datacampMessage.marketSkus.mskuList.first()
        assertEquals(datacampMsku.id, blueHiddenOfferSnapshot.marketSku)

        val datacampHidingReason = datacampMsku.status.aboStatus
        assertEquals(datacampHidingReason.meta.source, DataCampOfferMeta.DataSource.MARKET_ABO_MSKU)
        assertEquals(datacampHidingReason.reason, DataCampValidationResult.AboReason.MANUALLY_HIDDEN)
    }
}
