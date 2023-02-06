package ru.yandex.market.logistics.mqm.service

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

@DisplayName("Тесты для ClaimOrdersParserImplTest")
class ClaimOrdersParserImplTest: AbstractContextualTest()  {

    private var claimOrdersParser = ClaimOrdersParserImpl()
    @Test
    @DisplayName("Удачный парсинг файла с заказами для необходимых полей ")
    fun failToParseFileWithoutRequiredField() {
        val inputStream = getFile("service/claim_orders_parser/valid_csv_claim_orders.csv")
        val claimOrders = claimOrdersParser.getOrders(inputStream, "text/csv")

        assertSoftly {
            claimOrders.isEmpty()
        }
    }


    @Test
    @DisplayName("Удачный парсинг файла с заказами для необходимых полей ")
    fun checkParsingValidFileWithOnlyRequiredFields() {
        val inputStream = getFile("service/claim_orders_parser/valid_csv_claim_orders.csv")
        val claimOrders = claimOrdersParser.getOrders(inputStream, "text/csv")

        assertSoftly {
            claimOrders.count() shouldBe 7
            claimOrders[0].orderId shouldBe "82419625"
            claimOrders[2].shipmentDate shouldBe "2021-11-15"
            claimOrders[3].cost shouldBe "1439.7"
            claimOrders[4].previousStatus shouldBe "возвратный заказ готов для передачи ИМ"
            claimOrders[5].deliveryService shouldBe "Склад возвратов и невыкупленных заказов Софьино"
        }
    }

    @Test
    @DisplayName("Удачный парсинг файла с заказами для всех полей ")
    fun checkParsingValidFile() {
        val inputStream = getFile("service/claim_orders_parser/valid_csv_claim_orders_all_fields.csv")
        val claimOrders = claimOrdersParser.getOrders(inputStream, "text/csv")

        assertSoftly {
            claimOrders.count() shouldBe 7
            claimOrders[0].orderId shouldBe "82419625"
            claimOrders[2].shipmentDate shouldBe "2021-11-15"
            claimOrders[3].cost shouldBe "1439.7"
            claimOrders[4].previousStatus shouldBe "возвратный заказ готов для передачи ИМ"
            claimOrders[5].deliveryService shouldBe "Склад возвратов и невыкупленных заказов Софьино"
            claimOrders[6].address shouldBe "Склад возвратов"
            claimOrders[1].legalPartnerName shouldBe "Юр имя"
            claimOrders[2].partnerSubtypeName shouldBe "Подтип имени"
            claimOrders[3].partnerName shouldBe "Имя партнера"
        }
    }

    private fun getFile(url: String): ByteArrayInputStream {
        val input = this.javaClass.classLoader.getResource(url)!!
        val file = File(input.toURI())

        var content: ByteArray? = null
        try {
            content = file.readBytes()
        } catch (e: IOException) {
        }

        return ByteArrayInputStream(content)
    }
}
