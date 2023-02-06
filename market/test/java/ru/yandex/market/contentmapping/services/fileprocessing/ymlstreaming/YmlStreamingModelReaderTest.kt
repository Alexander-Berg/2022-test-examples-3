package ru.yandex.market.contentmapping.services.fileprocessing.ymlstreaming

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test

class YmlStreamingModelReaderTest {
    @Test
    fun `test vendor model yml`() {
        val stream = javaClass.classLoader.getResourceAsStream("data-files/vendor.model.yml.xml")!!
        val results = YmlStreamingModelReader.readYml(1L, stream).toList()

        results shouldHaveSize 4
        results[0].asClue {
            it.errors.shouldBeEmpty()

            val offer = it.offer!!
            offer.shopSku shouldBe "6dcf91d4-355e-40fd-8011-833bf389e396"
            offer.shopId shouldBe 1L
            offer.name shouldBe "Dr.Koffer Др.Коффер B402154-02-09 визитка"
            offer.description shouldBe "Сумка для тех, кому классические модели кажутся скучными."
            offer.shopCategoryName shouldBe "Dr.Koffer/Мужская коллекция/Мужские сумки/Барсетки"
            offer.shopVendor shouldBe "Dr.Koffer"
            offer.vendorCode shouldBe "8046"
            offer.barCode shouldBe "4670002035049"
            offer.shopValues shouldBe mapOf(
                    "Материал" to "Натуральная кожа",
                    "Цвет" to "Коричневый",
                    "Изображения из YML" to
                            "https://newb2b.koffer.ru/upload/iblock/de3/de3d1c4c108b074ad904f8d820937d31.jpg," +
                            "https://newb2b.koffer.ru/upload/iblock/835/835ee761f5e3ee7832036261277cdcf5.jpg," +
                            "https://newb2b.koffer.ru/upload/iblock/002/0020c59edaf9a4960d7e848e7a19f5cc.jpg"
            )
        }

        results[1].asClue {
            it.offer shouldBe null
            it.errors shouldHaveSize 1
            it.errors[0].asClue { error ->
                error.indexNumber shouldBe 1
                error.errorText shouldBe
                        "Не указан обязательный параметр <model> для товара c9a83d52-25d1-4be1-8035-15f9df009ce2"
            }
        }
    }

    @Test
    fun `test sample yml`() {
        val stream = javaClass.classLoader.getResourceAsStream("data-files/test.yml.xml")!!
        val results = YmlStreamingModelReader.readYml(1L, stream).toList()

        results.size shouldBe 3
        results[0].asClue {
            val offer = it.offer!!
            offer.shopSku shouldBe "29406"
            offer.name shouldBe "Michael Kors MK5925"
            offer.shopCategoryName shouldBe "Часы/Женские часы"
            offer.shopValues shouldBe mapOf(
                    "Изображения из YML" to
                            "https://optimawatches.ru/wp-content/uploads/2020/12/061220201607286961.jpeg"
            )
        }
    }

    @Test
    fun `test empty yml`() {
        val stream = javaClass.classLoader.getResourceAsStream("data-files/empty.yml.xml")!!
        val results = YmlStreamingModelReader.readYml(1L, stream).toList()

        results.shouldBeEmpty()
    }

    @Test
    fun `test very empty yml`() {
        val stream = javaClass.classLoader.getResourceAsStream("data-files/very-empty.yml.xml")!!
        val results = YmlStreamingModelReader.readYml(1L, stream).toList()

        results.shouldBeEmpty()
    }

    @Test
    fun `test shop sku yml`() {
        val stream = javaClass.classLoader.getResourceAsStream("data-files/test.yml.xml")!!
        val results = YmlStreamingModelReader.readYml(1L, stream).toList()

        results.size shouldBe 3
        results[2].asClue {
            val offer = it.offer!!
            offer.shopSku shouldBe "abc"
        }
    }
}
