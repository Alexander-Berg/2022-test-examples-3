package ru.yandex.market.contentmapping.config

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.market.ir.http.FormalizerService
import ru.yandex.market.ir.http.UltraControllerService
import ru.yandex.market.mbo.export.CategoryParametersService
import ru.yandex.market.mbo.http.MdmGoodsGroupService
import ru.yandex.market.mbo.tree.TovarTreeService
import ru.yandex.market.mboc.http.MboCategoryService
import ru.yandex.market.mboc.http.MboMappingsService

/**
 * @author yuramalinov
 * @created 24.02.2020
 */
@Profile("test")
@Configuration
open class ProtoServicesMockConfig : ProtoServicesConfig {
    @Bean
    override fun categoryParametersService(): CategoryParametersService {
        return Mockito.mock(CategoryParametersService::class.java)
    }

    @Bean
    override fun tovarTreeService(): TovarTreeService {
        return Mockito.mock(TovarTreeService::class.java)
    }

    @Bean
    override fun ultraControllerService(): UltraControllerService {
        return Mockito.mock(UltraControllerService::class.java)
    }

    @Bean
    override fun formalizerService(): FormalizerService {
        return Mockito.mock(FormalizerService::class.java)
    }

    @Bean
    override fun mboMappingsService(): MboMappingsService {
        return Mockito.mock(MboMappingsService::class.java)
    }

    @Bean
    override fun mboCategoryService(): MboCategoryService {
        return Mockito.mock(MboCategoryService::class.java)
    }

    @Bean
    override fun mdmGoodsGroupService(): MdmGoodsGroupService = Mockito.mock(MdmGoodsGroupService::class.java)
}
