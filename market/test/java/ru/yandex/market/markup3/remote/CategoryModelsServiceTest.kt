package ru.yandex.market.markup3.remote

import com.googlecode.protobuf.format.JsonFormat
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import ru.yandex.market.mbo.export.MboExport
import java.io.InputStreamReader

class CategoryModelsServiceTest {
    private lateinit var categoryModelsService: CategoryModelsService
    private lateinit var remoteCategoryModelsService: ru.yandex.market.mbo.export.CategoryModelsService
    private lateinit var modelStorageService: ModelStorageService

    companion object {
        fun filterCategoryModels(
            invocation: InvocationOnMock,
            exampleResponse: MboExport.GetCategoryModelsResponse.Builder
        ): MboExport.GetCategoryModelsResponse {
            val request = invocation.arguments[0] as MboExport.GetCategoryModelsRequest
            val modelIds = request.modelIdList.toSet()
            val models = exampleResponse.modelsList.filter { model -> modelIds.contains(model.id) }.toList()
            return exampleResponse.clearModels().addAllModels(models).build()
        }
    }

    @Before
    fun setUp() {
        val exampleResponse = MboExport.GetCategoryModelsResponse.newBuilder()
        val resource = javaClass.getResourceAsStream("/remote/exporter-getSkus.json") ?: error("test file not found")
        JsonFormat.merge(InputStreamReader(resource), exampleResponse)

        remoteCategoryModelsService = spy {
            doAnswer { invocation ->
                filterCategoryModels(invocation, exampleResponse)
            }.`when`(it).getSkus(Mockito.any())
            doAnswer { invocation ->
                filterCategoryModels(invocation, exampleResponse)
            }.`when`(it).getModels(Mockito.any())
        }
        modelStorageService = mock()
        categoryModelsService = CategoryModelsService(remoteCategoryModelsService, modelStorageService)
    }

    @Test
    fun `search in getSkus first`() {
        categoryModelsService.getSkusWithFallback(90578, listOf(100898502993, 441852073))
        Mockito.verify(remoteCategoryModelsService, Mockito.times(1)).getSkus(Mockito.any())
        Mockito.verify(remoteCategoryModelsService, Mockito.never()).getModels(Mockito.any())
    }

    @Test
    fun `use getModels fallback in CategoryModelsService`() {
        categoryModelsService.getSkusWithFallback(90578, listOf(1, 2))
        Mockito.verify(remoteCategoryModelsService, Mockito.times(1)).getSkus(Mockito.any())
        Mockito.verify(remoteCategoryModelsService, Mockito.times(1)).getModels(Mockito.any())
    }
}
