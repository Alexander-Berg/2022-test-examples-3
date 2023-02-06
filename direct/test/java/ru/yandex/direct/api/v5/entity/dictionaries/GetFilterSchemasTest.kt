package ru.yandex.direct.api.v5.entity.dictionaries

import com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum
import com.yandex.direct.api.v5.dictionaries.EnumFilterFieldProps
import com.yandex.direct.api.v5.dictionaries.FilterFieldItem
import com.yandex.direct.api.v5.dictionaries.FilterFieldOperator
import com.yandex.direct.api.v5.dictionaries.FilterFieldType.ENUM
import com.yandex.direct.api.v5.dictionaries.FilterFieldType.NUMBER
import com.yandex.direct.api.v5.dictionaries.FilterFieldType.STRING
import com.yandex.direct.api.v5.dictionaries.FilterSchemasItem
import com.yandex.direct.api.v5.dictionaries.GetRequest
import com.yandex.direct.api.v5.dictionaries.NumberFilterFieldProps
import com.yandex.direct.api.v5.dictionaries.StringFilterFieldProps
import com.yandex.direct.api.v5.general.ArrayOfString
import com.yandex.direct.api.v5.general.StringConditionOperatorEnum
import java.math.BigDecimal
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations.openMocks
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.qatools.allure.annotations.Description


@Api5Test
@RunWith(SpringRunner::class)
@Description("Получение фильтров условий нацеливания")
class GetFilterSchemasTest {

    @Autowired
    private lateinit var steps: Steps

    private lateinit var filterSchemas: List<FilterSchemasItem>

    @Before
    fun before() {
        openMocks(this)

        val clientInfo = steps.clientSteps().createDefaultClient()

        val dictionariesService = DictionariesServiceBuilder(
            steps.applicationContext()
        )
            .withClientAuth(clientInfo)
            .build();

        val request = GetRequest().apply {
            dictionaryNames = listOf(DictionaryNameEnum.FILTER_SCHEMAS)
        }
        val response = dictionariesService.get(request)
        filterSchemas = response.filterSchemas

        assertThat(filterSchemas).describedAs("Получен словарь фильтров условий нацеливания").isNotEmpty
    }

    @Test
    @Description("Test PerformanceDefault filter schema")
    fun test_PerformanceDefault_success() {

        val expectedSchemasItem = FilterSchemasItem().apply {
            name = "PerformanceDefault"
            fields = listOf(
                enumField("adult", listOf("0", "1"), listOf(EQUALS_ANY_1, EXISTS)),
                enumField(
                    "age",
                    listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "16", "18"),
                    listOf(EQUALS_ANY_1, EXISTS)
                ),
                numberField("categoryId", opers = listOf(EQUALS_ANY_20000, GREATER_THAN, IN_RANGE, LESS_THAN)),
                stringField("description", opers = listOf(CONTAINS_ANY, NOT_CONTAINS_ALL)),
                numberField("id", opers = listOf(EQUALS_ANY_100, GREATER_THAN, IN_RANGE, LESS_THAN)),
                enumField("manufacturer_warranty", listOf("0", "1"), listOf(EQUALS_ANY_1, EXISTS)),
                stringField("market_category", opers = listOf(CONTAINS_ANY, EXISTS, NOT_CONTAINS_ALL)),
                stringField("model", opers = listOf(CONTAINS_ANY, NOT_CONTAINS_ALL)),
                stringField("name", opers = listOf(CONTAINS_ANY, NOT_CONTAINS_ALL)),
                numberField(
                    "oldprice",
                    prec = 2,
                    opers = listOf(EQUALS_ANY_100, EXISTS, GREATER_THAN, IN_RANGE, LESS_THAN)
                ),
                enumField("pickup", listOf("true"), listOf(EQUALS_ANY_1, EXISTS)),
                numberField(
                    "price",
                    prec = 2,
                    opers = listOf(EQUALS_ANY_100, EXISTS, GREATER_THAN, IN_RANGE, LESS_THAN)
                ),
                enumField("store", listOf("0", "1"), listOf(EQUALS_ANY_1, EXISTS)),
                stringField("typePrefix", opers = listOf(CONTAINS_ANY, EXISTS, NOT_CONTAINS_ALL)),
                stringField("url", opers = listOf(CONTAINS_ANY, EQUALS_ANY_100, NOT_CONTAINS_ALL)),
                stringField("vendor", opers = listOf(CONTAINS_ANY, EXISTS, NOT_CONTAINS_ALL))
            )
        }

