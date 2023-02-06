package ru.yandex.market.wms.consolidation.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import ru.yandex.market.logistics.util.client.TvmTicketProvider
import ru.yandex.market.wms.common.spring.tvm.TvmTicketProviderStub
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles

@Order(-1)
@Configuration
class ConsolidationClientTestConfig {

    @Bean
    @Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST, ru.yandex.market.wms.shared.libs.env.conifg.Profiles.DEVELOPMENT)
    @Qualifier("consolidationTvmTicketProvider")
    fun consolidationTvmTicketProviderMock(): TvmTicketProvider {
        return TvmTicketProviderStub()
    }


}
