package ru.yandex.direct.bstransport.yt.repository.resources

import org.assertj.core.api.Assertions
import org.mockito.Mockito
import ru.yandex.adv.direct.banner.resources.BannerResources

abstract class AbstractBannerResourcesYtRepositoryTest(
    private val repositoryClass : Class<out BaseBannerResourcesYtRepository>
) {
    fun compareProtoWithColumns(resource: BannerResources, expectedColumnNameToValue: Map<String, Any?>) {

        val bannerButtonYtRepository = Mockito.mock(repositoryClass)
        Mockito.`when`(bannerButtonYtRepository.schemaWithMapping).thenCallRealMethod()
        Mockito.`when`(bannerButtonYtRepository.resourceColumnSchema).thenCallRealMethod()
        val columnWithMappers: List<BannerResourcesColumnMapping<out Any>> = bannerButtonYtRepository.schemaWithMapping
        val gotColumnNameToValue = columnWithMappers
            .map { it.columnSchema.name to it.fromProtoToYtMapper(resource) }
            .toMap()

        val columnNameToReverseMapping: Map<String, (Any?, BannerResources.Builder) -> Unit> = columnWithMappers
            .map { it.columnSchema.name to it.fromYtToProtoMapper as (Any?, BannerResources.Builder) -> Unit }
            .toMap()

        val builder = BannerResources.newBuilder()
        expectedColumnNameToValue.forEach {
            columnNameToReverseMapping[it.key]?.invoke(it.value, builder)
        }
        val gotBannerResources = builder.build()
        Assertions.assertThat(gotColumnNameToValue).isEqualTo(expectedColumnNameToValue)
        Assertions.assertThat(gotBannerResources).isEqualTo(resource)
    }
}
