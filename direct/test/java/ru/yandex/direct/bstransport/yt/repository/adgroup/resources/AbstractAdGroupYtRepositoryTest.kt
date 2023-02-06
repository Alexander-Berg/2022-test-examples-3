package ru.yandex.direct.bstransport.yt.repository.adgroup.resources

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.mockito.Mockito.mock
import ru.yandex.adv.direct.adgroup.AdGroup
import kotlin.reflect.KClass

abstract class AbstractAdGroupYtRepositoryTest(
    private val repositoryClass: KClass<out BaseAdGroupYtRepository>
) {
    fun compareProtoWithColumns(resource: AdGroup, expectedColumnNameToValue: Map<String, Any?>) {

        val ytRepository = mock(repositoryClass.java)
        whenever(ytRepository.schemaWithMapping).thenCallRealMethod()
        whenever(ytRepository.getResourceColumnSchema()).thenCallRealMethod()
        val columnWithMappers: List<AdGroupResourcesColumnMapping<out Any>> = ytRepository.schemaWithMapping
        val gotColumnNameToValue = columnWithMappers
            .map { it.columnSchema.name to it.fromProtoToYtMapper(resource) }
            .toMap()

        @Suppress("UNCHECKED_CAST")
        val columnNameToReverseMapping: Map<String, (Any?, AdGroup.Builder) -> Unit> = columnWithMappers
            .map { it.columnSchema.name to it.fromYtToProtoMapper as (Any?, AdGroup.Builder) -> Unit }
            .toMap()

        val builder = AdGroup.newBuilder()
        expectedColumnNameToValue.forEach {
            columnNameToReverseMapping[it.key]?.invoke(it.value, builder)
        }
        val gotAdGroups = builder.build()
        Assertions.assertThat(gotColumnNameToValue).isEqualTo(expectedColumnNameToValue)
        Assertions.assertThat(gotAdGroups).isEqualTo(resource)
    }
}
