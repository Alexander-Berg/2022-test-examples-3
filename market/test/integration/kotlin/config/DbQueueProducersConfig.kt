package ru.yandex.market.logistics.calendaring.config

import org.mockito.Mockito
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.test.mock.mockito.MockReset.after
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.market.logistics.calendaring.dbqueue.producer.ProcessMetaInfoChangeEventProducer
import ru.yandex.market.logistics.calendaring.dbqueue.producer.ReleaseQuotaProducer

@Configuration
open class DbQueueProducersConfig {

    @ConditionalOnMissingBean(ReleaseQuotaProducer::class)
    @Bean
    open fun releaseQuotaProducer(): ReleaseQuotaProducer {
        return Mockito.mock(ReleaseQuotaProducer::class.java, after())
    }

    @ConditionalOnMissingBean(ProcessMetaInfoChangeEventProducer::class)
    @Bean
    open fun processMetaInfoChangeEventProducer(): ProcessMetaInfoChangeEventProducer {
        return Mockito.mock(ProcessMetaInfoChangeEventProducer::class.java)
    }
}
