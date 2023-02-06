package ru.yandex.market.abo.core.category.restriction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest

/**
 * @author zilzilok
 */
class CategoryRestrictionServiceTest @Autowired constructor(
    private val categoryRestrictionService: CategoryRestrictionService
) : EmptyTest() {

    @Test
    fun crud() {
        categoryRestrictionService.update(listOf(CategoryRestriction(CategoryRestriction.Key("name", 123L))))
        categoryRestrictionService.findAllSupported(setOf("name"))
    }

    @Test
    fun `check different names`() {
        categoryRestrictionService.update(listOf(
            CategoryRestriction(CategoryRestriction.Key("name", 123L)),
            CategoryRestriction(CategoryRestriction.Key("name", 321L)),
            CategoryRestriction(CategoryRestriction.Key("qwe", 123L)),
            CategoryRestriction(CategoryRestriction.Key("qwe", 321L)),
            CategoryRestriction(CategoryRestriction.Key("asd", 123L))
        ))
        val categoryIdsMap = categoryRestrictionService.findAllSupported(setOf("name", "asd"))

        assertEquals(2, categoryIdsMap.size)
        assertEquals(2, categoryIdsMap["name"]?.size)
        assertEquals(1, categoryIdsMap["asd"]?.size)
        assertNull(categoryIdsMap["qwe"])
    }
}
