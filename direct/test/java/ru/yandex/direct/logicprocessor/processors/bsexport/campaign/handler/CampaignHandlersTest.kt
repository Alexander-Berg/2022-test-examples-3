package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration

@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class CampaignHandlersTest @Autowired constructor(
    private val handlers: List<ICampaignResourceHandler<*>>,
) {
    @Test
    fun `all handlers have different resource types`() {
        val handlersWithSameResourceType = handlers
            .map { it::class.java.simpleName to it.resourceType }
            .groupBy({ it.second }, { it.first })
            .filter { it.value.size > 1 }

        var errorMessage = "Handlers with same campaign resource types: "
        handlersWithSameResourceType
            .forEach { (resourceType, handlersNames) ->
                errorMessage =
                    errorMessage.plus("$handlersNames has resource type $resourceType, ")
            }

        errorMessage.removeSuffix(", ")
        assertThat(handlersWithSameResourceType).withFailMessage(errorMessage).isEmpty()
    }
}
