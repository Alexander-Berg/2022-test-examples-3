package ru.yandex.direct.grid.processing.service.conversioncenter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.conversionsourcetype.model.ConversionSourceType
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils

private const val QUERY_CONVERSION_SOURCE_TYPES_TEMPLATE = """{
  conversionSourceTypes {
    types {
      id
      code
      iconUrl
      name
      description
      activationUrl
      isDraft
      position
      isEditable
      nameEn
      descriptionEn
    }
  }
}
"""

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class ConversionCenterGraphQlServiceTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    private lateinit var defaultConversionSourceType: ConversionSourceType
    private lateinit var draftConversionSourceType: ConversionSourceType
    private lateinit var defaultConversionSourceTypeWithEn: ConversionSourceType
    private lateinit var draftConversionSourceTypeWithEn: ConversionSourceType

    @Before
    fun before() {
        defaultConversionSourceType = steps.conversionSourceTypeSteps().getDefaultConversionSourceType()
        draftConversionSourceType = steps.conversionSourceTypeSteps().getDraftConversionSourceType()
        defaultConversionSourceTypeWithEn = steps.conversionSourceTypeSteps().getDefaultConversionSourceTypeWithEn()
        draftConversionSourceTypeWithEn = steps.conversionSourceTypeSteps().getDraftConversionSourceTypeWithEn()
    }

    @Test
    fun getConversionSourceTypes_DefaultOperator() {
        val expectedConversionSourceType = steps.conversionSourceTypeSteps()
            .addConversionSourceTypeAndReturnAdded(defaultConversionSourceType.withName("getConversionSourceTypes_DefaultOperator"))
        val notExpectedConversionSourceType = steps.conversionSourceTypeSteps()
            .addConversionSourceTypeAndReturnAdded(draftConversionSourceType.withName("getConversionSourceTypes_DefaultOperator"))

        val context = ContextHelper.buildContext(createOrdinaryUser())

        val result = graphQlGetConversionSourceTypes(context)

        softly {
            assertThat(result).contains(fromConversionSourceType(expectedConversionSourceType))
            assertThat(result).doesNotContain(fromConversionSourceType(notExpectedConversionSourceType))
        }
    }

    @Test
    fun getConversionSourceTypes_DefaultOperator_WithEn() {
        val expectedConversionSourceType = steps.conversionSourceTypeSteps()
            .addConversionSourceTypeAndReturnAdded(defaultConversionSourceTypeWithEn.withName("getConversionSourceTypes_DefaultOperator"))
        val notExpectedConversionSourceType = steps.conversionSourceTypeSteps()
            .addConversionSourceTypeAndReturnAdded(draftConversionSourceTypeWithEn.withName("getConversionSourceTypes_DefaultOperator"))

        val context = ContextHelper.buildContext(createOrdinaryUser())

        val result = graphQlGetConversionSourceTypes(context)

        softly {
            assertThat(result).contains(fromConversionSourceType(expectedConversionSourceType))
            assertThat(result).doesNotContain(fromConversionSourceType(notExpectedConversionSourceType))
        }
    }

    @Test
    fun getConversionSourceTypes_InternalOperator() {
        val expectedConversionSourceType1 = steps.conversionSourceTypeSteps()
            .addConversionSourceTypeAndReturnAdded(defaultConversionSourceType.withName("getConversionSourceTypes_InternalOperator"))
        val expectedConversionSourceType2 = steps.conversionSourceTypeSteps()
            .addConversionSourceTypeAndReturnAdded(draftConversionSourceType.withName("getConversionSourceTypes_InternalOperator"))

        val context = ContextHelper.buildContext(createInternalUser())

        val result = graphQlGetConversionSourceTypes(context)

        assertThat(result)
            .contains(
                fromConversionSourceType(expectedConversionSourceType1),
                fromConversionSourceType(expectedConversionSourceType2)
            )
    }

    @Test
    fun getConversionSourceTypes_InternalOperator_WithEn() {
        val expectedConversionSourceType1 = steps.conversionSourceTypeSteps()
            .addConversionSourceTypeAndReturnAdded(defaultConversionSourceTypeWithEn.withName("getConversionSourceTypes_InternalOperator"))
        val expectedConversionSourceType2 = steps.conversionSourceTypeSteps()
            .addConversionSourceTypeAndReturnAdded(draftConversionSourceTypeWithEn.withName("getConversionSourceTypes_InternalOperator"))

        val context = ContextHelper.buildContext(createInternalUser())

        val result = graphQlGetConversionSourceTypes(context)

        assertThat(result)
            .contains(
                fromConversionSourceType(expectedConversionSourceType1),
                fromConversionSourceType(expectedConversionSourceType2)
            )
    }

    private fun graphQlGetConversionSourceTypes(context: GridGraphQLContext): List<*> {
        val response = processor.processQuery(null, QUERY_CONVERSION_SOURCE_TYPES_TEMPLATE, null, context)
        GraphQLUtils.logErrors(response.errors)
        assertThat(response.errors).isEmpty()
        val data: Map<String, Any> = response.getData()
        return (data["conversionSourceTypes"] as Map<*, *>)["types"] as List<*>
    }

    private fun createOrdinaryUser() = steps.clientSteps().createDefaultClient().chiefUserInfo!!.user!!

    private fun createInternalUser() = steps.clientSteps().createDefaultInternalClient().chiefUserInfo!!.user!!

    private fun fromConversionSourceType(conversionSourceType: ConversionSourceType): Map<*, *> {
        return mapOf(
            "id" to conversionSourceType.id.toString(),
            "name" to conversionSourceType.name,
            "nameEn" to conversionSourceType.nameEn,
            "description" to conversionSourceType.description,
            "descriptionEn" to conversionSourceType.descriptionEn,
            "iconUrl" to conversionSourceType.iconUrl,
            "activationUrl" to conversionSourceType.activationUrl,
            "isDraft" to conversionSourceType.isDraft,
            "position" to conversionSourceType.position,
            "code" to conversionSourceType.code.toString(),
            "isEditable" to conversionSourceType.isEditable
        )
    }
}
