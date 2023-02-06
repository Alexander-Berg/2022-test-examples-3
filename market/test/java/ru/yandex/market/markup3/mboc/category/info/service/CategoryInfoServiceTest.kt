package ru.yandex.market.markup3.mboc.category.info.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.mockito.Mockito.atLeastOnce
import ru.yandex.market.markup3.mboc.category.info.CategoryInfo
import ru.yandex.market.markup3.mboc.category.info.repository.CategoryInfoRepository
import java.time.Duration
import java.util.concurrent.TimeUnit

class CategoryInfoServiceTest {

    private val categoryInfoService: CategoryInfoService

    init {
        val categoryInfoRepository: CategoryInfoRepository = mock()
        categoryInfoService = CategoryInfoService(categoryInfoRepository, Duration.ofHours(1))

        whenever(categoryInfoRepository.findAll())
            .thenReturn(getCategoryList())
        categoryInfoService.getAllCategoryInfos()
    }

    @Test
    fun `extractClosestCategoryElementList should return same category item`() {
        val items = mapOf(
            4L to mutableListOf("Валенки кожаные со стразами"),
            5L to mutableListOf("Тапки скороходы"),
            8L to mutableListOf("Огурцы в шоколаде")
        )

        val resultItems =
            categoryInfoService.extractClosestCategoryElementList(4, 1, items, false)

        resultItems.size shouldBe 1
        resultItems[0] shouldBe "Валенки кожаные со стразами"
    }

    @Test
    fun `extractClosestCategoryElementList should return closest category item`() {
        val items = mapOf(
            8L to mutableListOf("Огурцы в шоколаде"),
            5L to mutableListOf("Тапки скороходы"),
            3L to mutableListOf("Абстрактная еда")
        )

        val resultItems =
            categoryInfoService.extractClosestCategoryElementList(4, 1, items, false)

        resultItems.size shouldBe 1
        resultItems[0] shouldBe "Тапки скороходы"
    }

    @Test
    fun `extractClosestCategoryElementList should return closest category items`() {
        val items = mapOf(
            9L to mutableListOf("Помидоры в карамели"),
            8L to mutableListOf("Огурцы в шоколаде"),
            10L to mutableListOf("Селедка с клубникой"),
            4L to mutableListOf("Валенки кожаные со стразами"),
            5L to mutableListOf("Тапки скороходы")
        )

        val resultItems =
            categoryInfoService.extractClosestCategoryElementList(9, 3, items, false)

        resultItems.size shouldBe 3
        resultItems shouldContainAll listOf("Селедка с клубникой", "Помидоры в карамели", "Огурцы в шоколаде")
    }

    @Test
    fun `extractClosestCategoryElementList should return any item on unknown category`() {
        val unknownCategoryId = Long.MAX_VALUE
        val items = mapOf(
            4L to mutableListOf("Валенки кожаные со стразами"),
            5L to mutableListOf("Тапки скороходы")
        )

        val resultItems =
            categoryInfoService.extractClosestCategoryElementList(unknownCategoryId, 1, items, false)

        resultItems.size shouldBe 1
    }

    private fun getCategoryList(): List<CategoryInfo> {
        return listOf(
            CategoryInfo(1, -1, "Все товары", "", false, true, true, "", "", false),
            CategoryInfo(2, 1, "Обувь", "", false, true, true, "", "", false),
            CategoryInfo(3, 1, "Еда", "", false, true, true, "", "", false),
            CategoryInfo(4, 2, "Валенки", "", false, true, true, "", "", true),
            CategoryInfo(5, 2, "Тапки", "", false, true, true, "", "", true),
            CategoryInfo(6, 3, "Фастфуд", "", false, true, true, "", "", true),
            CategoryInfo(7, 3, "Овощи", "", false, true, true, "", "", true),
            CategoryInfo(8, 7, "Огурцы", "", false, true, true, "", "", true),
            CategoryInfo(9, 7, "Помидоры", "", false, true, true, "", "", true),
            CategoryInfo(10, 6, "Милти", "", false, true, true, "", "", true),
            CategoryInfo(11, 6, "Вкусвилл", "", false, true, true, "", "", true),
        )
    }

    @Test
    fun `cache should updating after expiration`() {
        val categoryInfoRepository1: CategoryInfoRepository = mock()
        val categoryInfoService1 = CategoryInfoService(categoryInfoRepository1, Duration.ofSeconds(1))

        whenever(categoryInfoRepository1.findAll()).thenReturn(
            listOf(
                CategoryInfo(11, 6, "Вкусвилл", "", false, true, true, "", "", true)
            )
        )

        categoryInfoService1.getAllCategoryInfos().size shouldBe 1

        whenever(categoryInfoRepository1.findAll())
            .thenReturn(
                listOf(
                    CategoryInfo(11, 6, "Вкусвилл", "", false, true, true, "", "", true),
                    CategoryInfo(10, 6, "Милти", "", false, true, true, "", "", true),
                )
            )

        // init cache update after expiration
        categoryInfoService1.getAllCategoryInfos().size shouldBe 1

        TimeUnit.SECONDS.sleep(2)

        categoryInfoService1.getAllCategoryInfos().size shouldBe 2
        verify(categoryInfoRepository1, times(2)).findAll()
    }

    @Test
    fun `cache should not updating before expiration`() {
        val categoryInfoRepository1: CategoryInfoRepository = mock()
        val categoryInfoService1 = CategoryInfoService(categoryInfoRepository1, Duration.ofSeconds(100))

        // first generation cache contains 1 element
        whenever(categoryInfoRepository1.findAll()).thenReturn(
            listOf(
                CategoryInfo(11, 6, "Вкусвилл", "", false, true, true, "", "", true)
            )
        )

        categoryInfoService1.getAllCategoryInfos().size shouldBe 1

        // second generation cache should contains 2 element
        whenever(categoryInfoRepository1.findAll())
            .thenReturn(
                listOf(
                    CategoryInfo(11, 6, "Вкусвилл", "", false, true, true, "", "", true),
                    CategoryInfo(10, 6, "Милти", "", false, true, true, "", "", true),
                )
            )

        // init cache update after expiration
        categoryInfoService1.getAllCategoryInfos().size shouldBe 1

        TimeUnit.SECONDS.sleep(2)

        categoryInfoService1.getAllCategoryInfos().size shouldBe 1

        verify(categoryInfoRepository1, times(1)).findAll()
    }

    @Test
    fun `cache return old value on data source exception`() {
        val categoryInfoRepository1: CategoryInfoRepository = mock()
        val categoryInfoService1 = CategoryInfoService(categoryInfoRepository1, Duration.ofSeconds(1))

        whenever(categoryInfoRepository1.findAll()).thenReturn(
            listOf(
                CategoryInfo(11, 6, "Вкусвилл", "", false, true, true, "", "", true)
            )
        )

        categoryInfoService1.getAllCategoryInfos().size shouldBe 1

        whenever(categoryInfoRepository1.findAll())
            .thenThrow(RuntimeException("Data source not available"))

        // init cache update after expiration
        categoryInfoService1.getAllCategoryInfos().size shouldBe 1

        TimeUnit.SECONDS.sleep(2)

        val allCategoryInfosAfterUpdate = categoryInfoService1.getAllCategoryInfos()
        allCategoryInfosAfterUpdate.size shouldBe 1

        allCategoryInfosAfterUpdate.values
            .filter { it.name == "Вкусвилл" }.size shouldBe 1

        verify(categoryInfoRepository1, atLeastOnce()).findAll() // из кеша берет - вызывает меньше 2 раз
    }
}
