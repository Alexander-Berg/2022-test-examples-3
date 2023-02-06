package ru.yandex.market.mapi.section

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.skyscreamer.jsonassert.JSONCompareMode
import org.slf4j.LoggerFactory
import ru.yandex.market.mapi.core.AbstractNonSpringTest
import ru.yandex.market.mapi.core.ResolverClientResponseMock
import ru.yandex.market.mapi.core.assembler.AbstractAssembler
import ru.yandex.market.mapi.core.assembler.AssemblyResult
import ru.yandex.market.mapi.core.contract.ResolverClientResponse
import ru.yandex.market.mapi.core.model.MapiErrorDto
import ru.yandex.market.mapi.core.model.action.section.MergeSectionAction
import ru.yandex.market.mapi.core.model.analytics.SectionProcessContext
import ru.yandex.market.mapi.core.model.analytics.SnippetProcessContext
import ru.yandex.market.mapi.core.model.screen.AbstractSection
import ru.yandex.market.mapi.core.model.screen.AbstractSnippet
import ru.yandex.market.mapi.core.model.screen.ResourceResolver
import ru.yandex.market.mapi.core.model.section.EngineTestSection
import ru.yandex.market.mapi.core.util.AnalyticUtils
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.asResource
import ru.yandex.market.mapi.core.util.assertJson
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @author Ilya Kislitsyn / ilyakis@ / 24.01.2022
 */
abstract class AbstractSectionTest : AbstractNonSpringTest() {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun <R : Any> parse(data: String, type: KClass<R>): R {
        return JsonHelper.parse(data, type)
    }

    private fun AbstractAssembler<*>.doConvert(
        rawResponseList: List<ResolverClientResponse>,
        config: JsonNode? = null,
        section: AbstractSection,
    ): AssemblyResult {
        val resolvers = section.resources?.flatMap { res -> res.resolvers ?: emptyList() }
        val result = convert(rawResponseList, config, section, resolvers)
        for (snippet in result.content) {
            snippet.snippetId = snippet.buildSnippetId() ?: ""
        }
        return result
    }

    private fun AbstractAssembler<*>.doConvert(
        fileMap: Map<String, String>?,
        config: Any? = null,
        section: AbstractSection? = null
    ): AssemblyResult {
        val generated = generateSnippets(section)
        if (generated != null) {
            for (snippet in generated) {
                snippet.snippetId = snippet.buildSnippetId() ?: ""
            }
            return AssemblyResult(generated)
        }

        return doConvert(
            fileMap?.map { entry -> ResolverClientResponseMock(entry.value, entry.key) } ?: emptyList(),
            config?.let { JsonHelper.toTree(it) },
            section ?: EngineTestSection().apply { id = "generatedSectionId" }
        )
    }

    fun AbstractAssembler<*>.testAssembly(
        fileMap: Map<String, String>? = null,
        expected: String,
        compareMode: JSONCompareMode = JSONCompareMode.NON_EXTENSIBLE,
        config: Any? = null,
        section: AbstractSection? = null
    ): AssemblyResult {
        section?.preProcessSection()
        val assemblyResult = doConvert(fileMap, config, section)

        for (snippet in assemblyResult.content) {
            // to simplify tests
            snippet.internalType = null
        }

        val jsonToTest = TestAssemblyResult(assemblyResult)
        assertJson(jsonToTest, expected, "Result json", сompareMode = compareMode)

        return assemblyResult
    }

    fun AbstractAssembler<*>.testAssemblyErrors(
        fileMap: Map<String, String>,
        expected: List<String>,
        config: Any? = null
    ) {
        val result = doConvert(fileMap, config)
        val resultErrors = result.errors?.map { it.message }
        log.info("Result errors: $resultErrors")

        assertEquals(expected, resultErrors)
    }

    /**
     * Проверяет пост-обработку секции.
     */
    fun testSectionResult(
        section: AbstractSection,
        assembler: AbstractAssembler<*>? = null,
        resolver: ResourceResolver,
        resolverResponseMap: Map<String, String>? = null,
        expected: String,
        config: Any? = null,
        processSnippets: Boolean = false,
        withLoadMoreAction: Boolean = false
    ) {
        section.preProcessSection()
        val assembly = assembler?.doConvert(resolverResponseMap, config, section)
        val flatResult = section.flatten()

        val errors = assembly?.errors?.toMutableList() ?: ArrayList()
        val context = buildSectionContext(section, resolver, assembly, errors, withLoadMoreAction)
        section.postProcessSection(context)
        if (errors.isNotEmpty()) {
            section.errors = errors
        }
        if (context.hideSection) {
            section.content = emptyList()
        }

        if (processSnippets && assembler != null && assembly != null) {
            val content = assembly.content
            processSnippets(content, section, resolver, assembler, assembly)
            section.content = content
        }

        testSection(section, expected, flatResult, context)
    }

    private fun testSection(
        section: AbstractSection,
        expected: String,
        flatResult: List<AbstractSection>? = null,
        sectionContext: SectionProcessContext
    ) {
        val result = TestSectionWithInteractions(
            section,
            toDebug = section.ignoreSection.takeIf { it },
            toDivkit = sectionContext.toDivkitSection.takeIf { it },
            replacedSectionId = sectionContext.replacedSectionId,
            flattened = flatResult,
        )
        assertJson(result, expected, "Result json")
    }

