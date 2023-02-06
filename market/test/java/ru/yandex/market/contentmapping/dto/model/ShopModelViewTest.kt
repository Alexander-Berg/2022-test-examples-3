package ru.yandex.market.contentmapping.dto.model

import org.junit.Assert
import org.junit.Test
import ru.yandex.market.contentmapping.dto.model.mboc.ApprovedSkuMapping
import ru.yandex.market.contentmapping.dto.model.mboc.OfferTicketProcessingStatus
import ru.yandex.market.contentmapping.dto.model.mboc.ProcessingStatus
import ru.yandex.market.contentmapping.dto.model.mboc.SkuType
import ru.yandex.market.contentmapping.services.ShopModelViewService.Companion.guessShopModelProcessingStatus
import ru.yandex.market.contentmapping.testdata.TestDataUtils.testShopModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ShopModelViewTest {
	@Test
	fun testShopModelProcessingStatus() {
		//Не экспортировано, и не обновлено из КИ
		var m = testShopModel().copy(
				exported = null,
				updated = dateTimeOf("2020-07-29 11:30:40"),
				mbocSynced = null,
		)
		Assert.assertEquals(ShopModelProcessingStatus.NOT_SENT, guessShopModelProcessingStatus(m))

		//Экспортировано, но мы еще не обновляли статус из КИ
		m = testShopModel().copy(
				exported = dateTimeOf("2020-08-01 11:30:40"),
				exportTicketId = null,
				mbocSynced = null,
		)
		Assert.assertEquals(ShopModelProcessingStatus.NOT_SENT, guessShopModelProcessingStatus(m))
		m = testShopModel().copy(
				exported = dateTimeOf( "2020-08-01 09:59:00"),
				exportTicketId = 1L,
				mbocSynced = dateTimeOf("2020-08-01 11:00:00"),
		)
		Assert.assertEquals(ShopModelProcessingStatus.UNKNOWN, guessShopModelProcessingStatus(m))
		m = testShopModel().copy(
				exported = dateTimeOf("2020-08-01 10:01:00"),
				exportTicketId = 1L,
				mbocSynced = dateTimeOf("2020-08-01 11:00:00"),
		)
		Assert.assertEquals(ShopModelProcessingStatus.PROCESSING, guessShopModelProcessingStatus(m))

		//Экспортировано задолго до обновления статуса из КИ, статус оффера пуст
		m = testShopModel().copy(
				exported = dateTimeOf("2020-08-01 11:20:39"),
				exportTicketId = 1L,
				mbocSynced = dateTimeOf("2020-08-01 12:20:40"),
				offerTicketProcessingStatus = null,
		)
		Assert.assertEquals(ShopModelProcessingStatus.UNKNOWN, guessShopModelProcessingStatus(m))

		//Экспортировано задолго до обновления статуса из КИ, статус оффера PROCESSING
		m = testShopModel().copy(
				exported = dateTimeOf("2020-08-01 11:50:39"),
				exportTicketId = 1L,
				mbocSynced = dateTimeOf( "2020-08-01 12:20:40"),
				offerTicketProcessingStatus = OfferTicketProcessingStatus.PROCESSING,
		)
		Assert.assertEquals(ShopModelProcessingStatus.PROCESSING, guessShopModelProcessingStatus(m))

		//Добавлено в очередь на экспорт, статус оффера PROCESSING
		m = testShopModel().copy(
				exported = dateTimeOf("2020-08-01 11:50:39"),
				exportTicketId = 1L,
				mbocSynced = dateTimeOf( "2020-08-01 12:20:40"),
				offerTicketProcessingStatus = OfferTicketProcessingStatus.PROCESSING,
		)
		Assert.assertEquals(ShopModelProcessingStatus.WILL_BE_SENT, guessShopModelProcessingStatus(
				m,
				dateTimeOf( "2020-08-01 12:20:40")
		))

		//Экспортировано задолго до обновления статуса из КИ, статус оффера COMPLETE
		m = testShopModel().copy(
				exported = dateTimeOf("2020-08-01 11:50:39"),
				exportTicketId = 1L,
				mbocSynced = dateTimeOf("2020-08-01 12:20:40"),
				offerTicketProcessingStatus = OfferTicketProcessingStatus.COMPLETE,
		)
		Assert.assertEquals(ShopModelProcessingStatus.SUCCESS, guessShopModelProcessingStatus(m))

		//Экспортировано задолго до обновления статуса из КИ, статус оффера FAILURE
		m = testShopModel().copy(
				exported = dateTimeOf("2020-08-01 11:50:39"),
				exportTicketId = 1L,
				mbocSynced = dateTimeOf("2020-08-01 12:20:40"),
				offerTicketProcessingStatus = OfferTicketProcessingStatus.FAILURE,
		)
		Assert.assertEquals(ShopModelProcessingStatus.FAILURE, guessShopModelProcessingStatus(m))

        //Оффер в NEED_CONTENT с approved_mapping -> FAST_SKU
        m = testShopModel().copy(
            approvedSkuMapping = ApprovedSkuMapping(1L, SkuType.TYPE_MARKET),
            processingStatus = ProcessingStatus.NEED_CONTENT,
            datacampVersion = 1,
            exported = LocalDateTime.now()
        )
        Assert.assertEquals(ShopModelProcessingStatus.SUCCESS, guessShopModelProcessingStatus(m))
	}

    private fun dateTimeOf(dateTime: String): LocalDateTime {
		val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		return LocalDateTime.parse(dateTime, formatter)
	}

	companion object {
		private const val SHOP_SKU = "shopSku"

		fun guessShopModelProcessingStatus (
				s: ShopModel,
				addedToExport: LocalDateTime? = null
		): ShopModelProcessingStatus {
			return guessShopModelProcessingStatus(
					s.exportTicketId,
					s.offerTicketProcessingStatus,
					s.mbocSynced,
					s.exported,
					addedToExport,
					s.offerTicketId,
					s.processingStatus,
					s.datacampVersion,
					isValid = true,
                    hasMapping = s.approvedSkuMapping?.let { true } ?: false
            )
		}
	}
}
