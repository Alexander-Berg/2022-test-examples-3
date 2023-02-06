package ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration
import java.util.stream.Collectors

@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@ExtendWith(SpringExtension::class)
internal class BannerResourcesHandlerProviderTest {
    @Autowired
    private lateinit var bannerResourcesHandlerProvider: BannerResourcesHandlerProvider

    @Autowired
    private lateinit var handlerList: List<IBannerResourcesHandler>

    @Test
    fun test() {
        val bannerResourceTypes = BannerResourceType.values()
            .filter { resourceType: BannerResourceType -> BannerResourceType.ALL != resourceType }
            .filter { resourceType: BannerResourceType -> BannerResourceType.UNKNOWN != resourceType }

        val handlerClasses = bannerResourceTypes
            .flatMap { bannerResourceType: BannerResourceType? -> bannerResourcesHandlerProvider[bannerResourceType] }
            .map { obj: IBannerResourcesHandler -> obj.javaClass }
            .distinct()

        assertThat(handlerClasses).hasSameSizeAs(bannerResourceTypes)
    }

    @Test
    fun unknownBannerResourceTypeTest() {
        val gotHandlers = bannerResourcesHandlerProvider[BannerResourceType.UNKNOWN]
        assertThat(gotHandlers).isEmpty()
    }

    @Test
    fun allTypesTest() {
        val gotClasses = bannerResourcesHandlerProvider[BannerResourceType.ALL]
            .stream()
            .map { obj: IBannerResourcesHandler -> obj.javaClass }
            .collect(Collectors.toList())
        val expectedClasses = handlerList
            .map { obj: IBannerResourcesHandler -> obj.javaClass }

        assertThat(gotClasses).containsExactlyInAnyOrder(*expectedClasses.toTypedArray())

    }

    @Test
    fun nullBannerResourceTypeTest() {
        val gotHandlers = bannerResourcesHandlerProvider[null]
        assertThat(gotHandlers).isEmpty()
    }
}