    /**
     * Проверяет построенный ассемблером конент + действия и всю пост-обработку.
     */
    fun testContentResult(
        section: AbstractSection,
        assembler: AbstractAssembler<*>,
        resolver: ResourceResolver? = null,
        resolverResponseMap: Map<String, String>? = null,
        expected: String,
        config: Any? = null,
        filter: (Any) -> Boolean = { true },
    ) {
        section.preProcessSection()
        val assembly = assembler.doConvert(resolverResponseMap, config, section)

        var snippets = assembly.content

        processSnippets(snippets, section, resolver, assembler, assembly)

        snippets = snippets.filter(filter)
        testContent(snippets, expected)
    }

    private fun processSnippets(
        snippets: List<AbstractSnippet>,
        section: AbstractSection,
        resolver: ResourceResolver?,
        assembler: AbstractAssembler<*>,
        assembly: AssemblyResult?
    ) {
        snippets.forEachIndexed { index, snippet ->
            val context = buildSnippetContext(snippet, section, resolver, assembler, assembly)
            context.snippetEventParams.position = index

            assembly?.promises?.get(snippet)?.invoke(context)
            assembler.postProcessSnippetUnsafe(snippet, context)
        }
    }

    /**
     * Проверяет статический контент + действия и всю пост-обработку.
     */
    fun <S : AbstractSnippet> testStaticContentResult(
        section: AbstractSection,
        assembler: AbstractAssembler<S>,
        staticContentFile: String,
        expected: String
    ) {
        val contentTree = JsonHelper.parseTree(staticContentFile.asResource())
        assertTrue { contentTree.isArray }
        val contentArray = contentTree as ArrayNode

        val content = arrayListOf<AbstractSnippet>()
        contentArray.elements().forEachRemaining { staticNode ->
            val snippet = JsonHelper.parse(staticNode, assembler.getSnippetClass())
            if (assembler.fillStaticSnippet(snippet)) {
                content.add(snippet)
            }
        }

        processSnippets(content, section, resolver = null, assembler, assembly = null)

        testContent(content, expected)
    }

    private fun testContent(content: List<AbstractSnippet>, expected: String) {
        for (snippet in content) {
            // to simplify tests
            snippet.internalType = null
        }

        assertJson(TestSnippetsWithInteractions(content), expected, "Result json")
    }

    protected fun AbstractSection.addDefParams() {
        id = DEFAULT_SECTION_ID
        title = DEFAULT_SECTION_TITLE
        type = this::class.simpleName
    }

    protected fun buildAnyResolver(): ResourceResolver {
        return buildResolver("someResolver", mapOf("key" to "value", "param" to "target"))
    }

    protected fun buildResolver(
        name: String,
        data: Map<String, Any>,
        pagerParams: Map<String, Any>? = null
    ): ResourceResolver {
        return ResourceResolver.simple(name).apply {
            version = "v1"
            params = JsonHelper.toTree(data)
            pager = JsonHelper.toTree(pagerParams)
        }
    }

    private fun buildSectionContext(
        section: AbstractSection,
        resolver: ResourceResolver,
        assembly: AssemblyResult?,
        errors: MutableList<MapiErrorDto>,
        withLoadMoreAction: Boolean = false
    ): SectionProcessContext {
        return SectionProcessContext(
            AnalyticUtils.buildCommonSectionParams(123, section),
            content = assembly?.content ?: emptyList(),
            errors = errors
        ).apply {
            resolvers.add(resolver)
            resolverEventParams.resolvers.add(
                AnalyticUtils.buildResolverParams(
                    resolver,
                    assembly?.resolverEventParams
                )
            )
            assembly?.sectionEventParams?.let { x -> assembledSectionParams.addAll(x) }

            if (withLoadMoreAction) {
                loadMoreAction = MergeSectionAction(
                    sectionId = section.id,
                    params = mapOf("nextPageTokenTest" to 2)
                )
            }
        }
    }

    private fun buildSnippetContext(
        snippet: AbstractSnippet,
        section: AbstractSection,
        resolver: ResourceResolver?,
        assembler: AbstractAssembler<*>,
        assembly: AssemblyResult? = null
    ): SnippetProcessContext {
        val sectionEventParams = AnalyticUtils.buildCommonSectionParams(CMS_PAGE_ID, section)
        val context = SnippetProcessContext(
            snippet, section, assembler, sectionEventParams,
            assembly?.promises?.get(snippet)
        )

        if (resolver != null) {
            val resolverParams = AnalyticUtils.buildResolverParams(resolver, assembly?.resolverEventParams)
            context.snippetEventParams.resolverEventParams = listOf(resolverParams)
            context.snippetEventParams.resolvers = listOf(resolver)
        }

        return context
    }

    companion object {
        const val CMS_PAGE_ID: Long = 123
        const val DEFAULT_SECTION_ID = "987"
        const val DEFAULT_SECTION_TITLE = "some title"
    }
}

