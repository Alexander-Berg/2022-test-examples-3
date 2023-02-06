package ru.yandex.market.abo.core.hiding.rules.business

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.abo.core.hiding.rules.business.BusinessCategoryGroup.FASHION
import ru.yandex.market.abo.util.entity.DeletableEntityServiceTest
import java.util.Date

class BusinessCategoryRuleServiceTest @Autowired constructor(
    private val businessCategoryRuleService: BusinessCategoryRuleService,
    private val businessCategoryRuleRepo: BusinessCategoryRuleRepo
) : DeletableEntityServiceTest<BusinessCategoryRule, Long>() {

    @ParameterizedTest
    @MethodSource("vendor and group rules")
    fun `check vendor and group rules`(targetRule: BusinessCategoryRule) {
        businessCategoryRuleService.checkRule(targetRule)
    }

    @ParameterizedTest
    @MethodSource("vendor and group rules")
    fun `check exception vendor and group rules`(targetRule: BusinessCategoryRule) {
        businessCategoryRuleRepo.save(targetRule)

        assertThrows<RuntimeException> { businessCategoryRuleService.checkRule(targetRule) }
    }

    @ParameterizedTest
    @MethodSource("vendor and category rule")
    fun `check vendor and category rule`(existRule: BusinessCategoryRule, expectedCategoryIds: Array<Long>) {
        businessCategoryRuleRepo.save(existRule)

        val targetRule = BusinessCategoryRule().apply {
            businessId = BUSINESS_ID
            vendorId = VENDOR_ID
            categoryIds = arrayOf(1L, 2L)
        }
        businessCategoryRuleService.checkRule(targetRule)
        assertTrue(expectedCategoryIds contentEquals targetRule.categoryIds)
    }

    @ParameterizedTest
    @MethodSource("category rule")
    fun `check category rule`(existRule: BusinessCategoryRule, expectedCategoryIds: Array<Long>) {
        businessCategoryRuleRepo.save(existRule)

        val targetRule = BusinessCategoryRule().apply {
            businessId = BUSINESS_ID
            categoryIds = arrayOf(1L, 2L)
        }
        businessCategoryRuleService.checkRule(targetRule)
        assertTrue(expectedCategoryIds contentEquals targetRule.categoryIds)
    }

    @Test
    fun `check exception vendor and categories rules`() {
        val targetRule1 = BusinessCategoryRule().apply {
            businessId = BUSINESS_ID
            categoryIds = arrayOf(1L, 2L)
        }
        val targetRule2 = BusinessCategoryRule().apply {
            businessId = BUSINESS_ID
            categoryIds = arrayOf(1L, 2L)
        }
        businessCategoryRuleRepo.saveAll(listOf(targetRule1, targetRule2))

        assertThrows<RuntimeException> { businessCategoryRuleService.checkRule(targetRule1) }
        assertThrows<RuntimeException> { businessCategoryRuleService.checkRule(targetRule2) }
    }

    override fun service() = businessCategoryRuleService

    override fun extractId(entity: BusinessCategoryRule?) = entity?.businessId

    override fun newEntity() = BusinessCategoryRule(2L, 2L, null, arrayOf(456L, 654L), null, "коммент").apply {
        creationTime = Date()
        deleted = false
        createdUserId = -1
    }

    override fun example() = BusinessCategoryRule(null, 2L, null, null, null, null)

    companion object {
        private const val BUSINESS_ID = 1L
        private const val VENDOR_ID = 123L

        @JvmStatic
        fun `vendor and group rules`(): Iterable<Arguments> = listOf(
            Arguments.of(
                BusinessCategoryRule().apply {
                    businessId = BUSINESS_ID
                    vendorId = VENDOR_ID
                }
            ),
            Arguments.of(
                BusinessCategoryRule().apply {
                    businessId = BUSINESS_ID
                    categoryGroup = FASHION
                }
            ),
            Arguments.of(
                BusinessCategoryRule().apply {
                    businessId = BUSINESS_ID
                    vendorId = VENDOR_ID
                    categoryGroup = FASHION
                }
            )
        )

        @JvmStatic
        fun `vendor and category rule`(): Iterable<Arguments> = listOf(
            Arguments.of(
                BusinessCategoryRule().apply {
                    businessId = BUSINESS_ID
                    vendorId = VENDOR_ID
                    categoryIds = arrayOf(2L)
                },
                arrayOf(1L)
            ),
            Arguments.of(
                BusinessCategoryRule().apply {
                    businessId = BUSINESS_ID
                    vendorId = VENDOR_ID
                    categoryIds = arrayOf(3L)
                },
                arrayOf(1L, 2L)
            ),
            Arguments.of(
                BusinessCategoryRule().apply {
                    businessId = BUSINESS_ID
                    categoryIds = arrayOf(2L)
                },
                arrayOf(1L, 2L)
            )
        )

        @JvmStatic
        fun `category rule`(): Iterable<Arguments> = listOf(
            Arguments.of(
                BusinessCategoryRule().apply {
                    businessId = BUSINESS_ID
                    categoryIds = arrayOf(2L)
                },
                arrayOf(1L)
            ),
            Arguments.of(
                BusinessCategoryRule().apply {
                    businessId = BUSINESS_ID
                    categoryIds = arrayOf(3L)
                },
                arrayOf(1L, 2L)
            ),
            Arguments.of(
                BusinessCategoryRule().apply {
                    businessId = BUSINESS_ID
                    vendorId = VENDOR_ID
                    categoryIds = arrayOf(2L)
                },
                arrayOf(1L, 2L)
            )
        )
    }
}
