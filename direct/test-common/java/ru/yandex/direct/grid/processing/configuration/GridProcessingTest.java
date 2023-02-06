package ru.yandex.direct.grid.processing.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;

import ru.yandex.direct.core.testing.listener.LogTestInitTimeListener;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ContextConfiguration(classes = GridProcessingTestingConfiguration.class)
@TestExecutionListeners(value = {LogTestInitTimeListener.class}, mergeMode = MERGE_WITH_DEFAULTS)
public @interface GridProcessingTest {
}
