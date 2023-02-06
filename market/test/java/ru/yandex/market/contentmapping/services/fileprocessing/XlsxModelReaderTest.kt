package ru.yandex.market.contentmapping.services.fileprocessing

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainAll
import io.kotest.matchers.shouldBe
import org.junit.Assert
import org.junit.Test
import ru.yandex.market.contentmapping.dto.fileprocessing.ProcessingConfig

class XlsxModelReaderTest {
    companion object {
        private const val COMMON_XSL = "data-files/single-model.xlsx"
        private const val ASSORTMENT_XSL = "data-files/beru-assortment.xlsx"
        private const val CONTENT_XSL = "data-files/beru-content.xlsx"
        private const val OZON_XLS = "data-files/ozon-sample2.xlsx"
    }

    @Test
    fun readCommonExcel() {
        val result = XlsxModelReader.readExcel(0,
                readResource(COMMON_XSL),
                ProcessingConfig(
                    shopSku = "ean",
                    name = "название",
                    vendorCode = "артикул",
                    barcode = "ean"
        ))
        Assert.assertEquals(1, result.offers.size);
    }

    @Test
    fun readAssortmentExcel() {
        val result = XlsxModelReader.readExcel(0,
                readResource(ASSORTMENT_XSL),
                ProcessingConfig(
                    shopSku = "Ваш SKU",
                    name = "Название товара",
                    vendorCode = "Изготовитель",
                    barcode = "Штрихкод"
        ))
        Assert.assertEquals(1, result.offers.size);
    }

    @Test
    fun readContentExcel() {
        val result = XlsxModelReader.readExcel(0,
                readResource(CONTENT_XSL),
                ProcessingConfig(
                    shopSku = "Ваш SKU",
                    name = "Название товара",
                    vendorCode = "Торговая марка",
                    barcode = "Штрихкод"
        ))
        Assert.assertEquals(4, result.offers.size);
    }

    @Test
    fun testReadOzonSample2() {
        val result = XlsxModelReader.readExcel(0, readResource(OZON_XLS), ProcessingConfig(shopSku = "Артикул*"))

        result.errors.shouldBeEmpty()
        result.rowCount shouldBe 41
        result.offers shouldHaveSize 41
        result.offers[0].asClue {
            it.shopSku shouldBe "R50362"
            it.shopValues.keys shouldContainAll listOf(
                    "Аннотация", "Артикул*", "Бренд*", "Штрихкод (Серийный номер / EAN)", "Тип*",
                    "Вес в упаковке, г*", "Высота упаковки, мм*", "Длина упаковки, мм*", "Ширина упаковки, мм*",
                    "Материал лезвия", "Название модели*", "Название товара", "Назначение",
                    "Страна-изготовитель",)
            it.shopValues.keys shouldNotContainAll listOf(
                    "№", "Цвет товара", "НДС, %*"
            )
        }
    }

    private fun readResource(path: String) = javaClass.classLoader.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Can't find in resources: '$path'")
}
