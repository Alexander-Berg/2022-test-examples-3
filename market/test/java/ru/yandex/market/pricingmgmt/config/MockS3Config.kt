package ru.yandex.market.pricingmgmt.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.market.pricingmgmt.util.s3.MockS3ClientFactory
import ru.yandex.market.pricingmgmt.util.s3.S3ClientFactory

@Configuration
@Profile("unittest")
open class MockS3Config {
    companion object {
        const val bucketName: String = "testBucket"
    }

    @Bean
    open fun s3ClientFactory(): S3ClientFactory = MockS3ClientFactory()

    @Bean
    open fun s3BucketName(): String = bucketName
}
