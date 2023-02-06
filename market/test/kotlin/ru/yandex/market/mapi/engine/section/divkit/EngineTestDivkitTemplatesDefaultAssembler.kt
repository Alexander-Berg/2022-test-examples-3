package ru.yandex.market.mapi.engine.section.divkit

import com.fasterxml.jackson.databind.JsonNode
import com.yandex.div.dsl.context.card
import com.yandex.div.dsl.context.define
import com.yandex.div.dsl.context.resolve
import com.yandex.div.dsl.context.templates
import com.yandex.div.dsl.int
import com.yandex.div.dsl.model.Div
import com.yandex.div.dsl.model.divContainer
import com.yandex.div.dsl.model.divData
import com.yandex.div.dsl.model.divText
import com.yandex.div.dsl.model.state
import com.yandex.div.dsl.model.template
import com.yandex.div.dsl.reference
import org.springframework.stereotype.Service
import ru.yandex.market.mapi.core.assembler.AbstractSimpleAssembler
import ru.yandex.market.mapi.core.assembler.AssemblyResult
import ru.yandex.market.mapi.core.model.divkit.DivkitSnippet

@Service
open class EngineTestDivkitTemplatesDefaultAssembler :
    AbstractSimpleAssembler<EngineTestDivkitTemplatesDefaultAssembler.TestResponse, DivkitSnippet>() {
    override fun getName() = "EngineTestDivkitTemplatesDefault"
    override fun getSnippetClass() = DivkitSnippet::class
    override fun getResponseClass() = TestResponse::class

    open fun templateName() = "default_name_title"

    override fun doConvert(response: TestResponse, configNode: JsonNode?, builder: AssemblyResult.Builder) {
        val firstNameTextRef = reference<String>("first_name")
        val secondNameTextRef = reference<String>("second_name")

        val templates = templates<Div> {
            define(
                templateName(), divContainer(
                    items = listOf(
                        divText(
                            text = firstNameTextRef,
                            fontSize = int(13)
                        ),
                        divText(
                            text = secondNameTextRef,
                            fontSize = int(10)
                        )
                    )
                )
            )
        }

        val card = card {
            divData(
                logId = "name_title_log_id",
                states = listOf(
                    state(
                        stateId = 0,
                        div = template(
                            "name_title",
                            resolve(firstNameTextRef, response.firstName),
                            resolve(secondNameTextRef, response.secondName)
                        )
                    )
                )
            )
        }

        builder.addSectionEventParams(templates)
        builder.tryAdd {
            DivkitSnippet("name_title_${response.id}", card)
        }
    }

    data class TestResponse(
        val firstName: String,
        val secondName: String,
        val id: Int
    )
}
