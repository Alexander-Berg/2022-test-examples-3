package ru.yandex.direct.chassis.configuration

import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ContextConfiguration(classes = [ChassisTestConfiguration::class])
@WebAppConfiguration
annotation class ChassisTest
