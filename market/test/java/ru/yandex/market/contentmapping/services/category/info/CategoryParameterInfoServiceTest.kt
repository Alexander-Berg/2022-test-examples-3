package ru.yandex.market.contentmapping.services.category.info

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterInfo
import ru.yandex.market.contentmapping.repository.CategoryParameterInfoRepository
import ru.yandex.market.contentmapping.services.AutogenerationValidParameterService
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.contentmapping.utils.MboParameterConstants
import ru.yandex.market.mbo.export.MboParameters
import kotlin.random.Random

class CategoryParameterInfoServiceTest : BaseAppTestClass() {
    private lateinit var parameterInfoRepository: CategoryParameterInfoRepository
    private lateinit var categoryInfoService: CategoryInfoService
    private lateinit var autogenerationValidParameterService: AutogenerationValidParameterService
    private lateinit var categoryParameterInfoService: CategoryParameterInfoServiceImpl

    @Before
    fun startup() {
        mockParameterInfoRepository()
        mockCategoryInfoService()
        mockAutogenerationParameterInfoService()
        categoryParameterInfoService = CategoryParameterInfoServiceImpl(
            parameterInfoRepository, categoryInfoService, autogenerationValidParameterService
        )
    }

    @Test
    fun `should return valid params for single category`() {
        val answer = categoryParameterInfoService.getCategoryParametersWithSubCategories(categoryId3)
        assertThat(answer).hasSize(category3ValidParamIds.size)
        assertThat(answer.keys).containsExactlyInAnyOrderElementsOf(category3ValidParamIds)
    }

    @Test
    fun `should return all params that are in subcategories`() {
        val answer = categoryParameterInfoService.getCategoryParametersWithSubCategories(categoryId1)
        val expectedAnswer = (category1ValidParamIds + category3ValidParamIds).toSet()
        assertThat(answer.keys).containsExactlyInAnyOrderElementsOf(expectedAnswer)
    }

    private fun mockParameterInfoRepository() {
        val category1Infos = category1ParamIds.map { categoryInfos[it]!! }
        val category2Infos = category2ParamIds.map { categoryInfos[it]!! }
        val category3Infos = category3ParamIds.map { categoryInfos[it]!! }
        val category4Infos = category4ParamIds.map { categoryInfos[it]!! }
        parameterInfoRepository = mock {
            onBlocking { getByCategoryId(eq(categoryId1)) }.doReturn(category1Infos)
            onBlocking { getByCategoryId(eq(categoryId2)) }.doReturn(category2Infos)
            onBlocking { getByCategoryId(eq(categoryId3)) }.doReturn(category3Infos)
            onBlocking { getByCategoryId(eq(categoryId4)) }.doReturn(category4Infos)
        }
    }

    private fun mockCategoryInfoService() {
        categoryInfoService = mock {
            onBlocking { getAllDescendantsIds(eq(categoryId1)) }.doReturn(category1DescendantIds)
            onBlocking { getAllDescendantsIds(eq(categoryId2)) }.doReturn(category2DescendantIds)
            onBlocking { getAllDescendantsIds(eq(categoryId3)) }.doReturn(category3DescendantIds)
            onBlocking { getAllDescendantsIds(eq(categoryId4)) }.doReturn(category4DescendantIds)
        }
    }

    private fun mockAutogenerationParameterInfoService() {
        autogenerationValidParameterService = mock {
            onBlocking { getValidParametersForCategory(eq(categoryId1)) }.doReturn(category1ValidParamIds)
            onBlocking { getValidParametersForCategory(eq(categoryId2)) }.doReturn(category2ValidParamIds)
            onBlocking { getValidParametersForCategory(eq(categoryId3)) }.doReturn(category3ValidParamIds)
            onBlocking { getValidParametersForCategory(eq(categoryId4)) }.doReturn(category4ValidParamIds)
        }
    }

    private val categoryId1 = 1L
    private val categoryId2 = 2L
    private val categoryId3 = 3L
    private val categoryId4 = 4L

    private val category1DescendantIds = (1L..4L).toList()
    private val category2DescendantIds = (2L..4L).toList()
    private val category3DescendantIds = listOf(3L)
    private val category4DescendantIds = listOf(4L)

    private val partnerGroupParamId = MboParameterConstants.PARTNER_GROUP_ID
    private val datacampGroupNameParamId = MboParameterConstants.DATACAMP_GROUP_NAME

    private val partnerGroupParam = CategoryVirtualParameters.makePartnerGroupIdParameter(0L)
    private val datacampGroupParam = CategoryVirtualParameters.makePartnerGroupIdParameter(0L)

    private val staticParams = listOf(partnerGroupParam, datacampGroupParam)
    private val staticParamIds = listOf(partnerGroupParamId, datacampGroupNameParamId)

    private val category1ParamIds = (1L..3L).toSet() + staticParamIds
    private val category2ParamIds = (4L..6L).toSet() + staticParamIds
    private val category3ParamIds = (7L..9L).toSet() + staticParamIds
    private val category4ParamIds = (7L..13L).toSet() + category3ParamIds

    private val category1ValidParamIds = (1L..2L).toList() + staticParamIds
    private val category2ValidParamIds = (5L..6L).toList() + staticParamIds
    private val category3ValidParamIds = (7L..9L).toList() + staticParamIds
    private val category4ValidParamIds = (7L..13L).toList() + staticParamIds

    private val paramIds = (1L..20L).toSet()

    private val categoryInfos =
        paramIds.associateWith { generateCategoryParameterInfo(paramId = it, isRequired = false) } + mapOf(
            partnerGroupParamId to partnerGroupParam,
            datacampGroupNameParamId to datacampGroupParam
        )

    private fun generateCategoryParameterInfo(paramId: Long, isRequired: Boolean): CategoryParameterInfo {
        val name = paramNames[Random.nextInt(from = 0, until = paramNames.size - 1)]
        return CategoryParameterInfo(
            parameterId = paramId,
            xslName = name,
            name = name,
            unitName = null,
            valueType = MboParameters.ValueType.NUMERIC,
            isImportant = false,
            isMultivalue = false,
            isService = false,
            isRequiredForModelCreation = isRequired,
            isMandatoryForSignature = isRequired,
            commonFilterIndex = -1,
            commentForOperator = "",
            commentForPartner = "",
            minValue = 0.0,
            maxValue = 100.0,
            Int2ObjectOpenHashMap()
        )
    }

    companion object {
        val paramNames = listOf("вес", "цвет", "размер", "ширина", "длинна", "высота", "габариты", "карманы", "принт")
    }
}
