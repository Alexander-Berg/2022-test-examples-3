package ru.yandex.travel.hotels.extranet.extract

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles

@Configuration
@EnableAutoConfiguration
@EnableBatchProcessing
@ComponentScan("ru.yandex.travel.hotels.extranet")
@ActiveProfiles("test")
open class TestBatchConfiguration
