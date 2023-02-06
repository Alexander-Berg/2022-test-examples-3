package ru.yandex.market.replenishment.autoorder.repository

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbo.excel.ExcelFileAssertions
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.model.Currency
import ru.yandex.market.replenishment.autoorder.model.VAT
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.TenderSupplierResponse
import ru.yandex.market.replenishment.autoorder.repository.postgres.TenderSupplierResponseRepository
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin
import java.time.LocalDate
import java.time.LocalDateTime

@AutoConfigureMockMvc
@WithMockLogin
class TenderSupplierResponseRepositoryTest : FunctionalTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var repo: TenderSupplierResponseRepository

    @Test
    @DbUnitDataSet(before = ["TenderSupplierResponseRepositoryTest_getSupplierResponses.before.csv"])
    fun getSupplierResponses() {
        val results = repo.getSupplierResponses(3L)
        assertThat(results, hasSize(1))
        val (supplierId, supplierName, _, demand1pId, priceSpecJson) = results[0]
        assertEquals(300L, demand1pId)
        assertEquals(1L, supplierId)
        assertEquals("'Поставщик №020'", supplierName)
        assertEquals(
            """{"id": "foo", "specs": [{"id": "spec1", "fio": "login1", "login": "login1", "mdsUrl": "url1"}], "sskus": ["010.3", "010.2"], "status": "APPROVED", "orderId": 42}""",
            priceSpecJson
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderSupplierResponseRepositoryTest_upsert.before.csv"],
        after = ["TenderSupplierResponseRepositoryTest_upsert.after.csv"]
    )
    fun testUpsert_insertsNewRow() {
        val createdAt = LocalDate.of(2021, 6, 23)
        repo.upsert(
            TenderSupplierResponse(
                2L, 10L, 100L, 500L, 199.99,
                "Item have to be inserted as new demandId",
                createdAt.atTime(13, 41, 0),
                false, Currency.USD, null, VAT.VAT_20
            )
        )
        repo.upsert(
            TenderSupplierResponse(
                1L, 20L, 100L, 600L, 99.99,
                "Item have to be inserted as new supplier_id",
                createdAt.atTime(13, 41, 1),
                false, Currency.RUB, null, VAT.VAT_20
            )
        )
        repo.upsert(
            TenderSupplierResponse(
                1L, 10L, 200L, 700L, 9.99,
                "Item have to be inserted as new msku",
                createdAt.atTime(13, 41, 2),
                false, Currency.EUR, null, VAT.VAT_20
            )
        )
        repo.upsert(
            TenderSupplierResponse(
                1L, 20L, 300L, 800L, 12.3,
                "Set not delete for update",
                createdAt.atTime(13, 16, 0),
                false, Currency.USD, "000042.42", VAT.VAT_20
            )
        )
    }

    @Test
    @DbUnitDataSet(before = ["TenderSupplierResponseRepositoryTest_getForDecisionTree.before.csv"])
    fun getForDecisionTree() {
        val createdAt = LocalDateTime.of(2021, 6, 23, 13, 16, 0)
        val results = repo.getForDecisionTree(1L, listOf(172L)).sortedBy { it.ssku }
        assertThat(results, hasSize(2))
        var item = results[0]
        assertEquals(12.3, item.price)
        assertEquals(1, item.items)
        assertEquals("Comment for 100 msku", item.comment)
        assertEquals(createdAt, item.createdAt)
        assertEquals(10, item.supplierId)
        assertEquals(Currency.RUB, item.currency)
        assertEquals(80.0, item.minSum)
        item = results[1]
        assertEquals(56.4, item.price)
        assertEquals(2, item.items)
        assertEquals("Comment for 300 msku", item.comment)
        assertEquals(createdAt, item.createdAt)
        assertEquals(20, item.supplierId)
        assertEquals(Currency.RUB, item.currency)
        assertEquals(45.0, item.minSum)
    }

    @Test
    @DbUnitDataSet(before = ["TenderSupplierResponseRepositoryTest_getAllForExportSupplierResponse.before.csv"])
    fun getAllForExportSupplierResponse() {
        val excelData = mockMvc.perform(get("/api/v1/tender/48564405/580523/excel"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsByteArray
        ExcelFileAssertions.assertThat(excelData)
            .containsValue(1, "Бренд", "Merck KGaA")
            .containsValue(1, "Категория 1", "Витамины и минералы")
            .containsValue(1, "Категория 2", "Аптека")
            .containsValue(1, "Категория 3", "Все товары")
            .containsValue(1, "Название товара", "Фемибион наталкер ll таб. п.о+капс")
            .containsValue(1, "MSKU", 228093607)
            .containsValue(1, "SSKU", "005926.27746")
            .containsValue(1, "Артикул", null)
            .containsValue(1, "Штрихкоды", "4027269231781")
            .containsValue(1, "Потребность", 224)
            .containsValue(1, "кол-во", 5)
            .containsValue(1, "цена", 5)
            .containsValue(1, "валюта", "RUB")
            .containsValue(
                1, """
     комментарий
     (здесь вы можете указать ваш комментарий по товару. Наличие других цветов, доступность и др.)
     """.trimIndent(), ""
            )
            .containsValue(2, "Бренд", "Внешторг Фарма")
            .containsValue(2, "Категория 1", "Витамины и минералы")
            .containsValue(2, "Категория 2", "Аптека")
            .containsValue(2, "Категория 3", "Все товары")
            .containsValue(2, "Название товара", "Алфавит Классик таб., 120 шт.")
            .containsValue(2, "MSKU", 101315343827L)
            .containsValue(2, "SSKU", "005926.22972")
            .containsValue(2, "Артикул", "4660014182152")
            .containsValue(2, "Штрихкоды", "4660014182152")
            .containsValue(2, "Потребность", 57)
            .containsValue(2, "кол-во", 5)
            .containsValue(2, "цена", 5)
            .containsValue(2, "валюта", "RUB")
            .containsValue(
                2, """
     комментарий
     (здесь вы можете указать ваш комментарий по товару. Наличие других цветов, доступность и др.)
     """.trimIndent(), ""
            )
            .hasSize(2)
    }

    @Test
    @DbUnitDataSet(before = ["TenderSupplierResponseRepositoryTest_getAllForExportSupplierResponse_withoutRecs.before.csv"])
    fun getAllForExportSupplierResponse_withoutRecs() {
        val excelData = mockMvc.perform(get("/api/v1/tender/48564405/580523/excel"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsByteArray
        ExcelFileAssertions.assertThat(excelData)
            .containsValue(1, "Бренд", "Merck KGaA")
            .containsValue(1, "Категория 1", "Витамины и минералы")
            .containsValue(1, "Категория 2", "Аптека")
            .containsValue(1, "Категория 3", "Все товары")
            .containsValue(1, "Название товара", "Фемибион наталкер ll таб. п.о+капс")
            .containsValue(1, "MSKU", 228093607)
            .containsValue(1, "SSKU", "005926.27746")
            .containsValue(1, "Артикул", null)
            .containsValue(1, "Штрихкоды", "4027269231781")
            .containsValue(1, "Потребность", 224)
            .containsValue(1, "кол-во", 5)
            .containsValue(1, "цена", 5)
            .containsValue(1, "валюта", "RUB")
            .containsValue(
                1, """
     комментарий
     (здесь вы можете указать ваш комментарий по товару. Наличие других цветов, доступность и др.)
     """.trimIndent(), ""
            )
            .containsValue(2, "Бренд", "Внешторг Фарма")
            .containsValue(2, "Категория 1", "Витамины и минералы")
            .containsValue(2, "Категория 2", "Аптека")
            .containsValue(2, "Категория 3", "Все товары")
            .containsValue(2, "Название товара", "Алфавит Классик таб., 120 шт.")
            .containsValue(2, "MSKU", 101315343827L)
            .containsValue(2, "SSKU", "005926.22972")
            .containsValue(2, "Артикул", "4660014182152")
            .containsValue(2, "Штрихкоды", "4660014182152")
            .containsValue(2, "Потребность", 57)
            .containsValue(2, "кол-во", 5)
            .containsValue(2, "цена", 5)
            .containsValue(2, "валюта", "RUB")
            .containsValue(
                2, """
     комментарий
     (здесь вы можете указать ваш комментарий по товару. Наличие других цветов, доступность и др.)
     """.trimIndent(), ""
            )
            .hasSize(2)
    }

    @DbUnitDataSet(
        before = ["TenderSupplierResponseRepositoryTest_deanonymizeSupplier.before.csv"],
        after = ["TenderSupplierResponseRepositoryTest_deanonymizeSupplier.after.csv"]
    )
    @Test
    fun deanonymizeSupplier() {
        repo.deanonymizeSupplier(1, 42000000, 1)
    }

}