        val actualSchemasItem: FilterSchemasItem = filterSchemas.first { it.name == "PerformanceDefault" }
        assertThat(actualSchemasItem).describedAs("Filter schema")
            .`is`(matchedBy(beanDiffer(expectedSchemasItem).useCompareStrategy(onlyExpectedFields())))
    }

    @Test
    @Description("Test AutoAutoRu filter schema")
    fun test_AutoAutoRu_success() {

        val expectedSchemasItem = FilterSchemasItem().apply {
            name = "AutoAutoRu"
            fields = listOf(
                stringField("availability", opers = listOf(CONTAINS_ANY)),
                stringField("body_type", opers = listOf(CONTAINS_ANY, NOT_CONTAINS_ALL)),
                stringField("color", opers = listOf(CONTAINS_ANY, NOT_CONTAINS_ALL)),
                stringField("folder_id", opers = listOf(CONTAINS_ANY, NOT_CONTAINS_ALL)),
                stringField("mark_id", opers = listOf(CONTAINS_ANY, NOT_CONTAINS_ALL)),
                stringField("metallic", opers = listOf(CONTAINS_ANY)),
                numberField("price", prec = 2, opers = listOf(EQUALS_ANY_100, GREATER_THAN, IN_RANGE, LESS_THAN)),
                stringField("url", opers = listOf(CONTAINS_ANY, EQUALS_ANY_100, NOT_CONTAINS_ALL)),
                stringField("wheel", opers = listOf(CONTAINS_ANY)),
                numberField("year", min = BigDecimal.valueOf(1970.0), prec = 2, opers = listOf(EQUALS_ANY_10)),
            )
        }

        val actualSchemasItem: FilterSchemasItem = filterSchemas.first { it.name == "AutoAutoRu" }
        assertThat(actualSchemasItem).describedAs("Filter schema")
            .`is`(matchedBy(beanDiffer(expectedSchemasItem).useCompareStrategy(onlyExpectedFields())))
    }

    @Test
    @Description("Test EmptySchema filter schema")
    fun test_EmptySchema_success() {

        val expectedSchemasItem = FilterSchemasItem().apply {
            name = "EmptySchema"
            fields = emptyList()
        }

        val actualSchemasItem: FilterSchemasItem = filterSchemas.first { it.name == "EmptySchema" }
        assertThat(actualSchemasItem).describedAs("Filter schema")
            .`is`(matchedBy(beanDiffer(expectedSchemasItem).useCompareStrategy(onlyExpectedFields())))
    }

    companion object {
        val LESS_THAN = FilterFieldOperator().apply { type = StringConditionOperatorEnum.LESS_THAN; maxItems = 1 }
        val EXISTS = FilterFieldOperator().apply { type = StringConditionOperatorEnum.EXISTS; maxItems = 1 }
        val GREATER_THAN = FilterFieldOperator().apply { type = StringConditionOperatorEnum.GREATER_THAN; maxItems = 1 }
        val EQUALS_ANY_1 = FilterFieldOperator().apply { type = StringConditionOperatorEnum.EQUALS_ANY; maxItems = 1 }
        val EQUALS_ANY_10 = FilterFieldOperator().apply { type = StringConditionOperatorEnum.EQUALS_ANY; maxItems = 10 }
        val EQUALS_ANY_100 = FilterFieldOperator().apply { type = StringConditionOperatorEnum.EQUALS_ANY; maxItems = 100 }
        val EQUALS_ANY_20000 =
            FilterFieldOperator().apply { type = StringConditionOperatorEnum.EQUALS_ANY; maxItems = 20000 }
        val IN_RANGE = FilterFieldOperator().apply { type = StringConditionOperatorEnum.IN_RANGE; maxItems = 10 }
        val CONTAINS_ANY =
            FilterFieldOperator().apply { type = StringConditionOperatorEnum.CONTAINS_ANY; maxItems = 100 }
        val NOT_CONTAINS_ALL =
            FilterFieldOperator().apply { type = StringConditionOperatorEnum.NOT_CONTAINS_ALL; maxItems = 100 }
    }

    private fun enumField(
        name: String,
        vals: List<String>,
        opers: List<FilterFieldOperator>
    ) =
        FilterFieldItem().apply {
            this.name = name
            type = ENUM
            enumProps = EnumFilterFieldProps().apply { this.values = ArrayOfString().apply { this.items = vals } }
            operators = opers
        }

    private fun numberField(
        name: String,
        min: BigDecimal = BigDecimal.valueOf(0.0),
        max: BigDecimal = BigDecimal.valueOf(Long.MAX_VALUE.toDouble()), // 9.223372036854776E18
        prec: Int = 0,
        opers: List<FilterFieldOperator>
    ) =
        FilterFieldItem().apply {
            this.name = name
            type = NUMBER
            numberProps = NumberFilterFieldProps().apply { this.min = min; this.max = max; this.precision = prec }
            operators = opers
        }

    private fun stringField(
        name: String,
        min: Int = 1,
        max: Int = 175,
        opers: List<FilterFieldOperator>
    ) =
        FilterFieldItem().apply {
            this.name = name
            type = STRING
            stringProps = StringFilterFieldProps().apply { this.minLength = min; this.maxLength = max }
            operators = opers
        }

}
