package ru.yandex.market.contentmapping.repository

import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterInfo
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.mbo.export.MboParameters

class CategoryParameterInfoRepositoryTest : BaseAppTestClass() {
    @Autowired
    lateinit var categoryParameterInfoRepository: CategoryParameterInfoRepository
    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `it can select enum value_type`() {
        jdbcTemplate.execute("""
            insert into cm.category_parameters_info (
                category_id, parameter_id, name, xsl_name, value_type, 
                important, service, multivalue, required_for_model_creation, mandatory_for_signature, 
                common_filter_index, options, unit_name, min_value, max_value)
            values (
                1001, 10001, 'Param 10001', 'param_10001', 'NUMERIC'::cm.category_parameter_type, 
                true, false, false, true, true, 
                1, null, 'кг', 1.2, 3.4);
            """.trimIndent())

        val parameters: List<CategoryParameterInfo> = categoryParameterInfoRepository.getByCategoryId(1001L)

        assertThat(parameters).hasSize(1);
        parameters[0].let { p ->
            assertThat(p.valueType).isEqualTo(MboParameters.ValueType.NUMERIC)
            p.minValue.shouldNotBeNull()
            p.minValue?.let { it shouldBeExactly 1.2 }
            p.maxValue.shouldNotBeNull()
            p.maxValue?.let { it shouldBeExactly 3.4 }
        }
    }
}
