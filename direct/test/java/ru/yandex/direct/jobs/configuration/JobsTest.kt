package ru.yandex.direct.jobs.configuration

import org.springframework.test.context.ContextConfiguration

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ContextConfiguration(classes = [JobsTestingSpyConfiguration::class])
annotation class JobsTest()
