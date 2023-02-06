package ru.yandex.direct.grid.processing.configuration

import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
import ru.yandex.direct.core.testing.listener.LogTestInitTimeListener

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ContextConfiguration(classes = [GrutGridProcessingTestingConfiguration::class])
@TestExecutionListeners(value = [LogTestInitTimeListener::class], mergeMode = MERGE_WITH_DEFAULTS)
annotation class GrutGridProcessingTest 
