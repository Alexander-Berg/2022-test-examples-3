package ru.yandex.market.mbi.feed.processor.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.yandex.market.common.mds.s3.spring.configuration.MdsS3LocationConfiguration

@Configuration
@Import(
    MdsS3LocationConfiguration::class
)
open class MdsS3TestConfig
