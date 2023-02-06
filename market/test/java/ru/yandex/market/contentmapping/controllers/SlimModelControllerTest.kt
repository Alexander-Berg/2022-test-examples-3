package ru.yandex.market.contentmapping.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletWebRequest
import ru.yandex.market.contentmapping.controllers.helper.ControllerAccessHelper
import ru.yandex.market.contentmapping.dto.model.export.ShopModelValidationType
import ru.yandex.market.contentmapping.dto.model.mboc.ProcessingStatus
import ru.yandex.market.contentmapping.dto.web.CompressedList
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.mboc.http.SupplierOffer

class SlimModelControllerTest : BaseAppTestClass() {
    @Autowired
    lateinit var controller: SlimModelController

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var accessHelper: ControllerAccessHelper

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @After
    fun cleanup() {
        RequestContextHolder.resetRequestAttributes()
        reset(accessHelper)
    }

    @Test
    fun `it should find data and encode it properly`() {
        RequestContextHolder.setRequestAttributes(ServletWebRequest(ControllerTestUtils.mockHttpRequest()))
        doNothing().whenever(accessHelper).validateUserHasReadAccessToShop(any(), any())

        prepareData()

        val response = MockHttpServletResponse()

        controller.findSlimModels(SlimModelController.SlimModelRequest(shopId = 2L), response)

        val result: CompressedList<SlimModelController.SlimModel> = objectMapper.readValue(response.contentAsString)
        result.fieldNames shouldContainAll listOf("id", "shopId", "errors", "paramIds")
        result.data shouldHaveSize 3

        val properties = result.fieldNames
                .withIndex()
                .associate { it.value to it.index }

        result.data[0].asClue {
            it[properties["shopId"]!!] shouldBe 2
            it[properties["errors"]!!] shouldBe listOf("AG_INVALID_HTML_TAG", "AG_INVALID_PICTURE")
            it[properties["paramIds"]!!] shouldBe listOf(123, 321)
        }
    }

    @Test
    fun `it should return filters`() {
        RequestContextHolder.setRequestAttributes(ServletWebRequest(ControllerTestUtils.mockHttpRequest()))
        doNothing().whenever(accessHelper).validateUserHasReadAccessToShop(any(), any())

        prepareData()

        val filters = controller.filters(2, null)
        filters.processingStatusOptions shouldContainAll listOf(ProcessingStatus.NEED_CONTENT, ProcessingStatus.READY)
        filters.availabilityOptions shouldContainAll listOf(SupplierOffer.Availability.ACTIVE)
        filters.paramIdOptions shouldContainAll listOf(123, 321, 333)
        filters.errorOptions shouldContainAll listOf(
                ShopModelValidationType.AG_INVALID_HTML_TAG,
                ShopModelValidationType.AG_INVALID_PICTURE,
                ShopModelValidationType.AG_CATEGORY_RESTRICTION
        )
    }

    private fun prepareData() {
        jdbcTemplate.update("""
                insert into cm.temp_advanced_stat (
                    id, shop_id, shop_sku, market_category_id, processing_status, availability, 
                    allow_model_create_update, fill_rating, is_fill_rating_poor, is_valid, with_formalization, 
                    param_ids, errors) 
                values
                    (
                        1, 2, 'test', 42, 'NEED_CONTENT', 'ACTIVE',
                        true, 0.96, false, true, true,
                        array[123, 321], '{AG_INVALID_HTML_TAG,AG_INVALID_PICTURE}'::cm.shop_model_validation_type[]
                    ),
                    (
                        2, 2, 'test2', 42, 'READY', 'ACTIVE',
                        true, 0.96, false, true, true,
                        null, '{AG_CATEGORY_RESTRICTION}'::cm.shop_model_validation_type[]
                    ),              
                    (
                        3, 2, 'test3', 42, 'READY', 'ACTIVE',
                        true, 0.96, false, true, true,
                        array[333], null
                    )             
            """.trimIndent(), MapSqlParameterSource());
    }
}
